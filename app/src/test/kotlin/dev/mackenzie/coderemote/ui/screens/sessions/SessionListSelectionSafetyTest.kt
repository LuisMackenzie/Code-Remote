package dev.mackenzie.coderemote.ui.screens.sessions

import dev.mackenzie.coderemote.domain.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the destructive-selection safety rule on the session
 * list screen.
 *
 * Bug being locked down: a user could select sessions with no project filter
 * active, then tap a project title to apply a filter while still in selection
 * mode. The previously selected sessions outside the visible filter remained
 * in the ViewModel's `_selectedIds`, so Delete Selected would delete sessions
 * the user could no longer see.
 *
 * The fix prunes the selection to the currently visible IDs whenever the
 * filter-respecting visible set changes. The rule is implemented by the pure
 * helper [pruneSelectionToVisible] (delegated to by
 * [SessionListViewModel.retainSelection]) and driven by the screen from
 * `visibleSessionIds(displayedSessionGroups)`, so the whole path can be
 * exercised without a ViewModel or instrumented UI.
 */
class SessionListSelectionSafetyTest {

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

    @Test
    fun `pruneSelectionToVisible drops selected IDs that are outside the visible filter`() {
        // Models the bug scenario: the user selected all three sessions before
        // applying a filter. After the filter narrows the visible set to the
        // code-remote sessions, the hidden session b must be removed from the
        // selection so Delete Selected cannot reach it.
        val preFilterSelection = setOf("a", "b", "c")
        val visibleAfterFilter = setOf("a", "c")

        assertEquals(
            setOf("a", "c"),
            pruneSelectionToVisible(preFilterSelection, visibleAfterFilter)
        )
    }

    @Test
    fun `pruneSelectionToVisible is a no-op when every selected ID is visible with no filter`() {
        // With no filter active the visible set is the full session set, so
        // pruning must not alter the selection — preserving pre-filter
        // selection behavior.
        val selection = setOf("a", "b", "c")
        val allVisibleIds = setOf("a", "b", "c")

        assertEquals(selection, pruneSelectionToVisible(selection, allVisibleIds))
    }

    @Test
    fun `pruneSelectionToVisible clears the selection when no selected ID is visible`() {
        // Switching to a filter whose directory has none of the selected
        // sessions must drop the entire selection, exiting selection mode.
        val selection = setOf("a", "b")
        val visibleAfterFilter = setOf("c", "d")

        assertTrue(pruneSelectionToVisible(selection, visibleAfterFilter).isEmpty())
    }

    @Test
    fun `pruneSelectionToVisible preserves only the intersection when partially visible`() {
        val selection = setOf("a", "b", "c", "d")
        val visibleAfterFilter = setOf("b", "d", "e")

        assertEquals(
            setOf("b", "d"),
            pruneSelectionToVisible(selection, visibleAfterFilter)
        )
    }

    @Test
    fun `pruneSelectionToVisible on an empty selection stays empty`() {
        // Browsing with a filter while not in selection mode must not create
        // a selection out of nothing.
        assertTrue(
            pruneSelectionToVisible(emptySet(), setOf("a", "b")).isEmpty()
        )
    }

    @Test
    fun `end-to-end filter path prunes the pre-filter selection to visible filtered IDs`() {
        // Ties the filter helpers to the pruning helper to model the exact
        // path the screen drives: build the groups, apply a project filter,
        // derive the visible IDs, and prune a pre-filter selection so only
        // the visible filtered IDs survive — never the hidden session b.
        val groups = listOf(
            group(
                listOf(
                    sessionItem("a", "/home/user/code-remote"),
                    sessionItem("b", "/home/user/other-project"),
                    sessionItem("c", "/home/user/code-remote/"), // trailing slash normalizes to same dir
                )
            )
        )
        val preFilterSelection = setOf("a", "b", "c")

        val filtered = filterSessionGroupsByDirectory(groups, "/home/user/code-remote")
        val visibleIds = visibleSessionIds(filtered)

        assertEquals(setOf("a", "c"), visibleIds)
        assertEquals(
            setOf("a", "c"),
            pruneSelectionToVisible(preFilterSelection, visibleIds)
        )
    }

    @Test
    fun `clearing the filter keeps the previously pruned selection because all IDs are visible again`() {
        // After pruning under a filter, clearing the filter makes the full
        // set visible again. Pruning against the full set is a no-op, so the
        // (already safe) selection is preserved — Delete Selected still only
        // ever sees visible IDs.
        val groups = listOf(
            group(
                listOf(
                    sessionItem("a", "/home/user/code-remote"),
                    sessionItem("b", "/home/user/other-project"),
                    sessionItem("c", "/home/user/code-remote/"),
                )
            )
        )
        val preFilterSelection = setOf("a", "b", "c")

        // Apply the filter and prune.
        val filteredIds = visibleSessionIds(
            filterSessionGroupsByDirectory(groups, "/home/user/code-remote")
        )
        val pruned = pruneSelectionToVisible(preFilterSelection, filteredIds)
        assertEquals(setOf("a", "c"), pruned)

        // Clear the filter: the visible set is the full set again.
        val allIds = visibleSessionIds(filterSessionGroupsByDirectory(groups, null))
        assertEquals(setOf("a", "b", "c"), allIds)
        assertEquals(pruned, pruneSelectionToVisible(pruned, allIds))
    }
}
