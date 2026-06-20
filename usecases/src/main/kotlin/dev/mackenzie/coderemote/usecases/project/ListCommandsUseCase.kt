package dev.mackenzie.coderemote.usecases.project

import dev.mackenzie.coderemote.data.api.CommandInfo
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ListCommandsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): List<CommandInfo> = api.listCommands(conn)
}
