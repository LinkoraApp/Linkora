package com.sakethh.linkora.ui.components.menu

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link

data class MenuBtmSheetParam @OptIn(ExperimentalMaterial3Api::class) constructor(
    val link: MutableState<Link>?,
    val folder: MutableState<Folder>?,
    val btmModalSheetState: SheetState,
    val shouldBtmModalSheetBeVisible: MutableState<Boolean>,
    val menuBtmSheetFor: MenuBtmSheetType,
    val onDelete: () -> Unit,
    val onManageLinkReminders: () -> Unit,
    val onDeleteNote: () -> Unit,
    val onRename: () -> Unit,
    val onRefreshClick: () -> Unit,
    val onArchive: () -> Unit,
    val onAddToImportantLinks: (() -> Unit?)?,
    val onForceLaunchInAnExternalBrowser: () -> Unit = {},
    val showQuickActions: MutableState<Boolean>,
    val onCopy: () -> Unit = {},
    val onMoveToRootFoldersClick: () -> Unit = {},
    val onMove: () -> Unit = {},
    val shouldTransferringOptionShouldBeVisible: Boolean,
    val shouldShowArchiveOption: () -> Boolean,
    val showProgressBarDuringRemoteSave: MutableState<Boolean>,
    val showNote: MutableState<Boolean> = mutableStateOf(false)
)