package dev.mackenzie.coderemote.ui.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import dev.mackenzie.coderemote.domain.model.*
import dev.mackenzie.coderemote.data.api.PromptPart
import dev.mackenzie.coderemote.MainActivity
import kotlinx.coroutines.launch

import android.net.Uri
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import dev.mackenzie.coderemote.BuildConfig
import dev.mackenzie.coderemote.R
import androidx.compose.ui.res.stringResource
import dev.mackenzie.coderemote.ui.components.PulsingDotsIndicator
import dev.mackenzie.coderemote.ui.screens.chat.messages.ChatMessageBubble
import dev.mackenzie.coderemote.ui.screens.chat.ui.ChatInputBar
import dev.mackenzie.coderemote.ui.screens.chat.ui.ChatInputMode
import dev.mackenzie.coderemote.ui.screens.chat.ui.ImageAttachment
import dev.mackenzie.coderemote.ui.screens.chat.ui.ModelPickerDialog
import dev.mackenzie.coderemote.ui.screens.chat.ui.PermissionCard
import dev.mackenzie.coderemote.ui.screens.chat.ui.QuestionCard
import dev.mackenzie.coderemote.ui.screens.chat.ui.RevertBanner
import dev.mackenzie.coderemote.ui.screens.chat.terminal.SessionTerminalInline
import dev.mackenzie.coderemote.ui.screens.chat.terminal.TerminalKeyboardOverlay
import dev.mackenzie.coderemote.ui.screens.chat.terminal.applyTerminalModifiers
import dev.mackenzie.coderemote.ui.screens.chat.terminal.applyTermuxFnBindings
import dev.mackenzie.coderemote.ui.screens.chat.components.ErrorPayloadContent


