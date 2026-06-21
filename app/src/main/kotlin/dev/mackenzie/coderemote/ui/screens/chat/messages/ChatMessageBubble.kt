package dev.mackenzie.coderemote.ui.screens.chat.messages

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mackenzie.coderemote.R
import dev.mackenzie.coderemote.domain.model.Message
import dev.mackenzie.coderemote.domain.model.Part
import dev.mackenzie.coderemote.domain.model.ToolState
import dev.mackenzie.coderemote.ui.screens.chat.ChatMessage
import dev.mackenzie.coderemote.ui.components.PulsingDotsIndicator
import dev.mackenzie.coderemote.ui.components.ProviderIcon
import dev.mackenzie.coderemote.ui.screens.chat.LocalCollapseTools
import dev.mackenzie.coderemote.ui.screens.chat.LocalCompactMessages
import dev.mackenzie.coderemote.ui.screens.chat.LocalHapticFeedbackEnabled
import dev.mackenzie.coderemote.ui.screens.chat.components.ErrorPayloadContent
import dev.mackenzie.coderemote.ui.screens.chat.formatAssistantErrorMessage
import dev.mackenzie.coderemote.ui.screens.chat.isAmoledTheme
import dev.mackenzie.coderemote.ui.screens.chat.performHaptic

/**
 * Determine the "status text" for a group of step parts (like WebUI).
 * E.g., "Making edits", "Running commands", "Searching codebase", "Thinking"
 */
