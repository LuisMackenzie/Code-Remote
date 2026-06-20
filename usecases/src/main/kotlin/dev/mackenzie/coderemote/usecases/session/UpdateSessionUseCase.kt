package dev.mackenzie.coderemote.usecases.session

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.domain.model.Session
import javax.inject.Inject

class UpdateSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String, title: String): Session =
        api.updateSession(conn, sessionId, title)
}
