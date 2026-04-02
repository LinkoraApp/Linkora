package com.sakethh.linkora.data.remote.repository

import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.NewItemResponseDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.tag.CreateTagDTO
import com.sakethh.linkora.domain.dto.server.tag.RenameTagDTO
import com.sakethh.linkora.domain.repository.remote.RemoteTagsRepo
import com.sakethh.linkora.utils.postFlow
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

class RemoteTagsRepoImpl(
    private val syncServerClient: () -> HttpClient,
    private val baseUrl: suspend () -> String,
    private val authToken: suspend () -> String
) : RemoteTagsRepo {
    override suspend fun createATag(createTagDTO: CreateTagDTO): Flow<Result<NewItemResponseDTO>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.CREATE_TAG.name,
            outgoingBody = createTagDTO
        )
    }

    override suspend fun renameATag(renameTagDTO: RenameTagDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.RENAME_TAG.name,
            outgoingBody = renameTagDTO,
        )
    }

    override suspend fun deleteATag(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.DELETE_TAG.name,
            outgoingBody = idBasedDTO,
        )
    }
}