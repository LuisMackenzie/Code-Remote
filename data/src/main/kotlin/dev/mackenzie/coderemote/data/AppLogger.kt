package dev.mackenzie.coderemote.data

/**
 * Abstraction over platform logging (android.util.Log on Android, println on JVM).
 * Injected into :data classes that need logging without depending on Android.
 */
interface AppLogger {
    /** True when the app is in debug mode; used to gate verbose logs. */
    val isDebug: Boolean

    fun d(tag: String, message: String)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
