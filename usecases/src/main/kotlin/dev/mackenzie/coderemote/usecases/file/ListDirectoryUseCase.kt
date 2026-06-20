package dev.mackenzie.coderemote.usecases.file

import dev.mackenzie.coderemote.data.api.FileNode
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ListDirectoryUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        path: String = "",
        directory: String? = null
    ): List<FileNode> = api.listDirectory(conn, path, directory)
}
