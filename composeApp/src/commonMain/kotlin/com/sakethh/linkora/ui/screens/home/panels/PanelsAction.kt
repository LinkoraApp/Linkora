package com.sakethh.linkora.ui.screens.home.panels

import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder

sealed interface PanelsAction {
    data class UpdateFoldersSearchQuery(
        val query: String,
    ) : PanelsAction

    data class RemoveAFolderFromPanel(
        val folderId: Long,
        val panelId: Long,
    ) : PanelsAction

    data class AddANewFolderInAPanel(
        val panelFolder: PanelFolder,
    ) : PanelsAction

    data class AddANewAPanel(
        val panel: Panel,
        val onCompletion: () -> Unit,
    ) : PanelsAction

    data class DeleteAPanel(
        val panelId: Long,
        val onCompletion: () -> Unit,
    ) : PanelsAction

    data class RenameAPanel(
        val panelId: Long,
        val newName: String,
        val onCompletion: () -> Unit,
    ) : PanelsAction
}
