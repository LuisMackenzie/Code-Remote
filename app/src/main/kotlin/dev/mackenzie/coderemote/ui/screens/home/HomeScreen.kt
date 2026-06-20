package dev.mackenzie.coderemote.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.mackenzie.coderemote.R
import dev.mackenzie.coderemote.ui.components.PulsingDotsIndicator

/**
 * Home Screen - Server list and management
 * 
 * Each server card has Connect/Disconnect/Sessions buttons.
 * Multiple servers can be connected simultaneously.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSessions: (serverUrl: String, username: String, password: String, serverName: String, serverId: String) -> Unit = { _, _, _, _, _ -> },
    onNavigateToServerSettings: (serverUrl: String, username: String, password: String, serverName: String, serverId: String) -> Unit = { _, _, _, _, _ -> },
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Track battery optimization status, re-check when app resumes
    var isBatteryOptimized by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                isBatteryOptimized = !pm.isIgnoringBatteryOptimizations(context.packageName)
                viewModel.refreshLocalRuntimeState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // We need to track which server requested notification permission so we
    // can resume the connect flow after the permission dialog.
    var pendingConnectServerId by remember { mutableStateOf<String?>(null) }
    var pendingLocalStart by remember { mutableStateOf(false) }
    var showLocalLaunchOptionsDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Whether granted or denied, proceed with connection
        pendingConnectServerId?.let { viewModel.connectToServer(it) }
        pendingConnectServerId = null
    }

    val runCommandPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && pendingLocalStart) {
            viewModel.startLocalServer(context)
        } else if (!granted) {
            Toast.makeText(context, R.string.home_local_permission_required, Toast.LENGTH_LONG).show()
        }
        pendingLocalStart = false
    }

    fun requestNotificationPermissionAndConnect(serverId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pendingConnectServerId = serverId
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.connectToServer(serverId)
        }
    }

    fun requestRunCommandPermissionAndStartLocal() {
        val permissionState = ContextCompat.checkSelfPermission(
            context,
            "com.termux.permission.RUN_COMMAND",
        )
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            viewModel.startLocalServer(context)
            return
        }

        pendingLocalStart = true
        runCommandPermissionLauncher.launch("com.termux.permission.RUN_COMMAND")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = { viewModel.showAddServerDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.home_add_server))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.about_title))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    PulsingDotsIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        dotSize = 12.dp,
                        dotSpacing = 8.dp,
                    )
                }
                else -> {
                    HomeServerList(
                        uiState = uiState,
                        isBatteryOptimized = isBatteryOptimized,
                        onDisableBatteryOptimization = {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                Uri.parse("package:${context.packageName}"),
                            )
                            context.startActivity(intent)
                        },
                        onStartLocalServer = { requestRunCommandPermissionAndStartLocal() },
                        onStopLocalServer = { viewModel.stopLocalServer(context) },
                        onSetupLocalServer = {
                            val setupCommand = uiState.setupCommand ?: viewModel.getLocalSetupCommand()
                            clipboardManager.setText(AnnotatedString(setupCommand))
                            Toast.makeText(context, R.string.home_local_setup_copied, Toast.LENGTH_SHORT).show()
                            viewModel.setupLocalServer(context)
                        },
                        onCopyFixCommand = { command ->
                            clipboardManager.setText(AnnotatedString(command))
                            Toast.makeText(context, R.string.home_local_fix_command_copied, Toast.LENGTH_SHORT).show()
                        },
                        onOpenTermuxOverlaySettings = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:com.termux"),
                            )
                            context.startActivity(intent)
                        },
                        onOpenLocalLaunchOptions = { showLocalLaunchOptionsDialog = true },
                        onInstallTermux = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://f-droid.org/packages/com.termux/"),
                            )
                            context.startActivity(intent)
                        },
                        onAddServer = { viewModel.showAddServerDialog() },
                        onConnectServer = { serverId -> requestNotificationPermissionAndConnect(serverId) },
                        onDisconnectServer = { serverId -> viewModel.disconnectFromServer(serverId) },
                        onOpenSessions = { server ->
                            onNavigateToSessions(
                                server.url,
                                server.username,
                                server.password ?: "",
                                server.displayName,
                                server.id,
                            )
                        },
                        onOpenServerSettings = { server ->
                            onNavigateToServerSettings(
                                server.url,
                                server.username,
                                server.password ?: "",
                                server.displayName,
                                server.id,
                            )
                        },
                        onEditServer = { server -> viewModel.showEditServerDialog(server) },
                        onDeleteServer = { serverId -> viewModel.deleteServer(serverId) },
                    )
                }
            }
        }

        // Add/Edit Server Dialog
        if (uiState.showAddServerDialog) {
            ServerDialog(
                server = uiState.editingServer,
                onDismiss = { viewModel.hideServerDialog() },
                onSave = { name, url, username, password, autoConnect ->
                    viewModel.saveServer(name, url, username, password, autoConnect)
                }
            )
        }

        if (showLocalLaunchOptionsDialog) {
            LocalLaunchOptionsDialog(
                enabled = uiState.localProxyEnabled,
                proxyUrl = uiState.localProxyUrl,
                noProxyList = uiState.localProxyNoProxy,
                allowLanAccess = uiState.localServerAllowLan,
                serverUsername = uiState.localServerUsername,
                serverPassword = uiState.localServerPassword,
                runInBackground = uiState.localServerRunInBackground,
                autoStart = uiState.localServerAutoStart,
                startupTimeoutSec = uiState.localServerStartupTimeoutSec,
                onDismiss = { showLocalLaunchOptionsDialog = false },
                onProxyEnabledChange = viewModel::setLocalProxyEnabled,
                onProxyUrlChange = viewModel::setLocalProxyUrl,
                onNoProxyListChange = viewModel::setLocalProxyNoProxy,
                onAllowLanAccessChange = viewModel::setLocalServerAllowLan,
                onServerUsernameChange = viewModel::setLocalServerUsername,
                onServerPasswordChange = viewModel::setLocalServerPassword,
                onRunInBackgroundChange = viewModel::setLocalServerRunInBackground,
                onAutoStartChange = viewModel::setLocalServerAutoStart,
                onStartupTimeoutSecChange = viewModel::setLocalServerStartupTimeoutSec,
            )
        }

    }
}
