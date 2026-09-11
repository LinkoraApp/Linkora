package com.sakethh.linkora.ui.components

import LocalizedStrings
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.canPushToServer

enum class DeleteDialogBoxType {
    LINK,
    FOLDER,
    REMOVE_ENTIRE_DATA,
    SELECTED_DATA,
}

data class DeleteFolderOrLinkDialogParam(
    val onDismiss: () -> Unit,
    val deleteDialogBoxType: DeleteDialogBoxType,
    val onDeleteClick: (onCompletion: () -> Unit, deleteEverythingFromRemote: Boolean) -> Unit,
    val areFoldersSelectable: Boolean = false,
)

@Composable
fun DeleteFolderOrLinkDialog(
    preferences: AppPreferences,
    deleteFolderOrLinkDialogParam: DeleteFolderOrLinkDialogParam,
) {
    val localizedStrings = LocalizedStrings.current
    val isDeletionInProgress: MutableState<Boolean> = rememberSaveable {
        mutableStateOf(false)
    }
    val deleteEverythingFromRemote = rememberSaveable {
        mutableStateOf(false)
    }
    AlertDialog(
        modifier = Modifier.animateContentSize(),
        confirmButton = {
            if (isDeletionInProgress.value.not()) {
                Button(
                    modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidth()
                            .pressScaleEffect(),
                    onClick = {
                        isDeletionInProgress.value = true
                        deleteFolderOrLinkDialogParam.onDeleteClick(
                            {
                                isDeletionInProgress.value = false
                                deleteFolderOrLinkDialogParam.onDismiss()
                            },
                            deleteEverythingFromRemote.value,
                        )
                    },
                ) {
                    Text(
                        text = localizedStrings.Delete,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                    )
                }
            }
        },
        dismissButton = {
            if (isDeletionInProgress.value.not()) {
                OutlinedButton(
                    modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidth()
                            .pressScaleEffect(),
                    onClick = deleteFolderOrLinkDialogParam.onDismiss,
                ) {
                    Text(
                        text = localizedStrings.Cancel,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                    )
                }
            }
        },
        title = {
            Text(
                text =
                    if (isDeletionInProgress.value) {
                        localizedStrings.DeletionInProgress
                    } else {
                        deleteFolderOrLinkDialogParam.deleteDialogBoxType.getTitle(
                            localizedStrings = localizedStrings,
                            areFoldersSelectable = deleteFolderOrLinkDialogParam.areFoldersSelectable,
                        )
                    },
                style = MaterialTheme.typography.titleMedium,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                textAlign = TextAlign.Start,
            )
        },
        text = {
            if (
                isDeletionInProgress.value.not() &&
                preferences.canPushToServer() &&
                deleteFolderOrLinkDialogParam.deleteDialogBoxType ==
                DeleteDialogBoxType.REMOVE_ENTIRE_DATA
            ) {
                Row(
                    modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                            .clickable {
                                if (!isDeletionInProgress.value) {
                                    deleteEverythingFromRemote.value =
                                        !deleteEverythingFromRemote.value
                                }
                            },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = deleteEverythingFromRemote.value,
                        onCheckedChange = {
                            deleteEverythingFromRemote.value = it
                        },
                        enabled = isDeletionInProgress.value,
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text =
                            localizedStrings.DeleteEverythingFromRemoteDatabaseLabel,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            if (
                isDeletionInProgress.value.not() &&
                deleteFolderOrLinkDialogParam.deleteDialogBoxType == DeleteDialogBoxType.FOLDER
            ) {
                Text(
                    text = localizedStrings.FolderDeletionLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (isDeletionInProgress.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        onDismissRequest = {
            if (isDeletionInProgress.value.not()) {
                deleteFolderOrLinkDialogParam.onDismiss()
            }
        },
    )
}

private fun DeleteDialogBoxType.getTitle(areFoldersSelectable: Boolean, localizedStrings: LocalizedStrings): String = if (this == DeleteDialogBoxType.LINK && areFoldersSelectable) {
        localizedStrings.AreYouSureDeleteSelectedLinks
    } else if (this == DeleteDialogBoxType.LINK) {
        localizedStrings.AreYouSureDeleteLink
    } else if (this == DeleteDialogBoxType.FOLDER && areFoldersSelectable) {
        localizedStrings.AreYouSureDeleteSelectedFolders
    } else if (this == DeleteDialogBoxType.FOLDER) {
        localizedStrings.AreYouSureDeleteFolder
    } else if (this == DeleteDialogBoxType.SELECTED_DATA) {
        localizedStrings.AreYouSureDeleteSelectedItems
    } else {
        localizedStrings.AreYouSureDeleteEverything
    }
