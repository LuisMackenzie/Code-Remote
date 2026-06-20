package dev.mackenzie.coderemote.usecases.config

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConfigResponse
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class GetGlobalConfigUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): ServerConfigResponse = api.getGlobalConfig(conn)
}
