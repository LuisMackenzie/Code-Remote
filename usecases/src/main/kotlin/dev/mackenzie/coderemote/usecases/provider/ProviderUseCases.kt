package dev.mackenzie.coderemote.usecases.provider

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ProviderAuthMethod
import dev.mackenzie.coderemote.data.api.ProviderCatalogResponse
import dev.mackenzie.coderemote.data.api.ProviderOauthAuthorization
import dev.mackenzie.coderemote.data.api.ProvidersResponse
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class GetProvidersUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): ProvidersResponse = api.getProviders(conn)
}

class ListProviderCatalogUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): ProviderCatalogResponse = api.listProviderCatalog(conn)
}

class GetProviderAuthMethodsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): Map<String, List<ProviderAuthMethod>> =
        api.getProviderAuthMethods(conn)
}

class AuthorizeProviderOauthUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        providerId: String,
        methodIndex: Int
    ): ProviderOauthAuthorization? = api.authorizeProviderOauth(conn, providerId, methodIndex)
}

class CompleteProviderOauthUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        providerId: String,
        methodIndex: Int,
        code: String? = null
    ): Boolean = api.completeProviderOauth(conn, providerId, methodIndex, code)
}

class SetProviderApiKeyUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, providerId: String, apiKey: String): Boolean =
        api.setProviderApiKey(conn, providerId, apiKey)
}

class RemoveProviderAuthUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, providerId: String): Boolean =
        api.removeProviderAuth(conn, providerId)
}

class DisposeGlobalUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): Boolean = api.disposeGlobal(conn)
}
