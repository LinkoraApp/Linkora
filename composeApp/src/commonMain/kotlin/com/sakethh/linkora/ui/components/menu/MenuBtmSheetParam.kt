package com.sakethh.linkora.ui.components.menu

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class MenuBtmSheetParam @OptIn(ExperimentalMaterial3Api::class) constructor(
    val btmModalSheetState: SheetState,
    val shouldBtmModalSheetBeVisible: MutableState<Boolean>,
    val btmSheetFor: MenuItemType,
    val onDelete: () -> Unit,
    val onDeleteNote: () -> Unit,
    val onRename: () -> Unit,
    val onRefreshClick: () -> Unit,
    val onArchive: () -> Unit,
    val onUnarchiveClick: () -> Unit = {},
    val onImportantLinkClick: (() -> Unit?)? = null,
    val inArchiveScreen: MutableState<Boolean> = mutableStateOf(false),
    val inSpecificArchiveScreen: MutableState<Boolean> = mutableStateOf(false),
    val noteForSaving: String,
    val folderName: String,
    val linkTitle: String,
    val imgLink: String,
    val imgUserAgent: String,
    val forAChildFolder: MutableState<Boolean> = mutableStateOf(false),
    val webUrl: String = "",
    val forceBrowserLaunch: () -> Unit = {},
    val showQuickActions: MutableState<Boolean> = mutableStateOf(false),
    val shouldImportantLinkOptionBeVisible: MutableState<Boolean> = mutableStateOf(true),
    val onCopy: () -> Unit = {},
    val onMoveToRootFoldersClick: () -> Unit = {},
    val onMove: () -> Unit = {},
    val shouldTransferringOptionShouldBeVisible: Boolean = false
)