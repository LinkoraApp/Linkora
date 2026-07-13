package com.sakethh.linkora.ui.screens.home.state

import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.PanelFolder

data class ProcessedPanelFolders(
    val panelFolders: List<PanelFolder>,
    val links: List<List<Link>>,
    val folders: List<List<Folder>>,
)
