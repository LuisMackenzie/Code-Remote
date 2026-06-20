package dev.mackenzie.coderemote.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mackenzie.coderemote.R

@Composable
internal fun LocalRuntimeCard(
    termuxInstalled: Boolean,
    runtimeStatus: LocalRuntimeStatus,
    statusMessage: String?,
    fixCommand: String?,
    needsOverlaySettings: Boolean,
    localServerConnected: Boolean,
    localServerConnectionError: String?,
    showLocalServerSettings: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onSetup: () -> Unit,
    onCopyFixCommand: (String) -> Unit,
    onOpenTermuxOverlaySettings: () -> Unit,
    onOpenLocalSessions: () -> Unit,
    onOpenLocalServerSettings: () -> Unit,
    onOpenLocalLaunchOptions: () -> Unit,
    onInstallTermux: () -> Unit,
) {
    val isAmoled = MaterialTheme.colorScheme.background == Color.Black &&
        MaterialTheme.colorScheme.surface == Color.Black
    val cardContainerColor = if (isAmoled) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val cardContentColor = if (isAmoled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val compactActive = runtimeStatus == LocalRuntimeStatus.Running &&
                localServerConnected &&
                localServerConnectionError.isNullOrBlank()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_local_server_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = cardContentColor,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenLocalLaunchOptions) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = stringResource(R.string.home_local_launch_options),
                            tint = cardContentColor,
                        )
                    }
                    if (showLocalServerSettings) {
                        IconButton(onClick = onOpenLocalServerSettings) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings_title),
                                tint = cardContentColor,
                            )
                        }
                    }
                }
            }

            if (!compactActive) {
                Text(
                    text = stringResource(R.string.home_local_server_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = cardContentColor.copy(alpha = 0.85f),
                )
            }

            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (runtimeStatus == LocalRuntimeStatus.Error) {
                        MaterialTheme.colorScheme.error
                    } else {
                        cardContentColor
                    },
                )
            }

            if (runtimeStatus == LocalRuntimeStatus.Error && !fixCommand.isNullOrBlank()) {
                OutlinedButton(
                    onClick = { onCopyFixCommand(fixCommand) },
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
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_local_copy_fix_command))
                }
            }

            if (runtimeStatus == LocalRuntimeStatus.Error && needsOverlaySettings) {
                OutlinedButton(
                    onClick = onOpenTermuxOverlaySettings,
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
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_local_open_termux_overlay_settings))
                }
            }

            when {
                !termuxInstalled -> {
                    OutlinedButton(
                        onClick = onInstallTermux,
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
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_local_install_termux))
                    }
                }

                runtimeStatus == LocalRuntimeStatus.NeedsSetup -> {
                    Text(
                        text = stringResource(R.string.home_local_setup_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = cardContentColor.copy(alpha = 0.85f),
                    )
                    Button(
                        onClick = onSetup,
                        modifier = Modifier.fillMaxWidth(),
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
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_local_setup))
                    }

                    OutlinedButton(
                        onClick = onStart,
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
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.home_local_start))
                    }
                }

                runtimeStatus == LocalRuntimeStatus.Running ||
                    runtimeStatus == LocalRuntimeStatus.Starting ||
                    runtimeStatus == LocalRuntimeStatus.Stopping -> {
                    val actionLabel = when (runtimeStatus) {
                        LocalRuntimeStatus.Starting -> stringResource(R.string.home_local_status_starting)
                        LocalRuntimeStatus.Stopping -> stringResource(R.string.home_local_status_stopping)
                        else -> stringResource(R.string.home_local_stop)
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (localServerConnected) {
                            Button(
                                onClick = onOpenLocalSessions,
                                modifier = Modifier.fillMaxWidth(),
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
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.home_local_open_sessions))
                            }
                        }

                        OutlinedButton(
                            onClick = onStop,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = runtimeStatus == LocalRuntimeStatus.Running,
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
                            if (runtimeStatus == LocalRuntimeStatus.Starting || runtimeStatus == LocalRuntimeStatus.Stopping) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(actionLabel)
                        }
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onStart,
                            modifier = Modifier.fillMaxWidth(),
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
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.home_local_start))
                        }

                        OutlinedButton(
                            onClick = onSetup,
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
                            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.home_local_setup))
                        }
                    }
                }
            }

            if (
                runtimeStatus != LocalRuntimeStatus.Running &&
                runtimeStatus != LocalRuntimeStatus.Starting &&
                runtimeStatus != LocalRuntimeStatus.Stopping &&
                localServerConnected
            ) {
                if (!compactActive) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                }

                if (!localServerConnectionError.isNullOrBlank()) {
                    Text(
                        text = localServerConnectionError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                OutlinedButton(
                    onClick = onOpenLocalSessions,
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
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.home_local_open_sessions))
                }
            }
        }
    }
}
