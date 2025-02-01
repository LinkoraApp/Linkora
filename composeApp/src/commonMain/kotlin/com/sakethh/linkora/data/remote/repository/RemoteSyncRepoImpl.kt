package com.sakethh.linkora.data.remote.repository

import com.sakethh.linkora.common.network.Network
import com.sakethh.linkora.common.utils.asWebSocketUrl
import com.sakethh.linkora.common.utils.catchAsThrowableAndEmitFailure
import com.sakethh.linkora.common.utils.forceSaveWithoutRetrieving
import com.sakethh.linkora.common.utils.isSameAsCurrentClient
import com.sakethh.linkora.common.utils.updateLastSyncedWithServerTimeStamp
import com.sakethh.linkora.common.utils.wrappedResultFlow
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.RemoteRoute
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.asAddFolderDTO
import com.sakethh.linkora.domain.asAddLinkDTO
import com.sakethh.linkora.domain.dto.server.AllTablesDTO
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.TombstoneDTO
import com.sakethh.linkora.domain.dto.server.folder.AddFolderDTO
import com.sakethh.linkora.domain.dto.server.folder.FolderDTO
import com.sakethh.linkora.domain.dto.server.folder.UpdateFolderNameDTO
import com.sakethh.linkora.domain.dto.server.folder.UpdateFolderNoteDTO
import com.sakethh.linkora.domain.dto.server.link.AddLinkDTO
import com.sakethh.linkora.domain.dto.server.link.LinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateNoteOfALinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateTitleOfTheLinkDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelFolderDTO
import com.sakethh.linkora.domain.dto.server.panel.DeleteAPanelFromAFolderDTO
import com.sakethh.linkora.domain.dto.server.panel.PanelDTO
import com.sakethh.linkora.domain.dto.server.panel.PanelFolderDTO
import com.sakethh.linkora.domain.dto.server.panel.UpdatePanelNameDTO
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.model.WebSocketEvent
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.domain.repository.remote.RemoteLinksRepo
import com.sakethh.linkora.domain.repository.remote.RemotePanelsRepo
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import io.ktor.client.call.body
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.websocket.Frame
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import java.util.UUID

