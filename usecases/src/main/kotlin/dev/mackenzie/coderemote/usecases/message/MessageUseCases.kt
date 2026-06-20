package dev.mackenzie.coderemote.usecases.message

import dev.mackenzie.coderemote.data.api.ModelSelection
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.PromptPart
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.domain.model.MessageWithParts
import java.io.OutputStream
import javax.inject.Inject

class ListMessagesUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        limit: Int? = null
    ): List<MessageWithParts> = api.listMessages(conn, sessionId, limit)
}

class ListMessagesRawUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): String =
        api.listMessagesRaw(conn, sessionId)
}

class GetMessageUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String, messageId: String): MessageWithParts =
        api.getMessage(conn, sessionId, messageId)
}

class SendMessageUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        parts: List<PromptPart>,
        model: ModelSelection? = null,
        agent: String? = null,
        variant: String? = null,
        directory: String? = null
    ) = api.promptAsync(conn, sessionId, parts, model, agent, variant, directory)
}

class ExportSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        outputStream: OutputStream,
        onProgress: (Long) -> Unit = {}
    ) = api.exportSessionToStream(conn, sessionId, outputStream, onProgress)
}
