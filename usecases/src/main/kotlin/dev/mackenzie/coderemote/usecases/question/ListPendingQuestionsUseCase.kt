package dev.mackenzie.coderemote.usecases.question

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.QuestionRequest
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ListPendingQuestionsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, directory: String? = null): List<QuestionRequest> =
        api.listPendingQuestions(conn, directory)
}
