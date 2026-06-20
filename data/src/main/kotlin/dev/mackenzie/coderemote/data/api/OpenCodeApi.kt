package dev.mackenzie.coderemote.data.api

import dev.mackenzie.coderemote.domain.model.FileDiff
import dev.mackenzie.coderemote.domain.model.MessageWithParts
import dev.mackenzie.coderemote.domain.model.Project
import dev.mackenzie.coderemote.domain.model.ServerHealth
import dev.mackenzie.coderemote.domain.model.Session
import java.io.OutputStream

/**
 * OpenCode REST API contract.
 *
 * All methods take a [ServerConnection] so the client is stateless
 * and safe to use for multiple servers concurrently.
 * The Ktor-backed implementation lives in :app.
 */
interface OpenCodeApi {

    // ============ Global ============

    suspend fun getHealth(conn: ServerConnection): ServerHealth

    suspend fun getServerPaths(conn: ServerConnection): ServerPaths

    // ============ Project ============

    suspend fun listProjects(conn: ServerConnection): List<Project>

    suspend fun getCurrentProject(conn: ServerConnection): Project

    // ============ Agents ============

    suspend fun listAgents(conn: ServerConnection): List<AgentInfo>

    // ============ Session ============

    suspend fun listSessions(conn: ServerConnection, directory: String? = null): List<Session>

    suspend fun getSession(conn: ServerConnection, sessionId: String): Session

    suspend fun getSessionRaw(conn: ServerConnection, sessionId: String): String

    suspend fun createSession(
        conn: ServerConnection,
        title: String? = null,
        parentId: String? = null,
        directory: String? = null
    ): Session

    suspend fun deleteSession(conn: ServerConnection, sessionId: String): Boolean

    suspend fun updateSession(conn: ServerConnection, sessionId: String, title: String): Session

    suspend fun abortSession(conn: ServerConnection, sessionId: String, directory: String? = null): Boolean

    suspend fun getSessionDiff(conn: ServerConnection, sessionId: String): List<FileDiff>

    suspend fun shareSession(conn: ServerConnection, sessionId: String): Session

    suspend fun unshareSession(conn: ServerConnection, sessionId: String): Session

    suspend fun summarizeSession(
        conn: ServerConnection,
        sessionId: String,
        providerId: String,
        modelId: String
    ): Boolean

    suspend fun revertSession(conn: ServerConnection, sessionId: String, messageId: String): Session

    suspend fun unrevertSession(conn: ServerConnection, sessionId: String): Session

    suspend fun forkSession(conn: ServerConnection, sessionId: String, messageId: String? = null): Session

    suspend fun executeCommand(
        conn: ServerConnection,
        sessionId: String,
        command: String,
        arguments: String = "",
        directory: String? = null
    ): Boolean

    suspend fun runShellCommand(
        conn: ServerConnection,
        sessionId: String,
        command: String,
        agent: String,
        model: ModelSelection? = null,
        directory: String? = null
    ): Boolean

    // ============ PTY ============

    suspend fun createPty(
        conn: ServerConnection,
        title: String? = null,
        cwd: String? = null,
        directory: String? = null
    ): PtyInfo

    suspend fun removePty(conn: ServerConnection, ptyId: String): Boolean

    suspend fun updatePtySize(
        conn: ServerConnection,
        ptyId: String,
        cols: Int,
        rows: Int,
        directory: String? = null
    ): Boolean

    suspend fun openPtySocket(
        conn: ServerConnection,
        ptyId: String,
        cursor: Int = -1,
        directory: String? = null
    ): PtySocket

    // ============ Messages ============

    suspend fun listMessages(conn: ServerConnection, sessionId: String, limit: Int? = null): List<MessageWithParts>

    suspend fun listMessagesRaw(conn: ServerConnection, sessionId: String): String

    suspend fun exportSessionToStream(
        conn: ServerConnection,
        sessionId: String,
        outputStream: OutputStream,
        onProgress: (Long) -> Unit = {}
    )

    suspend fun getMessage(conn: ServerConnection, sessionId: String, messageId: String): MessageWithParts

    suspend fun promptAsync(
        conn: ServerConnection,
        sessionId: String,
        parts: List<PromptPart>,
        model: ModelSelection? = null,
        agent: String? = null,
        variant: String? = null,
        directory: String? = null
    )

    // ============ Permissions ============

    suspend fun replyToPermission(
        conn: ServerConnection,
        requestId: String,
        reply: String,
        message: String? = null,
        directory: String? = null
    ): Boolean

    suspend fun listPendingPermissions(conn: ServerConnection, directory: String? = null): List<PermissionRequest>

    // ============ Questions ============

    suspend fun replyToQuestion(
        conn: ServerConnection,
        requestId: String,
        answers: List<List<String>>,
        directory: String? = null
    ): Boolean

    suspend fun rejectQuestion(
        conn: ServerConnection,
        requestId: String,
        directory: String? = null
    ): Boolean

    suspend fun listPendingQuestions(conn: ServerConnection, directory: String? = null): List<QuestionRequest>

    // ============ Config / Providers ============

    suspend fun getProviders(conn: ServerConnection): ProvidersResponse

    suspend fun listProviderCatalog(conn: ServerConnection): ProviderCatalogResponse

    suspend fun getProviderAuthMethods(conn: ServerConnection): Map<String, List<ProviderAuthMethod>>

    suspend fun authorizeProviderOauth(
        conn: ServerConnection,
        providerId: String,
        methodIndex: Int
    ): ProviderOauthAuthorization?

    suspend fun completeProviderOauth(
        conn: ServerConnection,
        providerId: String,
        methodIndex: Int,
        code: String? = null
    ): Boolean

    suspend fun setProviderApiKey(conn: ServerConnection, providerId: String, apiKey: String): Boolean

    suspend fun removeProviderAuth(conn: ServerConnection, providerId: String): Boolean

    suspend fun getConfig(conn: ServerConnection): ServerConfigResponse

    suspend fun getGlobalConfig(conn: ServerConnection): ServerConfigResponse

    suspend fun updateConfig(conn: ServerConnection, patch: ServerConfigPatch): ServerConfigResponse

    suspend fun updateGlobalConfig(conn: ServerConnection, patch: ServerConfigPatch): ServerConfigResponse

    suspend fun disposeGlobal(conn: ServerConnection): Boolean

    // ============ Commands ============

    suspend fun listCommands(conn: ServerConnection): List<CommandInfo>

    // ============ Files ============

    suspend fun searchText(conn: ServerConnection, pattern: String): List<SearchMatch>

    suspend fun findFiles(
        conn: ServerConnection,
        query: String,
        type: String? = null,
        directory: String? = null,
        limit: Int? = null,
        dirs: String? = null
    ): List<String>

    suspend fun readFile(conn: ServerConnection, path: String): FileContent

    suspend fun listDirectory(conn: ServerConnection, path: String = "", directory: String? = null): List<FileNode>
}
