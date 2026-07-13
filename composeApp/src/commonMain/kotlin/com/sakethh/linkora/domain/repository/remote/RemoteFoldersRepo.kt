package com.sakethh.linkora.domain.repository.remote

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
import kotlinx.coroutines.flow.Flow

interface RemoteFoldersRepo {
    // these function names are the same as the ones on the server-side, just to make lookup easier

    suspend fun createFolder(addFolderDTO: AddFolderDTO): Flow<Result<NewItemResponseDTO>>

    suspend fun updateFolder(folderDTO: FolderDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun deleteFolder(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun markAsArchive(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun markAsRegularFolder(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun changeParentFolder(
        changeParentFolderDTO: ChangeParentFolderDTO,
    ): Flow<Result<TimeStampBasedResponse>>

    suspend fun updateFolderName(
        updateFolderNameDTO: UpdateFolderNameDTO,
    ): Flow<Result<TimeStampBasedResponse>>

    suspend fun updateFolderNote(
        updateFolderNoteDTO: UpdateFolderNoteDTO,
    ): Flow<Result<TimeStampBasedResponse>>

    suspend fun deleteFolderNote(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun markSelectedFoldersAsRoot(
        markSelectedFoldersAsRootDTO: MarkSelectedFoldersAsRootDTO,
    ): Flow<Result<TimeStampBasedResponse>>
}
