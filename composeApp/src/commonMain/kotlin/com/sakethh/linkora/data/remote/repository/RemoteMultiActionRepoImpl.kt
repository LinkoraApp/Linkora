package com.sakethh.linkora.data.remote.repository

import com.sakethh.linkora.utils.postFlow
import com.sakethh.linkora.domain.DeleteMultipleItemsDTO
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.ArchiveMultipleItemsDTO
import com.sakethh.linkora.domain.dto.server.CopyItemsDTO
import com.sakethh.linkora.domain.dto.server.CopyItemsHTTPResponseDTO
import com.sakethh.linkora.domain.dto.server.MarkItemsRegularDTO
import com.sakethh.linkora.domain.dto.server.MoveItemsDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.repository.remote.RemoteMultiActionRepo
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

class RemoteMultiActionRepoImpl(
    private val syncServerClient:()-> HttpClient,
    private val baseUrl: suspend () -> String,
    private val authToken: suspend () -> String
) : RemoteMultiActionRepo {
    override suspend fun archiveMultipleItems(archiveMultipleItemsDTO: ArchiveMultipleItemsDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.ARCHIVE_MULTIPLE_ITEMS.name,
            outgoingBody = archiveMultipleItemsDTO
        )
    }

    override suspend fun deleteMultipleItems(deleteMultipleItemsDTO: DeleteMultipleItemsDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.DELETE_MULTIPLE_ITEMS.name,
            outgoingBody = deleteMultipleItemsDTO
        )
    }

    override suspend fun moveMultipleItems(moveItemsDTO: MoveItemsDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.MOVE_EXISTING_ITEMS.name,
            outgoingBody = moveItemsDTO
        )
    }

    override suspend fun copyMultipleItems(copyItemsDTO: CopyItemsDTO): Flow<Result<CopyItemsHTTPResponseDTO>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.COPY_EXISTING_ITEMS.name,
            outgoingBody = copyItemsDTO
        )
    }

    override suspend fun markItemsAsRegular(markItemsRegularDTO: MarkItemsRegularDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.UNARCHIVE_MULTIPLE_ITEMS.name,
            outgoingBody = markItemsRegularDTO
        )
    }
}