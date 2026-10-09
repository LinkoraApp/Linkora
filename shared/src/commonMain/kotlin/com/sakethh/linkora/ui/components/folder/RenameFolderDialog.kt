package com.sakethh.linkora.ui.components.folder

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.highlightOnFocused
import com.sakethh.linkora.utils.replaceActual

@Stable
data class RenameFolderDialogParam
@OptIn(ExperimentalMaterial3Api::class)
constructor(
    val showDialogBox: Boolean,
    val sheetState: SheetState,
    val onHide: () -> Unit,
    val onSave: (
        newTitle: String,
        newNote: String,
        onCompletion: () -> Unit,
    ) -> Unit,
    val existingFolderName: String,
    val existingNote: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameFolderDialog(renameFolderDialogParam: RenameFolderDialogParam) {
    val localizedStrings = LocalizedStrings.current

    if (renameFolderDialogParam.showDialogBox) {
        var newFolderName by rememberSaveable(renameFolderDialogParam.existingFolderName) {
            mutableStateOf(renameFolderDialogParam.existingFolderName)
        }
        var newNote by rememberSaveable(renameFolderDialogParam.existingNote) {
            mutableStateOf(renameFolderDialogParam.existingNote)
        }
        var showProgressBar by rememberSaveable { mutableStateOf(false) }

        val content: ComposableContent = {
            Column(
                Modifier
                    .wrapContentSize()
                    .animateContentSize()
                    .padding(
                        bottom = 15.dp,
                        top = if (Platform.Android.onMobile()) 0.dp else 15.dp,
                    ).fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp),
                ) {
                    Text(
                        text = localizedStrings.RenameFolder.replaceActual(renameFolderDialogParam.existingFolderName),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(if (!Platform.Android.onMobile()) 0.85f else 1f),
                    )
                    if (!Platform.Android.onMobile() && !showProgressBar) {
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                            onClick = renameFolderDialogParam.onHide,
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    label = {
                        Text(
                            text = localizedStrings.NewName,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp),
                    readOnly = showProgressBar,
                )
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    label = {
                        Text(
                            text = localizedStrings.NewNote,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = newNote,
                    onValueChange = { newNote = it },
                    modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp),
                    readOnly = showProgressBar,
                )

                if (showProgressBar) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    return@Column
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    modifier =
                        Modifier
                            .padding(start = 15.dp, end = 15.dp)
                            .pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidth()
                            .pressScaleEffect()
                            .highlightOnFocused(shape = ButtonDefaults.shape),
                    onClick = {
                        showProgressBar = true
                        renameFolderDialogParam.onSave(
                            newFolderName,
                            newNote,
                            { showProgressBar = false },
                        )
                    },
                ) {
                    Text(
                        text = localizedStrings.Save,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedButton(
                    modifier =
                        Modifier
                            .padding(start = 15.dp, end = 15.dp)
                            .pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidth()
                            .pressScaleEffect()
                            .highlightOnFocused(shape = ButtonDefaults.shape),
                    onClick = renameFolderDialogParam.onHide,
                ) {
                    Text(
                        text = localizedStrings.Cancel,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                    )
                }
            }
        }

        if (Platform.Android.onMobile()) {
            ModalBottomSheet(
                sheetState = renameFolderDialogParam.sheetState,
                modifier = Modifier.imePadding(),
                properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
                onDismissRequest = { if (!showProgressBar) renameFolderDialogParam.onHide() },
            ) { content() }
        } else {
            BasicAlertDialog(
                modifier =
                    Modifier
                        .wrapContentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AlertDialogDefaults.containerColor),
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { if (!showProgressBar) renameFolderDialogParam.onHide() },
            ) { content() }
        }
    }
}
