package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import kotlinx.coroutines.flow.Flow

interface LocalMultiActionRepo {
    suspend fun archiveMultipleItems(
        linkIds: List<Long>,
        folderIds: List<Long>,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun deleteMultipleItems(
        linkIds: List<Long>,
        folderIds: List<Long>,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun moveMultipleItems(
        linkIds: List<Long>,
        folderIds: List<Long>,
        linkType: LinkType,
        newParentFolderId: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun copyMultipleItems(
        linkTagsPairs: List<LinkTagsPair>,
        folders: List<Folder>,
        linkType: LinkType,
        newParentFolderId: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun unArchiveMultipleItems(
        linkIds: List<Long>,
        folderIds: List<Long>,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>
}
