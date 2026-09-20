package com.sakethh.linkora.ui.screens.home.panels

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.panel.PanelFolder
import kotlinx.coroutines.flow.StateFlow

@Stable
data class SpecificPanelManagerScreenParam(
    val paddingValues: PaddingValues = PaddingValues(),
    val foldersOfTheSelectedPanel: StateFlow<List<PanelFolder>>,
    val foldersToIncludeInPanel: StateFlow<List<Folder>>,
    val foldersSearchQuery: String,
    val performAction: (PanelsAction) -> Unit,
)
