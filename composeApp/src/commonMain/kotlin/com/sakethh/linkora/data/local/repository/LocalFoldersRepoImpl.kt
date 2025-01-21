package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.common.utils.catchAsThrowableAndEmitFailure
import com.sakethh.linkora.common.utils.isNotNull
import com.sakethh.linkora.common.utils.performLocalOperationWithRemoteSyncFlow
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.domain.Message
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.asAddFolderDTO
import com.sakethh.linkora.domain.linkoraPlaceHolders
import com.sakethh.linkora.domain.mapToResultFlow
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.ui.utils.linkoraLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

class LocalFoldersRepoImpl(
    private val foldersDao: FoldersDao,
    private val remoteFoldersRepo: RemoteFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
    private val localPanelsRepo: LocalPanelsRepo
) : LocalFoldersRepo {

    override suspend fun insertANewFolder(
        folder: Folder, ignoreFolderAlreadyExistsException: Boolean
    ): Flow<Result<Message>> {
        val newLocalId = foldersDao.getLastIDOfFoldersTable() + 1
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = true, remoteOperation = {
            if (folder.parentFolderId != null) {
                val remoteParentFolderId = getRemoteIdOfThisLocalFolder(folder.parentFolderId)
                remoteFoldersRepo.createFolder(
                    folder.asAddFolderDTO().copy(parentFolderId = remoteParentFolderId)
                )
            } else {
                remoteFoldersRepo.createFolder(folder.asAddFolderDTO())
            }
        }, remoteOperationOnSuccess = {
            foldersDao.updateAFolderData(
                foldersDao.getThisFolderData(newLocalId).copy(remoteId = it.id)
            )
        }, localOperation = {
            if (folder.name.isEmpty() || linkoraPlaceHolders().contains(folder.name)) {
                throw Folder.InvalidName(if (folder.name.isEmpty()) "Folder name cannot be blank." else "\"${folder.name}\" is reserved.")
            }
            if (!ignoreFolderAlreadyExistsException) {
                when (folder.parentFolderId) {
                    null -> {
                        doesThisRootFolderExists(folder.name).first().onSuccess {
                            if (it.data) {
                                throw Folder.FolderAlreadyExists("Folder named \"${folder.name}\" already exists")
                            }
                        }
                    }

                    else -> {
                        doesThisChildFolderExists(folder.name, folder.parentFolderId).first()
                            .onSuccess {
                                if (it.data == 1) {
                                    getThisFolderData(folder.parentFolderId).first()
                                        .onSuccess { parentFolder ->
                                            throw Folder.FolderAlreadyExists("A folder named \"${folder.name}\" already exists in ${parentFolder.data.name}.")
                                        }
                                }
                            }
                    }
                }
            }
            val newId = foldersDao.insertANewFolder(folder.copy(localId = newLocalId))
            "Folder created successfully with id = $newId"
        })
    }


    override suspend fun duplicateAFolder(
        actualFolderId: Long, parentFolderID: Long?
    ): Flow<Result<Long>> {
        return performLocalOperationWithRemoteSyncFlow<Long, Long>(performRemoteOperation = false) {
            foldersDao.duplicateAFolder(actualFolderId, parentFolderID)
        }
    }

    override suspend fun insertMultipleNewFolders(foldersTable: List<Folder>): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow<Unit, Unit>(
            performRemoteOperation = false,
            localOperation = {
                foldersDao.insertMultipleNewFolders(foldersTable)
            },
        )
    }

    override suspend fun getAllArchiveFoldersAsList(): Flow<Result<List<Folder>>> {
        return performLocalOperationWithRemoteSyncFlow<List<Folder>, Unit>(
            performRemoteOperation = false,
            localOperation = {
            foldersDao.getAllArchiveFoldersAsList()
            },
        )
    }

    override suspend fun getAllRootFoldersAsList(): Flow<Result<List<Folder>>> {
        return performLocalOperationWithRemoteSyncFlow<List<Folder>, Unit>(performRemoteOperation = false) {
            foldersDao.getAllRootFoldersAsList()
        }
    }

    override suspend fun getAllFolders(): Flow<Result<List<Folder>>> {
        return performLocalOperationWithRemoteSyncFlow<List<Folder>, Unit>(performRemoteOperation = false) {
            foldersDao.getAllFolders()
        }
    }

    override suspend fun getSizeOfLinksOfThisFolder(folderID: Long): Flow<Result<Int>> {
        return performLocalOperationWithRemoteSyncFlow<Int, Unit>(performRemoteOperation = false) {
            foldersDao.getSizeOfLinksOfThisFolder(folderID)
        }
    }

    override suspend fun getAllFoldersAsList(): List<Folder> {
        return foldersDao.getAllFolders()
    }

    override suspend fun getChildFoldersOfThisParentIDAsList(parentFolderID: Long?): List<Folder> {
        return foldersDao.getChildFoldersOfThisParentIDAsAList(parentFolderID)
    }

    override suspend fun getLatestFoldersTableID(): Long {
        return foldersDao.getLatestFoldersTableID()
    }
    override suspend fun getThisFolderData(folderID: Long): Flow<Result<Folder>> {
        return performLocalOperationWithRemoteSyncFlow<Folder, Unit>(performRemoteOperation = false) {
            foldersDao.getThisFolderData(folderID)
        }
    }

    override suspend fun getLastIDOfFoldersTable(): Flow<Result<Long>> {
        return performLocalOperationWithRemoteSyncFlow<Long, Unit>(performRemoteOperation = false) {
            foldersDao.getLastIDOfFoldersTable()
        }
    }

    override suspend fun doesThisChildFolderExists(
        folderName: String, parentFolderID: Long?
    ): Flow<Result<Int>> {
        return performLocalOperationWithRemoteSyncFlow<Int, Unit>(performRemoteOperation = false) {
            foldersDao.doesThisChildFolderExists(
                folderName, parentFolderID
            )
        }
    }

    override suspend fun doesThisRootFolderExists(folderName: String): Flow<Result<Boolean>> {
        return performLocalOperationWithRemoteSyncFlow<Boolean, Unit>(performRemoteOperation = false) {
            foldersDao.doesThisRootFolderExists(folderName)
        }
    }

    override suspend fun isThisFolderMarkedAsArchive(folderID: Long): Flow<Result<Boolean>> {
        return performLocalOperationWithRemoteSyncFlow<Boolean, Unit>(performRemoteOperation = false) {
            foldersDao.isThisFolderMarkedAsArchive(folderID)
        }
    }

    override suspend fun getNewestFolder(): Flow<Result<Folder>> {
        return performLocalOperationWithRemoteSyncFlow<Folder, Unit>(performRemoteOperation = false) {
            foldersDao.getNewestFolder()
        }
    }

    override fun getFoldersCount(): Flow<Result<Int>> {
        return foldersDao.getFoldersCount().map {
            Result.Success(it)
        }.onStart {
            Result.Loading<Int>()
        }.catchAsThrowableAndEmitFailure()
    }

    override suspend fun changeTheParentIdOfASpecificFolder(
        sourceFolderId: Long, targetParentId: Long?
    ): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow<Unit, Unit>(performRemoteOperation = false) {
            foldersDao.changeTheParentIdOfASpecificFolder(
                sourceFolderId, targetParentId
            )
        }
    }

    override suspend fun sortFolders(sortOption: String): Flow<Result<List<Folder>>> {
        return foldersDao.sortFolders(sortOption).mapToResultFlow()
    }

    override suspend fun sortFolders(
        parentFolderId: Long, sortOption: String
    ): Flow<Result<List<Folder>>> {
        return foldersDao.sortFolders(parentFolderId, sortOption).mapToResultFlow()
    }

    override fun sortFoldersAsNonResultFlow(
        parentFolderId: Long, sortOption: String
    ): Flow<List<Folder>> {
        return foldersDao.sortFolders(parentFolderId, sortOption)
    }

    override suspend fun getChildFoldersOfThisParentIDAsFlow(parentFolderID: Long?): Flow<Result<List<Folder>>> {
        return foldersDao.getChildFoldersOfThisParentIDAsFlow(parentFolderID).mapToResultFlow()
    }

    override suspend fun getSizeOfChildFoldersOfThisParentID(parentFolderID: Long?): Flow<Result<Int>> {
        return performLocalOperationWithRemoteSyncFlow<Int, Unit>(performRemoteOperation = false) {
            foldersDao.getSizeOfChildFoldersOfThisParentID(parentFolderID)
        }
    }

    override suspend fun renameAFolderName(
        folderID: Long,
        existingFolderName: String,
        newFolderName: String,
        ignoreFolderAlreadyExistsException: Boolean
    ): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = true, remoteOperation = {
            val remoteId = getRemoteIdOfThisLocalFolder(folderID)
            if (remoteId.isNotNull()) {
                remoteFoldersRepo.updateFolderName(remoteId!!, newFolderName)
            } else {
                emptyFlow()
            }
        }, localOperation = {
            if (newFolderName.isEmpty() || linkoraPlaceHolders()
                    .contains(newFolderName) || existingFolderName == newFolderName
            ) {
                throw Folder.InvalidName(if (newFolderName.isEmpty()) "Folder name cannot be blank." else if (existingFolderName == newFolderName) "Nothing has changed to update." else "\"${newFolderName}\" is reserved.")
            }
            foldersDao.renameAFolderName(folderID, newFolderName)
        })
    }

    private suspend fun getRemoteIdOfThisLocalFolder(localFolderId: Long): Long? {
        return foldersDao.getThisFolderData(localFolderId).remoteId
    }
    override suspend fun markFolderAsArchive(folderID: Long): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = true, remoteOperation = {
            val remoteId = getRemoteIdOfThisLocalFolder(folderID)
            if (remoteId.isNotNull()) {
                remoteFoldersRepo.markAsArchive(remoteId!!)
            } else {
                emptyFlow()
            }
        }) {
            foldersDao.markFolderAsArchive(folderID)
        }
    }

    override suspend fun markMultipleFoldersAsArchive(folderIDs: Array<Long>): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow<Unit, Unit>(performRemoteOperation = false) {
            foldersDao.markMultipleFoldersAsArchive(folderIDs)
        }
    }

    override suspend fun markFolderAsRegularFolder(folderID: Long): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = true, remoteOperation = {
            val remoteFolderId = getRemoteIdOfThisLocalFolder(folderID)
            if (remoteFolderId.isNotNull()) {
                remoteFoldersRepo.markAsRegularFolder(remoteFolderId!!)
            } else {
                emptyFlow()
            }
        }) {
            foldersDao.markFolderAsRegularFolder(folderID)
        }
    }

    override suspend fun renameAFolderNote(folderID: Long, newNote: String): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = true, remoteOperation = {
            val remoteID = getRemoteIdOfThisLocalFolder(folderID)
            if (remoteID.isNotNull()) {
                remoteFoldersRepo.updateFolderNote(remoteID!!, newNote)
            } else {
                emptyFlow()
            }
        }) {
            foldersDao.renameAFolderNote(folderID, newNote)
        }
    }

    override suspend fun updateAFolderData(folder: Folder): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow<Unit, Unit>(performRemoteOperation = false) {
            foldersDao.updateAFolderData(folder)
        }
    }

    override suspend fun deleteAFolderNote(folderID: Long): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = true, remoteOperation = {
            val remoteId = getRemoteIdOfThisLocalFolder(folderID)
            if (remoteId.isNotNull()) {
                remoteFoldersRepo.deleteFolderNote(remoteId!!)
            } else {
                emptyFlow()
            }
        }) {
            foldersDao.deleteAFolderNote(folderID)
        }
    }

    override suspend fun deleteAFolder(folderID: Long): Flow<Result<Unit>> {
        // we need to hold the id because the local folder gets deleted first, so if we try to search after that, there will be nothing to search
        val remoteFolderId = getRemoteIdOfThisLocalFolder(folderID)
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = true, remoteOperation = {
            if (remoteFolderId.isNotNull()) {
                remoteFoldersRepo.deleteFolder(remoteFolderId!!)
            } else {
                emptyFlow()
            }
        }, remoteOperationOnSuccess = {
            linkoraLog(it)
        }, localOperation = {
            deleteLocalDataRelatedToTheFolder(folderID)
            localLinksRepo.deleteLinksOfFolder(folderID)
            foldersDao.deleteAFolder(folderID)
        })
    }

    private suspend fun deleteLocalDataRelatedToTheFolder(folderID: Long) {
        localPanelsRepo.deleteAFolderFromAllPanels(folderID)
        foldersDao.getChildFoldersOfThisParentIDAsAList(folderID).forEach {
            localPanelsRepo.deleteAFolderFromAllPanels(it.localId)
            foldersDao.deleteAFolder(it.localId)
            localLinksRepo.deleteLinksOfFolder(it.localId)
            deleteLocalDataRelatedToTheFolder(it.localId)
        }
    }

    override suspend fun deleteChildFoldersOfThisParentID(parentFolderId: Long): Flow<Result<Unit>> {
        return performLocalOperationWithRemoteSyncFlow<Unit, Unit>(performRemoteOperation = false) {
            foldersDao.deleteChildFoldersOfThisParentID(parentFolderId)
        }
    }

    override suspend fun isFoldersTableEmpty(): Flow<Result<Boolean>> {
        return performLocalOperationWithRemoteSyncFlow<Boolean, Unit>(performRemoteOperation = false) {
            foldersDao.isFoldersTableEmpty()
        }
    }

    override fun search(query: String, sortOption: String): Flow<Result<List<Folder>>> {
        return foldersDao.search(query, sortOption).mapToResultFlow()
    }

    override suspend fun deleteAllFolders() {
        foldersDao.deleteAllFolders()
    }
}