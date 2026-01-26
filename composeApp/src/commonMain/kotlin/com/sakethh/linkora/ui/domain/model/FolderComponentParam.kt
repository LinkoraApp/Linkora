package com.sakethh.linkora.ui.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.sakethh.linkora.domain.model.Folder

@Stable
data class FolderComponentParam(
    val name: String,
    val note: String,
    val path: List<Folder>?,
    val showPath: Boolean,
    val leadingIcon: ImageVector = Icons.Outlined.Folder,
    val onClick: () -> Unit,
    val onPathItemClick: (folder:Folder) -> Unit,
    val onLongClick: () -> Unit,
    val onMoreIconClick: () -> Unit,
    val isCurrentlyInDetailsView: MutableState<Boolean>,
    val showMoreIcon: MutableState<Boolean>,
    val isSelectedForSelection: MutableState<Boolean>,
    val showCheckBox: MutableState<Boolean>,
    val onCheckBoxChanged: (Boolean) -> Unit
)
