package dev.mackenzie.coderemote.usecases.file

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.SearchMatch
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class SearchTextUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, pattern: String): List<SearchMatch> =
        api.searchText(conn, pattern)
}
