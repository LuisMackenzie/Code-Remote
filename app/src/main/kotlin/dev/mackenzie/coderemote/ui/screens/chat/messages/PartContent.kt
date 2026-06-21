package dev.mackenzie.coderemote.ui.screens.chat.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.coil2.Coil2ImageTransformerImpl
import dev.mackenzie.coderemote.R
import dev.mackenzie.coderemote.domain.model.Part
import dev.mackenzie.coderemote.ui.screens.chat.LocalChatFontSize
import dev.mackenzie.coderemote.ui.screens.chat.components.preserveRawHtmlPayload
import dev.mackenzie.coderemote.ui.screens.chat.isAmoledTheme
import dev.mackenzie.coderemote.ui.screens.chat.messages.toolcards.BashToolCard
import dev.mackenzie.coderemote.ui.screens.chat.messages.toolcards.EditToolCard
import dev.mackenzie.coderemote.ui.screens.chat.messages.toolcards.ReadToolCard
import dev.mackenzie.coderemote.ui.screens.chat.messages.toolcards.SearchToolCard
import dev.mackenzie.coderemote.ui.screens.chat.messages.toolcards.TaskToolCard
import dev.mackenzie.coderemote.ui.screens.chat.messages.toolcards.ToolCallCard
import dev.mackenzie.coderemote.ui.screens.chat.messages.toolcards.WriteToolCard
import dev.mackenzie.coderemote.ui.theme.CodeTypography

@Composable
internal fun PartContent(
    part: Part,
    textColor: Color,
    isUser: Boolean = false
) {
    when (part) {
        is Part.Text -> {
            // Hide synthetic/ignored text parts (internal system content)
            if (part.text.isNotBlank() && part.synthetic != true && part.ignored != true) {
                MarkdownContent(
                    markdown = part.text,
                    textColor = textColor,
                    isUser = isUser
                )
            }
        }
        is Part.Reasoning -> {
            if (part.text.isNotBlank()) {
                ReasoningBlock(text = part.text)
            }
        }
        is Part.Tool -> {
            // todoread parts are filtered out entirely (WebUI convention)
            if (part.tool == "todoread") {
                // skip
            } else if (part.tool == "todowrite") {
                TodoListCard(tool = part)
            } else {
                // Dispatch to tool-specific renderers (like WebUI)
                when (part.tool) {
                    "edit", "multiedit" -> EditToolCard(tool = part)
                    "write" -> WriteToolCard(tool = part)
                    "bash" -> BashToolCard(tool = part)
                    "read" -> ReadToolCard(tool = part)
                    "glob", "grep" -> SearchToolCard(tool = part)
                    "task" -> TaskToolCard(tool = part)
                    else -> ToolCallCard(tool = part)
                }
            }
        }
        is Part.StepStart -> {
            // Visual separator between steps (hidden - WebUI doesn't show these)
        }
        is Part.StepFinish -> {
            // Token/cost info hidden from message bubbles (WebUI convention)
        }
        is Part.Patch -> {
            PatchCard(patch = part)
        }
        is Part.File -> {
            FileCard(file = part)
        }
        is Part.Permission -> {
            Text(
                text = stringResource(R.string.chat_permission_label, part.message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        is Part.Question -> {
            Text(
                text = stringResource(R.string.chat_question_inline, part.question),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        is Part.Abort -> {
            Text(
                text = stringResource(R.string.chat_aborted, part.reason),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        is Part.Retry -> {
            Text(
                text = stringResource(R.string.chat_retry, part.attempt, part.errorMessage),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        // Ignore less relevant parts
        is Part.Snapshot, is Part.Subtask, is Part.Compaction,
        is Part.Agent, is Part.SessionTurn, is Part.Unknown -> { /* skip */ }
    }
}

/**
 * Renders markdown content using mikepenz markdown renderer with code syntax highlighting.
 */
@Composable
private fun MarkdownContent(
    markdown: String,
    textColor: Color,
    isUser: Boolean
) {
    val normalizedMarkdown = remember(markdown) { preserveRawHtmlPayload(markdown) }
    val isAmoled = isAmoledTheme()

    // Inline code: keep text styling, but no opaque background so selection remains visible.
    val inlineCodeFg = when {
        isAmoled -> MaterialTheme.colorScheme.onSurface
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.primary
    }
    // Code blocks: distinct background
    val codeBlockBg = when {
        isAmoled -> MaterialTheme.colorScheme.surfaceContainerLow
        isUser -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val codeBlockFg = when {
        isAmoled -> MaterialTheme.colorScheme.onSurface
        isUser -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }

    // Font size from settings: small=13sp, medium=14sp (default), large=16sp
    val fontSizeSetting = LocalChatFontSize.current
    val (bodyFontSize, bodyLineHeight) = when (fontSizeSetting) {
        "small" -> 13.sp to 18.sp
        "large" -> 16.sp to 26.sp
        else -> 14.sp to 22.sp // medium
    }
    val (codeFontSize, codeLineHeight) = when (fontSizeSetting) {
        "small" -> 11.sp to 16.sp
        "large" -> 15.sp to 22.sp
        else -> 13.sp to 20.sp // medium
    }

    // Balanced text style with better line-height for readability
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(
        color = textColor,
        fontSize = bodyFontSize,
        lineHeight = bodyLineHeight
    )

    val colors = markdownColor(
        text = textColor,
        codeText = codeBlockFg,
        inlineCodeText = inlineCodeFg,
        linkText = when {
            isAmoled -> MaterialTheme.colorScheme.primary
            isUser -> MaterialTheme.colorScheme.onPrimaryContainer
            else -> MaterialTheme.colorScheme.primary
        },
        codeBackground = codeBlockBg,
        inlineCodeBackground = Color.Transparent,
        dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    val typography = markdownTypography(
        h1 = MaterialTheme.typography.titleLarge.copy(
            color = textColor,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp
        ),
        h2 = MaterialTheme.typography.titleMedium.copy(
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 28.sp
        ),
        h3 = MaterialTheme.typography.titleSmall.copy(
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp
        ),
        h4 = MaterialTheme.typography.bodyLarge.copy(
            color = textColor,
            fontWeight = FontWeight.SemiBold
        ),
        h5 = MaterialTheme.typography.bodyMedium.copy(
            color = textColor,
            fontWeight = FontWeight.SemiBold
        ),
        h6 = MaterialTheme.typography.bodyMedium.copy(
            color = textColor.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        ),
        text = bodyStyle,
        code = CodeTypography.copy(color = codeBlockFg, fontSize = codeFontSize, lineHeight = codeLineHeight),
        inlineCode = CodeTypography.copy(
            color = inlineCodeFg,
            fontSize = codeFontSize,
            fontWeight = FontWeight.Medium
        ),
        quote = bodyStyle.copy(
            color = textColor.copy(alpha = 0.65f),
            fontStyle = FontStyle.Italic
        ),
        paragraph = bodyStyle,
        ordered = bodyStyle,
        bullet = bodyStyle,
        list = bodyStyle,
        link = bodyStyle.copy(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    )

    val components = markdownComponents(
        codeBlock = highlightedCodeBlock,
        codeFence = highlightedCodeFence
    )

    SelectionContainer {
        Markdown(
            content = normalizedMarkdown,
            colors = colors,
            typography = typography,
            components = components,
            imageTransformer = Coil2ImageTransformerImpl,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
