package dev.mackenzie.coderemote.usecases.question

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class RejectQuestionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        requestId: String,
        directory: String? = null
    ): Boolean = api.rejectQuestion(conn, requestId, directory)
}
