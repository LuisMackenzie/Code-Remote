package dev.mackenzie.coderemote.usecases.question

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ReplyToQuestionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        requestId: String,
        answers: List<List<String>>,
        directory: String? = null
    ): Boolean = api.replyToQuestion(conn, requestId, answers, directory)
}