@Composable
private fun resolveStepsStatus(stepParts: List<Part>): String {
    val toolParts = stepParts.filterIsInstance<Part.Tool>()
    val hasRunning = toolParts.any { it.state is ToolState.Running }
    if (!hasRunning && toolParts.all { it.state is ToolState.Completed || it.state is ToolState.Error }) {
        // All done — summarize
        val editCount = toolParts.count { it.tool in listOf("edit", "write", "apply_patch", "multiedit") }
        val bashCount = toolParts.count { it.tool == "bash" }
        val searchCount = toolParts.count { it.tool in listOf("glob", "grep", "read", "list", "listDirectory") }
        return when {
            editCount > 0 && bashCount == 0 && searchCount == 0 -> {
                if (editCount == 1)
                    stringResource(R.string.chat_status_edits, editCount)
                else
                    stringResource(R.string.chat_status_edits_plural, editCount)
            }
            bashCount > 0 && editCount == 0 && searchCount == 0 -> {
                if (bashCount == 1)
                    stringResource(R.string.chat_status_commands, bashCount)
                else
                    stringResource(R.string.chat_status_commands_plural, bashCount)
            }
            else -> {
                if (toolParts.size == 1)
                    stringResource(R.string.chat_status_steps, toolParts.size)
                else
                    stringResource(R.string.chat_status_steps_plural, toolParts.size)
            }
        }
    }
    // Currently running — describe what's happening
    val runningTool = toolParts.lastOrNull { it.state is ToolState.Running }
    return when (runningTool?.tool) {
        "edit", "write", "multiedit" -> stringResource(R.string.chat_status_making_edits)
        "bash" -> stringResource(R.string.chat_status_running_commands)
        "read", "glob", "grep", "list", "listDirectory" -> stringResource(R.string.chat_status_searching)
        "webfetch" -> stringResource(R.string.chat_status_fetching_url)
        "task" -> stringResource(R.string.chat_status_running_subagent)
        "todowrite" -> stringResource(R.string.chat_status_updating_tasks)
        else -> stringResource(R.string.chat_status_thinking)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatMessageBubble(
    chatMessage: ChatMessage,
    onRevert: (() -> Unit)? = null,
    onCopyText: (() -> Unit)? = null
) {
    val isUser = chatMessage.isUser
    val isAmoled = isAmoledTheme()
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val backgroundColor = if (isAmoled) {
        Color.Black
    } else if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = if (isAmoled) {
        MaterialTheme.colorScheme.onSurface
    } else if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val bubbleBorder = if (isAmoled) {
        BorderStroke(
            1.dp,
            if (isUser) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
            }
        )
    } else {
        null
    }
    val hapticView = LocalView.current
    val hapticOn = LocalHapticFeedbackEnabled.current

    // Separate parts into text/reasoning (shown directly) and step parts (behind toggle)
    val visibleParts = if (isUser) {
        chatMessage.parts.filter { part ->
            when (part) {
                is Part.Text -> part.synthetic != true && part.ignored != true && part.text.isNotBlank()
                else -> true
            }
        }
    } else {
        chatMessage.parts
    }

    val userMessage = chatMessage.message as? Message.User
    val assistantMessage = chatMessage.message as? Message.Assistant
    val assistantErrorText = formatAssistantErrorMessage(assistantMessage?.error)
    val userFallbackText = userMessage?.summary?.body?.takeIf { it.isNotBlank() }
        ?: userMessage?.summary?.title?.takeIf { it.isNotBlank() }
    val userCommandLabel = if (isUser) {
        resolveUserCommandLabel(chatMessage.parts)
    } else {
        null
    }

    // For assistant messages: split into "content" (text, reasoning, patch) and "steps" (tool calls, step markers)
    val contentParts: List<Part>
    val stepParts: List<Part>
    if (!isUser) {
        contentParts = visibleParts.filter { part ->
            part is Part.Text || part is Part.Reasoning || part is Part.Patch ||
                    part is Part.File || part is Part.Permission || part is Part.Question ||
                    part is Part.Abort || part is Part.Retry
        }
        stepParts = visibleParts.filter { part ->
            part is Part.Tool || part is Part.StepStart || part is Part.StepFinish
        }
    } else {
        contentParts = visibleParts
        stepParts = emptyList()
    }

    val hasRenderableUserPart = contentParts.any(::isBubbleRenderablePart)
    val hasRenderableUserContent = !isUser || hasRenderableUserPart || userFallbackText != null || userCommandLabel != null
    val hasRenderableAssistantContent = isUser ||
            contentParts.isNotEmpty() ||
            stepParts.isNotEmpty() ||
            assistantErrorText != null
    if (!hasRenderableUserContent || !hasRenderableAssistantContent) {
        return
    }

    val hasSteps = stepParts.isNotEmpty()
    val autoExpand = LocalCollapseTools.current
    var stepsExpanded by remember(autoExpand) { mutableStateOf(autoExpand) }

    // Check if any tool is currently running (show spinner)
    val hasRunningTool = stepParts.any { it is Part.Tool && it.state is ToolState.Running }

    val bubbleContent: @Composable () -> Unit = {
        Surface(
            shape = RoundedCornerShape(
                topStart = if (isUser) 18.dp else 4.dp,
                topEnd = if (isUser) 4.dp else 18.dp,
                bottomStart = 18.dp,
                bottomEnd = 18.dp
            ),
            color = backgroundColor,
            border = bubbleBorder,
            tonalElevation = if (isAmoled || isUser) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            val compact = LocalCompactMessages.current
            Column(
                modifier = Modifier.padding(
                    horizontal = if (compact) 10.dp else 16.dp,
                    vertical = if (compact) 8.dp else 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp)
            ) {
                // "Response" header with provider icon and copy button — assistant messages only
                if (!isUser) {
                    val assistantMsg = assistantMessage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            val pid = assistantMsg?.providerId
                            if (pid != null) {
                                ProviderIcon(
                                    providerId = pid,
                                    size = 12.dp,
                                    tint = textColor.copy(alpha = 0.4f)
                                )
                            }
                            Text(
                                text = stringResource(R.string.chat_response),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = textColor.copy(alpha = 0.4f)
                            )
                        }
                        if (onCopyText != null) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.chat_copy),
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { performHaptic(hapticView, hapticOn); onCopyText() },
                                tint = textColor.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                // Steps toggle (like WebUI "Show/Hide steps")
                if (hasSteps) {
                    val stepsStatus = resolveStepsStatus(stepParts)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { performHaptic(hapticView, hapticOn); stepsExpanded = !stepsExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasRunningTool) {
                            PulsingDotsIndicator(
                                dotSize = 5.dp,
                                dotSpacing = 3.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        } else {
                            Icon(
                                imageVector = if (stepsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = textColor.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            text = if (stepsExpanded) stringResource(R.string.chat_hide_steps) else stepsStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.6f)
                        )
                    }

                    // Expanded step parts
                    AnimatedVisibility(visible = stepsExpanded) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (part in stepParts) {
                                PartContent(
                                    part = part,
                                    textColor = textColor,
                                    isUser = isUser
                                )
                            }
                        }
                    }
                }

                // Content parts (text, reasoning, patches, etc.)
                // Group image file parts into a compact thumbnail row
                val imageFiles = contentParts.filterIsInstance<Part.File>()
                    .filter { it.mime.startsWith("image/") && !it.url.isNullOrBlank() }
                val otherParts = contentParts.filter { part ->
                    !(part is Part.File && part.mime.startsWith("image/") && !part.url.isNullOrBlank())
                }
                val renderableOtherParts = otherParts.filter(::isBubbleRenderablePart)

                // Render image thumbnails as a horizontal row
                if (imageFiles.isNotEmpty()) {
                    ImageThumbnailRow(imageFiles = imageFiles)
                }

                // Render remaining parts
                for (part in renderableOtherParts) {
                    PartContent(
                        part = part,
                        textColor = textColor,
                        isUser = isUser
                    )
                }

                if (isUser && imageFiles.isEmpty() && renderableOtherParts.isEmpty() && userCommandLabel != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = textColor.copy(alpha = 0.7f)
                        )
                        Text(
                            text = userCommandLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor.copy(alpha = 0.85f)
                        )
                    }
                }

                if (!isUser && assistantErrorText != null) {
                    Surface(
                        color = if (isAmoled) Color.Black else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = if (isAmoled) 0.75f else 0.35f)),
                        tonalElevation = 0.dp,
                    ) {
                        ErrorPayloadContent(
                            text = assistantErrorText,
                            textStyle = MaterialTheme.typography.bodySmall,
                            textColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
                }

                // If text parts are absent but server provided a summary, render it.
                if (visibleParts.isEmpty() && isUser && userFallbackText != null) {
                    Text(
                        text = userFallbackText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (isUser && onRevert != null) {
            // Swipe-to-revert for user messages with confirmation dialog
            var showRevertConfirmation by remember { mutableStateOf(false) }
            val hapticEnabled = LocalHapticFeedbackEnabled.current
            val bubbleView = LocalView.current

            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value != SwipeToDismissBoxValue.Settled) {
                        if (hapticEnabled) {
                            @Suppress("DEPRECATION")
                            bubbleView.performHapticFeedback(
                                HapticFeedbackConstants.LONG_PRESS,
                                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                                        HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                            )
                        }
                        showRevertConfirmation = true
                    }
                    false // don't actually dismiss; wait for dialog confirmation
                }
            )

            if (showRevertConfirmation) {
                AlertDialog(
                    onDismissRequest = { showRevertConfirmation = false },
                    title = { Text(stringResource(R.string.chat_revert_title)) },
                    text = { Text(stringResource(R.string.chat_revert_message)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showRevertConfirmation = false
                                onRevert()
                            }
                        ) {
                            Text(stringResource(R.string.chat_revert), color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRevertConfirmation = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val direction = dismissState.dismissDirection
                    val bgColor = MaterialTheme.colorScheme.errorContainer
                    val iconAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Alignment.CenterStart
                    } else {
                        Alignment.CenterEnd
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(
                                topStart = 18.dp,
                                topEnd = 4.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 18.dp
                            ))
                            .background(bgColor)
                            .padding(horizontal = 20.dp),
                        contentAlignment = iconAlignment
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = stringResource(R.string.chat_revert),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = stringResource(R.string.chat_revert),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                },
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true
            ) {
                bubbleContent()
            }
        } else {
            bubbleContent()
        }
    }
}

