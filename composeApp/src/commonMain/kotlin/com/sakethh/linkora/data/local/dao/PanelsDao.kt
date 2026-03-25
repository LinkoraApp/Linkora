package com.sakethh.linkora.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import kotlinx.coroutines.flow.Flow

@Dao
interface PanelsDao {
    @Query("SELECT MAX(localId) FROM panel")
    suspend fun getLatestPanelID(): Long

    @Query("SELECT MAX(localId) FROM panel_folder")
    suspend fun getLatestPanelFolderID(): Long

    @Insert
    suspend fun addANewPanel(panel: Panel): Long

    @Update
    suspend fun updateAPanel(panel: Panel)

    @Update
    suspend fun updateAPanelFolder(panelFolder: PanelFolder)

    @Query("SELECT remoteId FROM panel WHERE localId = :localId")
    suspend fun getRemoteIdOfPanel(localId: Long): Long?

    @Query("UPDATE panel_folder SET folderName = :newName WHERE folderId = :id")
    suspend fun updateAFolderName(id: Long, newName: String)

    @Insert
    suspend fun addMultiplePanels(panels: List<Panel>)

    @Insert
    suspend fun addMultiplePanelFolders(panelFolders: List<PanelFolder>)

    @Query("DELETE FROM panel WHERE localId = :id")
    suspend fun deleteAPanel(id: Long)

    @Query("UPDATE panel SET panelName = :newName WHERE localId = :panelId")
    suspend fun updateAPanelName(newName: String, panelId: Long)

    @Query("UPDATE panel SET lastModified = :timestamp WHERE localId = :panelId")
    suspend fun updatePanelTimestamp(panelId: Long, timestamp: Long)

    @Query("DELETE FROM panel_folder WHERE connectedPanelId = :panelId")
    suspend fun deleteConnectedFoldersOfPanel(panelId: Long)

    @Insert
    suspend fun addANewFolderInAPanel(panelFolder: PanelFolder): Long

    @Query("DELETE FROM panel_folder WHERE connectedPanelId = :panelId AND folderId = :folderID ")
    suspend fun deleteAFolderFromAPanel(panelId: Long, folderID: Long)

    @Query("DELETE FROM panel_folder WHERE folderId = :folderID")
    suspend fun deleteAFolderFromAllPanels(folderID: Long)

    @Query("SELECT * FROM panel")
    fun getAllThePanels(): Flow<List<Panel>>

    @Query("SELECT * FROM panel")
    suspend fun getAllThePanelsAsAList(): List<Panel>

    @Query("SELECT EXISTS(SELECT 1 FROM panel)")
    suspend fun doesPanelsTableHaveData(): Boolean

    @Query("SELECT * FROM panel_folder")
    suspend fun getAllThePanelFoldersAsAList(): List<PanelFolder>

    @Query("SELECT * FROM panel_folder")
    fun getAllThePanelFoldersAsAFlow(): Flow<List<PanelFolder>>

    @Query("SELECT * FROM panel_folder WHERE connectedPanelId = :panelId")
    fun getAllTheFoldersFromAPanel(panelId: Long): Flow<List<PanelFolder>>

    @Query("SELECT * FROM panel WHERE localId=:panelId LIMIT 1") // there will always be only 1 panel with the given ID, but added `LIMIT 1` because why not.
    suspend fun getPanel(panelId: Long): Panel

    @Query("SELECT localId FROM panel WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getLocalPanelId(remoteId: Long): Long?

    @Query("SELECT localId FROM panel_folder WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getLocalPanelFolderId(remoteId: Long): Long?

    @Query("SELECT * FROM panel_folder WHERE localId=:localPanelFolderId LIMIT 1")
    suspend fun getPanelFolder(localPanelFolderId: Long): PanelFolder

    @Query("SELECT * FROM panel WHERE remoteId IS NULL")
    suspend fun getUnSyncedPanels(): List<Panel>

    @Query("SELECT * FROM panel_folder WHERE remoteId IS NULL")
    suspend fun getUnSyncedPanelFolders(): List<PanelFolder>
}