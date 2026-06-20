package dev.mackenzie.coderemote.data.api

/**
 * Abstraction over a PTY WebSocket connection.
 * The Ktor-backed implementation lives in :app; this interface allows
 * the API contract to live in pure JVM :data.
 */
interface PtySocket {
    suspend fun send(input: String)
    suspend fun close()
    suspend fun readLoop(onText: suspend (String) -> Unit)
}
