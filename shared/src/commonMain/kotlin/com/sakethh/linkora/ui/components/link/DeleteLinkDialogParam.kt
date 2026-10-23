package com.sakethh.linkora.ui.components.link

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.utils.pressScaleEffect

data class DeleteLinkDialogParam(
    val onDismiss: () -> Unit,
    val onDeleteClick: (onCompletion: () -> Unit) -> Unit,
)

@Composable
fun DeleteLinkDialog(deleteLinkDialogParam: DeleteLinkDialogParam) {
    val localizedStrings = LocalizedStrings.current
    val isDeletionInProgress: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) }

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
                        deleteLinkDialogParam.onDeleteClick {
                            isDeletionInProgress.value = false
                            deleteLinkDialogParam.onDismiss()
                        }
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
                    onClick = deleteLinkDialogParam.onDismiss,
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
                        localizedStrings.AreYouSureDeleteLink
                    },
                style = MaterialTheme.typography.titleMedium,
                fontSize = 22.sp,
                lineHeight = 27.sp,
                textAlign = TextAlign.Start,
            )
        },
        text = {
            if (isDeletionInProgress.value) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        onDismissRequest = { if (!isDeletionInProgress.value) deleteLinkDialogParam.onDismiss() },
    )
}
