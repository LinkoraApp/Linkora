package com.sakethh.linkora.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.sakethh.linkora.Localization
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.rememberLocalizedString
import com.sakethh.linkora.utils.replaceFirstPlaceHolderWith

@Composable
fun RenameAShelfPanelDialogBox(
    isDialogBoxVisible: MutableState<Boolean>,
    onRenameClick: (String, onCompletion: () -> Unit) -> Unit,
    panelName: String,
) {
    if (isDialogBoxVisible.value) {
        val newShelfName = rememberSaveable {
            mutableStateOf("")
        }
        val isInProgress = rememberSaveable {
            mutableStateOf(false)
        }
        AlertDialog(
            onDismissRequest = {
                if (isInProgress.value.not()) {
                    isDialogBoxVisible.value = false
                }
            },
            confirmButton = {
                if (isInProgress.value) return@AlertDialog
                Button(
                    modifier =
                    Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        .fillMaxWidth()
                        .pressScaleEffect(),
                    onClick = {
                        isInProgress.value = true
                        onRenameClick(
                            newShelfName.value,
                            {
                                isInProgress.value = false
                                isDialogBoxVisible.value = false
                            },
                        )
                    },
                ) {
                    Text(
                        text = Localization.Key.ChangePanelName.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                    )
                }
            },
            title = {
                Text(
                    text =
                    Localization.Key.EditPanelName.rememberLocalizedString()
                        .replaceFirstPlaceHolderWith(panelName),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 22.sp,
                    lineHeight = 27.sp,
                    textAlign = TextAlign.Start,
                )
            },
            text = {
                OutlinedTextField(
                    label = {
                        Text(
                            text = Localization.Key.NewNameForPanel.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = newShelfName.value,
                    onValueChange = {
                        newShelfName.value = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = isInProgress.value,
                )
            },
            dismissButton = {
                if (isInProgress.value.not()) {
                    OutlinedButton(
                        modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidth()
                            .pressScaleEffect(),
                        onClick = {
                            isDialogBoxVisible.value = false
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
        )
    }
}
