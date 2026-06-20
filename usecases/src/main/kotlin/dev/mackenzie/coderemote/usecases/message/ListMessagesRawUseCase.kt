package dev.mackenzie.coderemote.usecases.message

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ListMessagesRawUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): String =
        api.listMessagesRaw(conn, sessionId)
}
