package com.sakethh.linkora.domain.repository

import com.sakethh.linkora.domain.Result
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.flow.Flow

interface NetworkRepo {
    suspend fun testServerConnection(
        serverUrl: String,
        token: String,
    ): Flow<Result<HttpResponse>>
}
