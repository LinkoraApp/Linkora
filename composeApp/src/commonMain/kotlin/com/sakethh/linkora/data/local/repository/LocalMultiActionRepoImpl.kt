package com.sakethh.linkora.data.local.repository

import androidx.room3.Transactor
import androidx.room3.immediateTransaction
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.domain.DeleteMultipleItemsDTO
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.dto.server.ArchiveMultipleItemsDTO
import com.sakethh.linkora.domain.dto.server.CopyFolderDTO
import com.sakethh.linkora.domain.dto.server.CopyItemsDTO
import com.sakethh.linkora.domain.dto.server.CurrentFolder
import com.sakethh.linkora.domain.dto.server.FolderLink
import com.sakethh.linkora.domain.dto.server.MarkItemsRegularDTO
import com.sakethh.linkora.domain.dto.server.MoveItemsDTO
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalMultiActionRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteMultiActionRepo
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.utils.canPushToServer
import com.sakethh.linkora.utils.getSystemEpochSeconds
import com.sakethh.linkora.utils.performLocalOperationWithRemoteSyncFlow
import com.sakethh.linkora.utils.updateLastSyncedWithServerTimeStamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json

class LocalMultiActionRepoImpl(
    private val linksDao: LinksDao,
    private val foldersDao: FoldersDao,
    private val preferencesRepository: PreferencesRepository,
    private val remoteMultiActionRepo: RemoteMultiActionRepo,
    private val pendingSyncQueueRepo: PendingSyncQueueRepo,
    private val localFoldersRepo: LocalFoldersRepo,
    private val localTagsRepo: LocalTagsRepo,
    private val withWriterConnection: suspend (suspend (Transactor) -> Unit) -> Unit
) : LocalMultiActionRepo {
    override suspend fun archiveMultipleItems(
        linkIds: List<Long>, folderIds: List<Long>, viaSocket: Boolean
    ): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                val remoteLinkIds = linksDao.getRemoteIds(linkIds)
                val remoteFolderIds = foldersDao.getRemoteIds(folderIds)
                remoteMultiActionRepo.archiveMultipleItems(
                    ArchiveMultipleItemsDTO(
                        linkIds = remoteLinkIds,
                        folderIds = remoteFolderIds,
                        eventTimestamp = eventTimestamp,
                        correlation = preferences.correlation
                    )
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.ARCHIVE_MULTIPLE_ITEMS.name,
                        payload = Json.encodeToString(
                            ArchiveMultipleItemsDTO(
                                linkIds = linkIds,
                                folderIds = folderIds,
                                eventTimestamp = eventTimestamp,
                                correlation = preferences.correlation
                            )
                        )
                    )
                )
            }) {
            withWriterConnection { transactor ->
                transactor.immediateTransaction {
                    linksDao.archiveMultipleLinks(linkIds, eventTimestamp)
                    foldersDao.markMultipleFoldersAsArchive(folderIds, eventTimestamp)
                }
            }
        }
    }

    override suspend fun deleteMultipleItems(
        linkIds: List<Long>, folderIds: List<Long>, viaSocket: Boolean
    ): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        val remoteLinkIds = linksDao.getRemoteIds(linkIds)
        val remoteFolderIds = foldersDao.getRemoteIds(folderIds)
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                remoteMultiActionRepo.deleteMultipleItems(
                    DeleteMultipleItemsDTO(
                        linkIds = remoteLinkIds,
                        folderIds = remoteFolderIds,
                        eventTimestamp = eventTimestamp,
                        correlation = preferences.correlation
                    )
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.DELETE_MULTIPLE_ITEMS.name,
                        payload = Json.encodeToString(
                            DeleteMultipleItemsDTO(
                                linkIds = remoteLinkIds,
                                folderIds = remoteFolderIds,
                                eventTimestamp = eventTimestamp,
                                correlation = preferences.correlation
                            )
                        )
                    )
                )
            }) {
            withWriterConnection { transactor ->
                transactor.immediateTransaction {
                    linksDao.deleteMultipleLinks(linkIds)
                    localFoldersRepo.deleteMultipleFolders(folderIds, viaSocket = true).collect()
                }
            }
        }
    }


    override suspend fun moveMultipleItems(
        linkIds: List<Long>,
        folderIds: List<Long>,
        linkType: LinkType,
        newParentFolderId: Long,
        viaSocket: Boolean
    ): Flow<Result<Unit>> {
        val eventTimeStamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                val remoteLinkIds = linksDao.getRemoteIds(linkIds)
                val remoteFolderIds = foldersDao.getRemoteIds(folderIds)
                val remoteParentFolderId = foldersDao.getRemoteFolderId(newParentFolderId)

                require(remoteParentFolderId != null)

                remoteMultiActionRepo.moveMultipleItems(
                    MoveItemsDTO(
                        folderIds = remoteFolderIds,
                        linkIds = remoteLinkIds,
                        linkType = linkType,
                        newParentFolderId = remoteParentFolderId,
                        eventTimestamp = eventTimeStamp,
                        correlation = preferences.correlation
                    )
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.MOVE_EXISTING_ITEMS.name,
                        payload = Json.encodeToString(
                            MoveItemsDTO(
                                folderIds = folderIds,
                                linkIds = linkIds,
                                linkType = linkType,
                                newParentFolderId = newParentFolderId,
                                eventTimestamp = eventTimeStamp,
                                correlation = preferences.correlation
                            )
                        )
                    )
                )
            }) {
            withWriterConnection { transactor ->
                transactor.immediateTransaction {
                    foldersDao.moveFolders(newParentFolderId, folderIds, eventTimeStamp)
                    linksDao.moveLinks(newParentFolderId, linkType, linkIds, eventTimeStamp)
                }
            }
        }
    }


    override suspend fun copyMultipleItems(
        linkTagsPairs: List<LinkTagsPair>,
        folders: List<Folder>,
        linkType: LinkType,
        newParentFolderId: Long,
        viaSocket: Boolean
    ): Flow<Result<Unit>> {
        val eventTimeStamp = getSystemEpochSeconds()

        val copiedLinksIds = mutableMapOf<Long, Long>()

        lateinit var copiedFolders: List<CopyFolderDTO>

        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                val remoteParentFolderId = foldersDao.getRemoteFolderId(newParentFolderId)
                require(remoteParentFolderId != null)
                remoteMultiActionRepo.copyMultipleItems(
                    CopyItemsDTO(
                        folders = copiedFolders,
                        linkIds = copiedLinksIds.toMap(),
                        linkType = linkType,
                        newParentFolderId = remoteParentFolderId,
                        eventTimestamp = eventTimeStamp,
                        correlation = preferences.correlation
                    )
                )
            },
            remoteOperationOnSuccess = {
                withWriterConnection { transactor ->
                    transactor.immediateTransaction {
                        it.linkIds.forEach {
                            linksDao.updateRemoteLinkId(it.key, it.value)
                        }
                        it.folders.forEach {
                            foldersDao.updateARemoteLinkId(
                                it.currentFolder.newlyCopiedLocalId, it.currentFolder.sourceRemoteId
                            )
                            it.links.forEach {
                                linksDao.updateRemoteLinkId(
                                    it.newlyCopiedLocalId,
                                    it.sourceRemoteId
                                )
                            }
                        }
                    }
                }
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.COPY_EXISTING_ITEMS.name,
                        payload = Json.encodeToString(
                            CopyItemsDTO(
                                folders = copiedFolders,
                                linkIds = copiedLinksIds.toMap(),
                                linkType = linkType,
                                newParentFolderId = newParentFolderId,
                                eventTimestamp = eventTimeStamp,
                                correlation = preferences.correlation
                            )
                        )
                    )
                )
            }) {

            withWriterConnection { transactor ->
                transactor.immediateTransaction {

                    val newLinks = linkTagsPairs.map {
                        it.link.copy(
                            idOfLinkedFolder = newParentFolderId,
                            linkType = linkType,
                            localId = 0,
                            remoteId = null,
                            lastModified = eventTimeStamp
                        )
                    }
                    linksDao.addMultipleLinks(newLinks).forEachIndexed { index, newLinkId ->
                        copiedLinksIds[newLinkId] = linkTagsPairs[index].link.remoteId ?: -45454
                        val newLinkTags = linkTagsPairs[index].tags.map {
                            LinkTag(linkId = newLinkId, tagId = it.localId)
                        }
                        localTagsRepo.createLinkTags(newLinkTags)
                    }

                    copiedFolders = copyFolders(
                        destinationParentFolderId = newParentFolderId,
                        folders = folders,
                        eventTimeStamp
                    )
                }
            }
        }
    }

    private suspend fun copyFolders(
        destinationParentFolderId: Long, folders: List<Folder>, eventTimestamp: Long
    ): List<CopyFolderDTO> {
        val copyFoldersDTO = mutableListOf<CopyFolderDTO?>()
        val copyFoldersDeque = ArrayDeque<Pair<Folder, Long>>()
        copyFoldersDeque.addAll(folders.map { it to destinationParentFolderId })

        /*
        data class CopyFolderDTO(
            val currentFolder: CurrentFolder,
            val links: List<FolderLink>,
            val childFolders: List<CopyFolderDTO>
        )
        * */

        while (copyFoldersDeque.isNotEmpty()) {
            val (currentFolder, newParentId) = copyFoldersDeque.removeLast()

            val newIdOfCopiedCurrentFolder = foldersDao.insertANewFolder(
                currentFolder.copy(
                    parentFolderId = newParentId,
                    remoteId = null,
                    localId = 0,
                    lastModified = eventTimestamp
                )
            )

            val linksOfCurrentFolder = linksDao.getLinksOfThisFolderAsList(
                currentFolder.localId
            )

            val linksIdsOfCopiedFolder = linksDao.addMultipleLinks(
                linksOfCurrentFolder.map {
                    it.copy(
                        idOfLinkedFolder = newIdOfCopiedCurrentFolder,
                        localId = 0,
                        lastModified = eventTimestamp
                    )
                })

            if (currentFolder.remoteId != null) {
                val parentFolderId = currentFolder.parentFolderId
                copyFoldersDTO.add(
                    CopyFolderDTO(
                        currentFolder = CurrentFolder(
                            newlyCopiedLocalId = newIdOfCopiedCurrentFolder,
                            parentOfNewlyCopiedLocalId = newParentId,
                            sourceRemoteId = currentFolder.remoteId,
                            sourceRemoteParentId = if (parentFolderId == null) null else foldersDao.getRemoteFolderId(
                                parentFolderId
                            ),
                            isRootFolderForTheDestination = newParentId == destinationParentFolderId
                        ),
                        links = linksIdsOfCopiedFolder.mapIndexed { index, newLocalId ->
                            val remoteId =
                                linksOfCurrentFolder[index].remoteId ?: return@mapIndexed null
                            val parentFolderId = linksOfCurrentFolder[index].idOfLinkedFolder

                            val remoteParentId =
                                if (parentFolderId == null) null else foldersDao.getRemoteFolderId(
                                    parentFolderId
                                )

                            FolderLink(
                                newlyCopiedLocalId = newLocalId,
                                sourceRemoteId = remoteId,
                                sourceRemoteParentId = remoteParentId,
                                isRootFolderForTheDestination = false, // this doesn't matter for a link since it's already embedded with the folder
                                parentOfNewlyCopiedLocalId = -45454 // this doesn't matter for a link since it's already embedded with the folder
                            )
                        }.filterNotNull(),
                    )
                )
            }

            if (linksOfCurrentFolder.isNotEmpty()) {
                val tagsForCurrentLinksBatch =
                    localTagsRepo.getTagsForLinksAsMap(linksOfCurrentFolder.map { it.localId })


                val linkIdsMap =
                    linksOfCurrentFolder.map { it.localId }.zip(linksIdsOfCopiedFolder).toMap()

                val newLinkTags = tagsForCurrentLinksBatch.flatMap { (oldLinkId, tags) ->
                    val newLinkId = linkIdsMap[oldLinkId]
                    if (newLinkId != null) {
                        tags.map {
                            LinkTag(linkId = newLinkId, tagId = it.localId)
                        }
                    } else {
                        emptyList()
                    }
                }
                localTagsRepo.createLinkTags(newLinkTags)
            }

            val childFolders =
                localFoldersRepo.getChildFoldersAsList(parentFolderId = currentFolder.localId)
            copyFoldersDeque.addAll(
                childFolders.map {
                    it to newIdOfCopiedCurrentFolder
                }
            )
        }
        return copyFoldersDTO.filterNotNull()
    }


    override suspend fun unArchiveMultipleItems(
        linkIds: List<Long>, folderIds: List<Long>, viaSocket: Boolean
    ): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                val remoteLinkIds = linksDao.getRemoteIds(linkIds)
                val remoteFolderIds = foldersDao.getRemoteIds(folderIds)

                remoteMultiActionRepo.markItemsAsRegular(
                    MarkItemsRegularDTO(
                        foldersIds = remoteFolderIds,
                        linkIds = remoteLinkIds,
                        eventTimestamp = eventTimestamp,
                        correlation = preferences.correlation
                    )
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
                foldersDao.updateFoldersTimestamp(
                    timestamp = it.eventTimestamp, localFolderIDs = folderIds
                )
                linksDao.updateLinksTimestamp(
                    timestamp = it.eventTimestamp, localLinkIds = linkIds
                )
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.UNARCHIVE_MULTIPLE_ITEMS.name,
                        payload = Json.encodeToString(
                            MarkItemsRegularDTO(
                                foldersIds = folderIds,
                                linkIds = linkIds,
                                eventTimestamp = eventTimestamp,
                                correlation = preferences.correlation
                            )
                        )
                    )
                )
            }) {
            withWriterConnection { transactor ->
                transactor.immediateTransaction {
                    foldersDao.markMultipleFoldersAsRegular(
                        eventTimestamp = eventTimestamp, folderIDs = folderIds
                    )
                    linksDao.unarchiveLinks(linksIds = linkIds, eventTimestamp = eventTimestamp)
                }
            }
        }
    }
}