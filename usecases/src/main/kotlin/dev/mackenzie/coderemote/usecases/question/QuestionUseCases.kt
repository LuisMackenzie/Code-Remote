package dev.mackenzie.coderemote.usecases.question

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.QuestionRequest
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ListPendingQuestionsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, directory: String? = null): List<QuestionRequest> =
        api.listPendingQuestions(conn, directory)
}

class ReplyToQuestionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        requestId: String,
        answers: List<List<String>>,
        directory: String? = null
    ): Boolean = api.replyToQuestion(conn, requestId, answers, directory)
}

class RejectQuestionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        requestId: String,
        directory: String? = null
    ): Boolean = api.rejectQuestion(conn, requestId, directory)
}
