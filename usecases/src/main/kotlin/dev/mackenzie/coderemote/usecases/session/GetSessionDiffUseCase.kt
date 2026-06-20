package dev.mackenzie.coderemote.usecases.session

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.domain.model.FileDiff
import javax.inject.Inject

class GetSessionDiffUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): List<FileDiff> =
        api.getSessionDiff(conn, sessionId)
}
