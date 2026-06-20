package dev.mackenzie.coderemote.usecases.server

import dev.mackenzie.coderemote.data.repository.ServerRepository
import dev.mackenzie.coderemote.domain.model.ServerConfig
import javax.inject.Inject

class AddServerUseCase @Inject constructor(private val repo: ServerRepository) {
    suspend operator fun invoke(
        url: String,
        username: String = "opencode",
        password: String? = null,
        name: String? = null,
        autoConnect: Boolean = false
    ): ServerConfig = repo.addServer(url, username, password, name, autoConnect)
}
