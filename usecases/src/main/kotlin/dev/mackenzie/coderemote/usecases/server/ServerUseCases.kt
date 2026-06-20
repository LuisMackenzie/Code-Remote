package dev.mackenzie.coderemote.usecases.server

import dev.mackenzie.coderemote.data.repository.ServerRepository
import dev.mackenzie.coderemote.domain.model.ServerConfig
import dev.mackenzie.coderemote.domain.model.ServerHealth
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetServersUseCase @Inject constructor(private val repo: ServerRepository) {
    operator fun invoke(): Flow<List<ServerConfig>> = repo.servers
}

class GetServerUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(serverId: String): ServerConfig? = repo.getServer(serverId)
}

class AddServerUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(
        url: String,
        username: String = "opencode",
        password: String? = null,
        name: String? = null,
        autoConnect: Boolean = false
    ): ServerConfig = repo.addServer(url, username, password, name, autoConnect)
}

class UpdateServerUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(server: ServerConfig) = repo.updateServer(server)
}

class DeleteServerUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(serverId: String) = repo.deleteServer(serverId)
}

class SetAutoConnectUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(serverId: String, autoConnect: Boolean) =
        repo.setAutoConnect(serverId, autoConnect)
}

class CheckServerHealthUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(server: ServerConfig): Result<ServerHealth> = repo.checkHealth(server)
}
