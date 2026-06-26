package dev.mackenzie.coderemote.ui.screens.chat

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** Chat font size setting: "small", "medium", "large". */
internal val LocalChatFontSize = compositionLocalOf { "medium" }

/** Whether code blocks use word wrap instead of horizontal scroll. */
internal val LocalCodeWordWrap = compositionLocalOf { false }

/** Whether compact message spacing is enabled. */
internal val LocalCompactMessages = compositionLocalOf { false }

/** Whether tool cards are collapsed by default. */
internal val LocalCollapseTools = compositionLocalOf { false }

/** Whether haptic feedback is enabled. */
internal val LocalHapticFeedbackEnabled = compositionLocalOf { true }

/** Image save request callback available to image preview composables. */
internal val LocalImageSaveRequest = compositionLocalOf<(ByteArray, String, String?) -> Unit> { { _, _, _ -> } }

@Composable
internal fun isAmoledTheme(): Boolean {
    val colors = MaterialTheme.colorScheme
    return colors.background == Color.Black && colors.surface == Color.Black
}

@Composable
internal fun toolOutputContainerColor(isAmoled: Boolean): Color {
    return when {
        isAmoled -> Color.Black
        isSystemInDarkTheme() -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f)
    }
}

/**
 * Perform a light haptic tick if haptic feedback is enabled.
 * Call from composable context or from a click lambda that has access to a View.
 */
@Suppress("DEPRECATION")
internal fun performHaptic(view: View, enabled: Boolean) {
    if (enabled) {
        view.performHapticFeedback(
            HapticFeedbackConstants.CLOCK_TICK,
            HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                    HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }
}

/**
 * Conditionally applies horizontalScroll for code blocks.
 * When word wrap is enabled, no horizontal scroll is applied.
 */
@Composable
internal fun Modifier.codeHorizontalScroll(): Modifier {
    return if (!LocalCodeWordWrap.current) {
        this.horizontalScroll(rememberScrollState())
    } else {
        this
    }
}
