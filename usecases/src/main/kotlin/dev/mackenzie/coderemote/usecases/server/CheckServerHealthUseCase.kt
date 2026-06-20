package dev.mackenzie.coderemote.usecases.server

import dev.mackenzie.coderemote.data.repository.ServerRepository
import dev.mackenzie.coderemote.domain.model.ServerConfig
import dev.mackenzie.coderemote.domain.model.ServerHealth
import javax.inject.Inject

class CheckServerHealthUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(server: ServerConfig): Result<ServerHealth> = repo.checkHealth(server)
}
