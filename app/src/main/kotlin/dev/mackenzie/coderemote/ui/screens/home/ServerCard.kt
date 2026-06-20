package dev.mackenzie.coderemote.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mackenzie.coderemote.R
import dev.mackenzie.coderemote.domain.model.ServerConfig
import dev.mackenzie.coderemote.ui.theme.StatusConnected

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ServerCard(
    server: ServerConfig,
    isConnected: Boolean,
    isConnecting: Boolean,
    connectionError: String?,
    showServerSettings: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenSessions: () -> Unit,
    onServerSettings: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val isAmoled = MaterialTheme.colorScheme.background == Color.Black && MaterialTheme.colorScheme.surface == Color.Black
    val cardContainerColor = if (isAmoled) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val cardContentColor = if (isConnected && !isAmoled) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = cardContainerColor,
        ),
        border = if (isAmoled) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        } else {
            null
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = server.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = cardContentColor,
                    )
                    Text(
                        text = server.url,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cardContentColor.copy(alpha = 0.7f),
                    )
                    if (isConnected) {
                        Text(
                            text = stringResource(R.string.home_server_health_good),
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusConnected,
                        )
                    } else if (isConnecting) {
                        Text(
                            text = stringResource(R.string.home_connecting),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showServerSettings) {
                        IconButton(onClick = onServerSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.server_settings_title))
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
                            border = if (isAmoled) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                            } else {
                                null
                            },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.home_edit)) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.server_delete)) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                },
                            )
                        }
                    }
                }
            }

            if (connectionError != null) {
                Text(
                    text = connectionError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isConnected) {
                    Button(
                        onClick = onOpenSessions,
                        modifier = Modifier.weight(1f),
                        colors = if (isAmoled) {
                            ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                        border = if (isAmoled) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                        } else {
                            null
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.sessions_title), maxLines = 1)
                    }
                }
            }
            if (isConnected) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (isAmoled) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Black,
                                contentColor = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        },
                        border = if (isAmoled) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                        } else {
                            ButtonDefaults.outlinedButtonBorder
                        },
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.home_disconnect), maxLines = 1)
                    }
                }
            }
            if (!isConnected) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConnecting,
                    colors = if (isAmoled) {
                        ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Black,
                            disabledContentColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    },
                    border = if (isAmoled) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f))
                    } else {
                        null
                    },
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = if (isAmoled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.home_connecting))
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.home_connect))
                    }
                }
            }
        }
    }
}
