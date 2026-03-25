package com.sakethh.linkora.data.remote.repository.sync

import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.domain.DeleteMultipleItemsDTO
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.Result.Loading
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.asFolderDTO
import com.sakethh.linkora.domain.dto.server.ArchiveMultipleItemsDTO
import com.sakethh.linkora.domain.dto.server.CopyItemsDTO
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.MarkItemsRegularDTO
import com.sakethh.linkora.domain.dto.server.MoveItemsDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.folder.AddFolderDTO
import com.sakethh.linkora.domain.dto.server.folder.MarkSelectedFoldersAsRootDTO
import com.sakethh.linkora.domain.dto.server.folder.UpdateFolderNameDTO
import com.sakethh.linkora.domain.dto.server.folder.UpdateFolderNoteDTO
import com.sakethh.linkora.domain.dto.server.link.AddLinkDTO
import com.sakethh.linkora.domain.dto.server.link.DeleteDuplicateLinksDTO
import com.sakethh.linkora.domain.dto.server.link.LinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateNoteOfALinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateTitleOfTheLinkDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelFolderDTO
import com.sakethh.linkora.domain.dto.server.panel.DeleteAFolderFromAPanelDTO
import com.sakethh.linkora.domain.dto.server.panel.UpdatePanelNameDTO
import com.sakethh.linkora.domain.dto.server.tag.CreateTagDTO
import com.sakethh.linkora.domain.dto.server.tag.RenameTagDTO
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.domain.repository.remote.RemoteLinksRepo
import com.sakethh.linkora.domain.repository.remote.RemoteMultiActionRepo
import com.sakethh.linkora.domain.repository.remote.RemotePanelsRepo
import com.sakethh.linkora.domain.repository.remote.RemoteTagsRepo
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Utils
import com.sakethh.linkora.utils.updateLastSyncedWithServerTimeStamp
import com.sakethh.linkora.utils.wrappedResultFlow
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json

