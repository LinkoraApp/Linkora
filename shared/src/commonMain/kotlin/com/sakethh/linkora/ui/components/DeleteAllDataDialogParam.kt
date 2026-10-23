package com.sakethh.linkora.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.canPushToServer

data class DeleteAllDataDialogParam(
    val onDismiss: () -> Unit,
    val onDeleteClick: (onCompletion: () -> Unit, deleteEverythingFromRemote: Boolean) -> Unit,
)

@Composable
fun DeleteAllDataDialog(
    preferences: AppPreferences,
    deleteAllDataDialogParam: DeleteAllDataDialogParam,
) {
    val localizedStrings = LocalizedStrings.current
    val isDeletionInProgress: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) }
    val deleteEverythingFromRemote = rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        modifier = Modifier.animateContentSize(),
        confirmButton = {
            if (!isDeletionInProgress.value) {
                Button(
                    modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidth()
                            .pressScaleEffect(),
                    onClick = {
                        isDeletionInProgress.value = true
                        deleteAllDataDialogParam.onDeleteClick(
                            {
                                isDeletionInProgress.value = false
                                deleteAllDataDialogParam.onDismiss()
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
            if (!isDeletionInProgress.value) {
                OutlinedButton(
                    modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidth()
                            .pressScaleEffect(),
                    onClick = deleteAllDataDialogParam.onDismiss,
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
                    if (isDeletionInProgress.value) localizedStrings.DeletionInProgress
                    else localizedStrings.AreYouSureDeleteEverything,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                textAlign = TextAlign.Start,
            )
        },
        text = {
            if (!isDeletionInProgress.value && preferences.canPushToServer()) {
                Row(
                    modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidth()
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
                        onCheckedChange = { deleteEverythingFromRemote.value = it },
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = localizedStrings.DeleteEverythingFromRemoteDatabaseLabel,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
            if (isDeletionInProgress.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        onDismissRequest = {
            if (!isDeletionInProgress.value) deleteAllDataDialogParam.onDismiss()
        },
    )
}
