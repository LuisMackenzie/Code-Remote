package dev.mackenzie.coderemote.usecases.provider

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ProviderCatalogResponse
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ListProviderCatalogUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection): ProviderCatalogResponse = api.listProviderCatalog(conn)
}
