package dev.mackenzie.coderemote.ui.screens.sessions

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import dev.mackenzie.coderemote.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Android Compose UI test for the extracted [SessionListTopAppBar].
 *
 * Verifies the externally visible toolbar behavior introduced by the
 * project-filter feature without spinning up Hilt or [SessionListViewModel]:
 *  - active filter shows the selected project title and a close icon
 *  - tapping close fires the clear-filter callback and NOT navigate-back
 *  - inactive filter shows the normal title and a back icon
 *
 * Locale-independent: content descriptions are resolved through the test
 * instrumentation's target context so translated strings do not break the
 * assertions (the toolbar itself resolves the same strings via `stringResource`
 * at runtime, so both sides agree on the resource table).
 */
class SessionListTopAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun activeFilter_showsProjectTitleAndCloseIcon() {
        composeRule.setContent {
            MaterialTheme {
                SessionListTopAppBar(
                    title = "code-remote",
                    filterActive = true,
                    onNavigateBack = {},
                    onClearProjectFilter = {}
                )
            }
        }

        composeRule.onNodeWithText("code-remote").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.close))
            .assertIsDisplayed()
    }

    @Test
    fun tappingClose_invokesClearFilter_andDoesNotInvokeNavigateBack() {
        var clearFilterCalled = false
        var navigateBackCalled = false

        composeRule.setContent {
            MaterialTheme {
                SessionListTopAppBar(
                    title = "code-remote",
                    filterActive = true,
                    onNavigateBack = { navigateBackCalled = true },
                    onClearProjectFilter = { clearFilterCalled = true }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.close))
            .performClick()

        assertTrue("Clear-filter callback should fire when close is tapped", clearFilterCalled)
        assertFalse("Navigate-back should not fire while filter is active", navigateBackCalled)
    }

    @Test
    fun inactiveFilter_showsNormalTitleAndBackIcon() {
        composeRule.setContent {
            MaterialTheme {
                SessionListTopAppBar(
                    title = "My Server",
                    filterActive = false,
                    onNavigateBack = {},
                    onClearProjectFilter = {}
                )
            }
        }

        composeRule.onNodeWithText("My Server").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .assertIsDisplayed()
    }

    @Test
    fun tappingBack_invokesNavigateBack_andDoesNotInvokeClearFilter_whenFilterInactive() {
        var clearFilterCalled = false
        var navigateBackCalled = false

        composeRule.setContent {
            MaterialTheme {
                SessionListTopAppBar(
                    title = "My Server",
                    filterActive = false,
                    onNavigateBack = { navigateBackCalled = true },
                    onClearProjectFilter = { clearFilterCalled = true }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .performClick()

        assertTrue("Navigate-back should fire when back is tapped and filter is inactive", navigateBackCalled)
        assertFalse("Clear-filter should not fire when filter is inactive", clearFilterCalled)
    }
}
