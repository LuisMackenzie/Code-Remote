package dev.mackenzie.coderemote.usecases.session

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ExecuteCommandUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        command: String,
        arguments: String = "",
        directory: String? = null
    ): Boolean = api.executeCommand(conn, sessionId, command, arguments, directory)
}
