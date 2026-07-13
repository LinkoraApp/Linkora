package com.sakethh.linkora.domain.repository.remote

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.NewItemResponseDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.tag.CreateTagDTO
import com.sakethh.linkora.domain.dto.server.tag.RenameTagDTO
import kotlinx.coroutines.flow.Flow

interface RemoteTagsRepo {
    suspend fun createATag(createTagDTO: CreateTagDTO): Flow<Result<NewItemResponseDTO>>

    suspend fun renameATag(renameTagDTO: RenameTagDTO): Flow<Result<TimeStampBasedResponse>>

    suspend fun deleteATag(idBasedDTO: IDBasedDTO): Flow<Result<TimeStampBasedResponse>>
}
