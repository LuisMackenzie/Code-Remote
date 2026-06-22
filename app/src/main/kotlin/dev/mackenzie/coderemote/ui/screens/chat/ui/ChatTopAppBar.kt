package dev.mackenzie.coderemote.ui.screens.chat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.mackenzie.coderemote.R
import dev.mackenzie.coderemote.domain.model.SessionStatus
import dev.mackenzie.coderemote.ui.screens.chat.ChatUiState
import dev.mackenzie.coderemote.ui.screens.chat.formatTokenCount
import dev.mackenzie.coderemote.ui.screens.chat.isAmoledTheme

/**
 * Top app bar for the chat screen: session title, navigation, terminal shortcut,
 * abort action, and overflow menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTopAppBar(
    uiState: ChatUiState,
    onNavigateBack: () -> Unit,
    onOpenTerminal: () -> Unit,
    onAbort: () -> Unit,
    onOpenInWebView: () -> Unit,
    onNewSession: () -> Unit,
    onForkSession: () -> Unit,
    onCompactSession: () -> Unit,
    onReview: () -> Unit,
    onShare: () -> Unit,
    onUnshare: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val isAmoled = isAmoledTheme()

    TopAppBar(
        title = {
            Column {
                Text(
                    text = uiState.sessionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Subtitle: total tokens and cost for the session
                val totalTokens = uiState.totalInputTokens + uiState.totalOutputTokens
                if (totalTokens > 0 || uiState.totalCost > 0) {
                    val parts = mutableListOf<String>()
                    if (totalTokens > 0) {
                        parts.add(stringResource(R.string.chat_tokens_summary, formatTokenCount(totalTokens)))
                    }
                    if (uiState.totalCost > 0) {
                        parts.add(stringResource(R.string.chat_cost_format, String.format("%.4f", uiState.totalCost)))
                    }
                    if (parts.isNotEmpty()) {
                        Text(
                            text = parts.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
        },
        actions = {
            if (uiState.sessionStatus is SessionStatus.Busy) {
                IconButton(onClick = onAbort) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = stringResource(R.string.chat_stop),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = onOpenTerminal) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = stringResource(R.string.tool_terminal)
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
                    border = if (isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)) else null
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_open_in_web)) },
                        onClick = {
                            showMenu = false
                            onOpenInWebView()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Language, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_new_session)) },
                        onClick = {
                            showMenu = false
                            onNewSession()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_fork_session)) },
                        onClick = {
                            showMenu = false
                            onForkSession()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CopyAll, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_compact_session)) },
                        onClick = {
                            showMenu = false
                            onCompactSession()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Compress, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_review_changes)) },
                        onClick = {
                            showMenu = false
                            onReview()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.RateReview, contentDescription = null)
                        },
                    )
                    // Show Share or Unshare depending on current share status
                    if (uiState.shareUrl != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cmd_unshare)) },
                            onClick = {
                                showMenu = false
                                onUnshare()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.LinkOff, contentDescription = null)
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_share_session)) },
                            onClick = {
                                showMenu = false
                                onShare()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Share, contentDescription = null)
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_rename_session)) },
                        onClick = {
                            showMenu = false
                            onRename()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_export_session)) },
                        onClick = {
                            showMenu = false
                            onExport()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                        }
                    )
                }
            }
        }
    )
}
