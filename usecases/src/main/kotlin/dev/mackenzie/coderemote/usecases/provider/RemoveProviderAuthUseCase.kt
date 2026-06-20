package dev.mackenzie.coderemote.usecases.provider

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class RemoveProviderAuthUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, providerId: String): Boolean =
        api.removeProviderAuth(conn, providerId)
}
