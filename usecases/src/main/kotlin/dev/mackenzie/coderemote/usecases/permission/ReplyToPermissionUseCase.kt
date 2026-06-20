package dev.mackenzie.coderemote.usecases.permission

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ReplyToPermissionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        requestId: String,
        reply: String,
        message: String? = null,
        directory: String? = null
    ): Boolean = api.replyToPermission(conn, requestId, reply, message, directory)
}
