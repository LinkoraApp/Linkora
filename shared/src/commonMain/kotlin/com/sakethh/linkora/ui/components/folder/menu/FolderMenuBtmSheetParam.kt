package com.sakethh.linkora.ui.components.folder.menu

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import com.sakethh.linkora.domain.model.Folder

@Stable
data class FolderMenuBtmSheetParam
@OptIn(ExperimentalMaterial3Api::class)
constructor(
    val onDismiss: () -> Unit,
    val folder: Folder,
    val btmModalSheetState: SheetState,
    val onDelete: () -> Unit,
    val onDeleteNote: () -> Unit,
    val onRename: () -> Unit,
    val onArchive: () -> Unit,
    val onShare: (folderName: String) -> Unit,
    val showProgressBarDuringRemoteSave: MutableState<Boolean>,
)
