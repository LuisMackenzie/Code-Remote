package dev.mackenzie.coderemote.usecases.message

import dev.mackenzie.coderemote.data.api.ModelSelection
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.PromptPart
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

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
