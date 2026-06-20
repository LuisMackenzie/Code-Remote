package dev.mackenzie.coderemote.usecases.file

import dev.mackenzie.coderemote.data.api.FileContent
import dev.mackenzie.coderemote.data.api.FileNode
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.SearchMatch
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class SearchTextUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, pattern: String): List<SearchMatch> =
        api.searchText(conn, pattern)
}

class FindFilesUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        query: String,
        type: String? = null,
        directory: String? = null,
        limit: Int? = null,
        dirs: String? = null
    ): List<String> = api.findFiles(conn, query, type, directory, limit, dirs)
}

class ReadFileUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, path: String): FileContent =
        api.readFile(conn, path)
}

class ListDirectoryUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        path: String = "",
        directory: String? = null
    ): List<FileNode> = api.listDirectory(conn, path, directory)
}
