package com.sakethh.linkora.data.remote.repository

import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.NewItemResponseDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.folder.AddFolderDTO
import com.sakethh.linkora.domain.dto.server.folder.ChangeParentFolderDTO
import com.sakethh.linkora.domain.dto.server.folder.FolderDTO
import com.sakethh.linkora.domain.dto.server.folder.MarkSelectedFoldersAsRootDTO
import com.sakethh.linkora.domain.dto.server.folder.UpdateFolderNameDTO
import com.sakethh.linkora.domain.dto.server.folder.UpdateFolderNoteDTO
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.utils.postFlow
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.Flow

class RemoteFoldersRepoImpl(
    private val syncServerClient: () -> HttpClient,
    private val baseUrl: suspend () -> String,
    private val authToken: suspend () -> String
) : RemoteFoldersRepo {

    override suspend fun createFolder(addFolderDTO: AddFolderDTO): Flow<Result<NewItemResponseDTO>> {
        return postFlow<AddFolderDTO, NewItemResponseDTO>(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.CREATE_FOLDER.name,
            outgoingBody = addFolderDTO,
        )
    }

    override suspend fun updateFolder(folderDTO: FolderDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken, endPoint = SyncServerRoute.UPDATE_FOLDER.name,
            outgoingBody = folderDTO,
        )
    }

    override suspend fun deleteFolder(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken, endPoint = SyncServerRoute.DELETE_FOLDER.name,
            outgoingBody = idBasedDTO,
        )
    }

    override suspend fun markAsArchive(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.MARK_FOLDER_AS_ARCHIVE.name,
            outgoingBody = idBasedDTO
        )
    }

    override suspend fun markAsRegularFolder(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.MARK_AS_REGULAR_FOLDER.name,
            outgoingBody = idBasedDTO
        )
    }

    override suspend fun changeParentFolder(
        changeParentFolderDTO: ChangeParentFolderDTO
    ): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.CHANGE_PARENT_FOLDER.name,
            outgoingBody = changeParentFolderDTO
        )
    }

    override suspend fun updateFolderName(
        updateFolderNameDTO: UpdateFolderNameDTO
    ): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.UPDATE_FOLDER_NAME.name,
            outgoingBody = updateFolderNameDTO
        )
    }

    override suspend fun updateFolderNote(
        updateFolderNoteDTO: UpdateFolderNoteDTO
    ): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.UPDATE_FOLDER_NOTE.name,
            outgoingBody = updateFolderNoteDTO
        )
    }

    override suspend fun deleteFolderNote(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.DELETE_FOLDER_NOTE.name,
            outgoingBody = idBasedDTO
        )
    }

    override suspend fun markSelectedFoldersAsRoot(markSelectedFoldersAsRootDTO: MarkSelectedFoldersAsRootDTO): Flow<Result<TimeStampBasedResponse>> {
        return postFlow(
            syncServerClient = syncServerClient,
            baseUrl = baseUrl,
            authToken = authToken,
            endPoint = SyncServerRoute.MARK_FOLDERS_AS_ROOT.name,
            outgoingBody = markSelectedFoldersAsRootDTO
        )
    }
}