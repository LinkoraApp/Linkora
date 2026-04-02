package com.sakethh.linkora.data.remote.repository.sync

import com.sakethh.linkora.domain.DeleteMultipleItemsDTO
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.dto.server.ArchiveMultipleItemsDTO
import com.sakethh.linkora.domain.dto.server.CopyItemsSocketResponseDTO
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.dto.server.DeleteEverythingDTO
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.MarkItemsRegularDTO
import com.sakethh.linkora.domain.dto.server.MoveItemsDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.folder.FolderDTO
import com.sakethh.linkora.domain.dto.server.folder.MarkSelectedFoldersAsRootDTO
import com.sakethh.linkora.domain.dto.server.folder.UpdateFolderNameDTO
import com.sakethh.linkora.domain.dto.server.folder.UpdateFolderNoteDTO
import com.sakethh.linkora.domain.dto.server.link.DeleteDuplicateLinksDTO
import com.sakethh.linkora.domain.dto.server.link.LinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateNoteOfALinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateTitleOfTheLinkDTO
import com.sakethh.linkora.domain.dto.server.panel.DeleteAFolderFromAPanelDTO
import com.sakethh.linkora.domain.dto.server.panel.PanelDTO
import com.sakethh.linkora.domain.dto.server.panel.PanelFolderDTO
import com.sakethh.linkora.domain.dto.server.panel.UpdatePanelNameDTO
import com.sakethh.linkora.domain.dto.server.tag.RenameTagDTO
import com.sakethh.linkora.domain.dto.server.tag.TagDTO
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.WebSocketEvent
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalMultiActionRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.Utils.json
import com.sakethh.linkora.utils.updateLastSyncedWithServerTimeStamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class LocalDataUpdateService(
    private val localFoldersRepo: LocalFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val preferencesRepository: PreferencesRepository,
    private val localMultiActionRepo: LocalMultiActionRepo,
    private val localTagsRepo: LocalTagsRepo,
    private val remoteSyncRepo: RemoteSyncRepo
) {
    suspend fun <T> Flow<Result<T>>.collectAndUpdateTimestamp(eventTimestamp: Long) {
        this.collect {
            it.onSuccess {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(eventTimestamp)
            }
        }
    }

    suspend fun Correlation.isSameAsCurrentClient(): Boolean {
        return this.id == preferencesRepository.getPreferences().correlation.id
    }

    suspend fun updateLocalDBAccordingToEvent(
        deserializedWebSocketEvent: WebSocketEvent
    ) {
        when (val currentRoute = SyncServerRoute.valueOf(deserializedWebSocketEvent.operation)) {

            SyncServerRoute.UPDATE_FOLDER -> {
                val folderDTO =
                    json.decodeFromJsonElement<FolderDTO>(deserializedWebSocketEvent.payload)
                if (folderDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(folderDTO.eventTimestamp)
                    return
                }
                val localFolderId = localFoldersRepo.getLocalIdOfAFolder(folderDTO.id)
                if (localFolderId != null) {
                    localFoldersRepo.updateFolder(
                        viaSocket = true, folder = Folder(
                            name = folderDTO.name,
                            note = folderDTO.note,
                            parentFolderId = if (folderDTO.parentFolderId == null) null else localFoldersRepo.getLocalIdOfAFolder(
                                folderDTO.parentFolderId
                            ),
                            localId = localFolderId,
                            remoteId = folderDTO.id,
                            isArchived = folderDTO.isArchived,
                            lastModified = folderDTO.eventTimestamp,
                        )
                    ).collectAndUpdateTimestamp(folderDTO.eventTimestamp)
                }
            }

            SyncServerRoute.CREATE_TAG -> {
                val tagDto =
                    Json.decodeFromJsonElement<TagDTO>(deserializedWebSocketEvent.payload)
                if (tagDto.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(tagDto.eventTimestamp)
                    return
                }
                localTagsRepo.createATag(
                    viaSocket = true, tag = Tag(
                        remoteId = tagDto.id,
                        lastModified = tagDto.eventTimestamp,
                        name = tagDto.name
                    )
                ).collectAndUpdateTimestamp(tagDto.eventTimestamp)
            }

            SyncServerRoute.DELETE_TAG -> {
                val iDBasedDTO =
                    Json.decodeFromJsonElement<IDBasedDTO>(deserializedWebSocketEvent.payload)
                if (iDBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(iDBasedDTO.eventTimestamp)
                    return
                }
                localTagsRepo.deleteATag(
                    viaSocket = true, id = localTagsRepo.getLocalTagId(iDBasedDTO.id)
                ).collectAndUpdateTimestamp(iDBasedDTO.eventTimestamp)
            }

            SyncServerRoute.RENAME_TAG -> {
                val renameTagDTO =
                    Json.decodeFromJsonElement<RenameTagDTO>(deserializedWebSocketEvent.payload)
                if (renameTagDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(renameTagDTO.eventTimestamp)
                    return
                }
                localTagsRepo.renameATag(
                    viaSocket = true,
                    localTagId = localTagsRepo.getLocalTagId(renameTagDTO.id),
                    newName = renameTagDTO.newName,
                ).collectAndUpdateTimestamp(renameTagDTO.eventTimestamp)
            }

            SyncServerRoute.COPY_EXISTING_ITEMS -> {
                val copyItemsSocketResponseDTO =
                    Json.decodeFromJsonElement<CopyItemsSocketResponseDTO>(
                        deserializedWebSocketEvent.payload
                    )
                if (copyItemsSocketResponseDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        copyItemsSocketResponseDTO.eventTimestamp
                    )
                    return
                }
                remoteSyncRepo.applyUpdatesFromRemote(copyItemsSocketResponseDTO.eventTimestamp - 1)
                    .collectAndUpdateTimestamp(
                        copyItemsSocketResponseDTO.eventTimestamp
                    )
            }

            SyncServerRoute.UNARCHIVE_MULTIPLE_ITEMS -> {
                val markItemsRegularDTO = Json.decodeFromJsonElement<MarkItemsRegularDTO>(
                    deserializedWebSocketEvent.payload
                )
                if (markItemsRegularDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        markItemsRegularDTO.eventTimestamp
                    )
                    return
                }
                localMultiActionRepo.unArchiveMultipleItems(
                    linkIds = markItemsRegularDTO.linkIds.map {
                        localLinksRepo.getLocalLinkId(it) ?: -45454
                    }, folderIds = markItemsRegularDTO.foldersIds.map {
                        localFoldersRepo.getLocalIdOfAFolder(it) ?: -45454
                    }, viaSocket = true
                ).collectAndUpdateTimestamp(markItemsRegularDTO.eventTimestamp)
            }

            SyncServerRoute.DELETE_EVERYTHING -> {
                val deleteEverythingDTO = Json.decodeFromJsonElement<DeleteEverythingDTO>(
                    deserializedWebSocketEvent.payload
                )
                if (deleteEverythingDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        deleteEverythingDTO.eventTimestamp
                    )
                    return
                }
                remoteSyncRepo.deleteEverything(deleteOnRemote = false).collectAndUpdateTimestamp(
                    deleteEverythingDTO.eventTimestamp
                )
            }

            SyncServerRoute.DELETE_MULTIPLE_ITEMS -> {
                val deleteMultipleItemsDTO =
                    Json.decodeFromJsonElement<DeleteMultipleItemsDTO>(
                        deserializedWebSocketEvent.payload
                    )

                if (deleteMultipleItemsDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        deleteMultipleItemsDTO.eventTimestamp
                    )
                    return
                }

                localMultiActionRepo.deleteMultipleItems(linkIds = deleteMultipleItemsDTO.linkIds.map {
                    localLinksRepo.getLocalLinkId(it) ?: -45454
                }, folderIds = deleteMultipleItemsDTO.folderIds.map {
                    localFoldersRepo.getLocalIdOfAFolder(it) ?: -45454
                }, viaSocket = true)
                    .collectAndUpdateTimestamp(deleteMultipleItemsDTO.eventTimestamp)
            }

            SyncServerRoute.MOVE_EXISTING_ITEMS -> {
                val moveItemsDTO =
                    Json.decodeFromJsonElement<MoveItemsDTO>(deserializedWebSocketEvent.payload)

                if (moveItemsDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        moveItemsDTO.eventTimestamp
                    )
                    return
                }

                localMultiActionRepo.moveMultipleItems(
                    viaSocket = true,
                    linkIds = moveItemsDTO.linkIds.map {
                        localLinksRepo.getLocalLinkId(it) ?: -45454
                    },
                    folderIds = moveItemsDTO.folderIds.map {
                        localFoldersRepo.getLocalIdOfAFolder(it) ?: -45454
                    },
                    linkType = moveItemsDTO.linkType,
                    newParentFolderId = localFoldersRepo.getLocalIdOfAFolder(moveItemsDTO.newParentFolderId)
                        ?: -45454
                ).collectAndUpdateTimestamp(moveItemsDTO.eventTimestamp)
            }

            SyncServerRoute.ARCHIVE_MULTIPLE_ITEMS -> {
                val archiveMoveItemsDTO =
                    Json.decodeFromJsonElement<ArchiveMultipleItemsDTO>(
                        deserializedWebSocketEvent.payload
                    )

                if (archiveMoveItemsDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        archiveMoveItemsDTO.eventTimestamp
                    )
                    return
                }

                localMultiActionRepo.archiveMultipleItems(linkIds = archiveMoveItemsDTO.linkIds.map {
                    localLinksRepo.getLocalLinkId(it) ?: -45454
                }, folderIds = archiveMoveItemsDTO.folderIds.map {
                    localFoldersRepo.getLocalIdOfAFolder(it) ?: -45454
                }, viaSocket = true).collectAndUpdateTimestamp(archiveMoveItemsDTO.eventTimestamp)
            }

            SyncServerRoute.MARK_FOLDERS_AS_ROOT -> {
                val markSelectedFoldersAsRootDTO =
                    Json.decodeFromJsonElement<MarkSelectedFoldersAsRootDTO>(
                        deserializedWebSocketEvent.payload
                    )
                if (markSelectedFoldersAsRootDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        markSelectedFoldersAsRootDTO.eventTimestamp
                    )
                    return
                }
                localFoldersRepo.markFoldersAsRoot(
                    folderIDs = markSelectedFoldersAsRootDTO.folderIds.map {
                        localFoldersRepo.getLocalIdOfAFolder(it) ?: -45454
                    }, viaSocket = true
                ).collectAndUpdateTimestamp(markSelectedFoldersAsRootDTO.eventTimestamp)
            }

            SyncServerRoute.DELETE_DUPLICATE_LINKS -> {
                val deleteDuplicateLinksDTO =
                    Json.decodeFromJsonElement<DeleteDuplicateLinksDTO>(
                        deserializedWebSocketEvent.payload
                    )

                if (deleteDuplicateLinksDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        deleteDuplicateLinksDTO.eventTimestamp
                    )
                    return
                }

                localLinksRepo.deleteLinksLocally(deleteDuplicateLinksDTO.linkIds.map {
                    localLinksRepo.getLocalLinkId(it) ?: -45454
                }).collectAndUpdateTimestamp(deleteDuplicateLinksDTO.eventTimestamp)
            }

            // folders:

            SyncServerRoute.CREATE_FOLDER -> {
                val folderDto = json.decodeFromJsonElement<FolderDTO>(
                    deserializedWebSocketEvent.payload
                )
                if (folderDto.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(folderDto.eventTimestamp)
                    return
                }
                localFoldersRepo.insertANewFolder(
                    folder = Folder(
                        name = folderDto.name,
                        note = folderDto.note,
                        parentFolderId = if (folderDto.parentFolderId != null) localFoldersRepo.getLocalIdOfAFolder(
                            folderDto.parentFolderId
                        ) else null,
                        remoteId = folderDto.id,
                        isArchived = folderDto.isArchived,
                        lastModified = folderDto.eventTimestamp
                    ), ignoreFolderAlreadyExistsException = true, viaSocket = true
                ).collectAndUpdateTimestamp(folderDto.eventTimestamp)
            }

            SyncServerRoute.DELETE_FOLDER -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )
                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val folderId = localFoldersRepo.getLocalIdOfAFolder(idBasedDTO.id)
                if (folderId != null) {
                    localFoldersRepo.deleteAFolder(folderId, viaSocket = true)
                        .collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            SyncServerRoute.MARK_FOLDER_AS_ARCHIVE -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )
                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val folderId = localFoldersRepo.getLocalIdOfAFolder(idBasedDTO.id)
                if (folderId != null) {
                    localFoldersRepo.markFolderAsArchive(
                        folderId, viaSocket = true
                    ).collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            SyncServerRoute.MARK_AS_REGULAR_FOLDER -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )
                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val folderId = localFoldersRepo.getLocalIdOfAFolder(idBasedDTO.id)
                if (folderId != null) {
                    localFoldersRepo.markFolderAsRegularFolder(
                        folderId, viaSocket = true
                    ).collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            SyncServerRoute.UPDATE_FOLDER_NAME -> {
                val updateFolderNameDTO = json.decodeFromJsonElement<UpdateFolderNameDTO>(
                    deserializedWebSocketEvent.payload
                )
                if (updateFolderNameDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(updateFolderNameDTO.eventTimestamp)
                    return
                }

                val localFolderId =
                    localFoldersRepo.getLocalIdOfAFolder(updateFolderNameDTO.folderId)
                if (localFolderId != null) {
                    localFoldersRepo.getThisFolderData(localFolderId).collectLatest {
                        it.onSuccess {
                            localFoldersRepo.updateLocalFolderData(
                                it.data.copy(
                                    localId = localFolderId,
                                    name = updateFolderNameDTO.newFolderName
                                )
                            ).collect()
                            preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                updateFolderNameDTO.eventTimestamp
                            )
                        }
                    }
                }
            }

            SyncServerRoute.UPDATE_FOLDER_NOTE -> {
                val updateFolderNoteDTO = json.decodeFromJsonElement<UpdateFolderNoteDTO>(
                    deserializedWebSocketEvent.payload
                )
                if (updateFolderNoteDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(updateFolderNoteDTO.eventTimestamp)
                    return
                }

                val localFolderId =
                    localFoldersRepo.getLocalIdOfAFolder(updateFolderNoteDTO.folderId)
                if (localFolderId != null) {
                    localFoldersRepo.getThisFolderData(localFolderId).collectLatest {
                        it.onSuccess {
                            localFoldersRepo.updateLocalFolderData(
                                it.data.copy(
                                    localId = localFolderId, note = updateFolderNoteDTO.newNote
                                )
                            ).collect()
                            preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                updateFolderNoteDTO.eventTimestamp
                            )
                        }
                    }
                }
            }

            SyncServerRoute.DELETE_FOLDER_NOTE -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )
                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val localFolderId = localFoldersRepo.getLocalIdOfAFolder(idBasedDTO.id)
                if (localFolderId != null) {
                    localFoldersRepo.getThisFolderData(localFolderId).collectLatest {
                        it.onSuccess {
                            localFoldersRepo.deleteAFolderNote(
                                localFolderId, viaSocket = true
                            ).collect()
                            preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                        }
                    }
                }
            }


            // links:

            SyncServerRoute.UPDATE_LINK_TITLE -> {
                val updateTitleOfTheLinkDTO = json.decodeFromJsonElement<UpdateTitleOfTheLinkDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (updateTitleOfTheLinkDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        updateTitleOfTheLinkDTO.eventTimestamp
                    )
                    return
                }

                val localLinkId = localLinksRepo.getLocalLinkId(updateTitleOfTheLinkDTO.linkId)
                if (localLinkId != null) {
                    localLinksRepo.updateLinkTitle(
                        localLinkId, updateTitleOfTheLinkDTO.newTitleOfTheLink, viaSocket = true
                    ).collectAndUpdateTimestamp(updateTitleOfTheLinkDTO.eventTimestamp)
                }
            }

            SyncServerRoute.UPDATE_LINK_NOTE -> {
                val updateNoteOfALinkDTO = json.decodeFromJsonElement<UpdateNoteOfALinkDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (updateNoteOfALinkDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(updateNoteOfALinkDTO.eventTimestamp)
                    return
                }

                val localLinkId = localLinksRepo.getLocalLinkId(updateNoteOfALinkDTO.linkId)
                if (localLinkId != null) {
                    localLinksRepo.updateLinkNote(
                        localLinkId, updateNoteOfALinkDTO.newNote, viaSocket = true
                    ).collectAndUpdateTimestamp(updateNoteOfALinkDTO.eventTimestamp)
                }
            }

            SyncServerRoute.DELETE_A_LINK -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val localLinkId = localLinksRepo.getLocalLinkId(idBasedDTO.id)
                if (localLinkId != null) {
                    localLinksRepo.deleteALink(localLinkId, viaSocket = true)
                        .collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            SyncServerRoute.ARCHIVE_LINK -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val localLinkId = localLinksRepo.getLocalLinkId(idBasedDTO.id)
                if (localLinkId != null) {
                    localLinksRepo.archiveALink(localLinkId, viaSocket = true)
                        .collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            SyncServerRoute.UNARCHIVE_LINK -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val localLinkId = localLinksRepo.getLocalLinkId(idBasedDTO.id)
                if (localLinkId != null) {
                    val link = localLinksRepo.getALink(localLinkId)
                    localLinksRepo.updateALink(
                        link = link.copy(linkType = LinkType.SAVED_LINK),
                        updatedLinkTagsPair = null,
                        viaSocket = true
                    ).collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            SyncServerRoute.MARK_AS_IMP -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val localLinkId = localLinksRepo.getLocalLinkId(idBasedDTO.id)
                if (localLinkId != null) {
                    val link = localLinksRepo.getALink(localLinkId)
                    localLinksRepo.addANewLink(
                        link = link.copy(linkType = LinkType.IMPORTANT_LINK),
                        linkSaveConfig = LinkSaveConfig.forceSaveWithoutRetrieving(),
                        viaSocket = true,
                        selectedTagIds = localTagsRepo.getTags(localLinkId).map { it.localId })
                        .collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            SyncServerRoute.UNMARK_AS_IMP -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val localLinkId = localLinksRepo.getLocalLinkId(idBasedDTO.id)
                if (localLinkId != null) {
                    localLinksRepo.deleteALink(
                        linkId = localLinkId, viaSocket = true
                    ).collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            SyncServerRoute.UPDATE_LINK -> {
                val linkDTO = json.decodeFromJsonElement<LinkDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (linkDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(linkDTO.eventTimestamp)
                    return
                }

                val localLinkId = localLinksRepo.getLocalLinkId(linkDTO.id)
                if (localLinkId != null) {
                    val link = localLinksRepo.getALink(localLinkId)
                    localLinksRepo.updateALink(
                        link.copy(
                            linkType = linkDTO.linkType,
                            title = linkDTO.title,
                            url = linkDTO.url,
                            imgURL = linkDTO.imgURL,
                            note = linkDTO.note,
                            idOfLinkedFolder = when (linkDTO.linkType) {
                                LinkType.SAVED_LINK -> Constants.SAVED_LINKS_ID
                                LinkType.HISTORY_LINK -> Constants.HISTORY_ID
                                LinkType.IMPORTANT_LINK -> Constants.IMPORTANT_LINKS_ID
                                LinkType.ARCHIVE_LINK -> Constants.ARCHIVE_ID
                                else -> linkDTO.idOfLinkedFolder
                            },
                            userAgent = linkDTO.userAgent,
                            mediaType = linkDTO.mediaType,
                        ), updatedLinkTagsPair = LinkTagsPair(
                            link = link,
                            tags = localTagsRepo.getLocalTags(remoteIds = linkDTO.linkTags.map {
                                it.tagId
                            })
                        ), viaSocket = true
                    ).collectAndUpdateTimestamp(linkDTO.eventTimestamp)
                }
            }

            SyncServerRoute.CREATE_A_NEW_LINK -> {
                val linkDTO = json.decodeFromJsonElement<LinkDTO>(
                    deserializedWebSocketEvent.payload
                )
                linkoraLog("Received ${linkDTO.linkTags}")
                if (linkDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(linkDTO.eventTimestamp)
                    return
                }

                localLinksRepo.addANewLink(
                    link = Link(
                        linkType = linkDTO.linkType,
                        title = linkDTO.title,
                        url = linkDTO.url,
                        imgURL = linkDTO.imgURL,
                        note = linkDTO.note,
                        idOfLinkedFolder = if (linkDTO.idOfLinkedFolder != null) localFoldersRepo.getLocalIdOfAFolder(
                            linkDTO.idOfLinkedFolder
                        ) else null,
                        remoteId = linkDTO.id,
                        userAgent = linkDTO.userAgent,
                        mediaType = linkDTO.mediaType,
                        lastModified = linkDTO.eventTimestamp
                    ),
                    linkSaveConfig = LinkSaveConfig.forceSaveWithoutRetrieving(),
                    viaSocket = true,
                    selectedTagIds = localTagsRepo.getLocalTagIds(linkDTO.linkTags.map { it.tagId })
                ).collectAndUpdateTimestamp(linkDTO.eventTimestamp)
            }


            // panels:

            SyncServerRoute.ADD_A_NEW_PANEL -> {
                val panelDTO =
                    json.decodeFromJsonElement<PanelDTO>(deserializedWebSocketEvent.payload)

                if (panelDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(panelDTO.eventTimestamp)
                    return
                }

                localPanelsRepo.addANewPanel(
                    Panel(
                        panelName = panelDTO.panelName,
                        remoteId = panelDTO.panelId,
                        lastModified = panelDTO.eventTimestamp
                    ), viaSocket = true
                ).collectAndUpdateTimestamp(panelDTO.eventTimestamp)
            }

            SyncServerRoute.ADD_A_NEW_FOLDER_IN_A_PANEL -> {
                val panelFolderDTO = json.decodeFromJsonElement<PanelFolderDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (panelFolderDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(panelFolderDTO.eventTimestamp)
                    return
                }

                val localFolderId = localFoldersRepo.getLocalIdOfAFolder(panelFolderDTO.folderId)
                val localPanelsId = localPanelsRepo.getLocalPanelId(panelFolderDTO.connectedPanelId)
                if (localPanelsId != null && localFolderId != null) {
                    localPanelsRepo.addANewFolderInAPanel(
                        PanelFolder(
                            folderId = localFolderId,
                            panelPosition = panelFolderDTO.panelPosition,
                            folderName = panelFolderDTO.folderName,
                            connectedPanelId = localPanelsId,
                            remoteId = panelFolderDTO.id,
                            lastModified = panelFolderDTO.eventTimestamp
                        ), viaSocket = true
                    ).collectAndUpdateTimestamp(panelFolderDTO.eventTimestamp)
                }
            }

            SyncServerRoute.DELETE_A_PANEL -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val localPanelId = localPanelsRepo.getLocalPanelId(idBasedDTO.id)
                if (localPanelId != null) {
                    localPanelsRepo.deleteAPanel(localPanelId, viaSocket = true)
                        .collectAndUpdateTimestamp(
                            idBasedDTO.eventTimestamp
                        )
                }
            }

            SyncServerRoute.UPDATE_A_PANEL_NAME -> {
                val updatePanelNameDTO = json.decodeFromJsonElement<UpdatePanelNameDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (updatePanelNameDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(updatePanelNameDTO.eventTimestamp)
                    return
                }

                val localPanelId = localPanelsRepo.getLocalPanelId(updatePanelNameDTO.panelId)
                if (localPanelId != null) {
                    localPanelsRepo.updateAPanelName(
                        newName = updatePanelNameDTO.newName,
                        panelId = localPanelId,
                        viaSocket = true
                    ).collectAndUpdateTimestamp(updatePanelNameDTO.eventTimestamp)
                }
            }

            SyncServerRoute.DELETE_A_FOLDER_FROM_ALL_PANELS -> {
                val idBasedDTO = json.decodeFromJsonElement<IDBasedDTO>(
                    deserializedWebSocketEvent.payload
                )

                if (idBasedDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                    return
                }

                val localFolderId = localFoldersRepo.getLocalIdOfAFolder(idBasedDTO.id)
                if (localFolderId != null) {
                    localPanelsRepo.deleteAFolderFromAllPanels(localFolderId)
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(idBasedDTO.eventTimestamp)
                }
            }

            SyncServerRoute.DELETE_A_FOLDER_FROM_A_PANEL -> {
                val deleteAFolderFromAPanelDTO =
                    json.decodeFromJsonElement<DeleteAFolderFromAPanelDTO>(
                        deserializedWebSocketEvent.payload
                    )

                if (deleteAFolderFromAPanelDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        deleteAFolderFromAPanelDTO.eventTimestamp
                    )
                    return
                }

                val localFolderId =
                    localFoldersRepo.getLocalIdOfAFolder(deleteAFolderFromAPanelDTO.folderID)
                val localPanelId =
                    localPanelsRepo.getLocalPanelId(deleteAFolderFromAPanelDTO.panelId)
                if (localFolderId != null && localPanelId != null) {
                    localPanelsRepo.deleteAFolderFromAPanel(
                        localPanelId, localFolderId, viaSocket = true
                    ).collectAndUpdateTimestamp(deleteAFolderFromAPanelDTO.eventTimestamp)
                }
            }

            SyncServerRoute.CHANGE_PARENT_FOLDER -> linkoraLog("Nothing to do on $currentRoute")
            SyncServerRoute.UPDATE_LINKED_FOLDER_ID -> linkoraLog("Nothing to do on $currentRoute")
            SyncServerRoute.UPDATE_USER_AGENT -> linkoraLog("Nothing to do on $currentRoute")
            SyncServerRoute.GET_LINKS_FROM_A_FOLDER -> linkoraLog("Nothing to do on $currentRoute")
            SyncServerRoute.GET_LINKS -> linkoraLog("Nothing to do on $currentRoute")
            SyncServerRoute.TEST_BEARER -> linkoraLog("Nothing to do on $currentRoute")
            SyncServerRoute.GET_UPDATES -> linkoraLog("Nothing to do on $currentRoute")
            SyncServerRoute.GET_TOMBSTONES -> linkoraLog("Nothing to do on $currentRoute")

            SyncServerRoute.FORCE_SET_DEFAULT_FOLDER_TO_INTERNAL_IDS -> {
                val timeStampBasedResponse = json.decodeFromJsonElement<TimeStampBasedResponse>(
                    deserializedWebSocketEvent.payload
                )
                localLinksRepo.forceSetDefaultFolderToInternalIds(viaSocket = true)
                    .collectAndUpdateTimestamp(timeStampBasedResponse.eventTimestamp)
            }
        }
    }
}