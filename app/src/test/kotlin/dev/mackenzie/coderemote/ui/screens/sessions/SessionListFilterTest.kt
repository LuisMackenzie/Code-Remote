package dev.mackenzie.coderemote.ui.screens.sessions

import dev.mackenzie.coderemote.domain.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior-focused tests for the project-title filter on the session list.
 *
 * The screen hoists the filter into [selectedProjectDirectory] and drives it
 * through three pure helpers: [toggleProjectDirectoryFilter] (tap behavior),
 * [filterSessionGroupsByDirectory] (visible sessions), and
 * [hasSessionWithDirectory] (stale-filter detection used by the
 * clear-on-disappear effect).
 *
 * [projectTitleFromDirectory] covers the clickable project-title label shown
 * by `SessionRow`; it must agree with the normalization used by the filter
 * helpers so a trailing-slash directory still shows a clickable leaf.
 *
 * [sessionListToolbarTitle] drives the toolbar title: the selected project
 * name while a filter is active, otherwise the server name/default title.
 */
class SessionListFilterTest {

    private fun sessionItem(id: String, directory: String): SessionItem =
        SessionItem(
            session = Session(
                id = id,
                directory = directory,
                time = Session.Time(created = 0L, updated = 0L)
            )
        )

    private fun group(sessions: List<SessionItem>): ProjectSessionGroup =
        ProjectSessionGroup(
            projectId = "p1",
            projectName = "Project",
            directory = sessions.firstOrNull()?.session?.directory.orEmpty(),
            sessions = sessions
        )

    private fun ids(groups: List<ProjectSessionGroup>): List<String> =
        groups.flatMap { it.sessions.map { item -> item.session.id } }

    @Test
    fun `selecting a working directory filters to matching sessions and selecting again clears the filter`() {
        val groups = listOf(
            group(
                listOf(
                    sessionItem("a", "/home/user/code-remote"),
                    sessionItem("b", "/home/user/other-project"),
                    sessionItem("c", "/home/user/code-remote/"), // trailing slash normalizes to same dir
                )
            )
        )
        val workingDirectory = "/home/user/code-remote"

        // First tap on the project title: select the working directory.
        val selected = toggleProjectDirectoryFilter(
            current = null,
            workingDirectory = workingDirectory
        )
        assertEquals(
            listOf("a", "c"),
            ids(filterSessionGroupsByDirectory(groups, selected))
        )

        // Second tap on the same project title: clear the filter.
        val cleared = toggleProjectDirectoryFilter(
            current = selected,
            workingDirectory = workingDirectory
        )
        assertEquals(
            listOf("a", "b", "c"),
            ids(filterSessionGroupsByDirectory(groups, cleared))
        )
    }

    @Test
    fun `hasSessionWithDirectory detects when a selected directory disappears after reload`() {
        // After a delete/reload the group no longer contains any session for
        // the previously selected directory. The clear-on-disappear effect
        // relies on this to reset selectedProjectDirectory so the user is not
        // stranded on a generic empty screen.
        val reloaded = listOf(
            group(listOf(sessionItem("b", "/home/user/other-project")))
        )

        assertFalse(hasSessionWithDirectory(reloaded, "/home/user/code-remote"))
        assertTrue(hasSessionWithDirectory(reloaded, "/home/user/other-project"))
    }

    @Test
    fun `projectTitleFromDirectory returns the leaf for a normal path`() {
        assertEquals("code-remote", projectTitleFromDirectory("/home/user/code-remote"))
    }

    @Test
    fun `projectTitleFromDirectory returns the leaf for a trailing-slash directory`() {
        // Regression: previously `substringAfterLast("/")` yielded "" for a
        // trailing slash, so SessionRow hid the clickable project title.
        assertEquals("code-remote", projectTitleFromDirectory("/home/user/code-remote/"))
    }

    @Test
    fun `projectTitleFromDirectory returns the whole string when there is no slash`() {
        assertEquals("code-remote", projectTitleFromDirectory("code-remote"))
        assertEquals("code-remote", projectTitleFromDirectory("code-remote/"))
    }

    @Test
    fun `projectTitleFromDirectory returns null for root or blank directories so no title is rendered`() {
        assertNull(projectTitleFromDirectory("/"))
        assertNull(projectTitleFromDirectory(""))
        assertNull(projectTitleFromDirectory("///"))
    }

