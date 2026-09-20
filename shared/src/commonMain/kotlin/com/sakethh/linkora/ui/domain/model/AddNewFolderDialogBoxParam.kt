package com.sakethh.linkora.ui.domain.model

import com.sakethh.linkora.domain.model.Folder

data class AddNewFolderDialogBoxParam(
    val onDismiss: () -> Unit,
    val inCollectionDetailPane: Boolean,
    val onFolderCreateClick: (folderName: String, folderNote: String, onCompletion: () -> Unit) -> Unit,
    val currentFolder: Folder?,
)