/**
 * Chat Screen - conversation view with native markdown rendering.
 * Shows messages with streaming text rendered via mikepenz markdown renderer.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSession: (sessionId: String) -> Unit = {},
    onOpenInWebView: () -> Unit = {},
    initialSharedImages: List<Uri> = emptyList(),
    onSharedImagesConsumed: () -> Unit = {},
    startInTerminalMode: Boolean = false,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val draftText by viewModel.draftText.collectAsState()
    val draftAttachmentUris by viewModel.draftAttachmentUris.collectAsState()
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    // Sync inputText once from draft on first composition
    var draftTextInitialized by remember { mutableStateOf(false) }
    if (!draftTextInitialized && draftText.isNotEmpty()) {
        inputText = TextFieldValue(draftText, TextRange(draftText.length))
        draftTextInitialized = true
    } else if (!draftTextInitialized) {
        draftTextInitialized = true
    }
    // Listen for revert events that should restore text to the input field
    LaunchedEffect(Unit) {
        viewModel.revertedDraftEvent.collect { payload ->
            inputText = TextFieldValue(payload.text, TextRange(payload.text.length))
        }
    }
    val listState = rememberLazyListState()
    var showModelPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isTerminalMode by rememberSaveable { mutableStateOf(startInTerminalMode) }
    var terminalCtrlLatched by rememberSaveable { mutableStateOf(false) }
    var terminalAltLatched by rememberSaveable { mutableStateOf(false) }
    var terminalVirtualCtrlDown by remember { mutableStateOf(false) }
    var terminalVirtualFnDown by remember { mutableStateOf(false) }
    var suppressFnTildeUntil by remember { mutableStateOf(0L) }
    val terminalFocusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isAmoled = isAmoledTheme()
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val view = LocalView.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    var terminalOverlayHeightPx by remember { mutableStateOf(0) }

    // @ file mention state
    val fileSearchResults by viewModel.fileSearchResults.collectAsState()
    val confirmedFilePaths by viewModel.confirmedFilePaths.collectAsState()

    // Settings
    val chatFontSize by viewModel.chatFontSize.collectAsState()
    val codeWordWrap by viewModel.codeWordWrap.collectAsState()
    val confirmBeforeSend by viewModel.confirmBeforeSend.collectAsState()
    val compactMessages by viewModel.compactMessages.collectAsState()
    val collapseTools by viewModel.collapseTools.collectAsState()
    val hapticEnabled by viewModel.hapticFeedback.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val compressImageAttachments by viewModel.compressImageAttachments.collectAsState()
    val imageAttachmentMaxLongSide by viewModel.imageAttachmentMaxLongSide.collectAsState()
    val imageAttachmentWebpQuality by viewModel.imageAttachmentWebpQuality.collectAsState()
    val terminalVersion by viewModel.terminalVersion.collectAsState()
    val terminalConnected by viewModel.terminalConnected.collectAsState()
    val terminalTabs by viewModel.terminalTabs.collectAsState()
    val activeTerminalTabId by viewModel.activeTerminalTabId.collectAsState()
    val terminalFontSizeSp by viewModel.terminalFontSizeSp.collectAsState()
    if (BuildConfig.DEBUG) {
        LaunchedEffect(terminalFontSizeSp) {
            Log.d("TerminalZoom", "ChatScreen: terminalFontSizeSp CHANGED to $terminalFontSizeSp (flow identity=${System.identityHashCode(viewModel.terminalFontSizeSp)})")
        }
    }
    val terminalDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showSendConfirmDialog by remember { mutableStateOf(false) }
    // Pending send action: stored so the confirm dialog can trigger it
    var pendingSendAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var inputMode by rememberSaveable { mutableStateOf(ChatInputMode.NORMAL.name) }
    val isShellMode = inputMode == ChatInputMode.SHELL.name

    BackHandler(enabled = isTerminalMode) {
        if (terminalDrawerState.isOpen) {
            coroutineScope.launch { terminalDrawerState.close() }
        } else if (startInTerminalMode) {
            // Opened directly in terminal mode (e.g. from sessions list) —
            // back should navigate away, not show the chat view.
            onNavigateBack()
        } else {
            isTerminalMode = false
        }
    }

    LaunchedEffect(isTerminalMode) {
        if (isTerminalMode) {
            viewModel.openTerminalSession { ok ->
                if (!ok) {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.chat_terminal_connect_failed))
                    }
                    isTerminalMode = false
                }
            }
        } else {
            terminalCtrlLatched = false
            terminalAltLatched = false
            terminalVirtualCtrlDown = false
            terminalVirtualFnDown = false
        }
    }

    DisposableEffect(isTerminalMode) {
        val activity = context as? MainActivity
        if (isTerminalMode && activity != null) {
            activity.setTerminalKeyInterceptor { event ->
                when (event.keyCode) {
                    android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        terminalVirtualCtrlDown = event.action == android.view.KeyEvent.ACTION_DOWN
                        true
                    }
                    android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                        val wasDown = terminalVirtualFnDown
                        terminalVirtualFnDown = event.action == android.view.KeyEvent.ACTION_DOWN
                        if (BuildConfig.DEBUG) {
                            Log.d("TerminalInput", "VOL_UP: action=${if (event.action == android.view.KeyEvent.ACTION_DOWN) "DOWN" else "UP"} wasDown=$wasDown nowDown=$terminalVirtualFnDown")
                        }
                        if (wasDown && !terminalVirtualFnDown) {
                            // FN key released — some IMEs leak a delayed '~' character
                            // from the underlying key (e.g., Shift+` or dead-key residue).
                            // Suppress any standalone '~' arriving shortly after release.
                            suppressFnTildeUntil = SystemClock.elapsedRealtime() + 3_000L
                            if (BuildConfig.DEBUG) {
                                Log.d("TerminalInput", "FN released -> suppressFnTildeUntil set for 3s")
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        } else {
            activity?.setTerminalKeyInterceptor(null)
        }
        onDispose {
            activity?.setTerminalKeyInterceptor(null)
            terminalVirtualCtrlDown = false
            terminalVirtualFnDown = false
        }
    }

    // Force status bar black while terminal is visible.
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    DisposableEffect(isTerminalMode) {
        val activity = context as? android.app.Activity
        if (isTerminalMode && activity != null) {
            activity.window.statusBarColor = android.graphics.Color.BLACK
            androidx.core.view.WindowCompat.getInsetsController(
                activity.window, activity.window.decorView
            ).isAppearanceLightStatusBars = false
        }
        onDispose {
            val act = context as? android.app.Activity ?: return@onDispose
            act.window.statusBarColor = android.graphics.Color.TRANSPARENT
            androidx.core.view.WindowCompat.getInsetsController(
                act.window, act.window.decorView
            ).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    LaunchedEffect(isTerminalMode, terminalConnected) {
        if (isTerminalMode && terminalConnected) {
            terminalFocusRequester.requestFocus()
        }
    }

    fun pasteClipboardToTerminal() {
        if (!terminalConnected) return
        val clip = clipboardManager.getText()?.text ?: return
        if (clip.isEmpty()) return
        val cleaned = clip
            .replace(Regex("[\u001B\u0080-\u009F]"), "")
            .replace("\r\n", "\r")
            .replace('\n', '\r')
        if (cleaned.isNotEmpty()) {
            viewModel.sendTerminalInput(cleaned)
        }
    }

    fun sendTerminalChunk(chunk: String) {
        if (BuildConfig.DEBUG) {
            val codes = chunk.map { String.format("%04x", it.code) }
            val remain = suppressFnTildeUntil - SystemClock.elapsedRealtime()
            Log.d("TerminalInput", "sendTerminalChunk: chunk=$codes fnDown=$terminalVirtualFnDown suppressRemain=${remain}ms")
        }
        if (!terminalVirtualFnDown) {
            val now = SystemClock.elapsedRealtime()
            if (now < suppressFnTildeUntil && chunk.contains('~')) {
                // Guard against a leaked '~' after an FN key combo (e.g., Fn+0/F10).
                // The tilde may arrive alone ("~") or bundled with other characters.
                if (BuildConfig.DEBUG) {
                    Log.d("TerminalInput", "SUPPRESSING tilde from chunk='$chunk'")
                }
                val stripped = chunk.replace("~", "")
                suppressFnTildeUntil = 0L
                if (stripped.isEmpty()) return
                // Forward the non-tilde remainder.
                @Suppress("NAME_SHADOWING")
                val chunk = stripped
                // fall through with the cleaned chunk
                val ctrlActive2 = terminalCtrlLatched || terminalVirtualCtrlDown
                val altActive2 = terminalAltLatched
                val processed = applyTerminalModifiers(input = chunk, ctrl = ctrlActive2, alt = altActive2)
                if (processed.isEmpty()) return
                viewModel.sendTerminalInput(processed)
                if (terminalCtrlLatched) terminalCtrlLatched = false
                if (terminalAltLatched) terminalAltLatched = false
                return
            }
            if (chunk.isNotEmpty() && !chunk.contains('~')) {
                // Any other explicit input clears the temporary suppression window.
                suppressFnTildeUntil = 0L
            }
        }

        val ctrlActive = terminalCtrlLatched || terminalVirtualCtrlDown
        val altActive = terminalAltLatched

        // Termux-compatible shortcut: Ctrl+Alt+V pastes clipboard into terminal.
        if (!terminalVirtualFnDown && ctrlActive && altActive && chunk.length == 1 && chunk[0].lowercaseChar() == 'v') {
            pasteClipboardToTerminal()
            if (terminalCtrlLatched) terminalCtrlLatched = false
            if (terminalAltLatched) terminalAltLatched = false
            return
        }

        val processed = if (terminalVirtualFnDown) {
            val fnResult = applyTermuxFnBindings(chunk, viewModel.terminalEmulator.cursorKeysApplicationMode)
            if (fnResult.showVolumeUi) {
                val audio = context.getSystemService(AudioManager::class.java)
                audio?.adjustSuggestedStreamVolume(
                    AudioManager.ADJUST_SAME,
                    AudioManager.USE_DEFAULT_STREAM_TYPE,
                    AudioManager.FLAG_SHOW_UI
                )
            }
            if (fnResult.toggleKeyboard) {
                if (imeVisible) {
                    keyboardController?.hide()
                } else {
                    terminalFocusRequester.requestFocus()
                    keyboardController?.show()
                }
            }
            if (fnResult.output.contains("~")) {
                // Any FN binding that produces '~' in its escape sequence (F5-F12, Insert,
                // Delete, PageUp, PageDown) may cause the IME to leak a standalone '~' after
                // the Volume-Up (FN) key is released.
                suppressFnTildeUntil = SystemClock.elapsedRealtime() + 3_000L
            }
            fnResult.output
        } else {
            applyTerminalModifiers(
                input = chunk,
                ctrl = ctrlActive,
                alt = altActive
            )
        }
        if (processed.isEmpty()) return
        if (BuildConfig.DEBUG && processed.contains('~')) {
            Log.d("TerminalInput", "SENDING to server: '${processed.map { String.format("%04x", it.code) }}' fnDown=$terminalVirtualFnDown")
        }
        viewModel.sendTerminalInput(processed)
        if (terminalCtrlLatched) terminalCtrlLatched = false
        if (terminalAltLatched) terminalAltLatched = false
    }

    // Keep screen on while on chat screen (if enabled in settings)
    DisposableEffect(keepScreenOn) {
        val window = (context as? android.app.Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Image attachments — backed by ViewModel URIs for draft persistence
    val attachments = remember { mutableStateListOf<ImageAttachment>() }

    // Rebuild attachment objects from persisted draft URIs on first composition
    LaunchedEffect(draftAttachmentUris, compressImageAttachments, imageAttachmentMaxLongSide, imageAttachmentWebpQuality) {
        // Only rebuild if attachments list doesn't match URIs (e.g. on session restore)
        val currentUris = attachments.map { it.uri.toString() }.toSet()
        val draftUriSet = draftAttachmentUris.toSet()
        if (currentUris == draftUriSet) return@LaunchedEffect

        val restored = mutableListOf<ImageAttachment>()
        for (uriStr in draftAttachmentUris) {
            // Skip URIs already present
            if (uriStr in currentUris) {
                val existing = attachments.first { it.uri.toString() == uriStr }
                restored.add(existing)
                continue
            }
            try {
                val uri = android.net.Uri.parse(uriStr)
                if (uriStr.startsWith("data:image/", ignoreCase = true)) {
                    val mime = uriStr.substringAfter("data:").substringBefore(';').ifBlank { "image/png" }
                    val syntheticName = "image.${mime.substringAfter('/', "png")}".lowercase()
                    restored.add(
                        ImageAttachment(
                            uri = uri,
                            mime = mime,
                            filename = syntheticName,
                            dataUrl = uriStr,
                        )
                    )
                    continue
                }
                val prepared = buildAttachmentFromUri(
                    contentResolver = context.contentResolver,
                    uri = uri,
                    compressImages = compressImageAttachments,
                    maxLongSidePx = imageAttachmentMaxLongSide,
                    webpQuality = imageAttachmentWebpQuality
                )
                if (prepared != null) {
                    restored.add(prepared.attachment)
                }
            } catch (e: Exception) {
                Log.w("ChatScreen", "Failed to restore attachment $uriStr: ${e.message}")
                // Remove invalid URI from draft
                viewModel.removeDraftAttachment(draftAttachmentUris.indexOf(uriStr))
            }
        }
        attachments.clear()
        attachments.addAll(restored)
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        coroutineScope.launch {
            val optimizedComparisons = mutableListOf<AttachmentComparison>()
            for (uri in uris) {
                try {
                    // Take persistable URI permission so the URI survives app restarts
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        // Not all URIs support persistable permissions — that's OK
                    }

                    val prepared = buildAttachmentFromUri(
                        contentResolver = context.contentResolver,
                        uri = uri,
                        compressImages = compressImageAttachments,
                        maxLongSidePx = imageAttachmentMaxLongSide,
                        webpQuality = imageAttachmentWebpQuality
                    ) ?: continue

                    attachments.add(prepared.attachment)
                    viewModel.addDraftAttachment(uri.toString())
                    prepared.comparison?.let { optimizedComparisons.add(it) }
                } catch (_: Exception) {
                    // Skip files that fail to read
                }
            }
            if (optimizedComparisons.isNotEmpty()) {
                val totalOriginal = optimizedComparisons.sumOf { it.originalBytes }
                val totalOptimized = optimizedComparisons.sumOf { it.optimizedBytes }
                val totalTokensBefore = optimizedComparisons.sumOf { it.originalEstimatedTokens }
                val totalTokensAfter = optimizedComparisons.sumOf { it.optimizedEstimatedTokens }
                snackbarHostState.showSnackbar(
                    context.getString(
                        R.string.chat_images_optimized_summary,
                        optimizedComparisons.size,
                        formatFileSize(totalOriginal),
                        formatFileSize(totalOptimized),
                        totalTokensBefore,
                        totalTokensAfter
                    )
                )
            }
        }
    }

    // Session export via SAF (Storage Access Framework)
    // Flow: menu click → SAF file picker → stream API responses directly to file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportSession(context, uri) { success ->
                coroutineScope.launch {
                    if (success) {
                        snackbarHostState.showSnackbar(context.getString(R.string.chat_session_exported))
                    } else {
                        snackbarHostState.showSnackbar(context.getString(R.string.chat_session_export_failed))
                    }
                }
            }
        }
    }

    var pendingImageSave by remember { mutableStateOf<ImageSaveRequest?>(null) }
    val saveImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/*")
    ) { uri: Uri? ->
        val request = pendingImageSave
        pendingImageSave = null
        if (uri == null || request == null) return@rememberLauncherForActivityResult

        coroutineScope.launch {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(request.bytes) }
                    ?: error("Unable to open output stream")
            }.onSuccess {
                snackbarHostState.showSnackbar(context.getString(R.string.chat_image_saved))
            }.onFailure {
                snackbarHostState.showSnackbar(context.getString(R.string.chat_image_save_failed))
            }
        }
    }

    val requestSaveImage: (ByteArray, String, String?) -> Unit = { bytes, mime, filenameHint ->
        val baseName = filenameHint
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: "image_${System.currentTimeMillis()}"
        val fileName = "$baseName.${extensionForMime(mime)}"
        pendingImageSave = ImageSaveRequest(bytes = bytes, mime = mime, filename = fileName)
        saveImageLauncher.launch(fileName)
    }

    // Consume images shared from other apps via ACTION_SEND (one-shot)
    LaunchedEffect(initialSharedImages) {
        if (initialSharedImages.isEmpty()) return@LaunchedEffect
        val optimizedComparisons = mutableListOf<AttachmentComparison>()
        for (uri in initialSharedImages) {
            try {
                // Take persistable URI permission so the URI survives app restarts
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // Not all URIs support persistable permissions — that's OK
                }

                val prepared = buildAttachmentFromUri(
                    contentResolver = context.contentResolver,
                    uri = uri,
                    compressImages = compressImageAttachments,
                    maxLongSidePx = imageAttachmentMaxLongSide,
                    webpQuality = imageAttachmentWebpQuality
                ) ?: continue

                attachments.add(prepared.attachment)
                prepared.comparison?.let { optimizedComparisons.add(it) }
                viewModel.addDraftAttachment(uri.toString())
            } catch (e: Exception) {
                Log.w("ChatScreen", "Failed to read shared image: ${e.message}")
            }
        }
        if (optimizedComparisons.isNotEmpty()) {
            val totalOriginal = optimizedComparisons.sumOf { it.originalBytes }
            val totalOptimized = optimizedComparisons.sumOf { it.optimizedBytes }
            val totalTokensBefore = optimizedComparisons.sumOf { it.originalEstimatedTokens }
            val totalTokensAfter = optimizedComparisons.sumOf { it.optimizedEstimatedTokens }
            snackbarHostState.showSnackbar(
                context.getString(
                    R.string.chat_images_optimized_summary,
                    optimizedComparisons.size,
                    formatFileSize(totalOriginal),
                    formatFileSize(totalOptimized),
                    totalTokensBefore,
                    totalTokensAfter
                )
            )
        }
        onSharedImagesConsumed()
    }

    // Show errors as snackbar when messages are already loaded
    LaunchedEffect(uiState.error) {
        val error = uiState.error
        if (error != null && uiState.messages.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Whether auto-scroll should follow new content.
    // Disabled when user manually scrolls up; re-enabled when user scrolls back to bottom.
    var autoScrollEnabled by remember { mutableStateOf(true) }

    // True when the very bottom of the list is visible (accounting for offset within tall items)
    val isAtBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull() ?: return@derivedStateOf true
            val totalItems = info.totalItemsCount
            if (lastVisible.index < totalItems - 1) return@derivedStateOf false
            // Last item is visible — check if its bottom edge is within the viewport
            val itemBottom = lastVisible.offset + lastVisible.size
            val viewportEnd = info.viewportEndOffset
            itemBottom <= viewportEnd + 50 // 50px tolerance
        }
    }

    // When user touches the list, disable auto-scroll; re-enable when they reach the bottom
    LaunchedEffect(listState.isScrollInProgress, isAtBottom) {
        if (listState.isScrollInProgress) {
            // User is actively dragging/flinging — disable auto-scroll
            autoScrollEnabled = false
        } else if (isAtBottom) {
            // User stopped scrolling and ended up at the bottom — re-enable
            autoScrollEnabled = true
        }
    }


    // Auto-scroll to bottom when new content arrives (only if auto-scroll is enabled)
    // Track message count, part count, and content length of the last part to catch streaming updates
    val messageCount = uiState.messages.size
    val lastPartCount = uiState.messages.lastOrNull()?.parts?.size ?: 0
    val lastContentLength = uiState.messages.lastOrNull()?.parts?.lastOrNull()?.let { part ->
        when (part) {
            is Part.Text -> part.text.length
            is Part.Reasoning -> part.text.length
            is Part.Tool -> when (val s = part.state) {
                is ToolState.Completed -> s.output.length
                is ToolState.Error -> s.error.length
                is ToolState.Running -> s.title?.length ?: 1
                is ToolState.Pending -> 0
            }
            else -> 0
        }
    } ?: 0
    val pendingCount = uiState.pendingPermissions.size + uiState.pendingQuestions.size
    val isBusy = uiState.sessionStatus is SessionStatus.Busy
    LaunchedEffect(messageCount, lastPartCount, lastContentLength, pendingCount, isBusy) {
        if (messageCount > 0 && autoScrollEnabled) {
            val lastIndex = listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1
            listState.scrollToItem(lastIndex)
            // scrollToItem goes to the TOP of the last item; when a message is
            // taller than the viewport (e.g. streaming summarisation) we also
            // need to scroll past it so the user sees the bottom of that message.
            val lastItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastItem != null) {
                val viewport = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                val overflow = lastItem.size - viewport
                if (overflow > 0) {
                    listState.scrollBy(overflow.toFloat())
                }
            }
        }
    }

    // Also auto-scroll when first loading
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && messageCount > 0) {
            val lastIndex = listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1
            listState.scrollToItem(lastIndex)
            val lastItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            if (lastItem != null) {
                val viewport = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                val overflow = lastItem.size - viewport
                if (overflow > 0) {
                    listState.scrollBy(overflow.toFloat())
                }
            }
            autoScrollEnabled = true
        }
    }

    CompositionLocalProvider(
        LocalChatFontSize provides chatFontSize,
        LocalCodeWordWrap provides codeWordWrap,
        LocalCompactMessages provides compactMessages,
        LocalCollapseTools provides collapseTools,
        LocalHapticFeedbackEnabled provides hapticEnabled,
        LocalImageSaveRequest provides requestSaveImage,
    ) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isTerminalMode) {
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
                        IconButton(onClick = { viewModel.abortSession() }) {
                            Icon(
                                Icons.Default.Stop,
                                contentDescription = stringResource(R.string.chat_stop),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(onClick = { isTerminalMode = true }) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = stringResource(R.string.tool_terminal)
                        )
                    }
                    Box {
                        val isAmoled = isAmoledTheme()
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
                                    viewModel.createNewSession { session ->
                                        if (session != null) {
                                            onNavigateToSession(session.id)
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(context.getString(R.string.chat_session_create_failed))
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_fork_session)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.forkSession { session ->
                                        if (session != null) {
                                            onNavigateToSession(session.id)
                                        } else {
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(context.getString(R.string.chat_fork_failed))
                                            }
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CopyAll, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_compact_session)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.compactSession { ok ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (ok) context.getString(R.string.chat_session_compacted) else context.getString(R.string.chat_session_compact_failed)
                                            )
                                        }
                                    }
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Compress, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_review_changes)) },
                                onClick = {
                                    showMenu = false
                                    viewModel.executeCommand("review") { ok ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (ok) context.getString(R.string.chat_command_executed, "review") else context.getString(R.string.chat_command_failed, "review")
                                            )
                                        }
                                    }
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
                                        viewModel.unshareSession { ok ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (ok) context.getString(R.string.chat_session_unshared) else context.getString(R.string.chat_session_unshare_failed)
                                                )
                                            }
                                        }
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
                                        viewModel.shareSession { url ->
                                            coroutineScope.launch {
                                                if (url != null) {
                                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(url))
                                                    snackbarHostState.showSnackbar(context.getString(R.string.chat_share_url_copied))
                                                } else {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.chat_share_failed))
                                                }
                                            }
                                        }
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
                                    showRenameDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_export_session)) },
                                onClick = {
                                    showMenu = false
                                    val slug = uiState.sessionTitle
                                        .take(30)
                                        .replace(Regex("[^a-zA-Z0-9_-]"), "_")
                                        .ifBlank { "session" }
                                    exportLauncher.launch("$slug.json")
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
        },
        bottomBar = {
            val modelLabel = if (uiState.selectedModelId != null && uiState.providers.isNotEmpty()) {
                val provider = uiState.providers.find { it.id == uiState.selectedProviderId }
                val model = provider?.models?.get(uiState.selectedModelId)
                model?.name ?: uiState.selectedModelId ?: ""
            } else ""

            if (!isTerminalMode) {
            ChatInputBar(
                textFieldValue = inputText,
                onTextFieldValueChange = { newValue ->
                    val shouldAutoShell = !isShellMode && newValue.text.startsWith("!")
                    val normalizedValue = if (shouldAutoShell) {
                        val stripped = newValue.text.drop(1).trimStart()
                        val newCursor = (newValue.selection.start - 1).coerceAtLeast(0)
                        TextFieldValue(
                            text = stripped,
                            selection = TextRange(newCursor.coerceAtMost(stripped.length))
                        )
                    } else {
                        newValue
                    }

                    if (shouldAutoShell) {
                        inputMode = ChatInputMode.SHELL.name
                    }

                    inputText = normalizedValue
                    viewModel.updateDraftText(normalizedValue.text)
                    if (isShellMode || shouldAutoShell) {
                        viewModel.clearFileSearch()
                        return@ChatInputBar
                    }
                    // Detect @query before cursor for file mention
                    val cursorPos = normalizedValue.selection.start
                    val textBefore = normalizedValue.text.substring(0, cursorPos)
                    val atMatch = Regex("@(\\S*)$").find(textBefore)
                    if (atMatch != null) {
                        val query = atMatch.groupValues[1]
                        viewModel.searchFilesForMention(query)
                    } else {
                        viewModel.clearFileSearch()
                    }
                },
                onSend = {
                    val doSend = doSend@{
                        if (hapticEnabled) {
                            @Suppress("DEPRECATION")
                            val flags = android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING or
                                    android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM, flags)
                            } else {
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK, flags)
                            }
                        }
                        val rawText = inputText.text
                        val shellCommand = when {
                            isShellMode -> rawText.trim()
                            rawText.startsWith("!") -> rawText.drop(1).trimStart()
                            else -> null
                        }
                        if (shellCommand != null) {
                            if (shellCommand.isBlank()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.chat_shell_empty))
                                }
                                return@doSend
                            }
                            if (attachments.isNotEmpty()) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(context.getString(R.string.chat_shell_attachments_unsupported))
                                }
                                return@doSend
                            }
                            viewModel.runShellCommand(shellCommand) { ok ->
                                if (!ok) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.chat_shell_failed))
                                    }
                                }
                            }
                            inputText = TextFieldValue("")
                            if (isShellMode) {
                                inputMode = ChatInputMode.NORMAL.name
                            }
                            viewModel.clearConfirmedPaths()
                            viewModel.clearFileSearch()
                            viewModel.clearDraft()
                            return@doSend
                        }
                        // Build prompt parts: split text around confirmed @file mentions
                        val allParts = buildPromptParts(rawText, confirmedFilePaths, viewModel.getSessionDirectory())
                        // Add image attachments
                        val attachmentParts = attachments.map { att ->
                            PromptPart(
                                type = "file",
                                mime = att.mime,
                                url = att.dataUrl,
                                filename = att.filename
                            )
                        }
                        viewModel.sendMessage(allParts, attachmentParts)
                        inputText = TextFieldValue("")
                        attachments.clear()
                        viewModel.clearConfirmedPaths()
                        viewModel.clearFileSearch()
                        viewModel.clearDraft()
                    }
                    if (confirmBeforeSend) {
                        pendingSendAction = doSend
                        showSendConfirmDialog = true
                    } else {
                        doSend()
                    }
                },
                inputMode = if (isShellMode) ChatInputMode.SHELL else ChatInputMode.NORMAL,
                onInputModeChange = {
                    inputMode = it.name
                    if (it == ChatInputMode.SHELL) {
                        viewModel.clearFileSearch()
                    }
                },
                isSending = uiState.isSending,
                isBusy = uiState.sessionStatus is SessionStatus.Busy,
                messages = uiState.messages,
                attachments = attachments,
                onAttach = { imagePickerLauncher.launch("image/*") },
                onRemoveAttachment = { index ->
                    if (index in attachments.indices) {
                        attachments.removeAt(index)
                        viewModel.removeDraftAttachment(index)
                    }
                },
                onSaveAttachment = { bytes, mime, filename ->
                    requestSaveImage(bytes, mime, filename)
                },
                modelLabel = modelLabel,
                selectedProviderId = uiState.selectedProviderId,
                onModelClick = { showModelPicker = true },
                agents = uiState.agents,
                selectedAgent = uiState.selectedAgent,
                onAgentSelect = { viewModel.selectAgent(it) },
                variantNames = uiState.variantNames,
                selectedVariant = uiState.selectedVariant,
                onCycleVariant = { viewModel.cycleVariant() },
                commands = uiState.commands,
                fileSearchResults = fileSearchResults,
                confirmedFilePaths = confirmedFilePaths,
                onFileSelected = { path ->
                    // Replace @query with @path in text
                    val cursorPos = inputText.selection.start
                    val textBefore = inputText.text.substring(0, cursorPos)
                    val atMatch = Regex("@(\\S*)$").find(textBefore)
                    if (atMatch != null) {
                        val matchStart = atMatch.range.first
                        val replacement = "@$path "
                        val newText = inputText.text.substring(0, matchStart) + replacement +
                                inputText.text.substring(cursorPos)
                        val newCursor = matchStart + replacement.length
                        inputText = TextFieldValue(
                            text = newText,
                            selection = TextRange(newCursor)
                        )
                    }
                    viewModel.confirmFilePath(path)
                    viewModel.clearFileSearch()
                },
                onSlashCommand = { cmd ->
                    when (cmd.name) {
                        "new" -> {
                            // Create a new session and navigate to it
                            viewModel.createNewSession { session ->
                                if (session != null) {
                                    onNavigateToSession(session.id)
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.chat_session_create_failed))
                                    }
                                }
                            }
                        }
                        "compact" -> {
                            viewModel.compactSession { ok ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) context.getString(R.string.chat_session_compacted) else context.getString(R.string.chat_session_compact_failed)
                                    )
                                }
                            }
                        }
                        "fork" -> {
                            viewModel.forkSession { session ->
                                if (session != null) {
                                    onNavigateToSession(session.id)
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.chat_fork_failed))
                                    }
                                }
                            }
                        }
                        "share" -> {
                            viewModel.shareSession { url ->
                                coroutineScope.launch {
                                    if (url != null) {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(url))
                                        snackbarHostState.showSnackbar(context.getString(R.string.chat_share_url_copied))
                                    } else {
                                        snackbarHostState.showSnackbar(context.getString(R.string.chat_share_failed))
                                    }
                                }
                            }
                        }
                        "unshare" -> {
                            viewModel.unshareSession { ok ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) context.getString(R.string.chat_session_unshared) else context.getString(R.string.chat_session_unshare_failed)
                                    )
                                }
                            }
                        }
                        "undo" -> {
                            viewModel.undoMessage { ok ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) context.getString(R.string.chat_message_undone) else context.getString(R.string.chat_message_undo_failed)
                                    )
                                }
                            }
                        }
                        "redo" -> {
                            viewModel.redoMessage { ok ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) context.getString(R.string.chat_message_redone) else context.getString(R.string.chat_message_redo_failed)
                                    )
                                }
                            }
                        }
                        "rename" -> {
                            showRenameDialog = true
                        }
                        "shell" -> {
                            inputMode = ChatInputMode.SHELL.name
                        }
                        "review" -> {
                            viewModel.executeCommand("review") { ok ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) context.getString(R.string.chat_command_executed, "review") else context.getString(R.string.chat_command_failed, "review")
                                    )
                                }
                            }
                        }
                        else -> {
                            // Server command — execute via API
                            viewModel.executeCommand(cmd.name) { ok ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        if (ok) context.getString(R.string.chat_command_executed, cmd.name) else context.getString(R.string.chat_command_failed, cmd.name)
                                    )
                                }
                            }
                        }
                    }
                },
                contextWindow = uiState.contextWindow,
                lastContextTokens = uiState.lastContextTokens
            )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isTerminalMode -> {
                    // IME inset relative to content area. Some devices report 0 for
                    // ime.exclude(navigationBars), so keep a robust fallback to raw ime.
                    val imeBottomRaw = WindowInsets.ime.getBottom(density)
                    val navBottom = WindowInsets.navigationBars.getBottom(density)
                    val imeBottomPx = (imeBottomRaw - navBottom).coerceAtLeast(0).let { adjusted ->
                        if (adjusted == 0 && imeBottomRaw > 0) imeBottomRaw else adjusted
                    }
                    val imeBottomDp = with(density) { imeBottomPx.toDp() }
                    val overlayHeightDp = with(density) { terminalOverlayHeightPx.toDp() }

                    ModalNavigationDrawer(
                        drawerState = terminalDrawerState,
                        gesturesEnabled = true,
                        drawerContent = {
                            ModalDrawerSheet(
                                drawerContainerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
                                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                                drawerTonalElevation = 0.dp,
                                drawerShape = RoundedCornerShape(0.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .widthIn(min = 240.dp, max = 320.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(vertical = 8.dp)
                                            .imePadding(),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        items(terminalTabs, key = { it.id }) { tab ->
                                            val selected = tab.id == activeTerminalTabId
                                            val drawerItemShape = RoundedCornerShape(12.dp)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(drawerItemShape)
                                                    .then(
                                                        if (isAmoled && selected) {
                                                            Modifier.border(
                                                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                                                drawerItemShape
                                                            )
                                                        } else Modifier
                                                    )
                                            ) {
                                                NavigationDrawerItem(
                                                    label = {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Column(
                                                                modifier = Modifier.weight(1f),
                                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                                            ) {
                                                                Text(
                                                                    text = tab.title,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    style = MaterialTheme.typography.titleMedium,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                if (!tab.connected) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(999.dp),
                                                                        color = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                                        ) {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .size(6.dp)
                                                                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                                                            )
                                                                            Text(
                                                                                text = "Offline",
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                            if (!tab.connected) {
                                                                IconButton(
                                                                    onClick = {
                                                                        viewModel.reconnectTerminalTab(tab.id) { ok ->
                                                                            if (!ok) {
                                                                                coroutineScope.launch {
                                                                                    snackbarHostState.showSnackbar(context.getString(R.string.chat_terminal_connect_failed))
                                                                                }
                                                                            }
                                                                        }
                                                                    },
                                                                    modifier = Modifier.size(34.dp),
                                                                    colors = IconButtonDefaults.iconButtonColors(
                                                                        containerColor = if (isAmoled) {
                                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                                                        } else {
                                                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                                                        }
                                                                    )
                                                                ) {
                                                                    Icon(Icons.Default.Refresh, contentDescription = "Reconnect tab")
                                                                }
                                                            }
                                                            IconButton(
                                                                onClick = { viewModel.closeTerminalTab(tab.id) },
                                                                modifier = Modifier.size(34.dp),
                                                                colors = IconButtonDefaults.iconButtonColors(
                                                                    containerColor = if (isAmoled) {
                                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                                                                    } else {
                                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                                    }
                                                                )
                                                            ) {
                                                                Icon(Icons.Default.Close, contentDescription = "Close tab")
                                                            }
                                                        }
                                                    },
                                                    selected = selected,
                                                    shape = drawerItemShape,
                                                    colors = NavigationDrawerItemDefaults.colors(
                                                        selectedContainerColor = if (isAmoled) {
                                                            Color.Black
                                                        } else {
                                                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                                                        },
                                                        unselectedContainerColor = if (isAmoled) Color.Black else Color.Transparent,
                                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                                        unselectedTextColor = MaterialTheme.colorScheme.onSurface
                                                    ),
                                                    onClick = {
                                                        viewModel.switchTerminalTab(tab.id)
                                                        coroutineScope.launch { terminalDrawerState.close() }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider()

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.createTerminalTab { ok ->
                                                    if (!ok) {
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(context.getString(R.string.chat_terminal_connect_failed))
                                                        }
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("New")
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                keyboardController?.show()
                                                coroutineScope.launch { terminalDrawerState.close() }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
                                                contentColor = MaterialTheme.colorScheme.onSurface
                                            )
                                        ) {
                                            Icon(Icons.Default.Keyboard, contentDescription = null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Keyboard")
                                        }
                                    }

                                    }

                                    if (isAmoled) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .fillMaxHeight()
                                                .width(1.dp)
                                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SessionTerminalInline(
                                emulator = viewModel.terminalEmulator,
                                terminalVersion = terminalVersion,
                                connected = terminalConnected,
                                focusRequester = terminalFocusRequester,
                                onSendInput = ::sendTerminalChunk,
                                onPaste = ::pasteClipboardToTerminal,
                                onResize = { cols, rows ->
                                    viewModel.resizeTerminal(cols, rows)
                                },
                                fontSizeSp = terminalFontSizeSp,
                                onFontSizeChange = viewModel::setTerminalFontSize,
                                contentBottomPadding = overlayHeightDp + imeBottomDp,
                                modifier = Modifier.fillMaxSize()
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .fillMaxHeight()
                                    .padding(bottom = overlayHeightDp + imeBottomDp)
                                    .width(18.dp)
                                    .zIndex(0f)
                                    .pointerInput(terminalDrawerState) {
                                        detectTapGestures(
                                            onLongPress = {
                                                if (!terminalDrawerState.isOpen) {
                                                    coroutineScope.launch { terminalDrawerState.open() }
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(terminalDrawerState) {
                                        var dragged = 0f
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { _, dragAmount ->
                                                if (terminalDrawerState.isOpen) return@detectHorizontalDragGestures
                                                dragged += dragAmount
                                                if (dragged > 2f) {
                                                    coroutineScope.launch { terminalDrawerState.open() }
                                                    dragged = 0f
                                                }
                                            },
                                            onDragEnd = { dragged = 0f },
                                            onDragCancel = { dragged = 0f }
                                        )
                                    }
                            )

                        TerminalKeyboardOverlay(
                            connected = terminalConnected,
                            ctrlLatched = terminalCtrlLatched,
                            altLatched = terminalAltLatched,
                            cursorApp = viewModel.terminalEmulator.cursorKeysApplicationMode,
                            onToggleDrawer = { coroutineScope.launch { terminalDrawerState.apply { if (isOpen) close() else open() } } },
                            onToggleCtrl = { terminalCtrlLatched = !terminalCtrlLatched },
                            onToggleAlt = { terminalAltLatched = !terminalAltLatched },
                            onSendInput = ::sendTerminalChunk,
                            onCtrlC = { viewModel.sendTerminalInput("\u0003") },
                            onClear = { viewModel.clearTerminalBuffer() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .zIndex(1f)
                                    .fillMaxWidth()
                                    .padding(bottom = imeBottomDp)
                                    .onSizeChanged { terminalOverlayHeightPx = it.height }
                            )

                        }
                    }
                }
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    PulsingDotsIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null && uiState.messages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        ErrorPayloadContent(
                            text = uiState.error ?: stringResource(R.string.session_unknown_error),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            textColor = MaterialTheme.colorScheme.error,
                        )
                        Button(onClick = { viewModel.loadMessages() }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                uiState.messages.isEmpty() && !uiState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.chat_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = stringResource(R.string.chat_type_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
                else -> {
                    val messageSpacing = if (LocalCompactMessages.current) 4.dp else 12.dp
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(messageSpacing)
                    ) {
                        // "Load earlier messages" button at the top
                        if (uiState.hasOlderMessages) {
                            item(key = "load_older") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.isLoadingOlder) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PulsingDotsIndicator(
                                                dotSize = 6.dp,
                                                dotSpacing = 4.dp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = stringResource(R.string.chat_loading_earlier),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        TextButton(onClick = { viewModel.loadOlderMessages() }) {
                                            Text(stringResource(R.string.chat_load_earlier))
                                        }
                                    }
                                }
                            }
                        }

                        items(
                            uiState.messages,
                            key = { it.message.id }
                        ) { chatMessage ->
                            // Detect compaction trigger messages (user messages with Part.Compaction)
                            val isCompactionTrigger = chatMessage.isUser &&
                                chatMessage.parts.any { it is Part.Compaction }

                            // Show compact system-style divider for compaction triggers
                            // Long-press to revert (undo compaction and subsequent messages)
                            if (isCompactionTrigger) {
                                var showRevertDialog by remember { mutableStateOf(false) }

                                if (showRevertDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showRevertDialog = false },
                                        title = { Text(stringResource(R.string.chat_revert_title)) },
                                        text = { Text(stringResource(R.string.chat_revert_message)) },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    showRevertDialog = false
                                                    viewModel.revertMessage(chatMessage.message.id) { ok ->
                                                        coroutineScope.launch {
                                                            snackbarHostState.showSnackbar(
                                                                if (ok) context.getString(R.string.chat_message_reverted) else context.getString(R.string.chat_message_revert_failed)
                                                            )
                                                        }
                                                    }
                                                }
                                            ) {
                                                Text(stringResource(R.string.chat_revert), color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showRevertDialog = false }) {
                                                Text(stringResource(R.string.cancel))
                                            }
                                        }
                                    )
                                }

                                @OptIn(ExperimentalFoundationApi::class)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = { },
                                            onLongClick = { showRevertDialog = true }
                                        )
                                        .padding(vertical = 4.dp, horizontal = 32.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                    Text(
                                        text = stringResource(R.string.chat_summarized),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    )
                                }
                                return@items
                            }

                            ChatMessageBubble(
                                chatMessage = chatMessage,
                                onRevert = if (chatMessage.isUser) {
                                    {
                                        val revertText = chatMessage.parts
                                            .filterIsInstance<Part.Text>()
                                            .joinToString("\n") { it.text }
                                        viewModel.revertMessage(chatMessage.message.id, revertText) { ok ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    if (ok) context.getString(R.string.chat_message_reverted) else context.getString(R.string.chat_message_revert_failed)
                                                )
                                            }
                                        }
                                    }
                                } else null,
                                onCopyText = {
                                    val text = chatMessage.parts
                                        .filterIsInstance<Part.Text>()
                                        .joinToString("\n") { it.text }
                                    if (text.isNotBlank()) {
                                        clipboardManager.setText(
                                            androidx.compose.ui.text.AnnotatedString(text)
                                        )
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.chat_copied_clipboard))
                                        }
                                    }
                                }
                            )
                        }

                        // Revert banner
                        if (uiState.revert != null) {
                            item(key = "revert_banner") {
                                RevertBanner(onRedo = {
                                    viewModel.redoMessage { ok ->
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (ok) context.getString(R.string.chat_messages_restored) else context.getString(R.string.chat_message_redo_failed)
                                            )
                                        }
                                    }
                                })
                            }
                        }

                        // Pending permissions
                        items(
                            uiState.pendingPermissions,
                            key = { "perm_${it.id}" }
                        ) { permission ->
                            PermissionCard(
                                permission = permission,
                                onOnce = { viewModel.replyToPermission(permission.id, "once") },
                                onAlways = { viewModel.replyToPermission(permission.id, "always") },
                                onReject = { viewModel.replyToPermission(permission.id, "reject") }
                            )
                        }

                        // Pending questions
                        items(
                            uiState.pendingQuestions,
                            key = { "question_${it.id}" }
                        ) { question ->
                            QuestionCard(
                                question = question,
                                onSubmit = { answers ->
                                    viewModel.replyToQuestion(question.id, answers)
                                },
                                onReject = {
                                    viewModel.rejectQuestion(question.id)
                                }
                            )
                        }
                    }

                    // Scroll-to-bottom FAB
                    if (!isAtBottom && !autoScrollEnabled) {
                        SmallFloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    val lastIndex = listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1
                                    listState.scrollToItem(lastIndex)
                                    val lastItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                                    if (lastItem != null) {
                                        val viewport = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
                                        val overflow = lastItem.size - viewport
                                        if (overflow > 0) {
                                            listState.scrollBy(overflow.toFloat())
                                        }
                                    }
                                    autoScrollEnabled = true
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.chat_scroll_bottom),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Model picker dialog
    if (showModelPicker) {
        ModelPickerDialog(
            providers = uiState.providers,
            selectedProviderId = uiState.selectedProviderId,
            selectedModelId = uiState.selectedModelId,
            onSelect = { providerId, modelId ->
                viewModel.selectModel(providerId, modelId)
                showModelPicker = false
            },
            onDismiss = { showModelPicker = false }
        )
    }

    // Rename dialog
    if (showRenameDialog) {
        var renameText by remember { mutableStateOf(uiState.sessionTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.session_rename)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.session_rename_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameSession(renameText) { ok ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    if (ok) context.getString(R.string.chat_session_renamed) else context.getString(R.string.chat_session_rename_failed)
                                )
                            }
                        }
                        showRenameDialog = false
                    },
                    enabled = renameText.isNotBlank()
                ) {
                    Text(stringResource(R.string.session_rename_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Send confirmation dialog
    if (showSendConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showSendConfirmDialog = false
                pendingSendAction = null
            },
            title = { Text(stringResource(R.string.settings_confirm_send_title)) },
            text = { Text(stringResource(R.string.settings_confirm_send_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showSendConfirmDialog = false
                    pendingSendAction?.invoke()
                    pendingSendAction = null
                }) {
                    Text(stringResource(R.string.settings_send))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSendConfirmDialog = false
                    pendingSendAction = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    } // CompositionLocalProvider
}
