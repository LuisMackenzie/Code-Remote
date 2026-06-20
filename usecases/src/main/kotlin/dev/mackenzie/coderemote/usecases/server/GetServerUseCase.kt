package dev.mackenzie.coderemote.usecases.server

import dev.mackenzie.coderemote.data.repository.ServerRepository
import dev.mackenzie.coderemote.domain.model.ServerConfig
import javax.inject.Inject

class GetServerUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(serverId: String): ServerConfig? = repo.getServer(serverId)
}
