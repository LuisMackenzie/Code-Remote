package dev.mackenzie.coderemote.ui.screens.sessions

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM tests for the system/back button dispatch precedence on the session
 * list screen.
 *
 * The screen's [androidx.activity.compose.BackHandler] routes back presses
 * through [sessionListBackAction] so the precedence is a pure function of
 * two booleans and can be exercised without instrumented UI or a ViewModel:
 *   1. selection mode -> clear selection (takes priority over the filter)
 *   2. active project filter -> clear the filter
 *   3. otherwise -> navigate back
 *
 * Selection mode must win over the filter so a user who entered selection
 * mode while a filter was active does not lose their selection when they tap
 * back; the second back press then clears the filter, and the third returns
 * to the previous screen.
 */
class SessionListBackDispatchTest {

    @Test
    fun `selection mode takes precedence over an active project filter`() {
        assertEquals(
            SessionListBackAction.ClearSelection,
            sessionListBackAction(isSelectionMode = true, isProjectFilterActive = true)
        )
    }

    @Test
    fun `active filter clears the filter when selection mode is off`() {
        assertEquals(
            SessionListBackAction.ClearProjectFilter,
            sessionListBackAction(isSelectionMode = false, isProjectFilterActive = true)
        )
    }

    @Test
    fun `no selection and no filter navigates back`() {
        assertEquals(
            SessionListBackAction.NavigateBack,
            sessionListBackAction(isSelectionMode = false, isProjectFilterActive = false)
        )
    }

    @Test
    fun `selection mode clears selection even when no filter is active`() {
        assertEquals(
            SessionListBackAction.ClearSelection,
            sessionListBackAction(isSelectionMode = true, isProjectFilterActive = false)
        )
    }
}
