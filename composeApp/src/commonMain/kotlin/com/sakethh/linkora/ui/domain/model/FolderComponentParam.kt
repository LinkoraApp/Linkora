package com.sakethh.linkora.ui.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.vector.ImageVector
import com.sakethh.linkora.domain.model.Folder

data class FolderComponentParam(
    val folder: Folder,
    val onClick: () -> Unit,
    val onLongClick: () -> Unit,
    val onMoreIconClick: () -> Unit,
    val isCurrentlyInDetailsView: MutableState<Boolean>,
    val showMoreIcon: MutableState<Boolean>,
    val isSelectedForSelection: MutableState<Boolean>,
    val showCheckBox: MutableState<Boolean>,
    val onCheckBoxChanged: (Boolean) -> Unit,
    val mainIcon: ImageVector = Icons.Outlined.Folder
)
