package dev.mackenzie.coderemote.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mackenzie.coderemote.data.KeyValueStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val LOCALE_PREFS_NAME = "locale_prefs"

@Singleton
class DataStoreKeyValueStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>
) : KeyValueStorage {

    override fun observeString(key: String, default: String): Flow<String> =
        dataStore.data.map { it[stringPreferencesKey(key)] ?: default }

    override fun observeBoolean(key: String, default: Boolean): Flow<Boolean> =
        dataStore.data.map { it[booleanPreferencesKey(key)] ?: default }

    override fun observeInt(key: String, default: Int): Flow<Int> =
        dataStore.data.map { it[intPreferencesKey(key)] ?: default }

    override fun observeFloat(key: String, default: Float): Flow<Float> =
        dataStore.data.map { it[floatPreferencesKey(key)] ?: default }

    override fun observeStringSet(key: String, default: Set<String>): Flow<Set<String>> =
        dataStore.data.map { it[stringSetPreferencesKey(key)] ?: default }

    override suspend fun putString(key: String, value: String) {
        dataStore.edit { it[stringPreferencesKey(key)] = value }
    }

    override suspend fun putBoolean(key: String, value: Boolean) {
        dataStore.edit { it[booleanPreferencesKey(key)] = value }
    }

    override suspend fun putInt(key: String, value: Int) {
        dataStore.edit { it[intPreferencesKey(key)] = value }
    }

    override suspend fun putFloat(key: String, value: Float) {
        dataStore.edit { it[floatPreferencesKey(key)] = value }
    }

    override suspend fun putStringSet(key: String, value: Set<String>) {
        dataStore.edit { it[stringSetPreferencesKey(key)] = value }
    }

    override fun readStringSync(key: String, default: String): String {
        return context.getSharedPreferences(LOCALE_PREFS_NAME, Context.MODE_PRIVATE)
            .getString(key, default) ?: default
    }

    override fun putStringSync(key: String, value: String) {
        context.getSharedPreferences(LOCALE_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(key, value)
            .apply()
    }
}