    @Test
    fun `project title leaf agrees with the filter working directory for trailing slash sessions`() {
        // The clickable title shown by SessionRow is derived from the same
        // normalization the filter uses, so tapping it on a trailing-slash
        // session selects a working directory that actually matches that
        // session via filterSessionGroupsByDirectory.
        val groups = listOf(group(listOf(sessionItem("a", "/home/user/code-remote/"))))
        val workingDirectory = normalizedSessionDirectoryPublic("/home/user/code-remote/")

        val title = projectTitleFromDirectory("/home/user/code-remote/")
        assertEquals("code-remote", title)

        val selected = toggleProjectDirectoryFilter(current = null, workingDirectory = workingDirectory)
        assertEquals(listOf("a"), ids(filterSessionGroupsByDirectory(groups, selected)))
    }

    @Test
    fun `sessionListToolbarTitle returns server name when no project filter is active`() {
        assertEquals(
            "My Server",
            sessionListToolbarTitle(
                selectedProjectDirectory = null,
                serverName = "My Server",
                defaultTitle = "Sessions"
            )
        )
    }

    @Test
    fun `sessionListToolbarTitle returns default title when no filter and server name is empty`() {
        assertEquals(
            "Sessions",
            sessionListToolbarTitle(
                selectedProjectDirectory = null,
                serverName = "",
                defaultTitle = "Sessions"
            )
        )
    }

    @Test
    fun `sessionListToolbarTitle returns selected project leaf when filter is active`() {
        assertEquals(
            "code-remote",
            sessionListToolbarTitle(
                selectedProjectDirectory = "/home/user/code-remote",
                serverName = "My Server",
                defaultTitle = "Sessions"
            )
        )
    }

    @Test
    fun `sessionListToolbarTitle ignores server name while project filter is active`() {
        assertEquals(
            "other-project",
            sessionListToolbarTitle(
                selectedProjectDirectory = "/home/user/other-project",
                serverName = "My Server",
                defaultTitle = "Sessions"
            )
        )
    }

    @Test
    fun `sessionListToolbarTitle falls back to directory when no usable leaf exists`() {
        // Root directories have no leaf; showing the raw directory keeps the
        // title meaningful while the filter is active.
        assertEquals(
            "/",
            sessionListToolbarTitle(
                selectedProjectDirectory = "/",
                serverName = "My Server",
                defaultTitle = "Sessions"
            )
        )
    }

    @Test
    fun `visibleSessionIds returns every session ID when no project filter is active`() {
        // With no filter, the screen passes all session IDs to selectAll,
        // preserving the pre-filter Select All behavior.
        val groups = listOf(
            group(
                listOf(
                    sessionItem("a", "/home/user/code-remote"),
                    sessionItem("b", "/home/user/other-project"),
                    sessionItem("c", "/home/user/code-remote/"),
                )
            )
        )
        val ids = visibleSessionIds(filterSessionGroupsByDirectory(groups, null))
        assertEquals(setOf("a", "b", "c"), ids)
    }

    @Test
    fun `visibleSessionIds returns only filtered session IDs when a project filter is active`() {
        // Regression: Select All previously called viewModel.selectAll() with
        // no argument, which read the unfiltered sessionGroups and selected
        // every session — including hidden ones from other directories that
        // could then be deleted. With a filter active on code-remote, Select
        // All must only select the visible sessions a and c, never b.
        val groups = listOf(
            group(
                listOf(
                    sessionItem("a", "/home/user/code-remote"),
                    sessionItem("b", "/home/user/other-project"),
                    sessionItem("c", "/home/user/code-remote/"),
                )
            )
        )
        val ids = visibleSessionIds(
            filterSessionGroupsByDirectory(groups, "/home/user/code-remote")
        )
        assertEquals(setOf("a", "c"), ids)
    }

    @Test
    fun `visibleSessionIds is empty when the filter matches no sessions`() {
        // A filter with no matches yields no visible IDs, so Select All
        // selects nothing rather than sessions from other directories.
        val groups = listOf(
            group(listOf(sessionItem("a", "/home/user/code-remote")))
        )
        val ids = visibleSessionIds(
            filterSessionGroupsByDirectory(groups, "/home/user/missing-project")
        )
        assertTrue(ids.isEmpty())
    }

    /**
     * Thin wrapper so the test can reach the private [normalizedSessionDirectory]
     * through the same shape the production call site uses. Mirrors its body to
     * avoid widening its visibility solely for tests.
     */
    private fun normalizedSessionDirectoryPublic(directory: String): String =
        directory.trimEnd('/').ifEmpty { "/" }
}
