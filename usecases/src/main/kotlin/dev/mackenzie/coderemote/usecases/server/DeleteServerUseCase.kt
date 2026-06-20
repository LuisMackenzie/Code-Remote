package dev.mackenzie.coderemote.usecases.server

import dev.mackenzie.coderemote.data.repository.ServerRepository
import javax.inject.Inject

class DeleteServerUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(serverId: String) = repo.deleteServer(serverId)
}
