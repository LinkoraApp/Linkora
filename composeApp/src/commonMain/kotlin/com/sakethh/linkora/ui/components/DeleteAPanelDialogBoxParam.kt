package com.sakethh.linkora.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.rememberLocalizedString
import com.sakethh.linkora.utils.replaceFirstPlaceHolderWith

data class DeleteAPanelDialogBoxParam(
    val isDialogBoxVisible: MutableState<Boolean>,
    val onDeleteClick: (onCompletion: () -> Unit) -> Unit,
    val panelName: String,
)

@Composable
fun DeleteAPanelDialogBox(deleteAPanelDialogBoxParam: DeleteAPanelDialogBoxParam) {
    if (deleteAPanelDialogBoxParam.isDialogBoxVisible.value) {
        val isInProgress = rememberSaveable {
            mutableStateOf(false)
        }
        AlertDialog(
            confirmButton = {
                if (isInProgress.value) return@AlertDialog
                Button(
                    modifier = Modifier.fillMaxWidth().pressScaleEffect(),
                    onClick = {
                        isInProgress.value = true
                        deleteAPanelDialogBoxParam.onDeleteClick({
                            isInProgress.value = false
                            deleteAPanelDialogBoxParam.isDialogBoxVisible.value = false
                        })
                    },
                ) {
                    Text(
                        text = Localization.Key.PermanentlyDeleteThePanel.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                    )
                }
            },
            dismissButton = {
                if (isInProgress.value.not()) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth().pressScaleEffect(),
                        onClick = {
                            deleteAPanelDialogBoxParam.isDialogBoxVisible.value = false
                        },
                    ) {
                        Text(
                            text = Localization.Key.Cancel.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            },
            title = {
                Text(
                    text =
                    Localization.Key.AreYouSureWantToDeleteThePanel.rememberLocalizedString()
                        .replaceFirstPlaceHolderWith(deleteAPanelDialogBoxParam.panelName),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Start,
                )
            },
            text = {
                Text(
                    text =
                    Localization.Key.OnceDeletedThisPanelCannotBeRestored.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                )
            },
            onDismissRequest = {
                if (isInProgress.value.not()) {
                    deleteAPanelDialogBoxParam.isDialogBoxVisible.value = false
                }
            },
        )
    }
}
