package com.sakethh.linkora.screens.home.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.localDB.LocalDBFunctions
import com.sakethh.linkora.ui.theme.LinkoraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameDialogBox(
    shouldDialogBoxAppear: MutableState<Boolean>,
    coroutineScope: CoroutineScope, existingFolderName: String,
) {
    val newFolderName = rememberSaveable {
        mutableStateOf("")
    }
    val scrollState = rememberScrollState()
    if (shouldDialogBoxAppear.value) {
        LinkoraTheme {
            AlertDialog(modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AlertDialogDefaults.containerColor),
                onDismissRequest = { shouldDialogBoxAppear.value = false }) {
                Column(modifier = Modifier.verticalScroll(scrollState)) {
                    Text(
                        text = "Rename \"$existingFolderName\" folder:",
                        color = AlertDialogDefaults.textContentColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(start = 20.dp, top = 30.dp),
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Start
                    )
                    OutlinedTextField(
                        maxLines = 1,
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 30.dp
                        ),
                        label = {
                            Text(
                                text = "New Name",
                                color = AlertDialogDefaults.textContentColor,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 12.sp
                            )
                        },
                        textStyle = MaterialTheme.typography.titleSmall,
                        singleLine = true,
                        shape = RoundedCornerShape(5.dp),
                        value = newFolderName.value,
                        onValueChange = {
                            newFolderName.value = it
                        })
                    Button(colors = ButtonDefaults.buttonColors(containerColor = AlertDialogDefaults.titleContentColor),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .padding(
                                end = 20.dp,
                                top = 20.dp,
                            )
                            .align(Alignment.End),
                        onClick = {
                            coroutineScope.launch {
                                LocalDBFunctions.renameAFolder(
                                    existingName = existingFolderName,
                                    newName = newFolderName.value
                                )
                            }
                            shouldDialogBoxAppear.value = false
                        }) {
                        Text(
                            text = "Rename",
                            color = AlertDialogDefaults.containerColor,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp
                        )
                    }
                    OutlinedButton(colors = ButtonDefaults.outlinedButtonColors(),
                        border = BorderStroke(
                            width = 1.dp,
                            color = AlertDialogDefaults.textContentColor
                        ),
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier
                            .padding(
                                end = 20.dp,
                                top = 10.dp,
                                bottom = 30.dp
                            )
                            .align(Alignment.End),
                        onClick = {
                            shouldDialogBoxAppear.value = false
                        }) {
                        Text(
                            text = "Cancel",
                            color = AlertDialogDefaults.textContentColor,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}