package dev.mackenzie.coderemote.usecases.project

import dev.mackenzie.coderemote.data.api.AgentInfo
import dev.mackenzie.coderemote.data.api.CommandInfo
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.data.api.ServerPaths
import dev.mackenzie.coderemote.domain.model.Project
import dev.mackenzie.coderemote.domain.model.ServerHealth
import javax.inject.Inject

class GetHealthUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): ServerHealth = api.getHealth(conn)
}

class GetServerPathsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): ServerPaths = api.getServerPaths(conn)
}

class ListProjectsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): List<Project> = api.listProjects(conn)
}

class GetCurrentProjectUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): Project = api.getCurrentProject(conn)
}

class ListAgentsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): List<AgentInfo> = api.listAgents(conn)
}

class ListCommandsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): List<CommandInfo> = api.listCommands(conn)
}
