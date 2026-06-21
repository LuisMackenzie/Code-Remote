package dev.mackenzie.coderemote.ui.screens.chat.messages.toolcards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mackenzie.coderemote.R
import dev.mackenzie.coderemote.domain.model.Part
import dev.mackenzie.coderemote.domain.model.ToolState
import dev.mackenzie.coderemote.ui.components.PulsingDotsIndicator
import dev.mackenzie.coderemote.ui.screens.chat.LocalCollapseTools
import dev.mackenzie.coderemote.ui.screens.chat.LocalHapticFeedbackEnabled
import dev.mackenzie.coderemote.ui.screens.chat.codeHorizontalScroll
import dev.mackenzie.coderemote.ui.screens.chat.isAmoledTheme
import dev.mackenzie.coderemote.ui.screens.chat.performHaptic
import dev.mackenzie.coderemote.ui.screens.chat.toolOutputContainerColor
import dev.mackenzie.coderemote.ui.theme.CodeTypography
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Composable
internal fun ToolCallCard(tool: Part.Tool) {
    val isAmoled = isAmoledTheme()
    val stateColor = when (tool.state) {
        is ToolState.Pending -> MaterialTheme.colorScheme.outline
        is ToolState.Running -> MaterialTheme.colorScheme.tertiary
        is ToolState.Completed -> MaterialTheme.colorScheme.primary
        is ToolState.Error -> MaterialTheme.colorScheme.error
    }

    // Extract input args for context-specific display
    val input = when (val state = tool.state) {
        is ToolState.Pending -> state.input
        is ToolState.Running -> state.input
        is ToolState.Completed -> state.input
        is ToolState.Error -> state.input
    }

    // Resolve display info based on tool type
    val toolDisplay = resolveToolDisplay(tool.tool, tool.state, input)

    val autoExpand = LocalCollapseTools.current
    val hapticView = LocalView.current
    val hapticOn = LocalHapticFeedbackEnabled.current
    var expanded by remember(autoExpand) { mutableStateOf(autoExpand) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
        border = if (isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)) else null,
        tonalElevation = if (isAmoled) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .let { mod ->
                        if (tool.state is ToolState.Completed || tool.state is ToolState.Error) {
                            mod.clickable { performHaptic(hapticView, hapticOn); expanded = !expanded }
                        } else mod
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when (tool.state) {
                            is ToolState.Running -> Icons.Default.Sync
                            is ToolState.Completed -> toolDisplay.icon
                            is ToolState.Error -> Icons.Default.Error
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (tool.state is ToolState.Error) stateColor else toolDisplay.iconTint ?: stateColor
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = toolDisplay.title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (toolDisplay.subtitle != null) {
                            Text(
                                text = toolDisplay.subtitle,
                                style = CodeTypography.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // Expand indicator for completed/errored tools
                if (tool.state is ToolState.Completed || tool.state is ToolState.Error) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) stringResource(R.string.chat_collapse) else stringResource(R.string.chat_expand),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                } else if (tool.state is ToolState.Running) {
                    PulsingDotsIndicator(
                        dotSize = 5.dp,
                        dotSpacing = 3.dp,
                        color = stateColor
                    )
                }
            }

            // Expandable details
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val output = when (val s = tool.state) {
                        is ToolState.Completed -> s.output
                        is ToolState.Error -> s.error
                        else -> ""
                    }
                    if (output.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = toolOutputContainerColor(isAmoled),
                            border = if (isAmoled) BorderStroke(1.dp, stateColor.copy(alpha = 0.6f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = output.take(3000),
                                style = CodeTypography.copy(
                                    fontSize = 11.sp,
                                    color = if (isAmoled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier
                                    .padding(8.dp)
                                    .codeHorizontalScroll()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Display info for a tool call, resolved from tool name and input args.
 */
private data class ToolDisplayInfo(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector = Icons.Default.Check,
    val iconTint: Color? = null
)

/**
 * Resolve display info for a tool call based on its type and input arguments.
 * Matches WebUI tool registry behavior with human-readable titles.
 */
@Composable
private fun resolveToolDisplay(
    toolName: String,
    state: ToolState,
    input: Map<String, JsonElement>
): ToolDisplayInfo {
    // Use server-provided title if available
    val serverTitle = when (state) {
        is ToolState.Running -> state.title
        is ToolState.Completed -> state.title
        else -> null
    }

    val filePath = input["filePath"]?.jsonPrimitive?.contentOrNull
        ?: input["path"]?.jsonPrimitive?.contentOrNull
        ?: input["file"]?.jsonPrimitive?.contentOrNull
    val shortPath = filePath?.substringAfterLast('/')

    return when (toolName) {
        "read" -> {
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_read_file),
                subtitle = shortPath ?: filePath,
                icon = Icons.Default.Description
            )
        }
        "write" -> {
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_write_file),
                subtitle = shortPath ?: filePath,
                icon = Icons.Default.EditNote
            )
        }
        "edit" -> {
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_edit_file),
                subtitle = shortPath ?: filePath,
                icon = Icons.Default.Edit
            )
        }
        "bash" -> {
            val command = input["command"]?.jsonPrimitive?.contentOrNull
            val shortCmd = command?.let {
                if (it.length > 60) it.take(57) + "..." else it
            }
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_terminal),
                subtitle = shortCmd,
                icon = Icons.Default.Terminal
            )
        }
        "glob" -> {
            val pattern = input["pattern"]?.jsonPrimitive?.contentOrNull
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_find_files),
                subtitle = pattern,
                icon = Icons.Default.FolderOpen
            )
        }
        "grep" -> {
            val pattern = input["pattern"]?.jsonPrimitive?.contentOrNull
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_search_code),
                subtitle = pattern,
                icon = Icons.Default.Search
            )
        }
        "list", "listDirectory" -> {
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_list_directory),
                subtitle = filePath,
                icon = Icons.Default.Folder
            )
        }
        "webfetch" -> {
            val url = input["url"]?.jsonPrimitive?.contentOrNull
            val shortUrl = url?.let {
                try { java.net.URI(it).host } catch (_: Exception) { it.take(40) }
            }
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_fetch_url),
                subtitle = shortUrl,
                icon = Icons.Default.Language
            )
        }
        "task" -> {
            val description = input["description"]?.jsonPrimitive?.contentOrNull
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_sub_agent),
                subtitle = description,
                icon = Icons.Default.AccountTree
            )
        }
        "apply_patch" -> {
            ToolDisplayInfo(
                title = serverTitle ?: stringResource(R.string.tool_apply_patch),
                subtitle = shortPath,
                icon = Icons.Default.Compare
            )
        }
        else -> {
            ToolDisplayInfo(
                title = serverTitle ?: toolName,
                subtitle = null,
                icon = Icons.Default.Build
            )
        }
    }
}

// ============================================================================
// Tool input/output helpers shared by tool card composables
// ============================================================================

/**
 * Extract common tool input values.
 */
internal fun extractToolInput(tool: Part.Tool): Map<String, JsonElement> {
    return when (val state = tool.state) {
        is ToolState.Pending -> state.input
        is ToolState.Running -> state.input
        is ToolState.Completed -> state.input
        is ToolState.Error -> state.input
    }
}

internal fun extractToolOutput(tool: Part.Tool): String {
    return when (val s = tool.state) {
        is ToolState.Completed -> s.output
        is ToolState.Error -> s.error
        else -> ""
    }
}
