package dev.mackenzie.coderemote.usecases.provider

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ProviderOauthAuthorization
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class AuthorizeProviderOauthUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        providerId: String,
        methodIndex: Int
    ): ProviderOauthAuthorization? = api.authorizeProviderOauth(conn, providerId, methodIndex)
}
