package dev.mackenzie.coderemote.usecases.server

import dev.mackenzie.coderemote.data.repository.ServerRepository
import dev.mackenzie.coderemote.domain.model.ServerConfig
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetServersUseCase @Inject constructor(private val repo: ServerRepository) {
    operator fun invoke(): Flow<List<ServerConfig>> = repo.servers
}
