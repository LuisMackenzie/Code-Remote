package dev.mackenzie.coderemote.data.api

import dev.mackenzie.coderemote.domain.model.SseEvent
import kotlinx.coroutines.flow.Flow

/**
 * SSE (Server-Sent Events) client contract.
 *
 * Stateless — all connection info comes from the [ServerConnection] parameter.
 * The Ktor-backed implementation lives in :app.
 */
interface SseClient {

    /**
     * Connect to the global event stream.
     * Returns a Flow that emits SSE events.
     * The flow does NOT auto-reconnect internally — callers should handle
     * reconnection themselves (the service already does exponential backoff).
     */
    fun connectToGlobalEvents(conn: ServerConnection, directory: String? = null): Flow<SseEvent>
}