class RemoteSyncRepoImpl(
    private val localFoldersRepo: LocalFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val authToken: () -> String,
    private val baseUrl: () -> String,
    private val pendingSyncQueueRepo: PendingSyncQueueRepo,
    private val remoteFoldersRepo: RemoteFoldersRepo,
    private val remoteLinksRepo: RemoteLinksRepo,
    private val remotePanelsRepo: RemotePanelsRepo,
    private val preferencesRepository: PreferencesRepository
) : RemoteSyncRepo {
    private val json = Json {
        this.ignoreUnknownKeys = true
        this.encodeDefaults = true
        this.isLenient = true
        this.prettyPrint = true
    }
    override suspend fun readSocketEvents(currentCorrelation: Correlation): Flow<Result<Unit>> {
        return wrappedResultFlow {
            Network.client.webSocket(urlString = baseUrl().asWebSocketUrl() + "events", request = {
                bearerAuth(authToken())
                parameter(key = "correlation", value = Json.encodeToString(currentCorrelation))
            }) {
                this.incoming.consumeAsFlow().collectLatest {
                    if (it is Frame.Text) {
                        val deserializedWebSocketEvent =
                            json.decodeFromString<WebSocketEvent>((it.data).decodeToString())
                        updateLocalDBAccordingToEvent(deserializedWebSocketEvent)
                    }
                }
            }
        }
    }

    private suspend fun <T> Flow<Result<T>>.collectAndUpdateTimestamp(eventTimestamp: Long) {
        this.collectLatest {
            it.onSuccess {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(eventTimestamp)
            }
        }
    }

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

    override suspend fun pushPendingSyncQueueToServer(): Flow<Result<Unit>> {
        return wrappedResultFlow {
            pendingSyncQueueRepo.getAllItemsFromQueue().forEach { queueItem ->
                when (queueItem.operation) {
                    RemoteRoute.Folder.CREATE_FOLDER.name -> {
                        val addFolderDTO = Json.decodeFromString<AddFolderDTO>(queueItem.payload)
                        remoteFoldersRepo.createFolder(addFolderDTO.let {
                            it.copy(
                                parentFolderId = if (it.parentFolderId != null) localFoldersRepo.getRemoteIdOfAFolder(
                                    it.parentFolderId
                                ) else null
                            )
                        }).collectLatest {
                            it.onSuccess { remoteResponse ->
                                localFoldersRepo.getThisFolderData(addFolderDTO.offlineSyncItemId)
                                    .collectLatest {
                                        it.onSuccess {
                                            localFoldersRepo.updateLocalFolderData(
                                                it.data.copy(
                                                    remoteId = remoteResponse.data.id,
                                                )
                                            ).collect()
                                            preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                                remoteResponse.data.timeStampBasedResponse.eventTimestamp
                                            )
                                            pendingSyncQueueRepo.removeFromQueue(queueItem.id)
                                        }
                                    }
                            }
                        }
                    }

                    RemoteRoute.Folder.DELETE_FOLDER.name -> {
                        val idBasedDTO = Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteId = localFoldersRepo.getRemoteIdOfAFolder(idBasedDTO.id)!!
                        remoteFoldersRepo.deleteFolder(remoteId).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                    }

                    RemoteRoute.Folder.MARK_FOLDER_AS_ARCHIVE.name -> {
                        val idBasedDTO = Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteId = localFoldersRepo.getRemoteIdOfAFolder(idBasedDTO.id)!!
                        remoteFoldersRepo.markAsArchive(remoteId).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                    }

                    RemoteRoute.Folder.MARK_AS_REGULAR_FOLDER.name -> {
                        val idBasedDTO = Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteId = localFoldersRepo.getRemoteIdOfAFolder(idBasedDTO.id)!!
                        remoteFoldersRepo.markAsRegularFolder(remoteId)
                            .removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                    }

                    RemoteRoute.Folder.UPDATE_FOLDER_NAME.name -> {
                        val updateFolderNameDTO =
                            Json.decodeFromString<UpdateFolderNameDTO>(queueItem.payload)
                        val remoteId =
                            localFoldersRepo.getRemoteIdOfAFolder(updateFolderNameDTO.folderId)!!
                        remoteFoldersRepo.updateFolderName(
                            remoteId,
                            updateFolderNameDTO.newFolderName
                        ).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                    }

                    RemoteRoute.Folder.UPDATE_FOLDER_NOTE.name -> {
                        val updateFolderNoteDTO =
                            Json.decodeFromString<UpdateFolderNoteDTO>(queueItem.payload)
                        val remoteId =
                            localFoldersRepo.getRemoteIdOfAFolder(updateFolderNoteDTO.folderId)!!
                        remoteFoldersRepo.updateFolderNote(
                            remoteId,
                            updateFolderNoteDTO.newNote
                        ).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                    }

                    RemoteRoute.Folder.DELETE_FOLDER_NOTE.name -> {
                        val idBasedDTO = Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteId = localFoldersRepo.getRemoteIdOfAFolder(idBasedDTO.id)!!
                        remoteFoldersRepo.deleteFolderNote(remoteId)
                            .removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                    }

                    RemoteRoute.Link.UPDATE_LINK_TITLE.name -> {
                        val linkDTO =
                            Json.decodeFromString<LinkDTO>(queueItem.payload)
                        val remoteLinkId = localLinksRepo.getRemoteLinkId(linkDTO.id)!!
                        remoteLinksRepo.updateLink(
                            linkDTO.copy(
                                id = remoteLinkId
                            )
                        ).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                    }

                    RemoteRoute.Link.UPDATE_LINK_NOTE.name -> {
                        val linkDTO =
                            Json.decodeFromString<LinkDTO>(queueItem.payload)
                        val remoteLinkId = localLinksRepo.getRemoteLinkId(linkDTO.id)!!
                        remoteLinksRepo.updateLink(linkDTO.copy(id = remoteLinkId))
                            .removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                    }

                    RemoteRoute.Link.DELETE_A_LINK.name -> {
                        val idBasedDTO = Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteLinkId = localLinksRepo.getRemoteLinkId(idBasedDTO.id)!!
                        remoteLinksRepo.deleteALink(remoteLinkId).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                    }

                    RemoteRoute.Link.ARCHIVE_LINK.name -> {
                        val idBasedDTO = Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteLinkId = localLinksRepo.getRemoteLinkId(idBasedDTO.id)!!
                        remoteLinksRepo.archiveALink(remoteLinkId).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                    }

                    RemoteRoute.Link.UNARCHIVE_LINK.name -> {
                        val idBasedDTO = Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteLinkId = localLinksRepo.getRemoteLinkId(idBasedDTO.id)!!
                        remoteLinksRepo.unArchiveALink(remoteLinkId)
                            .removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                    }

                    RemoteRoute.Link.UPDATE_LINK.name -> {
                        val linkDTO = Json.decodeFromString<LinkDTO>(queueItem.payload)
                        val remoteId = localLinksRepo.getRemoteLinkId(linkDTO.id)!!
                        remoteLinksRepo.updateLink(linkDTO.copy(id = remoteId))
                            .removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                    }

                    RemoteRoute.Link.CREATE_A_NEW_LINK.name -> {
                        val addLinkDTO = Json.decodeFromString<AddLinkDTO>(queueItem.payload)
                        remoteLinksRepo.addANewLink(
                            addLinkDTO.copy(
                                idOfLinkedFolder = if (addLinkDTO.idOfLinkedFolder != null) localFoldersRepo.getRemoteIdOfAFolder(
                                    addLinkDTO.idOfLinkedFolder
                                ) else null
                            )
                        ).collectLatest {
                            it.onSuccess { remoteResponse ->
                                val updatedLink =
                                    localLinksRepo.getALink(addLinkDTO.offlineSyncItemId)
                                        .copy(remoteId = remoteResponse.data.id)
                                localLinksRepo.updateALink(updatedLink, viaSocket = true).collect()
                                preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                    remoteResponse.data.timeStampBasedResponse.eventTimestamp
                                )
                                pendingSyncQueueRepo.removeFromQueue(queueItem.id)
                            }
                        }
                    }

                    RemoteRoute.Panel.ADD_A_NEW_PANEL.name -> {
                        val addANewPanelDTO =
                            Json.decodeFromString<AddANewPanelDTO>(queueItem.payload)
                        remotePanelsRepo.addANewPanel(addANewPanelDTO).collectLatest {
                            it.onSuccess { remoteResponse ->
                                val updatedPanel =
                                    localPanelsRepo.getPanel(addANewPanelDTO.offlineSyncItemId)
                                        .copy(remoteId = remoteResponse.data.id)
                                localPanelsRepo.updatePanel(updatedPanel)
                                preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                    remoteResponse.data.timeStampBasedResponse.eventTimestamp
                                )
                                pendingSyncQueueRepo.removeFromQueue(queueItem.id)
                            }
                        }
                    }

                    RemoteRoute.Panel.ADD_A_NEW_FOLDER_IN_A_PANEL.name -> {
                        val addANewPanelFolderDTO =
                            Json.decodeFromString<AddANewPanelFolderDTO>(queueItem.payload)
                        val remoteFolderId =
                            localPanelsRepo.getRemotePanelId(addANewPanelFolderDTO.folderId)!!
                        val remoteConnectedPanelId =
                            localPanelsRepo.getRemotePanelId(addANewPanelFolderDTO.connectedPanelId)!!
                        remotePanelsRepo.addANewFolderInAPanel(
                            addANewPanelFolderDTO.copy(
                                folderId = remoteFolderId, connectedPanelId = remoteConnectedPanelId
                            )
                        ).collectLatest {
                            it.onSuccess { remoteResponse ->
                                val updatedPanel =
                                    localPanelsRepo.getPanel(addANewPanelFolderDTO.offlineSyncItemId)
                                        .copy(remoteId = remoteResponse.data.id)
                                preferencesRepository.updateLastSyncedWithServerTimeStamp(
                                    remoteResponse.data.timeStampBasedResponse.eventTimestamp
                                )
                                localPanelsRepo.updatePanel(updatedPanel)
                                pendingSyncQueueRepo.removeFromQueue(queueItem.id)
                            }
                        }
                    }

                    RemoteRoute.Panel.DELETE_A_PANEL.name -> {
                        val idBasedDTO = Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteId = localPanelsRepo.getRemotePanelId(idBasedDTO.id)!!
                        remotePanelsRepo.deleteAPanel(remoteId).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                    }

                    RemoteRoute.Panel.UPDATE_A_PANEL_NAME.name -> {
                        val updatePanelNameDTO =
                            Json.decodeFromString<UpdatePanelNameDTO>(queueItem.payload)
                        val remotePanelId =
                            localPanelsRepo.getRemotePanelId(updatePanelNameDTO.panelId)!!
                        remotePanelsRepo.updateAPanelName(updatePanelNameDTO.copy(panelId = remotePanelId))
                            .removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                    }

                    RemoteRoute.Panel.DELETE_A_FOLDER_FROM_ALL_PANELS.name -> {
                        val idBasedDTO = Json.decodeFromString<IDBasedDTO>(queueItem.payload)
                        val remoteFolderId = localFoldersRepo.getRemoteIdOfAFolder(idBasedDTO.id)!!
                        remotePanelsRepo.deleteAFolderFromAllPanels(remoteFolderId)
                            .removeQueueItemAndSyncTimestamp(
                                queueItem.id
                            )
                    }

                    RemoteRoute.Panel.DELETE_A_FOLDER_FROM_A_PANEL.name -> {
                        val deleteAPanelFromAFolderDTO =
                            Json.decodeFromString<DeleteAPanelFromAFolderDTO>(queueItem.payload)
                        val remoteFolderId =
                            localFoldersRepo.getRemoteIdOfAFolder(deleteAPanelFromAFolderDTO.folderID)!!
                        val remotePanelId =
                            localPanelsRepo.getRemotePanelId(deleteAPanelFromAFolderDTO.panelId)!!
                        remotePanelsRepo.deleteAFolderFromAPanel(
                            deleteAPanelFromAFolderDTO.copy(
                                panelId = remotePanelId,
                                folderID = remoteFolderId
                            )
                        ).removeQueueItemAndSyncTimestamp(
                            queueItem.id
                        )
                    }
                }
            }
        }
    }

    override suspend fun applyUpdatesFromRemote(timeStampAfter: Long): Flow<Result<Unit>> {


        /**
        `updateLocalDBAccordingToEvent` is supposed to handle "events" from the socket.

        i didn't feel like writing it all from scratch for this function (`applyUpdatesFromRemote`)
        so i just mocked it like, "oh, that’s not me, must be some other client 🤓☝️" and force update respectively.

        and that's why `randomCorrelation` exists.
         **/
        val randomCorrelation = Correlation(
            id = UUID.randomUUID().toString(), clientName = "🤨📸"
        )


        return channelFlow {
            send(Result.Loading("Fetching updates from server..."))
            Network.client.get(baseUrl() + RemoteRoute.SyncInLocalRoute.GET_UPDATES.name) {
                bearerAuth(authToken())
                contentType(ContentType.Application.Json)
                parameter("timestamp", timeStampAfter)
            }.body<AllTablesDTO>().let { remoteResponse ->
                send(Result.Loading("Received updates from server. Processing folders..."))

                remoteResponse.folders.forEach {
                    send(Result.Loading("Processing folder with id: ${it.id}"))
                    applyFolderUpdates(it)
                }

                coroutineScope {
                    awaitAll(async {
                        send(Result.Loading("Processing links..."))
                            remoteResponse.links.forEach { remoteLinkDTO ->
                                send(Result.Loading("Processing link with id: ${remoteLinkDTO.id}"))
                                val localId = localLinksRepo.getLocalLinkId(remoteLinkDTO.id)
                                if (localId == null) {
                                    send(Result.Loading("Creating new link with id: ${remoteLinkDTO.id}"))
                                    updateLocalDBAccordingToEvent(
                                        WebSocketEvent(
                                            operation = RemoteRoute.Link.CREATE_A_NEW_LINK.name,
                                            payload = json.encodeToJsonElement(
                                                remoteLinkDTO.copy(correlation = randomCorrelation)
                                            )
                                        )
                                    )
                                } else {
                                    send(Result.Loading("Updating existing link with id: ${remoteLinkDTO.id}"))
                                    updateLocalDBAccordingToEvent(
                                        WebSocketEvent(
                                            operation = RemoteRoute.Link.UPDATE_LINK.name,
                                            payload = json.encodeToJsonElement(
                                                remoteLinkDTO.copy(correlation = randomCorrelation)
                                            )
                                        )
                                    )
                                }
                            }
                    }, async {
                        send(Result.Loading("Processing panels..."))
                            remoteResponse.panels.forEach { remotePanelDTO ->
                                send(Result.Loading("Processing panel with id: ${remotePanelDTO.panelId}"))
                                val localId =
                                    localPanelsRepo.getLocalPanelId(remotePanelDTO.panelId)
                                if (localId == null) {
                                    send(Result.Loading("Creating new panel with id: ${remotePanelDTO.panelId}"))
                                    updateLocalDBAccordingToEvent(
                                        WebSocketEvent(
                                            operation = RemoteRoute.Panel.ADD_A_NEW_PANEL.name,
                                            payload = json.encodeToJsonElement(
                                                remotePanelDTO.copy(correlation = randomCorrelation)
                                            )
                                        )
                                    )
                                } else {
                                    send(Result.Loading("Updating existing panel name for id: ${remotePanelDTO.panelId}"))
                                    updateLocalDBAccordingToEvent(
                                        WebSocketEvent(
                                            operation = RemoteRoute.Panel.UPDATE_A_PANEL_NAME.name,
                                            payload = json.encodeToJsonElement(
                                                UpdatePanelNameDTO(
                                                    newName = remotePanelDTO.panelName,
                                                    panelId = remotePanelDTO.panelId,
                                                    correlation = randomCorrelation
                                                )
                                            )
                                        )
                                    )
                                }
                            }

                        send(Result.Loading("Processing panel folders..."))
                            remoteResponse.panelFolders.forEach { remotePanelFolder ->
                                send(Result.Loading("Adding folder to panel with id: ${remotePanelFolder.id}"))
                                // a panel_folder can only be added with this endpoint response because if it got deleted, it will be caught in the tombstones response, not here
                                updateLocalDBAccordingToEvent(
                                    WebSocketEvent(
                                        operation = RemoteRoute.Panel.ADD_A_NEW_FOLDER_IN_A_PANEL.name,
                                        payload = json.encodeToJsonElement(
                                            remotePanelFolder.copy(correlation = randomCorrelation)
                                        )
                                    )
                                )
                            }
                    })
                }
            }
            send(Result.Success(Unit))
        }.catchAsThrowableAndEmitFailure()
    }

    /**
    parent folders (if any) get inserted first, so there's no need to search and insert parent folders first. also, the server returns from oldest to newest, so we get the advantage of knowing that root folders/parent folders are always inserted first.
     * */
    private suspend fun applyFolderUpdates(
        folderDTO: FolderDTO
    ) {
        val localIdOfCurrentFolder = localFoldersRepo.getLocalIdOfAFolder(folderDTO.id)
        if (localIdOfCurrentFolder != null) {
            localFoldersRepo.updateLocalFolderData(
                Folder(
                    name = folderDTO.name,
                    note = folderDTO.note,
                    parentFolderId = if (folderDTO.parentFolderId != null) localFoldersRepo.getLocalIdOfAFolder(
                        folderDTO.parentFolderId
                    ) else null,
                    localId = localIdOfCurrentFolder,
                    remoteId = folderDTO.id,
                    isArchived = folderDTO.isArchived
                )
            ).collectAndUpdateTimestamp(folderDTO.eventTimestamp)
        } else {
            localFoldersRepo.insertANewFolder(
                folder = Folder(
                    name = folderDTO.name,
                    note = folderDTO.note,
                    parentFolderId = if (folderDTO.parentFolderId != null) localFoldersRepo.getLocalIdOfAFolder(
                        folderDTO.parentFolderId
                    ) else null,
                    remoteId = folderDTO.id,
                    isArchived = folderDTO.isArchived
                ), ignoreFolderAlreadyExistsException = true, viaSocket = true
            ).collectAndUpdateTimestamp(folderDTO.eventTimestamp)
        }
    }

    override suspend fun applyUpdatesBasedOnRemoteTombstones(timeStampAfter: Long): Flow<Result<Unit>> {
        return wrappedResultFlow {
            Network.client.get(baseUrl() + RemoteRoute.SyncInLocalRoute.GET_TOMBSTONES.name) {
                bearerAuth(authToken())
                contentType(ContentType.Application.Json)
                parameter("timestamp", timeStampAfter)
            }.body<List<TombstoneDTO>>().map {
                WebSocketEvent(
                    operation = it.operation,
                    payload = it.payload
                )
            }.forEach {
                updateLocalDBAccordingToEvent(it)
            }
        }
    }


    private suspend fun updateLocalDBAccordingToEvent(
        deserializedWebSocketEvent: WebSocketEvent
    ) {
        when (deserializedWebSocketEvent.operation) {

            // folders:

            RemoteRoute.Folder.CREATE_FOLDER.name -> {
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
                        isArchived = folderDto.isArchived
                    ), ignoreFolderAlreadyExistsException = true, viaSocket = true
                ).collectAndUpdateTimestamp(folderDto.eventTimestamp)
            }

            RemoteRoute.Folder.DELETE_FOLDER.name -> {
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

            RemoteRoute.Folder.MARK_FOLDER_AS_ARCHIVE.name -> {
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

            RemoteRoute.Folder.MARK_AS_REGULAR_FOLDER.name -> {
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

            RemoteRoute.Folder.UPDATE_FOLDER_NAME.name -> {
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

            RemoteRoute.Folder.UPDATE_FOLDER_NOTE.name -> {
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

            RemoteRoute.Folder.DELETE_FOLDER_NOTE.name -> {
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

            RemoteRoute.Link.UPDATE_LINK_TITLE.name -> {
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

            RemoteRoute.Link.UPDATE_LINK_NOTE.name -> {
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

            RemoteRoute.Link.DELETE_A_LINK.name -> {
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

            RemoteRoute.Link.ARCHIVE_LINK.name -> {
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

            RemoteRoute.Link.UNARCHIVE_LINK.name -> {
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
                        link.copy(linkType = LinkType.SAVED_LINK), viaSocket = true
                    ).collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            RemoteRoute.Link.MARK_AS_IMP.name -> {
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
                        link.copy(markedAsImportant = true), viaSocket = true
                    ).collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            RemoteRoute.Link.UNMARK_AS_IMP.name -> {
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
                        link.copy(markedAsImportant = false), viaSocket = true
                    ).collectAndUpdateTimestamp(idBasedDTO.eventTimestamp)
                }
            }

            RemoteRoute.Link.UPDATE_LINK.name -> {
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
                            idOfLinkedFolder = if (linkDTO.idOfLinkedFolder != null) localFoldersRepo.getLocalIdOfAFolder(
                                linkDTO.idOfLinkedFolder
                            ) else null,
                            userAgent = linkDTO.userAgent,
                            markedAsImportant = linkDTO.markedAsImportant,
                            mediaType = linkDTO.mediaType
                        ), viaSocket = true
                    ).collectAndUpdateTimestamp(linkDTO.eventTimestamp)
                }
            }

            RemoteRoute.Link.CREATE_A_NEW_LINK.name -> {
                val linkDTO = json.decodeFromJsonElement<LinkDTO>(
                    deserializedWebSocketEvent.payload
                )

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
                        markedAsImportant = linkDTO.markedAsImportant,
                        mediaType = linkDTO.mediaType
                    ), linkSaveConfig = forceSaveWithoutRetrieving(), viaSocket = true
                ).collectAndUpdateTimestamp(linkDTO.eventTimestamp)
            }


            // panels:

            RemoteRoute.Panel.ADD_A_NEW_PANEL.name -> {
                val panelDTO =
                    json.decodeFromJsonElement<PanelDTO>(deserializedWebSocketEvent.payload)

                if (panelDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(panelDTO.eventTimestamp)
                    return
                }

                localPanelsRepo.addaNewPanel(
                    Panel(
                        panelName = panelDTO.panelName, remoteId = panelDTO.panelId
                    ), viaSocket = true
                ).collectAndUpdateTimestamp(panelDTO.eventTimestamp)
            }

            RemoteRoute.Panel.ADD_A_NEW_FOLDER_IN_A_PANEL.name -> {
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
                            remoteId = panelFolderDTO.id
                        ), viaSocket = true
                    ).collectAndUpdateTimestamp(panelFolderDTO.eventTimestamp)
                }
            }

            RemoteRoute.Panel.DELETE_A_PANEL.name -> {
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

            RemoteRoute.Panel.UPDATE_A_PANEL_NAME.name -> {
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

            RemoteRoute.Panel.DELETE_A_FOLDER_FROM_ALL_PANELS.name -> {
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

            RemoteRoute.Panel.DELETE_A_FOLDER_FROM_A_PANEL.name -> {
                val deleteAPanelFromAFolderDTO =
                    json.decodeFromJsonElement<DeleteAPanelFromAFolderDTO>(
                        deserializedWebSocketEvent.payload
                    )

                if (deleteAPanelFromAFolderDTO.correlation.isSameAsCurrentClient()) {
                    preferencesRepository.updateLastSyncedWithServerTimeStamp(
                        deleteAPanelFromAFolderDTO.eventTimestamp
                    )
                    return
                }

                val localFolderId =
                    localFoldersRepo.getLocalIdOfAFolder(deleteAPanelFromAFolderDTO.folderID)
                val localPanelId =
                    localPanelsRepo.getLocalPanelId(deleteAPanelFromAFolderDTO.panelId)
                if (localFolderId != null && localPanelId != null) {
                    localPanelsRepo.deleteAFolderFromAPanel(
                        localPanelId, localFolderId, viaSocket = true
                    ).collectAndUpdateTimestamp(deleteAPanelFromAFolderDTO.eventTimestamp)
                }
            }
        }
    }

    override suspend fun <T> SendChannel<Result<T>>.pushNonSyncedDataToServer() {
        send(Result.Loading(message = "[SYNC] Starting non-synced data push to server"))

        send(Result.Loading(message = "[FOLDERS] Fetching unsynced folders"))
        localFoldersRepo.getUnSyncedFolders().forEach { currentFolder ->
            send(Result.Loading(message = "[FOLDERS] Processing folder (ID: ${currentFolder.localId}, Name: ${currentFolder.name})"))
            pendingSyncQueueRepo.addInQueue(
                PendingSyncQueue(
                    operation = RemoteRoute.Folder.CREATE_FOLDER.name,
                    payload = Json.encodeToString(
                        currentFolder.asAddFolderDTO()
                            .copy(offlineSyncItemId = currentFolder.localId)
                    )
                )
            )
            send(Result.Loading(message = "[FOLDERS] Queued folder (ID: ${currentFolder.localId}) for sync"))
        }

        send(Result.Loading(message = "[LINKS] Fetching unsynced links"))
        localLinksRepo.getUnSyncedLinks().forEach { currentLink ->
            send(Result.Loading(message = "[LINKS] Processing link (ID: ${currentLink.localId}, Title: ${currentLink.title})"))
            pendingSyncQueueRepo.addInQueue(
                PendingSyncQueue(
                    operation = RemoteRoute.Link.CREATE_A_NEW_LINK.name,
                    payload = Json.encodeToString(
                        currentLink.asAddLinkDTO().copy(offlineSyncItemId = currentLink.localId)
                    )
                )
            )
            send(Result.Loading(message = "[LINKS] Queued link (ID: ${currentLink.localId}) for sync"))
        }

        send(Result.Loading(message = "[PANELS] Fetching unsynced panels"))
        localPanelsRepo.getUnSyncedPanels().forEach { currentPanel ->
            send(Result.Loading(message = "[PANELS] Processing panel (ID: ${currentPanel.localId}, Name: ${currentPanel.panelName})"))
            pendingSyncQueueRepo.addInQueue(
                PendingSyncQueue(
                    operation = RemoteRoute.Panel.ADD_A_NEW_PANEL.name,
                    payload = Json.encodeToString(
                        AddANewPanelDTO(
                            panelName = currentPanel.panelName,
                            offlineSyncItemId = currentPanel.localId
                        )
                    )
                )
            )
            send(Result.Loading(message = "[PANELS] Queued panel (ID: ${currentPanel.localId}) for sync"))
        }

        send(Result.Loading(message = "[PANEL FOLDERS] Fetching unsynced panel folders"))
        localPanelsRepo.getUnSyncedPanelFolders().forEach { currentPanelFolder ->
            send(Result.Loading(message = "[PANEL FOLDERS] Processing panel folder (ID: ${currentPanelFolder.localId}, Panel ID: ${currentPanelFolder.connectedPanelId})"))
            pendingSyncQueueRepo.addInQueue(
                PendingSyncQueue(
                    operation = RemoteRoute.Panel.ADD_A_NEW_FOLDER_IN_A_PANEL.name,
                    payload = Json.encodeToString(
                        AddANewPanelFolderDTO(
                            folderId = currentPanelFolder.folderId,
                            panelPosition = currentPanelFolder.panelPosition,
                            folderName = currentPanelFolder.folderName,
                            connectedPanelId = currentPanelFolder.connectedPanelId,
                            offlineSyncItemId = currentPanelFolder.localId
                        )
                    )
                )
            )
            send(Result.Loading(message = "[PANEL FOLDERS] Queued panel folder (ID: ${currentPanelFolder.localId}) for sync"))
        }

        send(Result.Loading(message = "[SYNC] Pushing queued items to server"))
        pushPendingSyncQueueToServer().collect()
    }
}