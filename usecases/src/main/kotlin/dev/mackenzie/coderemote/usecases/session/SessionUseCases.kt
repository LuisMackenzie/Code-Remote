package dev.mackenzie.coderemote.usecases.session

import dev.mackenzie.coderemote.data.api.ModelSelection
import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.ServerConnection
import dev.mackenzie.coderemote.domain.model.FileDiff
import dev.mackenzie.coderemote.domain.model.Session
import javax.inject.Inject

class ListSessionsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, directory: String? = null): List<Session> =
        api.listSessions(conn, directory)
}

class GetSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): Session =
        api.getSession(conn, sessionId)
}

class GetSessionRawUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): String =
        api.getSessionRaw(conn, sessionId)
}

class CreateSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        title: String? = null,
        parentId: String? = null,
        directory: String? = null
    ): Session = api.createSession(conn, title, parentId, directory)
}

class DeleteSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): Boolean =
        api.deleteSession(conn, sessionId)
}

class UpdateSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String, title: String): Session =
        api.updateSession(conn, sessionId, title)
}

class AbortSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String, directory: String? = null): Boolean =
        api.abortSession(conn, sessionId, directory)
}

class GetSessionDiffUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): List<FileDiff> =
        api.getSessionDiff(conn, sessionId)
}

class ShareSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): Session =
        api.shareSession(conn, sessionId)
}

class UnshareSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): Session =
        api.unshareSession(conn, sessionId)
}

class SummarizeSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        providerId: String,
        modelId: String
    ): Boolean = api.summarizeSession(conn, sessionId, providerId, modelId)
}

class RevertSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String, messageId: String): Session =
        api.revertSession(conn, sessionId, messageId)
}

class UnrevertSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String): Session =
        api.unrevertSession(conn, sessionId)
}

class ForkSessionUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, sessionId: String, messageId: String? = null): Session =
        api.forkSession(conn, sessionId, messageId)
}

class ExecuteCommandUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        command: String,
        arguments: String = "",
        directory: String? = null
    ): Boolean = api.executeCommand(conn, sessionId, command, arguments, directory)
}

class RunShellCommandUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(
        conn: ServerConnection,
        sessionId: String,
        command: String,
        agent: String,
        model: ModelSelection? = null,
        directory: String? = null
    ): Boolean = api.runShellCommand(conn, sessionId, command, agent, model, directory)
}
