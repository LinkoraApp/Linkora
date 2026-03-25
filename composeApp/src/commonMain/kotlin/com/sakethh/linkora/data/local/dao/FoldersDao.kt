package com.sakethh.linkora.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.utils.Sorting
import kotlinx.coroutines.flow.Flow

@Dao
interface FoldersDao {
    @Insert
    suspend fun insertANewFolder(folders: Folder): Long

    @Query("INSERT INTO folders(name, note,parentFolderID,isArchived) SELECT name, note, :parentFolderID, isArchived FROM folders WHERE localId= :actualFolderId")
    suspend fun duplicateAFolder(actualFolderId: Long, parentFolderID: Long?): Long

    @Insert
    suspend fun insertMultipleNewFolders(foldersTable: List<Folder>)

    @Query("SELECT * FROM folders WHERE parentFolderID IS NULL AND isArchived = 1")
    fun getAllArchiveFoldersAsFlow(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentFolderID IS NULL AND isArchived = 1")
    suspend fun getAllArchiveFoldersAsList(): List<Folder>

    @Query("SELECT * FROM folders WHERE parentFolderID IS NULL AND isArchived = 0")
    fun getAllRootFoldersAsFlow(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentFolderID IS NULL")
    suspend fun getAllRootFoldersAsList(): List<Folder>

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersAsList(): List<Folder>

    @Query("SELECT EXISTS(SELECT 1 FROM folders)")
    suspend fun doesFolderTableHaveData(): Boolean

    @Query("SELECT * FROM folders")
    fun getAllFoldersAsFlow(): Flow<List<Folder>>

    @Query("SELECT localId FROM folders WHERE localId = (SELECT MAX(localId) FROM folders)")
    suspend fun getLatestFoldersTableID(): Long

    @Query("SELECT * FROM folders WHERE localId = :folderID")
    suspend fun getThisFolderData(folderID: Long): Folder

    @Query("SELECT COUNT(*) FROM folders WHERE name = :folderName AND parentFolderID = :parentFolderID")
    suspend fun doesFolderExists(folderName: String, parentFolderID: Long?): Int

    @Query("SELECT COUNT(*) FROM folders WHERE name = :folderName AND parentFolderID IS NULL")
    suspend fun doesThisRootFolderExists(folderName: String): Boolean

    @Query("SELECT * FROM folders WHERE parentFolderID = :parentFolderID")
    fun getChildFoldersAsFlow(parentFolderID: Long?): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE parentFolderID = :parentFolderID")
    suspend fun getChildFoldersAsList(parentFolderID: Long?): List<Folder>


    @Query("UPDATE folders SET name = :newFolderName WHERE localId = :folderID")
    suspend fun renameAFolderName(folderID: Long, newFolderName: String)


    @Query("UPDATE folders SET isArchived = 1 WHERE localId=:folderID")
    suspend fun markFolderAsArchive(folderID: Long)

    @Query("UPDATE folders SET isArchived = 1, lastModified = :eventTimestamp WHERE localId in (:folderIDs)")
    suspend fun markMultipleFoldersAsArchive(folderIDs: List<Long>, eventTimestamp: Long)

    @Query("UPDATE folders SET isArchived = 0, lastModified = :eventTimestamp WHERE localId in (:folderIDs)")
    suspend fun markMultipleFoldersAsRegular(folderIDs: List<Long>, eventTimestamp: Long)

    @Query("UPDATE folders SET isArchived = 0 WHERE localId=:folderID")
    suspend fun markFolderAsRegularFolder(folderID: Long)


    @Query("UPDATE folders SET note = :newNote WHERE localId = :folderID")
    suspend fun renameAFolderNote(folderID: Long, newNote: String)

    @Update
    suspend fun updateFolder(foldersTable: Folder)

    @Query("UPDATE folders SET lastModified =:timestamp WHERE localId =:localFolderID")
    suspend fun updateFolderTimestamp(timestamp: Long, localFolderID: Long)

    @Query("UPDATE folders SET lastModified =:timestamp WHERE localId IN (:localFolderIDs)")
    suspend fun updateFoldersTimestamp(timestamp: Long, localFolderIDs: List<Long>)

    @Query("UPDATE folders SET note = \"\" WHERE localId = :folderID")
    suspend fun deleteAFolderNote(folderID: Long)

    @Query("DELETE from folders WHERE localId = :folderID")
    suspend fun deleteAFolder(folderID: Long)

    @Query(
        """
    SELECT * FROM folders 
    WHERE parentFolderID = :parentFolderId
    ORDER BY 
        CASE WHEN :sortOption = '${Sorting.OLD_TO_NEW}' THEN localId END ASC,
        CASE WHEN :sortOption = '${Sorting.NEW_TO_OLD}' THEN localId END DESC,
        CASE WHEN :sortOption = '${Sorting.A_TO_Z}' THEN name COLLATE NOCASE END ASC,
        CASE WHEN :sortOption = '${Sorting.Z_TO_A}' THEN name COLLATE NOCASE END DESC
    LIMIT :pageSize
    OFFSET :startIndex
    """
    )
    fun getChildFolders(
        parentFolderId: Long,
        sortOption: String,
        pageSize: Int, startIndex: Long
    ): Flow<List<Folder>>

    @Query(
        """
    SELECT * FROM folders 
    WHERE parentFolderID = :parentFolderId
    """
    )
    suspend fun getChildFoldersAsList(
        parentFolderId: Long,
    ): List<Folder>

    @Query(
        """
    SELECT * FROM folders 
    WHERE parentFolderID = :parentFolderId
    ORDER BY 
        CASE WHEN :sortOption = '${Sorting.OLD_TO_NEW}' THEN localId END ASC,
        CASE WHEN :sortOption = '${Sorting.NEW_TO_OLD}' THEN localId END DESC,
        CASE WHEN :sortOption = '${Sorting.A_TO_Z}' THEN name COLLATE NOCASE END ASC,
        CASE WHEN :sortOption = '${Sorting.Z_TO_A}' THEN name COLLATE NOCASE END DESC
    """
    )
    fun getChildFolders(
        parentFolderId: Long,
        sortOption: String,
    ): Flow<List<Folder>>


    @Query("""
    SELECT * FROM folders 
    WHERE parentFolderID IS NULL 
      AND isArchived = :isArchived
      AND (
          :lastSeenId IS NULL 
          OR (:isAscending = 1 AND localId > :lastSeenId)
          OR (:isAscending = 0 AND localId < :lastSeenId)
      )
    ORDER BY 
        CASE WHEN :isAscending = 1 THEN localId END ASC,
        CASE WHEN :isAscending = 0 THEN localId END DESC
    LIMIT :pageSize
    """)
    fun getRootFoldersSortedById(
        isArchived: Boolean,
        lastSeenId: Long?,
        isAscending: Boolean,
        pageSize: Int
    ): Flow<List<Folder>>

    @Query("""
    SELECT * FROM folders 
    WHERE parentFolderID IS NULL 
      AND isArchived = :isArchived
      AND (
          :lastSeenName IS NULL OR :lastSeenName = '' OR
          (
              :isAscending = 1 AND (
                  name COLLATE NOCASE > :lastSeenName 
                  OR (name COLLATE NOCASE = :lastSeenName AND localId > :lastSeenId)
              )
          ) 
          OR 
          (
              :isAscending = 0 AND (
                  name COLLATE NOCASE < :lastSeenName 
                  OR (name COLLATE NOCASE = :lastSeenName AND localId > :lastSeenId)
              )
          )
      )
    ORDER BY 
        CASE WHEN :isAscending = 1 THEN name END COLLATE NOCASE ASC,
        CASE WHEN :isAscending = 0 THEN name END COLLATE NOCASE DESC,
        localId ASC
    LIMIT :pageSize
    """)
    fun getRootFoldersSortedByName(
        isArchived: Boolean,
        lastSeenName: String?,
        lastSeenId: Long?,
        isAscending: Boolean,
        pageSize: Int
    ): Flow<List<Folder>>

    @Query(
        "SELECT * FROM folders \n" +
                "    WHERE (LOWER(name) LIKE '%' || LOWER(:query) || '%' \n" +
                "           OR LOWER(note) LIKE '%' || LOWER(:query) || '%') \n" +
                "    ORDER BY \n" +
                "        CASE WHEN :sortOption = '${Sorting.A_TO_Z}' THEN name COLLATE NOCASE END ASC,\n" +
                "        CASE WHEN :sortOption = '${Sorting.Z_TO_A}' THEN name COLLATE NOCASE END DESC,\n" +
                "        CASE WHEN :sortOption = '${Sorting.NEW_TO_OLD}' THEN localId END DESC,\n" +
                "        CASE WHEN :sortOption = '${Sorting.OLD_TO_NEW}' THEN localId END ASC"
    )
    fun search(query: String, sortOption: String): Flow<List<Folder>>

    @Query("SELECT remoteId FROM folders WHERE localId = :localId LIMIT 1")
    suspend fun getRemoteFolderId(localId: Long): Long?

    @Query("SELECT remoteId FROM folders WHERE localId IN (:localIds)")
    suspend fun getRemoteIds(localIds: List<Long>): List<Long>

    @Query("SELECT localId FROM folders WHERE remoteId = :remoteId")
    suspend fun getLocalIdOfAFolder(remoteId: Long): Long?

    @Query("SELECT * FROM folders WHERE remoteId IS NULL")
    suspend fun getUnSyncedFolders(): List<Folder>

    @Query("UPDATE folders SET parentFolderId = :parentFolderId, lastModified = :eventTimestamp WHERE localId IN (:folderIDs)")
    suspend fun moveFolders(parentFolderId: Long, folderIDs: List<Long>, eventTimestamp: Long)

    @Query("UPDATE folders SET parentFolderId = NULL WHERE localId IN (:foldersIds)")
    suspend fun markFoldersAsRoot(foldersIds: List<Long>)

    @Query("UPDATE folders SET remoteId = :remoteId WHERE localId = :localId")
    suspend fun updateARemoteLinkId(localId: Long, remoteId: Long)
}