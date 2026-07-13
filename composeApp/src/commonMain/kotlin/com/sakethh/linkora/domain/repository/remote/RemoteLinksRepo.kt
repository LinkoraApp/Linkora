package com.sakethh.linkora.domain.repository.remote

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.NewItemResponseDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.link.AddLinkDTO
import com.sakethh.linkora.domain.dto.server.link.DeleteDuplicateLinksDTO
import com.sakethh.linkora.domain.dto.server.link.LinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateNoteOfALinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateTitleOfTheLinkDTO
import kotlinx.coroutines.flow.Flow

interface RemoteLinksRepo {
    suspend fun addANewLink(addLinkDTO: AddLinkDTO): Flow<Result<NewItemResponseDTO>>

    suspend fun deleteALink(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun updateLinkTitle(
        updateTitleOfTheLinkDTO: UpdateTitleOfTheLinkDTO,
    ): Flow<Result<TimeStampBasedResponse>>

    suspend fun updateALinkNote(
        updateNoteOfALinkDTO: UpdateNoteOfALinkDTO,
    ): Flow<Result<TimeStampBasedResponse>>

    suspend fun archiveALink(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun unArchiveALink(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun markALinkAsImp(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun markALinkAsNonImp(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun updateLink(linkDTO: LinkDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun deleteDuplicateLinks(
        deleteDuplicateLinksDTO: DeleteDuplicateLinksDTO,
    ): Flow<Result<TimeStampBasedResponse>>

    suspend fun forceSetDefaultFolderToInternalIds(): Flow<Result<TimeStampBasedResponse>>
}
