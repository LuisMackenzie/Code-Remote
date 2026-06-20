package dev.mackenzie.coderemote.usecases.project

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.data.api.ServerPaths
import javax.inject.Inject

class GetServerPathsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): ServerPaths = api.getServerPaths(conn)
}
