package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import kotlinx.coroutines.flow.Flow

interface LocalPanelsRepo {
    suspend fun addANewPanel(
        panel: Panel,
        viaSocket: Boolean = false,
    ): Flow<Result<Long>>

    suspend fun addANewPanelLocally(panel: Panel): Long

    suspend fun deleteAPanel(
        id: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun updateAPanelName(
        newName: String,
        panelId: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun updatePanel(panel: Panel)

    suspend fun addANewFolderInAPanel(
        panelFolder: PanelFolder,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    suspend fun deleteAFolderFromAllPanels(folderID: Long)

    suspend fun deleteAFolderFromAPanel(
        panelId: Long,
        folderID: Long,
        viaSocket: Boolean = false,
    ): Flow<Result<Unit>>

    fun getAllThePanels(): Flow<List<Panel>>

    suspend fun getAllThePanelsAsAList(): List<Panel>

    suspend fun isPanelsTableEmpty(): Boolean

    suspend fun getAllThePanelFoldersAsAList(): List<PanelFolder>

    fun getAllThePanelFoldersAsAFlow(): Flow<List<PanelFolder>>

    fun getAllTheFoldersFromAPanel(panelId: Long): Flow<List<PanelFolder>>

    suspend fun getPanel(panelId: Long): Panel

    suspend fun getPanelFolder(localId: Long): PanelFolder

    suspend fun getLocalPanelId(remoteId: Long): Long?

    suspend fun getRemotePanelId(localId: Long): Long?

    suspend fun getLocalPanelFolderId(remoteId: Long): Long?

    suspend fun updateAFolderName(
        folderID: Long,
        newName: String,
    )

    suspend fun addMultiplePanels(panels: List<Panel>)

    suspend fun addMultiplePanelFolders(panelFolders: List<PanelFolder>)

    suspend fun getLatestPanelID(): Long

    suspend fun getUnSyncedPanels(): List<Panel>

    suspend fun getUnSyncedPanelFolders(): List<PanelFolder>

    suspend fun updateAPanelFolder(panelFolder: PanelFolder)
}
