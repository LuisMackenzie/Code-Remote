package dev.mackenzie.coderemote.usecases.session

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.domain.model.Session
import javax.inject.Inject

class ListSessionsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, directory: String? = null): List<Session> =
        api.listSessions(conn, directory)
}
