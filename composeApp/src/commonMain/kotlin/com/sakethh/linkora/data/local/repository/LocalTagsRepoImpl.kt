package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.tag.CreateTagDTO
import com.sakethh.linkora.domain.dto.server.tag.RenameTagDTO
import com.sakethh.linkora.domain.mapToResultFlow
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteTagsRepo
import com.sakethh.linkora.utils.Sorting
import com.sakethh.linkora.utils.canPushToServer
import com.sakethh.linkora.utils.getSystemEpochSeconds
import com.sakethh.linkora.utils.performLocalOperationWithRemoteSyncFlow
import com.sakethh.linkora.utils.updateLastSyncedWithServerTimeStamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class LocalTagsRepoImpl(
    private val tagsDao: TagsDao,
    private val remoteTagsRepo: RemoteTagsRepo,
    private val preferencesRepository: PreferencesRepository,
    private val pendingSyncQueueRepo: PendingSyncQueueRepo
) : LocalTagsRepo {
    override suspend fun createATag(tag: Tag, viaSocket: Boolean): Flow<Result<Long>> {
        var newTagId: Long? = null
        val eventTimestamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                remoteTagsRepo.createATag(
                    createTagDTO = CreateTagDTO(
                        name = tag.name,
                        eventTimestamp = eventTimestamp,
                        correlation = preferences.correlation
                    )
                )
            },
            remoteOperationOnSuccess = {
                if (newTagId == null) return@performLocalOperationWithRemoteSyncFlow

                tagsDao.updateATag(
                    tag = tagsDao.getATag(newTagId).copy(
                        remoteId = it.id, lastModified = it.timeStampBasedResponse.eventTimestamp
                    )
                )
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.timeStampBasedResponse.eventTimestamp)
            },
            onRemoteOperationFailure = {
                if (newTagId == null) return@performLocalOperationWithRemoteSyncFlow

                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.CREATE_TAG.name, payload = Json.encodeToString(
                            CreateTagDTO(
                                name = tag.name,
                                eventTimestamp = eventTimestamp,
                                offlineSyncItemId = newTagId!!,
                                correlation = preferences.correlation
                            )
                        )
                    )
                )
            }) {
            newTagId = tagsDao.createATag(tag)
            newTagId
        }
    }

    override suspend fun createATagLocally(
        tag: Tag,
    ): Long {
        return tagsDao.createATag(tag)
    }

    override suspend fun createLinkTags(linksTags: List<LinkTag>) {
        tagsDao.createLinkTags(linksTags)
    }

    override suspend fun deleteLinkTagsBasedOnTags(tagIds: List<Long>, linkId: Long) {
        tagsDao.deleteLinkTagsBasedOnTags(tagIds, linkId)
    }

    override suspend fun deleteATag(id: Long, viaSocket: Boolean): Flow<Result<Unit>> {
        val tag = tagsDao.getATag(id)
        val eventTimestamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                require(tag.remoteId != null)
                remoteTagsRepo.deleteATag(
                    IDBasedDTO(
                        id = tag.remoteId, eventTimestamp = eventTimestamp,
                        correlation = preferences.correlation
                    )
                )
            },
            remoteOperationOnSuccess = {
                tagsDao.updateATag(tagsDao.getATag(id).copy(lastModified = it.eventTimestamp))
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                if (tag.remoteId != null) {
                    pendingSyncQueueRepo.addInQueue(
                        PendingSyncQueue(
                            operation = SyncServerRoute.DELETE_TAG.name,
                            payload = Json.encodeToString(
                                IDBasedDTO(
                                    id = tag.remoteId, eventTimestamp = eventTimestamp,
                                    correlation = preferences.correlation
                                )
                            )
                        )
                    )
                }
            }) {
            tagsDao.deleteATag(id)
        }
    }

    override fun getAllTags(sortOption: String): Flow<List<Tag>> {
        return tagsDao.getAllTags(sortOption)
    }

    override fun getTags(
        sortOption: String, pageSize: Int,
        lastSeenName: String?,
        lastSeenId: Long?
    ): Flow<Result<List<Tag>>> {
        return when (sortOption) {
            Sorting.A_TO_Z, Sorting.Z_TO_A -> tagsDao.getTagsSortedByName(
                lastSeenId = lastSeenId,
                lastSeenName = lastSeenName?.takeIf { it.isNotEmpty() },
                pageSize = pageSize,
                isAscending = sortOption == Sorting.A_TO_Z
            )

            else -> tagsDao.getTagsSortedById(
                lastSeenId = lastSeenId,
                isAscending = sortOption == Sorting.OLD_TO_NEW,
                pageSize = pageSize
            )
        }.mapToResultFlow()
    }

    override suspend fun renameATag(
        localTagId: Long, newName: String, viaSocket: Boolean
    ): Flow<Result<Unit>> {
        val tag = tagsDao.getATag(localTagId)
        val eventTimestamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                require(tag.remoteId != null)
                remoteTagsRepo.renameATag(
                    renameTagDTO = RenameTagDTO(
                        id = tag.remoteId,
                        newName = newName,
                        eventTimestamp = eventTimestamp,
                        correlation = preferences.correlation
                    )
                )
            },
            remoteOperationOnSuccess = {
                tagsDao.updateATag(
                    tagsDao.getATag(tag.localId).copy(lastModified = it.eventTimestamp)
                )
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.RENAME_TAG.name, payload = Json.encodeToString(
                            RenameTagDTO(
                                newName = newName,
                                id = tag.localId,
                                eventTimestamp = eventTimestamp,
                                correlation = preferences.correlation
                            )
                        )
                    )
                )
            }) {
            tagsDao.renameATag(localTagId, newName)
        }
    }

    override suspend fun getAllLinkTagsAsList(): List<LinkTag> {
        return tagsDao.getAllTagLinksAsList()
    }

    override fun getAllLinkTags(): Flow<List<LinkTag>> {
        return tagsDao.getAllLinkTags()
    }

    override suspend fun getAllTagsAsList(): List<Tag> {
        return tagsDao.getAllTagsAsList()
    }

    override suspend fun getLastInsertedIdFromTags(): Long {
        return tagsDao.getLastInsertedIdFromTags()
    }

    override fun getTagsBasedOnTheLinkId(linkId: Long): Flow<List<Tag>> {
        return tagsDao.getTagsBasedOnTheLinkId(linkId)
    }

    override suspend fun getTags(linkId: Long): List<Tag> {
        return tagsDao.getTags(linkId)
    }

    override fun getTagsForLinks(linkIds: List<Long>): Flow<Map<Long, List<Tag>>> {
        return tagsDao.getTagsWithLinkIds(linkIds).map { flatList ->
            flatList.groupBy { it.linkId }.mapValues { entry ->
                entry.value.map { it.tag }
            }
        }
    }

    override suspend fun getTagsForLinksAsMap(linkIds: List<Long>): Map<Long, List<Tag>> {
        return tagsDao.getTagsWithLinkIdsAsList(linkIds).groupBy { it.linkId }.mapValues { entry ->
            entry.value.map { it.tag }
        }
    }

    override fun search(query: String, sortOption: String): Flow<List<Tag>> {
        return tagsDao.search(query, sortOption)
    }

    override suspend fun getLocalTagIds(remoteIds: List<Long>): List<Long> {
        return tagsDao.getLocalTagIds(remoteIds)
    }

    override suspend fun getLocalTags(remoteIds: List<Long>): List<Tag> {
        return tagsDao.getLocalTags(remoteIds)
    }

    override suspend fun getLocalTagId(remoteId: Long): Long {
        return tagsDao.getLocalTagId(remoteId)
    }
}