package dev.mackenzie.coderemote.usecases.file

import dev.mackenzie.coderemote.data.api.FileContent
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ReadFileUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, path: String): FileContent =
        api.readFile(conn, path)
}
