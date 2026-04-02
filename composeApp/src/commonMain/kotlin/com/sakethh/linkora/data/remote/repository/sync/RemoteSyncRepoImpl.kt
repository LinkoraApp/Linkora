package com.sakethh.linkora.data.remote.repository.sync

import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.asAddFolderDTO
import com.sakethh.linkora.domain.asAddLinkDTO
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.dto.server.DeleteEverythingDTO
import com.sakethh.linkora.domain.dto.server.ServerDataDTO
import com.sakethh.linkora.domain.dto.server.TombstoneDTO
import com.sakethh.linkora.domain.dto.server.folder.FolderDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelFolderDTO
import com.sakethh.linkora.domain.dto.server.panel.UpdatePanelNameDTO
import com.sakethh.linkora.domain.dto.server.tag.CreateTagDTO
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.model.WebSocketEvent
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalDatabaseUtilsRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalMultiActionRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.domain.repository.remote.RemoteLinksRepo
import com.sakethh.linkora.domain.repository.remote.RemoteMultiActionRepo
import com.sakethh.linkora.domain.repository.remote.RemotePanelsRepo
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.domain.repository.remote.RemoteTagsRepo
import com.sakethh.linkora.platform.Network
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.Utils.json
import com.sakethh.linkora.utils.canPushToServer
import com.sakethh.linkora.utils.catchAsThrowableAndEmitFailure
import com.sakethh.linkora.utils.longPreferencesKey
import com.sakethh.linkora.utils.performLocalOperationWithRemoteSyncFlow
import com.sakethh.linkora.utils.postFlow
import com.sakethh.linkora.utils.updateLastSyncedWithServerTimeStamp
import com.sakethh.linkora.utils.wrappedResultFlow
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
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class RemoteSyncRepoImpl(
    private val localFoldersRepo: LocalFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val authToken: suspend () -> String,
    private val baseUrl: suspend () -> String,
    private val websocketScheme: () -> String,
    private val pendingSyncQueueRepo: PendingSyncQueueRepo,
    remoteFoldersRepo: RemoteFoldersRepo,
    remoteLinksRepo: RemoteLinksRepo,
    remotePanelsRepo: RemotePanelsRepo,
    private val preferencesRepository: PreferencesRepository,
    localMultiActionRepo: LocalMultiActionRepo,
    remoteMultiActionRepo: RemoteMultiActionRepo,
    linksDao: LinksDao,
    foldersDao: FoldersDao,
    localTagsRepo: LocalTagsRepo,
    remoteTagsRepo: RemoteTagsRepo,
    private val tagsDao: TagsDao,
    private val localDatabaseUtilsRepo: LocalDatabaseUtilsRepo,
    private val network: Network
) : RemoteSyncRepo {


    private val pendingQueueService: PendingQueueService = PendingQueueService(
        localFoldersRepo,
        localLinksRepo,
        localPanelsRepo,
        pendingSyncQueueRepo,
        remoteFoldersRepo,
        remoteLinksRepo,
        remotePanelsRepo,
        preferencesRepository,
        remoteMultiActionRepo,
        remoteTagsRepo,
        linksDao,
        foldersDao,
        tagsDao
    )

    private val localDataUpdateService = LocalDataUpdateService(
        localFoldersRepo,
        localLinksRepo,
        localPanelsRepo,
        preferencesRepository,
        localMultiActionRepo,
        localTagsRepo,
        this
    )

    override suspend fun readSocketEvents(currentCorrelation: Correlation): Flow<Result<Unit>> {
        val authToken = authToken()
        return wrappedResultFlow {
            network.getSyncServerClient().webSocket(urlString = run {
                websocketScheme() + baseUrl.invoke().run {
                    if (startsWith(prefix = "https")) {
                        substringAfter("https")
                    } else {
                        substringAfter("http")
                    }.run {
                        if (endsWith("/")) this else "$this/"
                    } + "events"
                }
            }.also {
                linkoraLog("Connecting to the websocket at $it")
            }, request = {
                bearerAuth(authToken)
                parameter(
                    key = "correlation", value = Json.encodeToString(currentCorrelation)
                )
            }) {
                this.incoming.consumeAsFlow().collect {
                    if (it is Frame.Text) {
                        val deserializedWebSocketEvent =
                            json.decodeFromString<WebSocketEvent>((it.data).decodeToString())
                        linkoraLog(deserializedWebSocketEvent)
                        localDataUpdateService.updateLocalDBAccordingToEvent(
                            deserializedWebSocketEvent
                        )
                    }
                }
            }
        }
    }

    suspend fun <T> Flow<Result<T>>.collectAndUpdateTimestamp(eventTimestamp: Long) {
        this.collect {
            it.onSuccess {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(eventTimestamp)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun applyUpdatesFromRemote(timeStampAfter: Long): Flow<Result<Unit>> {


        /**
        `localDataUpdateService.updateLocalDBAccordingToEvent` is supposed to handle "events" from the socket.

        i didn't feel like writing it all from scratch for this function (`applyUpdatesFromRemote`)
        so i just mocked it like, "oh, that’s not me, must be some other client 🤓☝️" and force update respectively.

        and that's why `randomCorrelation` exists.
         **/
        val randomCorrelation = Correlation(
            id = Uuid.random().toString(), clientName = "🤨📸"
        )


        return channelFlow {
            send(Result.Loading("Fetching updates from server..."))
            network.getSyncServerClient()
                .get(baseUrl() + SyncServerRoute.GET_UPDATES.name) {
                    bearerAuth(authToken())
                    contentType(ContentType.Application.Json)
                    parameter("eventTimestamp", timeStampAfter)
                }.body<ServerDataDTO>().let { remoteResponse ->
                    send(Result.Loading("Received updates from server. Processing folders..."))

                    remoteResponse.folders.forEach {
                        send(Result.Loading("Processing folder with id: ${it.id}"))
                        applyFolderUpdates(it)
                    }
                    remoteResponse.tags.forEach {
                        val localTag = try {
                            val localId = tagsDao.getLocalTagId(it.id)
                            tagsDao.getATag(localId)
                        } catch (_: Exception) {
                            null
                        }
                        if (localTag == null) {
                            tagsDao.createATag(
                                Tag(
                                    remoteId = it.id,
                                    lastModified = it.eventTimestamp,
                                    name = it.name
                                )
                            )
                        } else {
                            tagsDao.updateATag(
                                localTag.copy(
                                    name = it.name, lastModified = it.eventTimestamp
                                )
                            )
                        }
                    }
                    try {
                        val maxTimestamp = remoteResponse.tags.maxOf { it.eventTimestamp }
                        preferencesRepository.updateLastSyncedWithServerTimeStamp(maxTimestamp)
                    } catch (_: Exception) {
                    }
                    coroutineScope {
                        awaitAll(async {
                            send(Result.Loading("Processing links..."))
                            remoteResponse.links.forEach { remoteLinkDTO ->
                                send(Result.Loading("Processing link with id: ${remoteLinkDTO.id}"))
                                val localId = localLinksRepo.getLocalLinkId(remoteLinkDTO.id)
                                if (localId == null) {
                                    send(Result.Loading("Creating new link with id: ${remoteLinkDTO.id}"))
                                    localDataUpdateService.updateLocalDBAccordingToEvent(
                                        WebSocketEvent(
                                            operation = SyncServerRoute.CREATE_A_NEW_LINK.name,
                                            payload = json.encodeToJsonElement(
                                                remoteLinkDTO.copy(correlation = randomCorrelation)
                                            )
                                        )
                                    )
                                } else {
                                    send(Result.Loading("Updating existing link with id: ${remoteLinkDTO.id}"))
                                    localDataUpdateService.updateLocalDBAccordingToEvent(
                                        WebSocketEvent(
                                            operation = SyncServerRoute.UPDATE_LINK.name,
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
                                    localDataUpdateService.updateLocalDBAccordingToEvent(
                                        WebSocketEvent(
                                            operation = SyncServerRoute.ADD_A_NEW_PANEL.name,
                                            payload = json.encodeToJsonElement(
                                                remotePanelDTO.copy(correlation = randomCorrelation)
                                            )
                                        )
                                    )
                                } else {
                                    send(Result.Loading("Updating existing panel name for id: ${remotePanelDTO.panelId}"))
                                    localDataUpdateService.updateLocalDBAccordingToEvent(
                                        WebSocketEvent(
                                            operation = SyncServerRoute.UPDATE_A_PANEL_NAME.name,
                                            payload = json.encodeToJsonElement(
                                                UpdatePanelNameDTO(
                                                    newName = remotePanelDTO.panelName,
                                                    panelId = remotePanelDTO.panelId,
                                                    correlation = randomCorrelation,
                                                    eventTimestamp = remotePanelDTO.eventTimestamp
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
                                if (localPanelsRepo.getLocalPanelFolderId(remotePanelFolder.id) == null) {
                                    localDataUpdateService.updateLocalDBAccordingToEvent(
                                        WebSocketEvent(
                                            operation = SyncServerRoute.ADD_A_NEW_FOLDER_IN_A_PANEL.name,
                                            payload = json.encodeToJsonElement(
                                                remotePanelFolder.copy(correlation = randomCorrelation)
                                            )
                                        )
                                    )
                                }
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
                    isArchived = folderDTO.isArchived,
                    lastModified = folderDTO.eventTimestamp
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
                    isArchived = folderDTO.isArchived,
                    lastModified = folderDTO.eventTimestamp
                ), ignoreFolderAlreadyExistsException = true, viaSocket = true
            ).collectAndUpdateTimestamp(folderDTO.eventTimestamp)
        }
    }

    override suspend fun applyUpdatesBasedOnRemoteTombstones(timeStampAfter: Long): Flow<Result<Unit>> {
        return wrappedResultFlow {
            network.getSyncServerClient()
                .get(baseUrl() + SyncServerRoute.GET_TOMBSTONES.name) {
                    bearerAuth(authToken())
                    contentType(ContentType.Application.Json)
                    parameter("eventTimestamp", timeStampAfter)
                }.body<List<TombstoneDTO>>().map {
                    WebSocketEvent(
                        operation = it.operation, payload = it.payload
                    )
                }.forEach {
                    localDataUpdateService.updateLocalDBAccordingToEvent(it)
                }
        }
    }

    override suspend fun <T> SendChannel<Result<T>>.pushPendingSyncQueueToServer(): Flow<Result<Unit>> {
        return with(pendingQueueService) {
            pushPendingSyncQueueToServer()
        }
    }


    override suspend fun <T> SendChannel<Result<T>>.pushNonSyncedDataToServer() {
        send(Result.Loading(message = "[SYNC] Starting non-synced data push to server"))

        send(Result.Loading(message = "[FOLDERS] Fetching unsynced folders"))
        localFoldersRepo.getUnSyncedFolders().forEach { currentFolder ->
            send(Result.Loading(message = "[FOLDERS] Processing folder (ID: ${currentFolder.localId}, Name: ${currentFolder.name})"))
            pendingSyncQueueRepo.addInQueue(
                PendingSyncQueue(
                    operation = SyncServerRoute.CREATE_FOLDER.name,
                    payload = Json.encodeToString(
                        currentFolder.asAddFolderDTO(preferencesRepository.getPreferences().correlation)
                            .copy(offlineSyncItemId = currentFolder.localId)
                    )
                )
            )
            send(Result.Loading(message = "[FOLDERS] Queued folder (ID: ${currentFolder.localId}) for sync"))
        }

        send(Result.Loading(message = "[TAGS] Fetching unsynced tags"))
        tagsDao.getUnsyncedTags().forEach { currentTag ->
            send(Result.Loading(message = "[TAGS] Processing tag (ID: ${currentTag.localId}, Name: ${currentTag.name})"))
            pendingSyncQueueRepo.addInQueue(
                PendingSyncQueue(
                    operation = SyncServerRoute.CREATE_TAG.name, payload = Json.encodeToString(
                        CreateTagDTO(
                            name = currentTag.name,
                            eventTimestamp = currentTag.lastModified,
                            offlineSyncItemId = currentTag.localId,
                            correlation = preferencesRepository.getPreferences().correlation
                        )
                    )
                )
            )
            send(Result.Loading(message = "[TAGS] Queued tag (ID: ${currentTag.localId}) for sync"))
        }

        send(Result.Loading(message = "[LINKS] Fetching unsynced links"))
        localLinksRepo.getUnSyncedLinks().forEach { currentLink ->
            send(Result.Loading(message = "[LINKS] Processing link (ID: ${currentLink.localId}, Title: ${currentLink.title})"))
            pendingSyncQueueRepo.addInQueue(
                PendingSyncQueue(
                    operation = SyncServerRoute.CREATE_A_NEW_LINK.name,
                    payload = Json.encodeToString(
                        currentLink.asAddLinkDTO(
                            remoteTagIds = tagsDao.getTags(currentLink.localId).map {
                                it.remoteId ?: -45454
                            }, correlation = preferencesRepository.getPreferences().correlation
                        ).copy(offlineSyncItemId = currentLink.localId)
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
                    operation = SyncServerRoute.ADD_A_NEW_PANEL.name,
                    payload = Json.encodeToString(
                        AddANewPanelDTO(
                            panelName = currentPanel.panelName,
                            offlineSyncItemId = currentPanel.localId,
                            eventTimestamp = currentPanel.lastModified,
                            correlation = preferencesRepository.getPreferences().correlation
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
                    operation = SyncServerRoute.ADD_A_NEW_FOLDER_IN_A_PANEL.name,
                    payload = Json.encodeToString(
                        AddANewPanelFolderDTO(
                            folderId = currentPanelFolder.folderId,
                            panelPosition = currentPanelFolder.panelPosition,
                            folderName = currentPanelFolder.folderName,
                            connectedPanelId = currentPanelFolder.connectedPanelId,
                            offlineSyncItemId = currentPanelFolder.localId,
                            eventTimestamp = currentPanelFolder.lastModified,
                            correlation = preferencesRepository.getPreferences().correlation
                        )
                    )
                )
            )
            send(Result.Loading(message = "[PANEL FOLDERS] Queued panel folder (ID: ${currentPanelFolder.localId}) for sync"))
        }

        send(Result.Loading(message = "[SYNC] Pushing queued items to server"))
        pushPendingSyncQueueToServer().collect()
    }

    override suspend fun deleteEverything(deleteOnRemote: Boolean): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow<Unit, DeleteEverythingDTO>(
            canPushToServer = {
                preferencesRepository.getPreferences().canPushToServer()
            },
            performRemoteOperation = deleteOnRemote,
            remoteOperation = {
                postFlow(
                    syncServerClient = { network.getSyncServerClient() },
                    baseUrl = baseUrl,
                    authToken = authToken,
                    endPoint = SyncServerRoute.DELETE_EVERYTHING.name,
                    outgoingBody = DeleteEverythingDTO(correlation = preferencesRepository.getPreferences().correlation)
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            }) {
            localDatabaseUtilsRepo.resetDatabase()
            preferencesRepository.changePreferenceValue(
                preferenceKey = longPreferencesKey(
                    AppPreferences.LAST_SELECTED_PANEL_ID.key
                ), newValue = Constants.DEFAULT_PANELS_ID
            )
        }
    }
}