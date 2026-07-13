package com.sakethh.linkora.data.local.repository

import androidx.room3.Transactor
import androidx.room3.immediateTransaction
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.asAddFolderDTO
import com.sakethh.linkora.domain.asFolderDTO
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.folder.MarkSelectedFoldersAsRootDTO
import com.sakethh.linkora.domain.linkoraPlaceHolders
import com.sakethh.linkora.domain.mapToResultFlow
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.utils.Sorting
import com.sakethh.linkora.utils.canPushToServer
import com.sakethh.linkora.utils.getSystemEpochSeconds
import com.sakethh.linkora.utils.performLocalOperationWithRemoteSyncFlow
import com.sakethh.linkora.utils.updateLastSyncedWithServerTimeStamp
import com.sakethh.linkora.utils.wrappedResultFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.json.Json

class LocalFoldersRepoImpl(
    private val foldersDao: FoldersDao,
    private val remoteFoldersRepo: RemoteFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val pendingSyncQueueRepo: PendingSyncQueueRepo,
    private val preferencesRepository: PreferencesRepository,
    private val withWriterConnection: suspend (suspend (Transactor) -> Unit) -> Unit,
) : LocalFoldersRepo {
    override suspend fun insertANewFolder(
        folder: Folder,
        viaSocket: Boolean,
    ): Flow<Result<Long>> {
        var newLocalId: Long? = null
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                if (newLocalId == null) return@performLocalOperationWithRemoteSyncFlow emptyFlow()

                if (folder.parentFolderId != null) {
                    val remoteParentFolderId = getRemoteIdOfAFolder(folder.parentFolderId)
                    remoteFoldersRepo.createFolder(
                        folder
                            .asAddFolderDTO(preferences.correlation)
                            .copy(parentFolderId = remoteParentFolderId),
                    )
                } else {
                    remoteFoldersRepo.createFolder(folder.asAddFolderDTO(preferences.correlation))
                }
            },
            remoteOperationOnSuccess = {
                if (newLocalId == null) return@performLocalOperationWithRemoteSyncFlow

                foldersDao.updateFolder(
                    foldersDao.getThisFolderData(newLocalId).copy(remoteId = it.id),
                )
                preferencesRepository.updateLastSyncedWithServerTimeStamp(
                    it.timeStampBasedResponse.eventTimestamp,
                )
            },
            onRemoteOperationFailure = {
                if (newLocalId == null) return@performLocalOperationWithRemoteSyncFlow

                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.CREATE_FOLDER.name,
                        payload =
                        Json.encodeToString(
                            folder
                                .asAddFolderDTO(preferences.correlation)
                                .copy(
                                    offlineSyncItemId = newLocalId!!,
                                ),
                        ),
                    ),
                )
            },
            localOperation = {
                if (folder.name.isEmpty() || linkoraPlaceHolders().contains(folder.name)) {
                    throw Folder.InvalidName(
                        if (folder.name.isEmpty()) {
                            "Folder name cannot be blank."
                        } else {
                            "\"${folder.name}\" is reserved."
                        },
                    )
                }
                newLocalId = foldersDao.insertANewFolder(folder.copy(localId = 0))
                newLocalId
            },
        )
    }

    override suspend fun insertANewFolderLocally(folder: Folder): Long = foldersDao.insertANewFolder(folder)

    override suspend fun getAllRootFoldersAsList(): List<Folder> = foldersDao.getAllRootFoldersAsList()

    override fun getAllFoldersAsResultList(): Flow<Result<List<Folder>>> = performLocalOperationWithRemoteSyncFlow<List<Folder>, Unit>(
        canPushToServer = {
            false
        },
        performRemoteOperation = false,
    ) {
        foldersDao.getAllFoldersAsList()
    }

    override fun getAllFoldersAsFlow(): Flow<List<Folder>> = foldersDao.getAllFoldersAsFlow()

    override suspend fun getAllFoldersAsList(): List<Folder> = foldersDao.getAllFoldersAsList()

    override suspend fun isFoldersTableEmpty(): Boolean = !foldersDao.doesFolderTableHaveData()

    override suspend fun getChildFoldersOfThisParentIDAsList(parentFolderID: Long?): List<Folder> = foldersDao.getChildFoldersAsList(parentFolderID)

    override suspend fun getLatestFoldersTableID(): Long = foldersDao.getLatestFoldersTableID()

    override suspend fun getThisFolderData(folderID: Long): Flow<Result<Folder>> = performLocalOperationWithRemoteSyncFlow<Folder, Unit>(
        canPushToServer = {
            false
        },
        performRemoteOperation = false,
    ) {
        foldersDao.getThisFolderData(folderID)
    }

    override suspend fun doesThisChildFolderExists(
        folderName: String,
        parentFolderID: Long?,
    ): Flow<Result<Int>> = performLocalOperationWithRemoteSyncFlow<Int, Unit>(
        canPushToServer = {
            false
        },
        performRemoteOperation = false,
    ) {
        foldersDao.doesFolderExists(
            folderName,
            parentFolderID,
        )
    }

    override suspend fun doesThisRootFolderExists(folderName: String): Flow<Result<Boolean>> = performLocalOperationWithRemoteSyncFlow<Boolean, Unit>(
        canPushToServer = {
            false
        },
        performRemoteOperation = false,
    ) {
        foldersDao.doesThisRootFolderExists(folderName)
    }

    override suspend fun getRootFolders(
        sortOption: String,
        isArchived: Boolean,
        pageSize: Int,
        lastSeenName: String?,
        lastSeenId: Long?,
    ): Flow<Result<List<Folder>>> = when (sortOption) {
        Sorting.A_TO_Z,
        Sorting.Z_TO_A,
        ->
            foldersDao.getRootFoldersSortedByName(
                lastSeenId = lastSeenId,
                lastSeenName = lastSeenName?.takeIf { it.isNotEmpty() },
                isAscending = sortOption == Sorting.A_TO_Z,
                pageSize = pageSize,
                isArchived = isArchived,
            )

        else ->
            foldersDao.getRootFoldersSortedById(
                lastSeenId = lastSeenId,
                isAscending = sortOption == Sorting.OLD_TO_NEW,
                pageSize = pageSize,
                isArchived = isArchived,
            )
    }.mapToResultFlow()

    override suspend fun getChildFolders(
        parentFolderId: Long,
        sortOption: String,
        pageSize: Int,
        startIndex: Long,
    ): Flow<Result<List<Folder>>> = foldersDao.getChildFolders(parentFolderId, sortOption, pageSize, startIndex).mapToResultFlow()

    override suspend fun getChildFoldersAsList(parentFolderId: Long): List<Folder> = foldersDao.getChildFoldersAsList(parentFolderId)

    override fun sortFoldersAsNonResultFlow(
        parentFolderId: Long,
        sortOption: String,
    ): Flow<List<Folder>> = foldersDao.getChildFolders(parentFolderId, sortOption)

    override suspend fun getChildFoldersOfThisParentIDAsFlow(
        parentFolderID: Long?,
    ): Flow<Result<List<Folder>>> = foldersDao.getChildFoldersAsFlow(parentFolderID).mapToResultFlow()

    override suspend fun getRemoteIdOfAFolder(localId: Long): Long? = foldersDao.getRemoteFolderId(localId)

    override suspend fun getLocalIdOfAFolder(remoteId: Long): Long? = foldersDao.getLocalIdOfAFolder(remoteId)

    override suspend fun markFolderAsArchive(
        folderID: Long,
        viaSocket: Boolean,
    ): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                val remoteId = getRemoteIdOfAFolder(folderID)
                require(remoteId != null)
                remoteFoldersRepo.markAsArchive(
                    IDBasedDTO(
                        remoteId,
                        eventTimestamp,
                        preferences.correlation,
                    ),
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
                foldersDao.updateFolderTimestamp(it.eventTimestamp, folderID)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.MARK_FOLDER_AS_ARCHIVE.name,
                        payload =
                        Json.encodeToString(
                            IDBasedDTO(
                                folderID,
                                eventTimestamp,
                                preferences.correlation,
                            ),
                        ),
                    ),
                )
            },
        ) {
            foldersDao.markFolderAsArchive(folderID)
            foldersDao.updateFolderTimestamp(eventTimestamp, folderID)
        }
    }

    override suspend fun markFolderAsRegularFolder(
        folderID: Long,
        viaSocket: Boolean,
    ): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                val remoteFolderId = getRemoteIdOfAFolder(folderID)
                require(remoteFolderId != null)
                remoteFoldersRepo.markAsRegularFolder(
                    IDBasedDTO(
                        remoteFolderId,
                        eventTimestamp,
                        preferences.correlation,
                    ),
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
                foldersDao.updateFolderTimestamp(it.eventTimestamp, folderID)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.MARK_AS_REGULAR_FOLDER.name,
                        payload =
                        Json.encodeToString(
                            value =
                            IDBasedDTO(
                                folderID,
                                eventTimestamp,
                                preferences.correlation,
                            ),
                        ),
                    ),
                )
            },
        ) {
            foldersDao.markFolderAsRegularFolder(folderID)
            foldersDao.updateFolderTimestamp(eventTimestamp, folderID)
        }
    }

    override suspend fun updateLocalFolderData(folder: Folder): Flow<Result<Unit>> = performLocalOperationWithRemoteSyncFlow<Unit, Unit>(
        canPushToServer = {
            false
        },
        performRemoteOperation = false,
    ) {
        foldersDao.updateFolder(folder.copy(lastModified = getSystemEpochSeconds()))
    }

    override suspend fun updateFolder(
        folder: Folder,
        viaSocket: Boolean,
    ): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                require(folder.remoteId != null)

                val remoteFolderDTO =
                    folder.asFolderDTO(
                        remoteId = folder.remoteId,
                        remoteParentFolderId =
                        if (folder.parentFolderId == null) {
                            null
                        } else {
                            foldersDao.getRemoteFolderId(
                                folder.parentFolderId,
                            )
                        },
                        preferences.correlation,
                    )
                remoteFoldersRepo.updateFolder(remoteFolderDTO.copy(eventTimestamp = eventTimestamp))
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.UPDATE_FOLDER.name,
                        payload =
                        Json.encodeToString(
                            value = folder.copy(lastModified = eventTimestamp),
                        ),
                    ),
                )
            },
            localOperation = {
                foldersDao.updateFolder(folder.copy(lastModified = eventTimestamp))
            },
        )
    }

    override suspend fun deleteAFolderNote(
        folderID: Long,
        viaSocket: Boolean,
    ): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        val remoteId = getRemoteIdOfAFolder(folderID)
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                require(remoteId != null)
                remoteFoldersRepo.deleteFolderNote(
                    IDBasedDTO(
                        remoteId,
                        eventTimestamp,
                        preferences.correlation,
                    ),
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.DELETE_FOLDER_NOTE.name,
                        payload =
                        Json.encodeToString(
                            value =
                            IDBasedDTO(
                                folderID,
                                eventTimestamp,
                                preferences.correlation,
                            ),
                        ),
                    ),
                )
            },
        ) {
            foldersDao.deleteAFolderNote(folderID)
        }
    }

    override suspend fun deleteAFolder(
        folderID: Long,
        viaSocket: Boolean,
    ): Flow<Result<Unit>> {
        // we need to hold the id because the local folder gets deleted first, so if we try to search
        // after that, there will be nothing to search
        val remoteFolderId = getRemoteIdOfAFolder(folderID)
        val preferences = preferencesRepository.getPreferences()
        val eventTimestamp = getSystemEpochSeconds()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                require(remoteFolderId != null)
                remoteFoldersRepo.deleteFolder(
                    IDBasedDTO(
                        remoteFolderId,
                        eventTimestamp,
                        preferences.correlation,
                    ),
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.DELETE_FOLDER.name,
                        payload =
                        Json.encodeToString(
                            value =
                            IDBasedDTO(
                                folderID,
                                eventTimestamp,
                                preferences.correlation,
                            ),
                        ),
                    ),
                )
            },
            localOperation = {
                deleteChildData(folderID)
                localPanelsRepo.deleteAFolderFromAllPanels(folderID)
                localLinksRepo.deleteLinksOfFolder(folderID).collect()
                foldersDao.deleteAFolder(folderID)
            },
        )
    }

    override suspend fun deleteMultipleFolders(
        folderIDs: List<Long>,
        viaSocket: Boolean,
    ): Flow<Result<Unit>> = wrappedResultFlow {
        folderIDs.forEach {
            deleteAFolder(it, viaSocket = true).collect()
        }
    }

    private suspend fun deleteChildData(folderID: Long) {
        withWriterConnection { transactor ->
            transactor.immediateTransaction {
                val foldersToDelete = ArrayDeque<Folder>()
                foldersToDelete.addAll(foldersDao.getChildFoldersAsList(folderID))

                while (foldersToDelete.isNotEmpty()) {
                    val currentFolder = foldersToDelete.removeLast()

                    localPanelsRepo.deleteAFolderFromAllPanels(currentFolder.localId)
                    foldersDao.deleteAFolder(currentFolder.localId)
                    localLinksRepo.deleteLinksOfFolder(currentFolder.localId).collect()

                    val childFolders = foldersDao.getChildFoldersAsList(currentFolder.localId)
                    foldersToDelete.addAll(childFolders)
                }
            }
        }
    }

    override fun search(
        query: String,
        sortOption: String,
    ): Flow<Result<List<Folder>>> = foldersDao.search(query, sortOption).mapToResultFlow()

    override suspend fun getUnSyncedFolders(): List<Folder> = foldersDao.getUnSyncedFolders()

    override suspend fun markFoldersAsRoot(
        folderIDs: List<Long>,
        viaSocket: Boolean,
    ): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        val preferences = preferencesRepository.getPreferences()
        return performLocalOperationWithRemoteSyncFlow(
            canPushToServer = {
                preferences.canPushToServer()
            },
            performRemoteOperation = !viaSocket,
            remoteOperation = {
                val remoteFolderIds = foldersDao.getRemoteIds(folderIDs)
                require(remoteFolderIds != null)
                remoteFoldersRepo.markSelectedFoldersAsRoot(
                    MarkSelectedFoldersAsRootDTO(
                        folderIds = remoteFolderIds,
                        eventTimestamp = eventTimestamp,
                        correlation = preferences.correlation,
                    ),
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.MARK_FOLDERS_AS_ROOT.name,
                        payload =
                        Json.encodeToString(
                            MarkSelectedFoldersAsRootDTO(
                                folderIds = folderIDs,
                                eventTimestamp = eventTimestamp,
                                correlation = preferences.correlation,
                            ),
                        ),
                    ),
                )
            },
        ) {
            foldersDao.markFoldersAsRoot(folderIDs)
        }
    }
}
