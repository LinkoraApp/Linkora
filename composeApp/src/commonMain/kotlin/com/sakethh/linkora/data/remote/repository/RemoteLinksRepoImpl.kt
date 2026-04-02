package com.sakethh.linkora.data.remote.repository

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.NewItemResponseDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.link.AddLinkDTO
import com.sakethh.linkora.domain.dto.server.link.DeleteDuplicateLinksDTO
import com.sakethh.linkora.domain.dto.server.link.LinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateNoteOfALinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateTitleOfTheLinkDTO
import com.sakethh.linkora.domain.repository.remote.RemoteLinksRepo
import com.sakethh.linkora.utils.getFlow
import com.sakethh.linkora.utils.postFlow
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

class RemoteLinksRepoImpl(
    private val syncServerClient: () -> HttpClient,
    private val baseUrl: suspend () -> String,
    private val authToken: suspend () -> String
) : RemoteLinksRepo {
    override suspend fun addANewLink(addLinkDTO: AddLinkDTO): Flow<Result<NewItemResponseDTO>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.CREATE_A_NEW_LINK.name,
            outgoingBody = addLinkDTO
        )
    }

    override suspend fun deleteALink(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.DELETE_A_LINK.name,
            outgoingBody = idBasedDTO
        )
    }

    override suspend fun updateLinkTitle(
        updateTitleOfTheLinkDTO: UpdateTitleOfTheLinkDTO
    ): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.UPDATE_LINK_TITLE.name,
            outgoingBody = updateTitleOfTheLinkDTO
        )
    }

    override suspend fun updateALinkNote(
        updateNoteOfALinkDTO: UpdateNoteOfALinkDTO
    ): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.UPDATE_LINK_NOTE.name,
            outgoingBody = updateNoteOfALinkDTO
        )
    }

    override suspend fun archiveALink(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.ARCHIVE_LINK.name,
            outgoingBody = idBasedDTO
        )
    }

    override suspend fun unArchiveALink(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.UNARCHIVE_LINK.name,
            outgoingBody = idBasedDTO
        )
    }

    override suspend fun markALinkAsImp(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.MARK_AS_IMP.name,
            outgoingBody = idBasedDTO
        )
    }

    override suspend fun markALinkAsNonImp(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.UNMARK_AS_IMP.name,
            outgoingBody = idBasedDTO
        )
    }

    override suspend fun updateLink(linkDTO: LinkDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.UPDATE_LINK.name,
            outgoingBody = linkDTO
        )
    }

    override suspend fun deleteDuplicateLinks(deleteDuplicateLinksDTO: DeleteDuplicateLinksDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.DELETE_DUPLICATE_LINKS.name,
            outgoingBody = deleteDuplicateLinksDTO
        )
    }

    override suspend fun forceSetDefaultFolderToInternalIds(): Flow<Result<TimeStampBasedResponse>> {
        return getFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.FORCE_SET_DEFAULT_FOLDER_TO_INTERNAL_IDS.name,
        )
    }
}