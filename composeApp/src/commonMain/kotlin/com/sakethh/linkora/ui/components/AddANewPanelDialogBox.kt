package com.sakethh.linkora.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.highlightOnFocused

data class AddANewPanelParam(
    val isDialogBoxVisible: MutableState<Boolean>,
    val onCreateClick: (shelfName: String, onCompletion: () -> Unit) -> Unit,
)

@Composable
fun AddANewPanelDialogBox(addANewPanelParam: AddANewPanelParam) {
    val localizedStrings = LocalizedStrings.current
    if (addANewPanelParam.isDialogBoxVisible.value) {
        val focusRequester = remember {
            FocusRequester()
        }
        val customShelfName = rememberSaveable {
            mutableStateOf("")
        }
        val isInProgress = rememberSaveable {
            mutableStateOf(false)
        }
        AlertDialog(
            title = {
                Text(
                    text = localizedStrings.AddANewPanel,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 22.sp,
                    lineHeight = 28.sp,
                )
            },
            onDismissRequest = {
                if (isInProgress.value.not()) {
                    addANewPanelParam.isDialogBoxVisible.value = false
                }
            },
            text = {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    maxLines = 1,
                    label = {
                        Text(
                            text = localizedStrings.PanelName,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    singleLine = true,
                    value = customShelfName.value,
                    onValueChange = {
                        customShelfName.value = it
                    },
                    readOnly = isInProgress.value,
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                if (isInProgress.value) return@AlertDialog
                Button(
                    modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .highlightOnFocused(shape = ButtonDefaults.shape)
                            .fillMaxWidth()
                            .pressScaleEffect(),
                    onClick = {
                        isInProgress.value = true
                        addANewPanelParam.onCreateClick(
                            customShelfName.value,
                            {
                                addANewPanelParam.isDialogBoxVisible.value = false
                                isInProgress.value = false
                            },
                        )
                    },
                ) {
                    Text(
                        text = localizedStrings.AddANewPanel,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                    )
                }
            },
            dismissButton = {
                if (isInProgress.value.not()) {
                    OutlinedButton(
                        modifier =
                            Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .highlightOnFocused(shape = ButtonDefaults.shape)
                                .fillMaxWidth()
                                .pressScaleEffect(),
                        onClick = {
                            addANewPanelParam.isDialogBoxVisible.value = false
                        },
                    ) {
                        Text(
                            text = localizedStrings.Cancel,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp,
                        )
                    }
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            },
        )
    }
}
