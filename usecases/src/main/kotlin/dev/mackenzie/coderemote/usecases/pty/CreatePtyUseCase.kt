package dev.mackenzie.coderemote.usecases.pty

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.PtyInfo
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class CreatePtyUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        title: String? = null,
        cwd: String? = null,
        directory: String? = null
    ): PtyInfo = api.createPty(conn, title, cwd, directory)
}
