package dev.mackenzie.coderemote.ui.screens.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mackenzie.coderemote.R
import dev.mackenzie.coderemote.ui.components.PulsingDotsIndicator
import dev.mackenzie.coderemote.ui.screens.chat.ChatUiState
import dev.mackenzie.coderemote.ui.screens.chat.components.ErrorPayloadContent

/**
 * Center content shown when the chat has no messages: loading indicator, error retry, or empty hint.
 */
@Composable
internal fun ChatEmptyOrErrorContent(
    uiState: ChatUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when {
            uiState.isLoading && uiState.messages.isEmpty() -> {
                PulsingDotsIndicator()
            }
            uiState.error != null && uiState.messages.isEmpty() -> {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    ErrorPayloadContent(
                        text = uiState.error ?: stringResource(R.string.session_unknown_error),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        textColor = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onRetry) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
            uiState.messages.isEmpty() && !uiState.isLoading -> {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.chat_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        text = stringResource(R.string.chat_type_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}
