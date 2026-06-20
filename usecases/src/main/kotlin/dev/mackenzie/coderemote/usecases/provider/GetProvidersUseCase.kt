package dev.mackenzie.coderemote.usecases.provider

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ProvidersResponse
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class GetProvidersUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): ProvidersResponse = api.getProviders(conn)
}
