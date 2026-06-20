package dev.mackenzie.coderemote.data

/**
 * Abstraction over file-based storage in the app's internal directory.
 * Allows [DraftRepository] to live in pure JVM :data without Context.
 */
interface FileStorage {
    fun readText(fileName: String): String?
    fun writeText(fileName: String, content: String)
}
