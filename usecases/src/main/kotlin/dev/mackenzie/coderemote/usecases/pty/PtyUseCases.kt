package dev.mackenzie.coderemote.usecases.pty

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.PtyInfo
import dev.mackenzie.coderemote.data.api.PtySocket
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

class RemovePtyUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, ptyId: String): Boolean =
        api.removePty(conn, ptyId)
}

class UpdatePtySizeUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        ptyId: String,
        cols: Int,
        rows: Int,
        directory: String? = null
    ): Boolean = api.updatePtySize(conn, ptyId, cols, rows, directory)
}

class OpenPtySocketUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        ptyId: String,
        cursor: Int = -1,
        directory: String? = null
    ): PtySocket = api.openPtySocket(conn, ptyId, cursor, directory)
}
