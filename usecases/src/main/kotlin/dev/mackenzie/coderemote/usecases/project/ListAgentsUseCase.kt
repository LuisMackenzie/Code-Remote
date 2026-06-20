package dev.mackenzie.coderemote.usecases.project

import dev.mackenzie.coderemote.data.api.AgentInfo
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ListAgentsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): List<AgentInfo> = api.listAgents(conn)
}
