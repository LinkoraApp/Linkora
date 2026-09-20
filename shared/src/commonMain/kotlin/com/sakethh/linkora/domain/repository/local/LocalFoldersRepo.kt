package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface LocalFoldersRepo {
    suspend fun insertANewFolder(
        folder: Folder,
        viaSocket: Boolean = false,
    ): Flow<Result<Long>>

    suspend fun insertANewFolderLocally(folder: Folder): Long

    suspend fun getAllRootFoldersAsList(): List<Folder>

    fun getAllFoldersAsResultList(): Flow<Result<List<Folder>>>

    suspend fun getAllFoldersAsList(): List<Folder>

    suspend fun isFoldersTableEmpty(): Boolean

    fun getAllFoldersAsFlow(): Flow<List<Folder>>

    suspend fun getChildFoldersOfThisParentIDAsList(parentFolderID: Long?): List<Folder>

    suspend fun getLatestFoldersTableID(): Long

    suspend fun getThisFolderData(folderID: Long): Flow<Result<Folder>>

    suspend fun doesThisChildFolderExists(
        folderName: String,
        parentFolderID: Long?,
    ): Flow<Result<Int>>

    suspend fun doesThisRootFolderExists(folderName: String): Flow<Result<Boolean>>

    suspend fun getRootFolders(
        sortOption: String,
        isArchived: Boolean,
        pageSize: Int,
        lastSeenName: String?,
        lastSeenId: Long?,
    ): Flow<Result<List<Folder>>>

    suspend fun getChildFolders(
        parentFolderId: Long,
        sortOption: String,
        pageSize: Int,
        startIndex: Long,
    ): Flow<Result<List<Folder>>>

    suspend fun getChildFoldersAsList(parentFolderId: Long): List<Folder>

    fun sortFoldersAsNonResultFlow(
        parentFolderId: Long,
        sortOption: String,
    ): Flow<List<Folder>>

    suspend fun getChildFoldersOfThisParentIDAsFlow(parentFolderID: Long?): Flow<Result<List<Folder>>>

    suspend fun markFolderAsArchive(
        folderID: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun markFolderAsRegularFolder(
        folderID: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun markFoldersAsRoot(
        folderIDs: List<Long>,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun updateLocalFolderData(folder: Folder): Flow<Result<Unit>>

    suspend fun updateFolder(
        folder: Folder,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun deleteAFolderNote(
        folderID: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun deleteAFolder(
        folderID: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun deleteMultipleFolders(
        folderIDs: List<Long>,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    fun search(
        query: String,
        sortOption: String,
    ): Flow<Result<List<Folder>>>

    suspend fun getRemoteIdOfAFolder(localId: Long): Long?

    suspend fun getLocalIdOfAFolder(remoteId: Long): Long?

    suspend fun getUnSyncedFolders(): List<Folder>
}
