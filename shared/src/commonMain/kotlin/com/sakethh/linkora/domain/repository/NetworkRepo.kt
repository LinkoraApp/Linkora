package com.sakethh.linkora.domain.repository

interface NetworkRepo {
    suspend fun isSyncServerReachable(
        serverUrl: String,
        token: String,
    ): Boolean
}
