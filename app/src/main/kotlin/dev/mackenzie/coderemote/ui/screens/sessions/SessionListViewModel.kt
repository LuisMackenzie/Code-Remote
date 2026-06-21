package dev.mackenzie.coderemote.ui.screens.sessions

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import dev.mackenzie.coderemote.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mackenzie.coderemote.data.api.FileNode
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.data.repository.EventReducer
import dev.mackenzie.coderemote.domain.model.Project
import dev.mackenzie.coderemote.domain.model.Session
import dev.mackenzie.coderemote.domain.model.SessionStatus
import dev.mackenzie.coderemote.usecases.file.FindFilesUseCase
import dev.mackenzie.coderemote.usecases.file.ListDirectoryUseCase
import dev.mackenzie.coderemote.usecases.project.GetServerPathsUseCase
import dev.mackenzie.coderemote.usecases.project.ListProjectsUseCase
import dev.mackenzie.coderemote.usecases.session.CreateSessionUseCase
import dev.mackenzie.coderemote.usecases.session.DeleteSessionUseCase
import dev.mackenzie.coderemote.usecases.session.ExecuteCommandUseCase
import dev.mackenzie.coderemote.usecases.session.ListSessionsUseCase
import dev.mackenzie.coderemote.usecases.session.RunShellCommandUseCase
import dev.mackenzie.coderemote.usecases.session.UpdateSessionUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SessionListViewModel"

data class SessionListUiState(
    val sessionGroups: List<ProjectSessionGroup> = emptyList(),
    val projects: List<Project> = emptyList(),
    val serverName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
)

/** A group of sessions belonging to a project. */
data class ProjectSessionGroup(
    val projectId: String,
    val projectName: String,
    val directory: String,
    val sessions: List<SessionItem>,
    /** Per-session tilde-path labels (sessionId -> tildePath) for flat display. */
    val sessionDirLabels: Map<String, String> = emptyMap()
)

/** Helper for session directory info. */
private data class SessionDirInfo(val name: String, val tildePath: String)

data class SessionItem(
    val session: Session,
    val status: SessionStatus = SessionStatus.Idle
)

/**
 * Prunes [selectedIds] so it never contains IDs outside [visibleIds].
 *
 * This enforces the safety rule that no hidden session can remain selected —
 * and therefore be deleted via Delete Selected — after a project filter
 * narrows the visible set. Pure on purpose so the rule can be exercised by a
 * focused JVM test without a ViewModel or instrumented UI.
 */
internal fun pruneSelectionToVisible(
    selectedIds: Set<String>,
    visibleIds: Set<String>,
): Set<String> = selectedIds.intersect(visibleIds)

