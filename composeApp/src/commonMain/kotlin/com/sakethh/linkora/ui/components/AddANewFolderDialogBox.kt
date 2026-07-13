package com.sakethh.linkora.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.ui.domain.model.AddNewFolderDialogBoxParam
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.inDoubleQuotes

@Composable
fun AddANewFolderDialogBox(addNewFolderDialogBoxParam: AddNewFolderDialogBoxParam) {
    val scrollState = rememberScrollState()
    val isFolderCreationInProgress = rememberSaveable {
        mutableStateOf(false)
    }
    val folderNameTextFieldValue = rememberSaveable {
        mutableStateOf("")
    }
    val noteTextFieldValue = rememberSaveable {
        mutableStateOf("")
    }
    val focusRequester = remember {
        FocusRequester()
    }
    AlertDialog(
        dismissButton = {
            if (!isFolderCreationInProgress.value) {
                androidx.compose.material3.OutlinedButton(
                    modifier =
                    Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        .fillMaxWidth()
                        .pressScaleEffect(),
                    onClick = addNewFolderDialogBoxParam.onDismiss,
                ) {
                    Text(
                        text = Localization.rememberLocalizedString(Localization.Key.Cancel),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                    )
                }
            }
        },
        confirmButton = {
            if (!isFolderCreationInProgress.value) {
                Button(
                    modifier =
                    Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        .fillMaxWidth()
                        .pressScaleEffect(),
                    onClick = {
                        isFolderCreationInProgress.value = true
                        addNewFolderDialogBoxParam.onFolderCreateClick(
                            folderNameTextFieldValue.value,
                            noteTextFieldValue.value,
                            {
                                addNewFolderDialogBoxParam.onDismiss()
                                isFolderCreationInProgress.value = false
                            },
                        )
                    },
                ) {
                    Text(
                        text = Localization.rememberLocalizedString(Localization.Key.Create),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                    )
                }
            }
        },
        modifier = Modifier.animateContentSize().wrapContentHeight(),
        onDismissRequest = {
            if (!isFolderCreationInProgress.value) {
                addNewFolderDialogBoxParam.onDismiss()
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                OutlinedTextField(
                    readOnly = isFolderCreationInProgress.value,
                    modifier = Modifier.focusRequester(focusRequester).fillMaxWidth(),
                    maxLines = 1,
                    label = {
                        Text(
                            text = Localization.rememberLocalizedString(Localization.Key.FolderName),
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    singleLine = true,
                    value = folderNameTextFieldValue.value,
                    onValueChange = {
                        folderNameTextFieldValue.value = it
                    },
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    readOnly = isFolderCreationInProgress.value,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text =
                            Localization.rememberLocalizedString(
                                Localization.Key.NoteForCreatingTheFolder,
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = noteTextFieldValue.value,
                    onValueChange = {
                        noteTextFieldValue.value = it
                    },
                )
                if (isFolderCreationInProgress.value) {
                    Spacer(modifier = Modifier.height(40.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        title = {
            Text(
                text =
                if (
                    addNewFolderDialogBoxParam.inCollectionDetailPane &&
                    addNewFolderDialogBoxParam.currentFolder != null
                ) {
                    Localization.rememberLocalizedString(Localization.Key.CreateANewFolderIn)
                        .replace(
                            LinkoraPlaceHolder.First.value,
                            addNewFolderDialogBoxParam.currentFolder.name.inDoubleQuotes(),
                        )
                } else {
                    Localization.rememberLocalizedString(Localization.Key.CreateANewFolder)
                },
                style = MaterialTheme.typography.titleMedium,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            )
        },
    )
}
