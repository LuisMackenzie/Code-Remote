package dev.mackenzie.coderemote.data.api

/** Thrown when SSE returns 401 */
class SseAuthException(message: String) : Exception(message)

/** Thrown for non-2xx SSE responses */
class SseConnectionException(message: String) : Exception(message)
