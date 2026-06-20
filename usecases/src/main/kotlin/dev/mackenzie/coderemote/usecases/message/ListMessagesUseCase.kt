package dev.mackenzie.coderemote.usecases.message

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.domain.model.MessageWithParts
import javax.inject.Inject

class ListMessagesUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        limit: Int? = null
    ): List<MessageWithParts> = api.listMessages(conn, sessionId, limit)
}
