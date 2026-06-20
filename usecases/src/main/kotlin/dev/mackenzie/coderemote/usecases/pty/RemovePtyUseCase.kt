package dev.mackenzie.coderemote.usecases.pty

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class RemovePtyUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, ptyId: String): Boolean =
        api.removePty(conn, ptyId)
}
