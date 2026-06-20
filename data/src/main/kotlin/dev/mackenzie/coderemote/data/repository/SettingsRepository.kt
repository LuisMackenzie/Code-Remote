package dev.mackenzie.coderemote.data.repository

import dev.mackenzie.coderemote.data.DataConstants
import dev.mackenzie.coderemote.data.KeyValueStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val LOCALE_PREFS_KEY = "app_language"

/**
 * App-wide settings stored in [KeyValueStorage].
 *
 * All keys are private constants; access is via typed Flows and setters.
 * The synchronous locale read ([getStoredLanguage]) is available for
 * early access before DI initializes.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val storage: KeyValueStorage
) {
    fun getStoredLanguage(): String = storage.readStringSync(LOCALE_PREFS_KEY, "")

    val appLanguage: Flow<String> = storage.observeString(LANGUAGE_KEY, "")

    val appTheme: Flow<String> = storage.observeString(THEME_KEY, "system")

    suspend fun setAppLanguage(languageCode: String) {
        storage.putStringSync(LOCALE_PREFS_KEY, languageCode)
        storage.putString(LANGUAGE_KEY, languageCode)
    }

    suspend fun setAppTheme(theme: String) {
        storage.putString(THEME_KEY, theme)
    }

    val dynamicColor: Flow<Boolean> = storage.observeBoolean(DYNAMIC_COLOR_KEY, true)

    suspend fun setDynamicColor(enabled: Boolean) {
        storage.putBoolean(DYNAMIC_COLOR_KEY, enabled)
    }

    val chatFontSize: Flow<String> = storage.observeString(FONT_SIZE_KEY, "medium")

    suspend fun setChatFontSize(size: String) {
        storage.putString(FONT_SIZE_KEY, size)
    }

    val notificationsEnabled: Flow<Boolean> = storage.observeBoolean(NOTIFICATIONS_KEY, true)

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        storage.putBoolean(NOTIFICATIONS_KEY, enabled)
    }

    val initialMessageCount: Flow<Int> = storage.observeInt(INITIAL_MESSAGE_COUNT_KEY, 50)

    suspend fun setInitialMessageCount(count: Int) {
        storage.putInt(INITIAL_MESSAGE_COUNT_KEY, count)
    }

    val codeWordWrap: Flow<Boolean> = storage.observeBoolean(CODE_WORD_WRAP_KEY, false)

    suspend fun setCodeWordWrap(enabled: Boolean) {
        storage.putBoolean(CODE_WORD_WRAP_KEY, enabled)
    }

    val confirmBeforeSend: Flow<Boolean> = storage.observeBoolean(CONFIRM_BEFORE_SEND_KEY, false)

    suspend fun setConfirmBeforeSend(enabled: Boolean) {
        storage.putBoolean(CONFIRM_BEFORE_SEND_KEY, enabled)
    }

    val amoledDark: Flow<Boolean> = storage.observeBoolean(AMOLED_DARK_KEY, false)

    suspend fun setAmoledDark(enabled: Boolean) {
        storage.putBoolean(AMOLED_DARK_KEY, enabled)
    }

    val compactMessages: Flow<Boolean> = storage.observeBoolean(COMPACT_MESSAGES_KEY, false)

    suspend fun setCompactMessages(enabled: Boolean) {
        storage.putBoolean(COMPACT_MESSAGES_KEY, enabled)
    }

    val collapseTools: Flow<Boolean> = storage.observeBoolean(COLLAPSE_TOOLS_KEY, false)

    suspend fun setCollapseTools(enabled: Boolean) {
        storage.putBoolean(COLLAPSE_TOOLS_KEY, enabled)
    }

    val hapticFeedback: Flow<Boolean> = storage.observeBoolean(HAPTIC_FEEDBACK_KEY, true)

    suspend fun setHapticFeedback(enabled: Boolean) {
        storage.putBoolean(HAPTIC_FEEDBACK_KEY, enabled)
    }

    val reconnectMode: Flow<String> = storage.observeString(RECONNECT_MODE_KEY, "normal")

    suspend fun setReconnectMode(mode: String) {
        storage.putString(RECONNECT_MODE_KEY, mode)
    }

    val keepScreenOn: Flow<Boolean> = storage.observeBoolean(KEEP_SCREEN_ON_KEY, false)

    suspend fun setKeepScreenOn(enabled: Boolean) {
        storage.putBoolean(KEEP_SCREEN_ON_KEY, enabled)
    }

    val silentNotifications: Flow<Boolean> = storage.observeBoolean(SILENT_NOTIFICATIONS_KEY, false)

    suspend fun setSilentNotifications(enabled: Boolean) {
        storage.putBoolean(SILENT_NOTIFICATIONS_KEY, enabled)
    }

    val compressImageAttachments: Flow<Boolean> = storage.observeBoolean(COMPRESS_IMAGE_ATTACHMENTS_KEY, true)

    suspend fun setCompressImageAttachments(enabled: Boolean) {
        storage.putBoolean(COMPRESS_IMAGE_ATTACHMENTS_KEY, enabled)
    }

    val imageAttachmentMaxLongSide: Flow<Int> = storage.observeInt(IMAGE_ATTACHMENT_MAX_LONG_SIDE_KEY, 1440)
        .map { value -> if (value <= 0) 0 else value.coerceIn(720, 4096) }

    suspend fun setImageAttachmentMaxLongSide(px: Int) {
        storage.putInt(
            IMAGE_ATTACHMENT_MAX_LONG_SIDE_KEY,
            if (px <= 0) 0 else px.coerceIn(720, 4096)
        )
    }

    val imageAttachmentWebpQuality: Flow<Int> = storage.observeInt(IMAGE_ATTACHMENT_WEBP_QUALITY_KEY, 60)
        .map { it.coerceIn(1, 100) }

    suspend fun setImageAttachmentWebpQuality(quality: Int) {
        storage.putInt(IMAGE_ATTACHMENT_WEBP_QUALITY_KEY, quality.coerceIn(1, 100))
    }

    val showLocalRuntime: Flow<Boolean> = storage.observeBoolean(SHOW_LOCAL_RUNTIME_KEY, true)

    suspend fun setShowLocalRuntime(enabled: Boolean) {
        storage.putBoolean(SHOW_LOCAL_RUNTIME_KEY, enabled)
    }

    val terminalFontSize: Flow<Float> = storage.observeFloat(TERMINAL_FONT_SIZE_KEY, 13f)
        .map { it.coerceIn(6f, 20f) }

    suspend fun setTerminalFontSize(size: Float) {
        storage.putFloat(TERMINAL_FONT_SIZE_KEY, size.coerceIn(6f, 20f))
    }

    val localSetupCompleted: Flow<Boolean> = storage.observeBoolean(LOCAL_SETUP_COMPLETED_KEY, false)

    suspend fun setLocalSetupCompleted(completed: Boolean) {
        storage.putBoolean(LOCAL_SETUP_COMPLETED_KEY, completed)
    }

    val localProxyEnabled: Flow<Boolean> = storage.observeBoolean(LOCAL_PROXY_ENABLED_KEY, false)

    suspend fun setLocalProxyEnabled(enabled: Boolean) {
        storage.putBoolean(LOCAL_PROXY_ENABLED_KEY, enabled)
    }

    val localProxyUrl: Flow<String> = storage.observeString(LOCAL_PROXY_URL_KEY, "")

    suspend fun setLocalProxyUrl(url: String) {
        storage.putString(LOCAL_PROXY_URL_KEY, url.trim())
    }

    val localProxyNoProxy: Flow<String> = storage.observeString(
        LOCAL_PROXY_NO_PROXY_KEY,
        DataConstants.DEFAULT_NO_PROXY_LIST
    )

    suspend fun setLocalProxyNoProxy(value: String) {
        val normalized = value
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(",")
        storage.putString(
            LOCAL_PROXY_NO_PROXY_KEY,
            if (normalized.isBlank()) DataConstants.DEFAULT_NO_PROXY_LIST else normalized
        )
    }

    val localServerAllowLan: Flow<Boolean> = storage.observeBoolean(LOCAL_SERVER_ALLOW_LAN_KEY, false)

    suspend fun setLocalServerAllowLan(enabled: Boolean) {
        storage.putBoolean(LOCAL_SERVER_ALLOW_LAN_KEY, enabled)
    }

    val localServerUsername: Flow<String> = storage.observeString(LOCAL_SERVER_USERNAME_KEY, "")

    suspend fun setLocalServerUsername(value: String) {
        storage.putString(LOCAL_SERVER_USERNAME_KEY, value.trim())
    }

    val localServerPassword: Flow<String> = storage.observeString(LOCAL_SERVER_PASSWORD_KEY, "")

    suspend fun setLocalServerPassword(value: String) {
        storage.putString(LOCAL_SERVER_PASSWORD_KEY, value.trim())
    }

    val localServerRunInBackground: Flow<Boolean> = storage.observeBoolean(LOCAL_SERVER_RUN_IN_BACKGROUND_KEY, true)

    suspend fun setLocalServerRunInBackground(enabled: Boolean) {
        storage.putBoolean(LOCAL_SERVER_RUN_IN_BACKGROUND_KEY, enabled)
    }

    val localServerAutoStart: Flow<Boolean> = storage.observeBoolean(LOCAL_SERVER_AUTO_START_KEY, false)

    suspend fun setLocalServerAutoStart(enabled: Boolean) {
        storage.putBoolean(LOCAL_SERVER_AUTO_START_KEY, enabled)
    }

    val localServerStartupTimeoutSec: Flow<Int> = storage.observeInt(LOCAL_SERVER_STARTUP_TIMEOUT_SEC_KEY, 30)
        .map { it.coerceIn(10, 120) }

    suspend fun setLocalServerStartupTimeoutSec(value: Int) {
        storage.putInt(LOCAL_SERVER_STARTUP_TIMEOUT_SEC_KEY, value.coerceIn(10, 120))
    }

    fun hiddenModels(serverId: String): Flow<Set<String>> =
        storage.observeStringSet(serverModelHiddenKey(serverId), emptySet())

    suspend fun setModelVisibility(serverId: String, providerId: String, modelId: String, visible: Boolean) {
        val key = "$providerId:$modelId"
        val prefsKey = serverModelHiddenKey(serverId)
        val current = storage.observeStringSet(prefsKey, emptySet()).first()
        storage.putStringSet(
            prefsKey,
            if (visible) current - key else current + key
        )
    }

    private fun serverModelHiddenKey(serverId: String) = SERVER_MODEL_HIDDEN_PREFIX + serverId

    companion object {
        private const val LANGUAGE_KEY = "app_language"
        private const val THEME_KEY = "app_theme"
        private const val DYNAMIC_COLOR_KEY = "dynamic_color"
        private const val FONT_SIZE_KEY = "chat_font_size"
        private const val NOTIFICATIONS_KEY = "notifications_enabled"
        private const val INITIAL_MESSAGE_COUNT_KEY = "initial_message_count"
        private const val CODE_WORD_WRAP_KEY = "code_word_wrap"
        private const val CONFIRM_BEFORE_SEND_KEY = "confirm_before_send"
        private const val AMOLED_DARK_KEY = "amoled_dark"
        private const val COMPACT_MESSAGES_KEY = "compact_messages"
        private const val COLLAPSE_TOOLS_KEY = "collapse_tools"
        private const val HAPTIC_FEEDBACK_KEY = "haptic_feedback"
        private const val RECONNECT_MODE_KEY = "reconnect_mode"
        private const val KEEP_SCREEN_ON_KEY = "keep_screen_on"
        private const val SILENT_NOTIFICATIONS_KEY = "silent_notifications"
        private const val COMPRESS_IMAGE_ATTACHMENTS_KEY = "compress_image_attachments"
        private const val IMAGE_ATTACHMENT_MAX_LONG_SIDE_KEY = "image_attachment_max_long_side"
        private const val IMAGE_ATTACHMENT_WEBP_QUALITY_KEY = "image_attachment_webp_quality"
        private const val SHOW_LOCAL_RUNTIME_KEY = "show_local_runtime"
        private const val TERMINAL_FONT_SIZE_KEY = "terminal_font_size"
        private const val LOCAL_SETUP_COMPLETED_KEY = "local_setup_completed"
        private const val LOCAL_PROXY_ENABLED_KEY = "local_proxy_enabled"
        private const val LOCAL_PROXY_URL_KEY = "local_proxy_url"
        private const val LOCAL_PROXY_NO_PROXY_KEY = "local_proxy_no_proxy"
        private const val LOCAL_SERVER_ALLOW_LAN_KEY = "local_server_allow_lan"
        private const val LOCAL_SERVER_USERNAME_KEY = "local_server_username"
        private const val LOCAL_SERVER_PASSWORD_KEY = "local_server_password"
        private const val LOCAL_SERVER_RUN_IN_BACKGROUND_KEY = "local_server_run_in_background"
        private const val LOCAL_SERVER_AUTO_START_KEY = "local_server_auto_start"
        private const val LOCAL_SERVER_STARTUP_TIMEOUT_SEC_KEY = "local_server_startup_timeout_sec"

        private const val SERVER_MODEL_HIDDEN_PREFIX = "server_model_hidden_"
    }
}
