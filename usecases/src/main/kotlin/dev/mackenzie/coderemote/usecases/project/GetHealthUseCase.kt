package dev.mackenzie.coderemote.usecases.project

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.domain.model.ServerHealth
import javax.inject.Inject

class GetHealthUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): ServerHealth = api.getHealth(conn)
}