@HiltViewModel
class SessionListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventReducer: EventReducer,
    private val listProjects: ListProjectsUseCase,
    private val listSessions: ListSessionsUseCase,
    private val createSession: CreateSessionUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val updateSession: UpdateSessionUseCase,
    private val executeCommand: ExecuteCommandUseCase,
    private val runShellCommand: RunShellCommandUseCase,
    private val getServerPaths: GetServerPathsUseCase,
    private val listDirectory: ListDirectoryUseCase,
    private val findFiles: FindFilesUseCase,
) : ViewModel() {

    val serverUrl: String = savedStateHandle.get<String>("serverUrl") ?: ""
    private val username: String = savedStateHandle.get<String>("username") ?: ""
    private val password: String = savedStateHandle.get<String>("password") ?: ""
    val serverName: String = savedStateHandle.get<String>("serverName") ?: ""
    val serverId: String = savedStateHandle.get<String>("serverId") ?: ""

    private val conn = ServerConnection.from(serverUrl, username, password.ifEmpty { null })

    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    private val _homeDir = MutableStateFlow<String?>(null)
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _navigateToSession = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToSession: SharedFlow<String> = _navigateToSession.asSharedFlow()

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SessionListUiState> = combine(
        listOf(
            eventReducer.sessions,
            eventReducer.sessionStatuses,
            eventReducer.serverSessions,
            _isLoading,
            _error,
            _projects,
            _homeDir,
            _selectedIds,
        )
    ) { values ->
        val allSessions = values[0] as List<Session>
        val statuses = values[1] as Map<String, SessionStatus>
        val serverSessions = values[2] as Map<String, Set<String>>
        val loading = values[3] as Boolean
        val error = values[4] as String?
        val projects = values[5] as List<Project>
        val homeDir = values[6] as String?
        val selectedIds = values[7] as Set<String>

        // Filter sessions belonging to this server
        val serverSessionIds = serverSessions[serverId] ?: emptySet()
        val sessions = allSessions
            .filter { it.id in serverSessionIds && !it.isArchived && it.parentId == null }
            .sortedByDescending { it.time.updated }
            .map { session ->
                SessionItem(
                    session = session,
                    status = statuses[session.id] ?: SessionStatus.Idle
                )
            }

        // Build a flat list sorted by time — each session carries its own
        // tilde-path label derived from session.directory
        val allItems = sessions.map { item ->
            val dir = item.session.directory.trimEnd('/').ifEmpty { "/" }
            val tildePath = if (homeDir != null && dir.startsWith(homeDir)) {
                "~" + dir.removePrefix(homeDir)
            } else {
                dir
            }
            val dirName = dir.substringAfterLast('/').ifEmpty { "/" }
            item to SessionDirInfo(dirName, tildePath)
        }

        // Single group containing all sessions (flat, sorted by time)
        val groups = listOf(
            ProjectSessionGroup(
                projectId = "",
                projectName = "",
                directory = "",
                sessions = allItems.map { it.first },
                sessionDirLabels = allItems.associate { it.first.session.id to it.second.tildePath }
            )
        )

        val visibleSessionIds = allItems.map { it.first.session.id }.toSet()
        val validSelectedIds = selectedIds.intersect(visibleSessionIds)
        if (validSelectedIds != selectedIds) {
            _selectedIds.value = validSelectedIds
        }

        SessionListUiState(
            sessionGroups = groups,
            projects = projects,
            serverName = serverName,
            isLoading = loading,
            error = error,
            selectedIds = validSelectedIds,
            isSelectionMode = validSelectedIds.isNotEmpty(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SessionListUiState(serverName = serverName)
    )

    init {
        loadHomeDir()
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // Load all projects first
                val projects = listProjects(conn)
                _projects.value = projects
                if (BuildConfig.DEBUG) Log.d(TAG, "Loaded ${projects.size} projects for multi-project session fetch")

                if (projects.isEmpty()) {
                    // Fallback: load sessions without directory header (server CWD only)
                    val sessions = listSessions(conn)
                    eventReducer.setSessions(serverId, sessions)
                    if (BuildConfig.DEBUG) Log.d(TAG, "Loaded ${sessions.size} sessions (no projects)")
                } else {
                    // Load sessions for each project using its worktree as directory
                    var totalSessions = 0
                    for (project in projects) {
                        try {
                            val sessions = listSessions(conn, directory = project.worktree)
                            eventReducer.setSessions(serverId, sessions)
                            totalSessions += sessions.size
                            if (BuildConfig.DEBUG) Log.d(TAG, "Loaded ${sessions.size} sessions for project ${project.displayName}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to load sessions for project ${project.displayName}: ${e.message}")
                        }
                    }
                    if (BuildConfig.DEBUG) Log.d(TAG, "Total: loaded $totalSessions sessions across ${projects.size} projects for server $serverId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load sessions", e)
                _error.value = e.message ?: "Failed to load sessions"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadProjects() {
        viewModelScope.launch {
            try {
                val projects = listProjects(conn)
                _projects.value = projects
                if (BuildConfig.DEBUG) Log.d(TAG, "Loaded ${projects.size} projects")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load projects", e)
            }
        }
    }

    private fun loadHomeDir() {
        viewModelScope.launch {
            getHomeDirectory()
        }
    }

    fun createNewSession(directory: String? = null) {
        viewModelScope.launch {
            try {
                val session = createSession(conn, directory = directory)
                // The SSE stream should pick up the new session, but also add directly
                eventReducer.setSessions(serverId, listOf(session))
                if (BuildConfig.DEBUG) Log.d(TAG, "Created new session: ${session.id}")
                _navigateToSession.tryEmit(session.id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create session", e)
                _error.value = e.message ?: "Failed to create session"
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val success = deleteSession(conn, sessionId)
                if (success) {
                    if (BuildConfig.DEBUG) Log.d(TAG, "Deleted session $sessionId")
                    loadSessions()
                } else {
                    _error.value = "Failed to delete session"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete session", e)
                _error.value = e.message ?: "Failed to delete session"
            }
        }
    }

    fun toggleSelection(sessionId: String) {
        _selectedIds.update { selected ->
            if (sessionId in selected) selected - sessionId else selected + sessionId
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    /**
     * Constrains the current selection to [visibleIds], dropping any selected
     * session ID that is no longer visible.
     *
     * The screen calls this whenever the filter-respecting visible set changes
     * (e.g. a project filter is applied while selection mode is active) so that
     * Delete Selected can never act on sessions the user cannot see. With no
     * filter active [visibleIds] is the full session set, so this is a no-op
     * and the pre-filter selection behavior is preserved.
     */
    fun retainSelection(visibleIds: Set<String>) {
        val current = _selectedIds.value
        val pruned = pruneSelectionToVisible(current, visibleIds)
        if (pruned != current) {
            _selectedIds.value = pruned
        }
    }

    /**
     * Replaces the current selection with exactly [sessionIds].
     *
     * The screen passes the IDs derived from the filter-respecting
     * `displayedSessionGroups` so Select All under an active project filter
     * selects only the sessions the user can see, never hidden sessions from
     * other directories that could then be deleted via Delete Selected.
     */
    fun selectAll(sessionIds: Set<String>) {
        _selectedIds.value = sessionIds
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _selectedIds.value
            if (ids.isEmpty()) return@launch
            try {
                val results = coroutineScope {
                    ids.map { id ->
                        async {
                            id to deleteSession(conn, id)
                        }
                    }.awaitAll()
                }
                val failed = results.filterNot { it.second }
                if (failed.isNotEmpty()) {
                    _error.value = "Failed to delete ${failed.size} session(s)"
                }
                clearSelection()
                loadSessions()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete selected sessions", e)
                _error.value = e.message ?: "Failed to delete selected sessions"
            }
        }
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            try {
                updateSession(conn, sessionId, newTitle)
                if (BuildConfig.DEBUG) Log.d(TAG, "Renamed session $sessionId to '$newTitle'")
                loadSessions()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rename session", e)
                _error.value = e.message ?: "Failed to rename session"
            }
        }
    }

    // ============ Directory browsing for Open Project ============

    /** Get the server's home directory (cached). */
    suspend fun getHomeDirectory(): String {
        _homeDir.value?.let { return it }
        return try {
            val paths = getServerPaths(conn)
            val home = paths.home
            _homeDir.value = home
            if (BuildConfig.DEBUG) Log.d(TAG, "Server home directory: $home")
            home
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get server paths", e)
            "/"
        }
    }

    /** List directories in a given path on the server. */
    suspend fun listDirectories(directory: String): List<FileNode> {
        return try {
            val nodes = listDirectory(conn, path = "", directory = directory)
            nodes.filter { it.type == "directory" }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list directory: $directory", e)
            emptyList()
        }
    }

    /** Search for directories matching a query, scoped to a base directory. */
    suspend fun searchDirectories(query: String, directory: String): List<String> {
        return try {
            findFiles(conn, query = query, type = "directory", directory = directory, limit = 50)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search directories", e)
            emptyList()
        }
    }

    /** Create a directory inside the currently browsed path. */
    suspend fun createDirectory(parentDirectory: String, folderName: String): Result<String> {
        val sanitized = folderName.trim().trim('/').replace(Regex("/+"), "/")
        if (sanitized.isBlank() || sanitized == "." || sanitized == "..") {
            return Result.failure(IllegalArgumentException("Invalid folder name"))
        }

        return runCatching {
            val targetDirectory = if (parentDirectory == "/") {
                "/$sanitized"
            } else {
                "${parentDirectory.trimEnd('/')}/$sanitized"
            }

            val tempSession = createSession(
                conn = conn,
                title = "mkdir",
                directory = parentDirectory,
            )

            try {
                val escaped = sanitized.replace("'", "'\"'\"'")
                val command = "mkdir -p -- '$escaped'"

                val runShellOk = runCatching {
                    runShellCommand(
                        conn = conn,
                        sessionId = tempSession.id,
                        command = command,
                        agent = "build",
                        directory = parentDirectory,
                    )
                }.getOrElse { false }

                if (!runShellOk) {
                    val executeOk = executeCommand(
                        conn = conn,
                        sessionId = tempSession.id,
                        command = "bash",
                        arguments = "-lc \"$command\"",
                        directory = parentDirectory,
                    )
                    if (!executeOk) {
                        throw IllegalStateException("Failed to create directory")
                    }
                }
            } finally {
                runCatching { deleteSession(conn, tempSession.id) }
            }

            repeat(6) {
                if (directoryExists(targetDirectory)) {
                    return@runCatching targetDirectory
                }
                delay(200)
            }

            throw IllegalStateException("Directory was not created")
        }
    }

    private suspend fun directoryExists(directory: String): Boolean {
        return try {
            listDirectory(conn, path = "", directory = directory)
            true
        } catch (_: Exception) {
            false
        }
    }
}
