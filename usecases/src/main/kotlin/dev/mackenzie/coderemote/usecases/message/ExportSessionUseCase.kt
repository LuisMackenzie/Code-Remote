package dev.mackenzie.coderemote.usecases.message

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import java.io.OutputStream
import javax.inject.Inject

class ExportSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        outputStream: OutputStream,
        onProgress: (Long) -> Unit = {}
    ) = api.exportSessionToStream(conn, sessionId, outputStream, onProgress)
}
