package dev.mackenzie.coderemote.usecases.config

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConfigPatch
import dev.mackenzie.coderemote.data.api.ServerConfigResponse
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class UpdateConfigUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, patch: ServerConfigPatch): ServerConfigResponse =
        api.updateConfig(conn, patch)
}
