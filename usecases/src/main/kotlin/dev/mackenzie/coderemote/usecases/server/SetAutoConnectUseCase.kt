package dev.mackenzie.coderemote.usecases.server

import dev.mackenzie.coderemote.data.repository.ServerRepository
import javax.inject.Inject

class SetAutoConnectUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(serverId: String, autoConnect: Boolean) =
        repo.setAutoConnect(serverId, autoConnect)
}
