package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import kotlinx.coroutines.flow.Flow

interface LocalLinksRepo {
    suspend fun addANewLink(
        link: Link,
        selectedTagIds: List<Long>?,
        linkSaveConfig: LinkSaveConfig,
        viaSocket: Boolean = false
    ): Flow<Result<Unit>>

    suspend fun addMultipleLinks(links: List<Link>): List<Long>

    suspend fun getLinks(
        linkType: LinkType, parentFolderId: Long, sortOption: String,
        pageSize: Int, startIndex: Long
    ): Flow<Result<List<Link>>>

    suspend fun getLinks(
        tagId: Long, sortOption: String,
        pageSize: Int, startIndex: Long
    ): Flow<Result<List<Link>>>

    fun getLinksAsNonResultFlow(
        linkType: LinkType, parentFolderId: Long, sortOption: String
    ): Flow<List<Link>>


    suspend fun getLinks(
        linkType: LinkType, sortOption: String,
        pageSize: Int, startIndex: Long
    ): Flow<Result<List<Link>>>

    fun getLinksAsNonResultFlow(
        linkType: LinkType, sortOption: String
    ): Flow<List<Link>>

    suspend fun getAllLinks(sortOption: String): Flow<Result<List<Link>>>

    suspend fun deleteLinksOfFolder(folderId: Long): Flow<Result<Unit>>

    suspend fun deleteALinkNote(linkId: Long): Flow<Result<Unit>>

    suspend fun deleteALink(linkId: Long, viaSocket: Boolean = false): Flow<Result<Unit>>
    suspend fun deleteMultipleLinks(
        linkIds: List<Long>, viaSocket: Boolean = false
    ): Flow<Result<Unit>>

    suspend fun archiveALink(linkId: Long, viaSocket: Boolean = false): Flow<Result<Unit>>
    suspend fun updateLinkNote(
        linkId: Long, newNote: String, viaSocket: Boolean = false
    ): Flow<Result<Unit>>

    suspend fun updateLinkTitle(
        linkId: Long, newTitle: String, viaSocket: Boolean = false
    ): Flow<Result<Unit>>

    suspend fun isInArchive(url: String): Flow<Result<Boolean>>

    fun search(query: String, sortOption: String): Flow<Result<List<Link>>>

    suspend fun getLinksOfThisFolderAsList(folderID: Long): List<Link>

    suspend fun getAllLinks(): List<Link>
    fun getAllLinksAsFlow(): Flow<List<Link>>

    suspend fun updateALink(
        link: Link,
        updatedLinkTagsPair: LinkTagsPair?,
        viaSocket: Boolean = false
    ): Flow<Result<Unit>>

    suspend fun refreshLinkMetadata(link: Link,refreshLinkType: RefreshLinkType): Flow<Result<Unit>>

    suspend fun getLocalLinkId(remoteID: Long): Long?
    suspend fun getRemoteLinkId(localId: Long): Long?

    suspend fun getALink(localLinkId: Long): Link

    suspend fun getLatestId(): Long
    suspend fun getUnSyncedLinks(): List<Link>
    suspend fun changeIdOfALink(existingId: Long, newId: Long)
    suspend fun doesLinkExist(linkType: LinkType, url: String): Boolean
    suspend fun deleteDuplicateLinks(viaSocket: Boolean = false): Flow<Result<Unit>>
    suspend fun deleteLinksLocally(linksIds: List<Long>): Flow<Result<Unit>>

    fun getAllLinks(
        applyLinkFilters: Boolean,
        activeLinkFilters: List<String>,
        sortOption: String,
        pageSize: Int,
        startIndex: Long
    ): Flow<Result<List<Link>>>

}