private fun isBubbleRenderablePart(part: Part): Boolean {
    return when (part) {
        is Part.Text,
        is Part.Reasoning,
        is Part.Patch,
        is Part.File,
        is Part.Permission,
        is Part.Question,
        is Part.Abort,
        is Part.Retry,
        is Part.Tool -> true
        else -> false
    }
}

@Composable
private fun resolveUserCommandLabel(parts: List<Part>): String? {
    val subtaskParts = parts.filterIsInstance<Part.Subtask>()

    val commandFromSubtask = subtaskParts
        .firstNotNullOfOrNull { it.command }
        ?.removePrefix("/")
        ?.trim()
        ?.lowercase()

    val commandFromText = parts
        .filterIsInstance<Part.Text>()
        .firstNotNullOfOrNull { textPart ->
            val text = textPart.text.trim()
            if (!text.startsWith("/")) return@firstNotNullOfOrNull null
            text.removePrefix("/").substringBefore(' ').trim().lowercase().takeIf { it.isNotBlank() }
        }

    val inferredReviewFromPrompt = subtaskParts.any { subtask ->
        val prompt = subtask.prompt.lowercase()
        val description = subtask.description?.lowercase().orEmpty()
        "review changes" in prompt || "review" in description
    }

    val command = commandFromSubtask ?: commandFromText ?: if (inferredReviewFromPrompt) "review" else null

    return when (command) {
        "review" -> stringResource(R.string.menu_review_changes)
        null -> {
            val hasNonRenderableOnly = parts.any { part ->
                part !is Part.Text &&
                        part !is Part.Reasoning &&
                        part !is Part.Patch &&
                        part !is Part.File &&
                        part !is Part.Permission &&
                        part !is Part.Question &&
                        part !is Part.Abort &&
                        part !is Part.Retry
            }
            if (hasNonRenderableOnly) stringResource(R.string.chat_tool_running_command) else null
        }
        else -> stringResource(R.string.chat_tool_running_command)
    }
}
