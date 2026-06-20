package dev.mackenzie.coderemote.usecases.pty

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.PtySocket
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class OpenPtySocketUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        ptyId: String,
        cursor: Int = -1,
        directory: String? = null
    ): PtySocket = api.openPtySocket(conn, ptyId, cursor, directory)
}
