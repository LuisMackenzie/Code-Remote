package dev.mackenzie.coderemote.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.mackenzie.coderemote.local.LocalServerManager
import dev.mackenzie.coderemote.domain.model.ServerConfig

@Composable
internal fun HomeServerList(
    uiState: HomeUiState,
    isBatteryOptimized: Boolean,
    onDisableBatteryOptimization: () -> Unit,
    onStartLocalServer: () -> Unit,
    onStopLocalServer: () -> Unit,
    onSetupLocalServer: () -> Unit,
    onCopyFixCommand: (String) -> Unit,
    onOpenTermuxOverlaySettings: () -> Unit,
    onOpenLocalLaunchOptions: () -> Unit,
    onInstallTermux: () -> Unit,
    onAddServer: () -> Unit,
    onConnectServer: (String) -> Unit,
    onDisconnectServer: (String) -> Unit,
    onOpenSessions: (ServerConfig) -> Unit,
    onOpenServerSettings: (ServerConfig) -> Unit,
    onEditServer: (ServerConfig) -> Unit,
    onDeleteServer: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val localServer = uiState.servers.firstOrNull { it.url == LocalServerManager.LOCAL_SERVER_URL }
    val remoteServers = uiState.servers.filterNot { it.url == LocalServerManager.LOCAL_SERVER_URL }
    val localServerId = localServer?.id

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (isBatteryOptimized) {
            item(key = "__battery_banner") {
                BatteryOptimizationBanner(onDisable = onDisableBatteryOptimization)
            }
        }

        if (uiState.showLocalRuntime) {
            item(key = "__local_runtime") {
                LocalRuntimeCard(
                    termuxInstalled = uiState.termuxInstalled,
                    runtimeStatus = uiState.localRuntimeStatus,
                    statusMessage = uiState.localRuntimeMessage,
                    fixCommand = uiState.localRuntimeFixCommand,
                    needsOverlaySettings = uiState.localRuntimeNeedsOverlaySettings,
                    localServerConnected = localServerId?.let { it in uiState.connectedServerIds } == true,
                    localServerConnectionError = localServerId?.let { uiState.connectionErrors[it] },
                    showLocalServerSettings = localServerId?.let { it in uiState.serverSettingsReadyIds } == true,
                    onStart = onStartLocalServer,
                    onStop = onStopLocalServer,
                    onSetup = onSetupLocalServer,
                    onCopyFixCommand = onCopyFixCommand,
                    onOpenTermuxOverlaySettings = onOpenTermuxOverlaySettings,
                    onOpenLocalSessions = { localServer?.let(onOpenSessions) },
                    onOpenLocalServerSettings = { localServer?.let(onOpenServerSettings) },
                    onOpenLocalLaunchOptions = onOpenLocalLaunchOptions,
                    onInstallTermux = onInstallTermux,
                )
            }
        }

        if (remoteServers.isEmpty()) {
            item(key = "__empty_servers") {
                val hasLocalCard = uiState.showLocalRuntime
                EmptyServersView(
                    onAddServer = onAddServer,
                    modifier = if (hasLocalCard) {
                        Modifier.fillParentMaxHeight(0.5f)
                    } else {
                        Modifier.fillParentMaxHeight(0.8f)
                    },
                )
            }
        }

        items(remoteServers, key = { it.id }) { server ->
            ServerCard(
                server = server,
                isConnected = server.id in uiState.connectedServerIds,
                isConnecting = server.id in uiState.connectingServerIds,
                connectionError = uiState.connectionErrors[server.id],
                showServerSettings = server.id in uiState.serverSettingsReadyIds,
                onConnect = { onConnectServer(server.id) },
                onDisconnect = { onDisconnectServer(server.id) },
                onOpenSessions = { onOpenSessions(server) },
                onServerSettings = { onOpenServerSettings(server) },
                onEdit = { onEditServer(server) },
                onDelete = { onDeleteServer(server.id) },
            )
        }
    }
}
