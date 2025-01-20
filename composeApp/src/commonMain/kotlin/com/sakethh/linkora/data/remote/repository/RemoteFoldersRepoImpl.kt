package com.sakethh.linkora.data.remote.repository

import com.sakethh.linkora.common.utils.catchAsExceptionAndEmitFailure
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.domain.Message
import com.sakethh.linkora.domain.RemoteRoute
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.ChangeParentFolderDTO
import com.sakethh.linkora.domain.dto.FolderDTO
import com.sakethh.linkora.domain.dto.NewItemResponseDTO
import com.sakethh.linkora.domain.dto.UpdateFolderNameDTO
import com.sakethh.linkora.domain.dto.UpdateFolderNoteDTO
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RemoteFoldersRepoImpl(
    private val httpClient: HttpClient,
    private val baseUrl: () -> String,
    private val authToken: () -> String,
    private val foldersDao: FoldersDao
) : RemoteFoldersRepo {

    private suspend inline fun <reified IncomingBody> HttpResponse.handleResponseBody(): Result<IncomingBody> {
        return if (this.status.isSuccess().not()) {
            Result.Failure(this.status.value.toString() + " " + this.status.description)
        } else {
            Result.Success(this.body<IncomingBody>())
        }
    }

    private inline fun <reified OutgoingBody, reified IncomingBody> postFlow(
        endPoint: String,
        body: OutgoingBody,
        contentType: ContentType = ContentType.Application.Json
    ): Flow<Result<IncomingBody>> {
        return flow {
            emit(Result.Loading())
            httpClient.post(baseUrl() + endPoint) {
                bearerAuth(authToken())
                contentType(contentType)
                setBody(body)
            }.handleResponseBody<IncomingBody>().run {
                emit(this)
            }
        }.catchAsExceptionAndEmitFailure()
    }

    override suspend fun createFolder(folderDTO: FolderDTO): Flow<Result<NewItemResponseDTO>> {
        return postFlow<FolderDTO, NewItemResponseDTO>(
            endPoint = RemoteRoute.Folder.CREATE_FOLDER.name,
            body = folderDTO,
        )
    }

    override suspend fun deleteFolder(folderId: Long): Flow<Result<Message>> {
        return postFlow(
            endPoint = RemoteRoute.Folder.DELETE_FOLDER.name,
            body = folderId,
        )
    }

    override suspend fun markAsArchive(folderId: Long): Flow<Result<Message>> {
        return postFlow(
            endPoint = RemoteRoute.Folder.MARK_AS_ARCHIVE.name, body = folderId.toString()
        )
    }

    override suspend fun markAsRegularFolder(folderId: Long): Flow<Result<Message>> {
        return postFlow(
            endPoint = RemoteRoute.Folder.MARK_AS_REGULAR_FOLDER.name,
            body = folderId,
        )
    }

    override suspend fun changeParentFolder(
        folderId: Long, newParentFolderId: Long?
    ): Flow<Result<Message>> {
        return postFlow(
            endPoint = RemoteRoute.Folder.CHANGE_PARENT_FOLDER.name,
            body = ChangeParentFolderDTO(folderId, newParentFolderId),
        )
    }

    override suspend fun updateFolderName(
        folderId: Long, newFolderName: String
    ): Flow<Result<Message>> {
        return postFlow(
            endPoint = RemoteRoute.Folder.UPDATE_FOLDER_NAME.name,
            body = UpdateFolderNameDTO(folderId, newFolderName),
        )
    }

    override suspend fun updateFolderNote(folderId: Long, newNote: String): Flow<Result<Message>> {
        return postFlow(
            endPoint = RemoteRoute.Folder.UPDATE_FOLDER_NOTE.name,
            body = UpdateFolderNoteDTO(folderId, newNote),
        )
    }

    override suspend fun deleteFolderNote(folderId: Long): Flow<Result<Message>> {
        return postFlow(
            endPoint = RemoteRoute.Folder.DELETE_FOLDER_NOTE.name,
            body = folderId,
        )
    }
}