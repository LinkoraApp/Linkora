package com.sakethh.linkora.ui.components.menu

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.domain.model.LinkTagsPair

@Stable
data class MenuBtmSheetParam @OptIn(ExperimentalMaterial3Api::class) constructor(
    val onDismiss: () -> Unit,
    val linkTagsPair: LinkTagsPair?,
    val folder: Folder?,
    val btmModalSheetState: SheetState,
    val menuBtmSheetFor: MenuBtmSheetType,
    val onDelete: () -> Unit,
    val onDeleteNote: () -> Unit,
    val onRename: () -> Unit,
    val onRefresh: (RefreshLinkType) -> Unit,
    val onArchive: () -> Unit,
    val onAddToImportantLinks: (() -> Unit?)?,
    val onForceLaunchInAnExternalBrowser: () -> Unit,
    val showQuickActions: MutableState<Boolean>,
    val onShare: (url: String) -> Unit,
    val shouldTransferringOptionShouldBeVisible: Boolean,
    val shouldShowArchiveOption: () -> Boolean,
    val showProgressBarDuringRemoteSave: MutableState<Boolean>,
    val onTagClick: (Tag) -> Unit,
    val showNote: MutableState<Boolean> = mutableStateOf(false)
)