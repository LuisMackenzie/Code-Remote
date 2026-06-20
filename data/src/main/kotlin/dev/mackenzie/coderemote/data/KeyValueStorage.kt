package dev.mackenzie.coderemote.data

import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over persistent key-value storage (DataStore on Android).
 * Allows [SettingsRepository] and [ServerRepository] to live in pure JVM :data
 * without depending on androidx.datastore or Context.
 */
interface KeyValueStorage {

    fun observeString(key: String, default: String = ""): Flow<String>
    fun observeBoolean(key: String, default: Boolean = false): Flow<Boolean>
    fun observeInt(key: String, default: Int = 0): Flow<Int>
    fun observeFloat(key: String, default: Float = 0f): Flow<Float>
    fun observeStringSet(key: String, default: Set<String> = emptySet()): Flow<Set<String>>

    suspend fun putString(key: String, value: String)
    suspend fun putBoolean(key: String, value: Boolean)
    suspend fun putInt(key: String, value: Int)
    suspend fun putFloat(key: String, value: Float)
    suspend fun putStringSet(key: String, value: Set<String>)

    /**
     * Synchronous string read from a separate fast-read backing store
     * (e.g., SharedPreferences). Used for early access before DI is ready.
     */
    fun readStringSync(key: String, default: String = ""): String

    /**
     * Synchronous string write to the fast-read backing store.
     * Used for settings that must be readable before DI initializes.
     */
    fun putStringSync(key: String, value: String)
}