class PendingQueueService(
    private val localFoldersRepo: LocalFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val pendingSyncQueueRepo: PendingSyncQueueRepo,
    private val remoteFoldersRepo: RemoteFoldersRepo,
    private val remoteLinksRepo: RemoteLinksRepo,
    private val remotePanelsRepo: RemotePanelsRepo,
    private val preferencesRepository: PreferencesRepository,
    private val remoteMultiActionRepo: RemoteMultiActionRepo,
    private val remoteTagsRepo: RemoteTagsRepo,
    private val linksDao: LinksDao,
    private val foldersDao: FoldersDao,
    private val tagsDao: TagsDao
) {

    private suspend inline fun Flow<Result<TimeStampBasedResponse>>.removeQueueItemAndSyncTimestamp(
        queueId: Long
    ) {
        this.collectLatest {
            it.onSuccess {
                pendingSyncQueueRepo.removeFromQueue(queueId)
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.data.eventTimestamp)
            }
        }
    }

    fun <T> SendChannel<Result<T>>.pushPendingSyncQueueToServer(): Flow<Result<Unit>> {
        return wrappedResultFlow {
            send(Loading(message = "[SYNC] Starting to push pending sync queue to server"))

            send(Loading(message = "[QUEUE] Fetching all items from the pending sync queue"))
            pendingSyncQueueRepo.getAllItemsFromQueue().forEach { queueItem ->
                send(Loading(message = "[QUEUE] Processing queue item (ID: ${queueItem.id}, Operation: ${queueItem.operation})"))

                when (val currentRoute = SyncServerRoute.valueOf(queueItem.operation)) {
                    SyncServerRoute.CREATE_TAG -> {
                        val createTagDTO =
                            Utils.json.decodeFromString<CreateTagDTO>(queueItem.payload)
                        remoteTagsRepo.createATag(createTagDTO).collect {
                            it.onSuccess {
                                val remoteTagId = it.data.id
                                tagsDao.updateRemoteId(
                                    localId = createTagDTO.offlineSyncItemId,
                                    newRemoteId = remoteTagId
                                )
                                pendingSyncQueueRepo.removeFromQueue(queueItem.id)
                                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.data.timeStampBasedResponse.eventTimestamp)
                            }
                        }
                    }

                    SyncServerRoute.DELETE_TAG -> {
                        val idBasedDTO = Utils.json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        remoteTagsRepo.deleteATag(idBasedDTO)
                            .removeQueueItemAndSyncTimestamp(queueItem.id)
                    }

                    SyncServerRoute.RENAME_TAG -> {
                        val renameTagDTO =
                            Utils.json.decodeFromString<RenameTagDTO>(queueItem.payload)
                        val remoteId = tagsDao.getRemoteTagId(renameTagDTO.id)
                        if (remoteId != null) {
                            remoteTagsRepo.renameATag(renameTagDTO.copy(id = remoteId))
                                .removeQueueItemAndSyncTimestamp(queueItem.id)
                        }
                    }

                    SyncServerRoute.COPY_EXISTING_ITEMS -> {
                        val copiedFoldersDTO =
                            Json.decodeFromString<CopyItemsDTO>(queueItem.payload)
                        val remoteParentFolderId =
                            foldersDao.getRemoteFolderId(copiedFoldersDTO.newParentFolderId)
                        if (remoteParentFolderId != null) {
                            remoteMultiActionRepo.copyMultipleItems(
                                copiedFoldersDTO.copy(
                                    newParentFolderId = remoteParentFolderId
                                )
                            ).collect {
                                it.onSuccess {
                                    it.data.linkIds.forEach {
                                        linksDao.updateRemoteLinkId(it.key, it.value)
                                    }
                                    it.data.folders.forEach {
                                        foldersDao.updateARemoteLinkId(
                                            it.currentFolder.newlyCopiedLocalId,
                                            it.currentFolder.sourceRemoteId
                                        )
                                        it.links.forEach {
                                            linksDao.updateRemoteLinkId(
                                                it.newlyCopiedLocalId,
                                                it.sourceRemoteId
                                            )
                                        }
                                    }
                                    preferencesRepository.updateLastSyncedWithServerTimeStamp(it.data.eventTimestamp)
                                }
                            }
                        }
                    }

                    SyncServerRoute.UNARCHIVE_MULTIPLE_ITEMS -> {
                        val markItemsRegularDTO =
                            Json.decodeFromString<MarkItemsRegularDTO>(queueItem.payload)
                        val remoteFolderIds =
                            foldersDao.getRemoteIds(markItemsRegularDTO.foldersIds)
                        val remoteLinksIds = linksDao.getRemoteIds(markItemsRegularDTO.linkIds)
                        if (remoteFolderIds != null && remoteLinksIds != null) {
                            remoteMultiActionRepo.markItemsAsRegular(
                                markItemsRegularDTO.copy(
                                    foldersIds = remoteFolderIds, linkIds = remoteLinksIds
                                )
                            ).removeQueueItemAndSyncTimestamp(queueItem.id)
                        }
                    }

                    SyncServerRoute.DELETE_MULTIPLE_ITEMS -> {
                        val deleteMultipleItemsDTO =
                            Json.decodeFromString<DeleteMultipleItemsDTO>(queueItem.payload)
                        remoteMultiActionRepo.deleteMultipleItems(deleteMultipleItemsDTO)
                            .removeQueueItemAndSyncTimestamp(queueItem.id)
                    }

                    SyncServerRoute.ARCHIVE_MULTIPLE_ITEMS -> {
                        val archiveMultipleItemsDTO =
                            Json.decodeFromString<ArchiveMultipleItemsDTO>(queueItem.payload)

                        val remoteFolderIds =
                            foldersDao.getRemoteIds(archiveMultipleItemsDTO.folderIds)
                        val remoteLinksIds = linksDao.getRemoteIds(archiveMultipleItemsDTO.linkIds)

                        if (remoteLinksIds != null && remoteFolderIds != null) {
                            remoteMultiActionRepo.archiveMultipleItems(archiveMultipleItemsDTO.run {
                                copy(linkIds = remoteLinksIds, folderIds = remoteFolderIds)
                            }).removeQueueItemAndSyncTimestamp(queueItem.id)
                        }
                    }

                    SyncServerRoute.MOVE_EXISTING_ITEMS -> {
                        val moveItemsDTO =
                            Json.decodeFromString<MoveItemsDTO>(queueItem.payload)

                        val remoteFolderIds = foldersDao.getRemoteIds(moveItemsDTO.folderIds)
                        val remoteLinksIds = linksDao.getRemoteIds(moveItemsDTO.linkIds)
                        val newRemoteParentFolderId = localFoldersRepo.getRemoteIdOfAFolder(
                            moveItemsDTO.newParentFolderId
                        )

                        if (remoteFolderIds != null && remoteLinksIds != null && newRemoteParentFolderId != null) {
                            remoteMultiActionRepo.moveMultipleItems(
                                moveItemsDTO.copy(
                                    folderIds = remoteFolderIds,
                                    linkIds = remoteLinksIds,
                                    newParentFolderId = newRemoteParentFolderId
                                )
                            ).removeQueueItemAndSyncTimestamp(queueItem.id)
                        }
                    }

                    // TODO: NESTED collectLatest
                    SyncServerRoute.CREATE_FOLDER -> {
                        send(Loading(message = "[FOLDER] Creating folder from queue item (ID: ${queueItem.id})"))
                        val addFolderDTO =
                            Json.decodeFromString<AddFolderDTO>(queueItem.payload)
                        remoteFoldersRepo.createFolder(addFolderDTO.let {
                            it.copy(
                                parentFolderId = if (it.parentFolderId != null) localFoldersRepo.getRemoteIdOfAFolder(
                                    it.parentFolderId
                                ) else null
                            )
                        }).collectLatest {
                            it.onSuccess { remoteResponse ->
                                send(Loading(message = "[FOLDER] Successfully created folder on server (Remote ID: ${remoteResponse.data.id})"))
                                localFoldersRepo.getThisFolderData(addFolderDTO.offlineSyncItemId)
                                    .collectLatest {
                                        it.onSuccess {
                                            send(Loading(message = "[FOLDER] Updating local folder data (Local ID: ${it.data.localId}, Remote ID: ${remoteResponse.data.id})"))
                                            localFoldersRepo.updateLocalFolderData(
                                                it.data.copy(
                                                    remoteId = remoteResponse.data.id,
                                                )
                                            ).collect()
                                            preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                                remoteResponse.data.timeStampBasedResponse.eventTimestamp
                                            )
                                            pendingSyncQueueRepo.removeFromQueue(queueItem.id)
                                            send(Loading(message = "[FOLDER] Removed queue item (ID: ${queueItem.id}) after successful sync"))
                                        }
                                    }
                            }
                        }
                    }

                    SyncServerRoute.DELETE_FOLDER -> {
                        send(Loading(message = "[FOLDER] Deleting folder from queue item (ID: ${queueItem.id})"))
                        val idBasedDTO =
                            Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteId =
                            localFoldersRepo.getRemoteIdOfAFolder(idBasedDTO.id) ?: return@forEach

                        remoteFoldersRepo.deleteFolder(idBasedDTO.copy(id = remoteId))
                            .removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                        send(Loading(message = "[FOLDER] Removed queue item (ID: ${queueItem.id}) after deleting folder"))
                    }

                    SyncServerRoute.MARK_FOLDER_AS_ARCHIVE -> {
                        send(Loading(message = "[FOLDER] Marking folder as archive from queue item (ID: ${queueItem.id})"))
                        val idBasedDTO =
                            Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteId = localFoldersRepo.getRemoteIdOfAFolder(idBasedDTO.id)
                        if (remoteId != null) {
                            remoteFoldersRepo.markAsArchive(idBasedDTO.copy(id = remoteId))
                                .removeQueueItemAndSyncTimestamp(
                                    queueItem.id
                                )
                            send(Loading(message = "[FOLDER] Removed queue item (ID: ${queueItem.id}) after marking folder as archive"))
                        }
                    }

                    SyncServerRoute.MARK_AS_REGULAR_FOLDER -> {
                        send(Loading(message = "[FOLDER] Marking folder as regular from queue item (ID: ${queueItem.id})"))
                        val idBasedDTO =
                            Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteId = localFoldersRepo.getRemoteIdOfAFolder(idBasedDTO.id)
                        if (remoteId != null) {
                            remoteFoldersRepo.markAsRegularFolder(idBasedDTO.copy(id = remoteId))
                                .removeQueueItemAndSyncTimestamp(
                                    queueItem.id
                                )
                        }
                        send(Loading(message = "[FOLDER] Removed queue item (ID: ${queueItem.id}) after marking folder as regular"))
                    }

                    SyncServerRoute.UPDATE_FOLDER -> {
                        val localFolder = Utils.json.decodeFromString<Folder>(queueItem.payload)

                        val remoteParentFolderId =
                            if (localFolder.parentFolderId != null) foldersDao.getRemoteFolderId(
                                localFolder.parentFolderId
                            ) else null

                        if (localFolder.remoteId != null) {
                            val remoteFolderDTO = localFolder.asFolderDTO(
                                remoteId = localFolder.remoteId,
                                remoteParentFolderId = remoteParentFolderId
                            )
                            remoteFoldersRepo.updateFolder(remoteFolderDTO)
                                .removeQueueItemAndSyncTimestamp(queueItem.id)
                        }
                    }

                    SyncServerRoute.UPDATE_FOLDER_NAME -> {
                        send(Loading(message = "[FOLDER] Updating folder name from queue item (ID: ${queueItem.id})"))
                        val updateFolderNameDTO =
                            Json.decodeFromString<UpdateFolderNameDTO>(queueItem.payload)
                        val remoteId =
                            localFoldersRepo.getRemoteIdOfAFolder(updateFolderNameDTO.folderId)

                        if (remoteId != null) {
                            remoteFoldersRepo.updateFolderName(
                                updateFolderNameDTO.copy(folderId = remoteId)
                            ).removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                            send(Loading(message = "[FOLDER] Removed queue item (ID: ${queueItem.id}) after updating folder name"))
                        }
                    }

                    SyncServerRoute.UPDATE_FOLDER_NOTE -> {
                        send(Loading(message = "[FOLDER] Updating folder note from queue item (ID: ${queueItem.id})"))
                        val updateFolderNoteDTO =
                            Json.decodeFromString<UpdateFolderNoteDTO>(queueItem.payload)
                        val remoteId =
                            localFoldersRepo.getRemoteIdOfAFolder(updateFolderNoteDTO.folderId)
                        if (remoteId != null) {
                            remoteFoldersRepo.updateFolderNote(
                                updateFolderNoteDTO.copy(folderId = remoteId)
                            ).removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                            send(Loading(message = "[FOLDER] Removed queue item (ID: ${queueItem.id}) after updating folder note"))
                        }
                    }

                    SyncServerRoute.DELETE_FOLDER_NOTE -> {
                        send(Loading(message = "[FOLDER] Deleting folder note from queue item (ID: ${queueItem.id})"))
                        val idBasedDTO =
                            Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteId = localFoldersRepo.getRemoteIdOfAFolder(idBasedDTO.id)

                        if (remoteId != null) {
                            remoteFoldersRepo.deleteFolderNote(idBasedDTO.copy(id = remoteId))
                                .removeQueueItemAndSyncTimestamp(
                                    queueItem.id
                                )
                            send(Loading(message = "[FOLDER] Removed queue item (ID: ${queueItem.id}) after deleting folder note"))
                        }
                    }

                    SyncServerRoute.UPDATE_LINK_TITLE -> {
                        send(Loading(message = "[LINK] Updating link title from queue item (ID: ${queueItem.id})"))
                        val updateTitleOfTheLinkDTO =
                            Json.decodeFromString<UpdateTitleOfTheLinkDTO>(queueItem.payload)
                        val remoteLinkId =
                            localLinksRepo.getRemoteLinkId(updateTitleOfTheLinkDTO.linkId)
                        if (remoteLinkId != null) {
                            remoteLinksRepo.updateLinkTitle(
                                updateTitleOfTheLinkDTO.copy(linkId = remoteLinkId)
                            ).removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                            send(Loading(message = "[LINK] Removed queue item (ID: ${queueItem.id}) after updating link title"))
                        }
                    }

                    SyncServerRoute.UPDATE_LINK_NOTE -> {
                        send(Loading(message = "[LINK] Updating link note from queue item (ID: ${queueItem.id})"))
                        val updateNoteOfALinkDTO =
                            Json.decodeFromString<UpdateNoteOfALinkDTO>(queueItem.payload)
                        val remoteLinkId =
                            localLinksRepo.getRemoteLinkId(updateNoteOfALinkDTO.linkId)
                        if (remoteLinkId != null) {
                            remoteLinksRepo.updateALinkNote(updateNoteOfALinkDTO.copy(linkId = remoteLinkId))
                                .removeQueueItemAndSyncTimestamp(
                                    queueItem.id
                                )
                            send(Loading(message = "[LINK] Removed queue item (ID: ${queueItem.id}) after updating link note"))
                        }
                    }

                    SyncServerRoute.DELETE_A_LINK -> {
                        send(Loading(message = "[LINK] Deleting link from queue item (ID: ${queueItem.id})"))
                        val idBasedDTO =
                            Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        remoteLinksRepo.deleteALink(idBasedDTO).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                        send(Loading(message = "[LINK] Removed queue item (ID: ${queueItem.id}) after deleting link"))
                    }

                    SyncServerRoute.ARCHIVE_LINK -> {
                        send(Loading(message = "[LINK] Archiving link from queue item (ID: ${queueItem.id})"))
                        val idBasedDTO =
                            Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteLinkId = localLinksRepo.getRemoteLinkId(idBasedDTO.id)
                        if (remoteLinkId != null) {
                            remoteLinksRepo.archiveALink(idBasedDTO.copy(id = remoteLinkId))
                                .removeQueueItemAndSyncTimestamp(
                                    queueItem.id
                                )
                            send(Loading(message = "[LINK] Removed queue item (ID: ${queueItem.id}) after archiving link"))
                        }
                    }

                    SyncServerRoute.UNARCHIVE_LINK -> {
                        send(Loading(message = "[LINK] Unarchiving link from queue item (ID: ${queueItem.id})"))
                        val idBasedDTO =
                            Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteLinkId = localLinksRepo.getRemoteLinkId(idBasedDTO.id)

                        if (remoteLinkId != null) {
                            remoteLinksRepo.unArchiveALink(idBasedDTO.copy(id = remoteLinkId))
                                .removeQueueItemAndSyncTimestamp(
                                    queueItem.id
                                )
                        }

                        send(Loading(message = "[LINK] Removed queue item (ID: ${queueItem.id}) after unarchiving link"))
                    }

                    SyncServerRoute.UPDATE_LINK -> {
                        send(Loading(message = "[LINK] Updating link from queue item (ID: ${queueItem.id})"))
                        val linkDTO = Json.decodeFromString<LinkDTO>(queueItem.payload)
                        val remoteId = localLinksRepo.getRemoteLinkId(linkDTO.id)

                        if (remoteId != null) {
                            remoteLinksRepo.updateLink(linkDTO.copy(id = remoteId))
                                .removeQueueItemAndSyncTimestamp(
                                    queueItem.id
                                )
                            send(Loading(message = "[LINK] Removed queue item (ID: ${queueItem.id}) after updating link"))
                        }
                    }

                    SyncServerRoute.CREATE_A_NEW_LINK -> {
                        send(Loading(message = "[LINK] Creating new link from queue item (ID: ${queueItem.id})"))
                        val addLinkDTO =
                            Json.decodeFromString<AddLinkDTO>(queueItem.payload)
                        val remoteTagIds =
                            tagsDao.getTags(addLinkDTO.offlineSyncItemId).map { it.remoteId }
                        if (null in remoteTagIds) return@forEach

                        remoteLinksRepo.addANewLink(
                            addLinkDTO.copy(
                                tags = remoteTagIds.filterNotNull(),
                                idOfLinkedFolder = if (addLinkDTO.idOfLinkedFolder != null) localFoldersRepo.getRemoteIdOfAFolder(
                                    addLinkDTO.idOfLinkedFolder
                                ) else null
                            )
                        ).collectLatest {
                            it.onSuccess { remoteResponse ->
                                send(Loading(message = "[LINK] Successfully created link on server (Remote ID: ${remoteResponse.data.id})"))
                                val updatedLink =
                                    localLinksRepo.getALink(addLinkDTO.offlineSyncItemId)
                                        .copy(remoteId = remoteResponse.data.id)
                                localLinksRepo.updateALink(
                                    updatedLink, updatedLinkTagsPair = null, viaSocket = true
                                ).collect()
                                preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                    remoteResponse.data.timeStampBasedResponse.eventTimestamp
                                )
                                pendingSyncQueueRepo.removeFromQueue(queueItem.id)
                                send(Loading(message = "[LINK] Removed queue item (ID: ${queueItem.id}) after creating link"))
                            }
                        }
                    }

                    SyncServerRoute.ADD_A_NEW_PANEL -> {
                        send(Loading(message = "[PANEL] Adding new panel from queue item (ID: ${queueItem.id})"))
                        val addANewPanelDTO =
                            Json.decodeFromString<AddANewPanelDTO>(queueItem.payload)
                        remotePanelsRepo.addANewPanel(addANewPanelDTO).collectLatest {
                            it.onSuccess { remoteResponse ->
                                send(Loading(message = "[PANEL] Successfully added panel on server (Remote ID: ${remoteResponse.data.id})"))
                                val updatedPanel =
                                    localPanelsRepo.getPanel(addANewPanelDTO.offlineSyncItemId)
                                        .copy(remoteId = remoteResponse.data.id)
                                localPanelsRepo.updatePanel(updatedPanel)
                                preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                    remoteResponse.data.timeStampBasedResponse.eventTimestamp
                                )
                                pendingSyncQueueRepo.removeFromQueue(queueItem.id)
                                send(Loading(message = "[PANEL] Removed queue item (ID: ${queueItem.id}) after adding panel"))
                            }
                        }
                    }

                    SyncServerRoute.ADD_A_NEW_FOLDER_IN_A_PANEL -> {
                        send(Loading(message = "[PANEL] Adding new folder in panel from queue item (ID: ${queueItem.id})"))
                        val addANewPanelFolderDTO =
                            Json.decodeFromString<AddANewPanelFolderDTO>(queueItem.payload)
                        val remoteFolderId =
                            localFoldersRepo.getRemoteIdOfAFolder(addANewPanelFolderDTO.folderId)
                        val remoteConnectedPanelId =
                            localPanelsRepo.getRemotePanelId(addANewPanelFolderDTO.connectedPanelId)

                        if (remoteFolderId != null && remoteConnectedPanelId != null) {
                            remotePanelsRepo.addANewFolderInAPanel(
                                addANewPanelFolderDTO.copy(
                                    folderId = remoteFolderId,
                                    connectedPanelId = remoteConnectedPanelId
                                )
                            ).collectLatest {
                                it.onSuccess { remoteResponse ->
                                    send(Loading(message = "[PANEL] Successfully added folder in panel on server (Remote ID: ${remoteResponse.data.id})"))
                                    val updatedPanel =
                                        localPanelsRepo.getPanelFolder(addANewPanelFolderDTO.offlineSyncItemId)
                                            .copy(remoteId = remoteResponse.data.id)
                                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                        remoteResponse.data.timeStampBasedResponse.eventTimestamp
                                    )
                                    localPanelsRepo.updateAPanelFolder(updatedPanel)
                                    pendingSyncQueueRepo.removeFromQueue(queueItem.id)
                                    send(Loading(message = "[PANEL] Removed queue item (ID: ${queueItem.id}) after adding folder in panel"))
                                }
                            }
                        }
                    }

                    SyncServerRoute.DELETE_A_PANEL -> {
                        send(Loading(message = "[PANEL] Deleting panel from queue item (ID: ${queueItem.id})"))
                        val idBasedDTO =
                            Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        remotePanelsRepo.deleteAPanel(idBasedDTO).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                        send(Loading(message = "[PANEL] Removed queue item (ID: ${queueItem.id}) after deleting panel"))
                    }

                    SyncServerRoute.UPDATE_A_PANEL_NAME -> {
                        send(Loading(message = "[PANEL] Updating panel name from queue item (ID: ${queueItem.id})"))
                        val updatePanelNameDTO =
                            Json.decodeFromString<UpdatePanelNameDTO>(queueItem.payload)
                        val remotePanelId =
                            localPanelsRepo.getRemotePanelId(updatePanelNameDTO.panelId)

                        if (remotePanelId != null) {
                            remotePanelsRepo.updateAPanelName(updatePanelNameDTO.copy(panelId = remotePanelId))
                                .removeQueueItemAndSyncTimestamp(
                                    queueItem.id
                                )
                            send(Loading(message = "[PANEL] Removed queue item (ID: ${queueItem.id}) after updating panel name"))
                        }
                    }

                    SyncServerRoute.DELETE_A_FOLDER_FROM_A_PANEL -> {
                        send(Loading(message = "[PANEL] Deleting folder from a panel from queue item (ID: ${queueItem.id})"))
                        val deleteAFolderFromAPanelDTO =
                            Json.decodeFromString<DeleteAFolderFromAPanelDTO>(queueItem.payload)

                        remotePanelsRepo.deleteAFolderFromAPanel(
                            deleteAFolderFromAPanelDTO
                        ).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                        send(Loading(message = "[PANEL] Removed queue item (ID: ${queueItem.id}) after deleting folder from panel"))

                    }

                    SyncServerRoute.DELETE_DUPLICATE_LINKS -> {
                        send(Loading(message = "Decoding duplicate links"))
                        val deleteDuplicateLinksDTO =
                            Json.decodeFromString<DeleteDuplicateLinksDTO>(queueItem.payload)

                        send(Loading(message = "Mapping local link IDs to remote IDs"))
                        remoteLinksRepo.deleteDuplicateLinks(deleteDuplicateLinksDTO).also {
                            send(Loading(message = "Deleting duplicate links from remote repository"))
                        }.removeQueueItemAndSyncTimestamp(queueItem.id)
                    }

                    SyncServerRoute.MARK_FOLDERS_AS_ROOT -> {
                        val markSelectedFoldersAsRootDTO =
                            Json.decodeFromString<MarkSelectedFoldersAsRootDTO>(queueItem.payload)
                        val remoteFolderIds =
                            foldersDao.getRemoteIds(markSelectedFoldersAsRootDTO.folderIds)
                        if (remoteFolderIds != null) {
                            remoteFoldersRepo.markSelectedFoldersAsRoot(
                                markSelectedFoldersAsRootDTO.copy(folderIds = remoteFolderIds)
                            ).removeQueueItemAndSyncTimestamp(queueItem.id)
                        }
                    }

                    SyncServerRoute.CHANGE_PARENT_FOLDER -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.DELETE_A_FOLDER_FROM_ALL_PANELS -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.UPDATE_LINKED_FOLDER_ID -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.UPDATE_USER_AGENT -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.GET_LINKS_FROM_A_FOLDER -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.GET_LINKS -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.MARK_AS_IMP -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.UNMARK_AS_IMP -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.TEST_BEARER -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.GET_UPDATES -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.GET_TOMBSTONES -> linkoraLog("Nothing to do on $currentRoute")
                    SyncServerRoute.DELETE_EVERYTHING -> linkoraLog("Nothing to do on $currentRoute")


                    SyncServerRoute.FORCE_SET_DEFAULT_FOLDER_TO_INTERNAL_IDS -> {
                        remoteLinksRepo.forceSetDefaultFolderToInternalIds()
                            .removeQueueItemAndSyncTimestamp(queueItem.id)
                    }
                }
            }
            send(Loading(message = "[SYNC] Completed pushing pending sync queue to server"))
        }
    }

}