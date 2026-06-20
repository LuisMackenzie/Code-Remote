package dev.mackenzie.coderemote.usecases.provider

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ProviderAuthMethod
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class GetProviderAuthMethodsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): Map<String, List<ProviderAuthMethod>> =
        api.getProviderAuthMethods(conn)
}
