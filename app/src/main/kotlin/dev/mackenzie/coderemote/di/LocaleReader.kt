package dev.mackenzie.coderemote.di

import android.content.Context

/**
 * Reads the stored app language synchronously from SharedPreferences.
 *
 * Used by [android.app.Activity.attachBaseContext] and service base contexts
 * before Hilt is initialized. Mirrors the fast-read path inside
 * [DataStoreKeyValueStorage.readStringSync] but can be called without DI.
 */
object LocaleReader {
    private const val LOCALE_PREFS_NAME = "locale_prefs"
    private const val LOCALE_PREFS_KEY = "app_language"

    fun getStoredLanguage(context: Context): String {
        return context.getSharedPreferences(LOCALE_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(LOCALE_PREFS_KEY, "") ?: ""
    }
}
