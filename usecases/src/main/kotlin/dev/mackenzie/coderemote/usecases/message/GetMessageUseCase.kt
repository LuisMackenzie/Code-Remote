package dev.mackenzie.coderemote.usecases.message

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.domain.model.MessageWithParts
import javax.inject.Inject

class GetMessageUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String, messageId: String): MessageWithParts =
        api.getMessage(conn, sessionId, messageId)
}
