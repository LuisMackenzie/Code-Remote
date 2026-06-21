package dev.mackenzie.coderemote.ui.screens.chat.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mackenzie.coderemote.R
import dev.mackenzie.coderemote.domain.model.SseEvent
import dev.mackenzie.coderemote.ui.screens.chat.LocalHapticFeedbackEnabled
import dev.mackenzie.coderemote.ui.screens.chat.isAmoledTheme
import dev.mackenzie.coderemote.ui.screens.chat.performHaptic
import dev.mackenzie.coderemote.ui.theme.CodeTypography

@Composable
internal fun PermissionCard(
    permission: SseEvent.PermissionAsked,
    onOnce: () -> Unit,
    onAlways: () -> Unit,
    onReject: () -> Unit,
) {
    val isAmoled = isAmoledTheme()
    val hapticView = LocalView.current
    val hapticOn = LocalHapticFeedbackEnabled.current
    val containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.tertiaryContainer
    val contentColor = if (isAmoled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
        border = if (isAmoled) BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isAmoled) MaterialTheme.colorScheme.tertiary else contentColor,
                )
                Text(
                    text = stringResource(R.string.permission_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor,
                )
            }
            Text(
                text = permission.permission,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
            )
            if (permission.patterns.isNotEmpty()) {
                Text(
                    text = permission.patterns.joinToString(", "),
                    style = CodeTypography.copy(
                        fontSize = 11.sp,
                        color = contentColor.copy(alpha = 0.7f),
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        performHaptic(hapticView, hapticOn)
                        onReject()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.permission_deny), maxLines = 1)
                }
                OutlinedButton(
                    onClick = {
                        performHaptic(hapticView, hapticOn)
                        onOnce()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.permission_allow_once), maxLines = 1)
                }
                Button(
                    onClick = {
                        performHaptic(hapticView, hapticOn)
                        onAlways()
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(R.string.permission_allow_always), maxLines = 1)
                }
            }
        }
    }
}
