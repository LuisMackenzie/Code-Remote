package dev.mackenzie.coderemote.usecases.draft

import dev.mackenzie.coderemote.data.repository.DraftRepository
import javax.inject.Inject

class ClearDraftUseCase @Inject constructor(private val repo: DraftRepository) {
    operator fun invoke(sessionId: String) = repo.clearDraft(sessionId)
}
