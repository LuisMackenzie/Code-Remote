package dev.mackenzie.coderemote.usecases.session

import dev.mackenzie.coderemote.data.api.ModelSelection
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class RunShellCommandUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        command: String,
        agent: String,
        model: ModelSelection? = null,
        directory: String? = null
    ): Boolean = api.runShellCommand(conn, sessionId, command, agent, model, directory)
}
