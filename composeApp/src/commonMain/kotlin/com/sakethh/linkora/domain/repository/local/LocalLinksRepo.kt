package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.link.Link
import kotlinx.coroutines.flow.Flow

interface LocalLinksRepo {
    suspend fun addANewLink(
        link: Link,
        linkSaveConfig: LinkSaveConfig,
        viaSocket: Boolean = false
    ): Flow<Result<Unit>>

    suspend fun addMultipleLinks(links: List<Link>)

    suspend fun sortLinks(
        linkType: LinkType, parentFolderId: Long, sortOption: String
    ): Flow<Result<List<Link>>>

    fun sortLinksAsNonResultFlow(
        linkType: LinkType, parentFolderId: Long, sortOption: String
    ): Flow<List<Link>>

    suspend fun sortLinks(
        linkType: LinkType, sortOption: String
    ): Flow<Result<List<Link>>>

    fun sortLinksAsNonResultFlow(
        linkType: LinkType, sortOption: String
    ): Flow<List<Link>>

    suspend fun sortAllLinks(sortOption: String): Flow<Result<List<Link>>>

    suspend fun deleteLinksOfFolder(folderId: Long): Flow<Result<Unit>>

    suspend fun deleteALinkNote(linkId: Long): Flow<Result<Unit>>

    suspend fun deleteALink(linkId: Long, viaSocket: Boolean = false): Flow<Result<Unit>>

    suspend fun archiveALink(linkId: Long, viaSocket: Boolean = false): Flow<Result<Unit>>

    suspend fun updateLinkNote(
        linkId: Long, newNote: String,
        viaSocket: Boolean = false
    ): Flow<Result<Unit>>

    suspend fun updateLinkTitle(
        linkId: Long, newTitle: String,
        viaSocket: Boolean = false
    ): Flow<Result<Unit>>

    suspend fun markedAsImportant(linkUrl: String): Flow<Result<Boolean>>

    suspend fun isInArchive(url: String): Flow<Result<Boolean>>

    fun search(query: String, sortOption: String): Flow<Result<List<Link>>>

    suspend fun getLinksOfThisFolderAsList(folderID: Long): List<Link>

    suspend fun getAllLinks(): List<Link>

    suspend fun deleteAllLinks()

    suspend fun updateALink(link: Link, viaSocket: Boolean = false): Flow<Result<Unit>>

    suspend fun refreshLinkMetadata(link: Link): Flow<Result<Unit>>

    suspend fun getLocalLinkId(remoteID: Long): Long?
    suspend fun getRemoteLinkId(localId: Long): Long?

    suspend fun getALink(localLinkId: Long): Link
}