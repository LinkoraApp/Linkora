package com.sakethh.linkora.network.repository

import com.sakethh.linkora.domain.repository.NetworkRepo
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.http.isSuccess

class NetworkRepoImpl(
    private val syncServerClient: () -> HttpClient,
) : NetworkRepo {
    override suspend fun isSyncServerReachable(
        serverUrl: String,
        token: String,
    ): Boolean {
        return syncServerClient()
            .get(serverUrl) {
                bearerAuth(token)
            }
            .status
            .isSuccess()
    }
}
