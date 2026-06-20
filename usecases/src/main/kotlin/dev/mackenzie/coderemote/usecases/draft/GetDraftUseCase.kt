package dev.mackenzie.coderemote.usecases.draft

import dev.mackenzie.coderemote.data.repository.Draft
import dev.mackenzie.coderemote.data.repository.DraftRepository
import javax.inject.Inject

class GetDraftUseCase @Inject constructor(private val repo: DraftRepository) {
    operator fun invoke(sessionId: String): Draft? = repo.getDraft(sessionId)
}
