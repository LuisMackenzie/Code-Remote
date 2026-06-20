package dev.mackenzie.coderemote.usecases.permission

import dev.mackenzie.coderemote.data.api.OpenCodeApi
import dev.mackenzie.coderemote.data.api.PermissionRequest
import dev.mackenzie.coderemote.data.api.ServerConnection
import javax.inject.Inject

class ListPendingPermissionsUseCase @Inject constructor(private val api: OpenCodeApi) {
    suspend operator fun invoke(conn: ServerConnection, directory: String? = null): List<PermissionRequest> =
        api.listPendingPermissions(conn, directory)
}
