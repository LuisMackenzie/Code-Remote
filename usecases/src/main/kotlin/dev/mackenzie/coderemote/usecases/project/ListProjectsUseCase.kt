package dev.mackenzie.coderemote.usecases.project

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.domain.model.Project
import javax.inject.Inject

class ListProjectsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): List<Project> = api.listProjects(conn)
}
