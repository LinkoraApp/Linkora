package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.Tag
import kotlinx.coroutines.flow.Flow

interface LocalTagsRepo {
    suspend fun createATag(
        tag: Tag,
        viaSocket: Boolean = false,
    ): Flow<Result<Long>>

    suspend fun createATagLocally(tag: Tag): Long

    suspend fun createLinkTags(linksTags: List<LinkTag>)

    suspend fun deleteLinkTagsBasedOnTags(
        tagIds: List<Long>,
        linkId: Long,
    )

    suspend fun deleteATag(
        id: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun renameATag(
        localTagId: Long,
        newName: String,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun getLastInsertedIdFromTags(): Long

    fun getAllTags(sortOption: String): Flow<List<Tag>>

    fun getTags(
        sortOption: String,
        pageSize: Int,
        lastSeenName: String?,
        lastSeenId: Long?,
    ): Flow<Result<List<Tag>>>

    suspend fun getAllTagsAsList(): List<Tag>

    suspend fun getAllLinkTagsAsList(): List<LinkTag>

    fun getAllLinkTags(): Flow<List<LinkTag>>

    fun getTagsBasedOnTheLinkId(linkId: Long): Flow<List<Tag>>

    suspend fun getTags(linkId: Long): List<Tag>

    fun getTagsForLinks(linkIds: List<Long>): Flow<Map<Long, List<Tag>>>

    suspend fun getTagsForLinksAsMap(linkIds: List<Long>): Map<Long, List<Tag>>

    fun search(
        query: String,
        sortOption: String,
    ): Flow<List<Tag>>

    suspend fun getLocalTagIds(remoteIds: List<Long>): List<Long>

    suspend fun getLocalTags(remoteIds: List<Long>): List<Tag>

    suspend fun getLocalTagId(remoteId: Long): Long
}
