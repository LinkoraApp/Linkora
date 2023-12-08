package com.sakethh.linkora.customComposables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakethh.linkora.btmSheet.SelectableFolderUIComponent
import com.sakethh.linkora.localDB.LocalDataBase
import com.sakethh.linkora.localDB.commonVMs.CreateVM
import com.sakethh.linkora.localDB.commonVMs.UpdateVM
import com.sakethh.linkora.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.screens.collections.specificCollectionScreen.SpecificScreenType
import com.sakethh.linkora.screens.settings.SettingsScreenVM
import com.sakethh.linkora.ui.theme.LinkoraTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNewLinkDialogBox(
    shouldDialogBoxAppear: MutableState<Boolean>,
    screenType: SpecificScreenType,
    onSaveClick: () -> Unit,
    parentFolderID: Long?
) {
    val isDataExtractingForTheLink = rememberSaveable {
        mutableStateOf(false)
    }
    val foldersTableData =
        LocalDataBase.localDB.readDao().getAllRootFolders().collectAsState(
            initial = emptyList()
        ).value
    val isDropDownMenuIconClicked = rememberSaveable {
        mutableStateOf(false)
    }
    val isAutoDetectTitleEnabled = rememberSaveable {
        mutableStateOf(SettingsScreenVM.Settings.isAutoDetectTitleForLinksEnabled.value)
    }
    val isCreateANewFolderIconClicked = rememberSaveable {
        mutableStateOf(false)
    }
    val btmModalSheetState = androidx.compose.material3.rememberModalBottomSheetState()
    if (isDataExtractingForTheLink.value) {
        isDropDownMenuIconClicked.value = false
    }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    if (shouldDialogBoxAppear.value) {
        val linkTextFieldValue = rememberSaveable {
            mutableStateOf("")
        }
        val titleTextFieldValue = rememberSaveable {
            mutableStateOf("")
        }
        val noteTextFieldValue = rememberSaveable {
            mutableStateOf("")
        }
        val selectedFolderName = rememberSaveable {
            mutableStateOf("Saved Links")
        }
        LinkoraTheme {
            AlertDialog(modifier = Modifier
                .wrapContentHeight()
                .animateContentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(AlertDialogDefaults.containerColor),
                onDismissRequest = {
                    if (!isDataExtractingForTheLink.value) {
                        shouldDialogBoxAppear.value = false
                    }
                }) {
                Column(modifier = Modifier.verticalScroll(scrollState)) {
                    Text(
                        text = "Save new link",
                        color = AlertDialogDefaults.titleContentColor,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 22.sp,
                        modifier = Modifier.padding(start = 20.dp, top = 30.dp)
                    )
                    OutlinedTextField(readOnly = isDataExtractingForTheLink.value,
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp, top = 20.dp
                        ),
                        label = {
                            Text(
                                text = "Link",
                                color = AlertDialogDefaults.textContentColor,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 12.sp
                            )
                        },
                        textStyle = MaterialTheme.typography.titleSmall,
                        singleLine = true,
                        shape = RoundedCornerShape(5.dp),
                        value = linkTextFieldValue.value,
                        onValueChange = {
                            linkTextFieldValue.value = it
                        })
                    if (!SettingsScreenVM.Settings.isAutoDetectTitleForLinksEnabled.value && !isAutoDetectTitleEnabled.value) {
                        OutlinedTextField(readOnly = isDataExtractingForTheLink.value,
                            modifier = Modifier.padding(
                                start = 20.dp, end = 20.dp, top = 15.dp
                            ),
                            label = {
                                Text(
                                    text = "Title for the link",
                                    color = AlertDialogDefaults.textContentColor,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 12.sp
                                )
                            },
                            textStyle = MaterialTheme.typography.titleSmall,
                            singleLine = true,
                            value = titleTextFieldValue.value,
                            onValueChange = {
                                titleTextFieldValue.value = it
                            })
                    }
                    OutlinedTextField(readOnly = isDataExtractingForTheLink.value,
                        modifier = Modifier.padding(
                            start = 20.dp, end = 20.dp, top = 15.dp
                        ),
                        label = {
                            Text(
                                text = "Note for why you're saving this link",
                                color = AlertDialogDefaults.textContentColor,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 12.sp
                            )
                        },
                        textStyle = MaterialTheme.typography.titleSmall,
                        singleLine = true,
                        value = noteTextFieldValue.value,
                        onValueChange = {
                            noteTextFieldValue.value = it
                        })
                    if (SettingsScreenVM.Settings.isAutoDetectTitleForLinksEnabled.value) {
                        Card(
                            border = BorderStroke(
                                1.dp,
                                contentColorFor(MaterialTheme.colorScheme.surface)
                            ),
                            colors = CardDefaults.cardColors(containerColor = AlertDialogDefaults.containerColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 15.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(
                                        top = 10.dp, bottom = 10.dp
                                    ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(
                                                start = 10.dp, end = 10.dp
                                            )
                                    )
                                }
                                Text(
                                    text = "Title will be automatically detected as this setting is enabled.",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                )
                            }
                        }
                    }
                    if (screenType == SpecificScreenType.ROOT_SCREEN) {
                        Row(
                            Modifier.padding(
                                start = 20.dp, end = 20.dp, top = 20.dp
                            ), horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Save in",
                                color = LocalContentColor.current,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(top = 15.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(border = BorderStroke(
                                width = 1.dp, color = MaterialTheme.colorScheme.primary
                            ), onClick = {
                                if (!isDataExtractingForTheLink.value) {
                                    isDropDownMenuIconClicked.value = true
                                }
                            }) {
                                Text(
                                    text = selectedFolderName.value,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 18.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth(0.80f)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Icon(tint = MaterialTheme.colorScheme.primary,
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable {
                                        if (!isDataExtractingForTheLink.value) {
                                            isDropDownMenuIconClicked.value = true
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                        }
                    }
                    if (!SettingsScreenVM.Settings.isAutoDetectTitleForLinksEnabled.value) {
                        Row(
                            modifier = Modifier
                                .padding(top = 20.dp)
                                .fillMaxWidth()
                                .clickable {
                                    if (!isDataExtractingForTheLink.value) {
                                        isAutoDetectTitleEnabled.value =
                                            !isAutoDetectTitleEnabled.value
                                    }
                                }
                                .padding(
                                    start = 10.dp, end = 20.dp
                                ), verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.Checkbox(enabled = !isDataExtractingForTheLink.value,
                                checked = isAutoDetectTitleEnabled.value,
                                onCheckedChange = {
                                    isAutoDetectTitleEnabled.value = it
                                })
                            Text(
                                text = "Force Auto-detect title",
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 16.sp
                            )
                        }
                    }
                    if (!isDataExtractingForTheLink.value) {
                        Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .padding(
                                    end = 20.dp,
                                    top = 20.dp,
                                    start = 20.dp
                                )
                                .fillMaxWidth()
                                .align(Alignment.End),
                            onClick = {
                                onSaveClick()
                            }) {
                            Text(
                                text = "Save",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 16.sp
                            )
                        }
                        OutlinedButton(colors = ButtonDefaults.outlinedButtonColors(),
                            border = BorderStroke(
                                width = 1.dp, color = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier
                                .padding(
                                    end = 20.dp, top = 10.dp, bottom = 30.dp, start = 20.dp
                                )
                                .fillMaxWidth()
                                .align(Alignment.End),
                            onClick = {
                                shouldDialogBoxAppear.value = false
                            }) {
                            Text(
                                text = "Cancel",
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(30.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                if (isDropDownMenuIconClicked.value) {
                    ModalBottomSheet(sheetState = btmModalSheetState, onDismissRequest = {
                        coroutineScope.launch {
                            if (btmModalSheetState.isVisible) {
                                btmModalSheetState.hide()
                            }
                        }.invokeOnCompletion {
                            isDropDownMenuIconClicked.value = false
                        }
                    }) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Save in:",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 24.sp,
                                        modifier = Modifier.padding(
                                            start = 20.dp
                                        )
                                    )
                                    Icon(imageVector = Icons.Outlined.CreateNewFolder,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .clickable {
                                                isCreateANewFolderIconClicked.value = true
                                            }
                                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                                            .size(30.dp),
                                        tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            item {
                                Divider(
                                    modifier = Modifier.padding(
                                        start = 20.dp, end = 65.dp
                                    ), color = MaterialTheme.colorScheme.outline.copy(0.25f)
                                )
                            }
                            item {
                                SelectableFolderUIComponent(
                                    onClick = {
                                        selectedFolderName.value = "Saved Links"
                                        coroutineScope.launch {
                                            if (btmModalSheetState.isVisible) {
                                                btmModalSheetState.hide()
                                            }
                                        }.invokeOnCompletion {
                                            coroutineScope.launch {
                                                if (btmModalSheetState.isVisible) {
                                                    btmModalSheetState.hide()
                                                }
                                            }.invokeOnCompletion {
                                                isDropDownMenuIconClicked.value = false
                                            }
                                        }
                                    },
                                    folderName = "Saved Links",
                                    imageVector = Icons.Outlined.Link,
                                    _isComponentSelected = selectedFolderName.value == "Saved Links"
                                )
                            }
                            item {
                                SelectableFolderUIComponent(
                                    onClick = {
                                        selectedFolderName.value = "Important Links"
                                        coroutineScope.launch {
                                            if (btmModalSheetState.isVisible) {
                                                btmModalSheetState.hide()
                                            }
                                        }.invokeOnCompletion {
                                            coroutineScope.launch {
                                                if (btmModalSheetState.isVisible) {
                                                    btmModalSheetState.hide()
                                                }
                                            }.invokeOnCompletion {
                                                isDropDownMenuIconClicked.value = false
                                            }
                                        }
                                    },
                                    folderName = "Important Links",
                                    imageVector = Icons.Outlined.StarOutline,
                                    _isComponentSelected = selectedFolderName.value == "Important Links"
                                )
                            }
                            items(foldersTableData) {
                                SelectableFolderUIComponent(
                                    onClick = {
                                        selectedFolderName.value = it.folderName
                                        CollectionsScreenVM.selectedFolderData.value.id = it.id
                                        coroutineScope.launch {
                                            if (btmModalSheetState.isVisible) {
                                                btmModalSheetState.hide()
                                            }
                                        }.invokeOnCompletion {
                                            isDropDownMenuIconClicked.value = false
                                        }
                                    },
                                    folderName = it.folderName,
                                    imageVector = Icons.Outlined.Folder,
                                    _isComponentSelected = selectedFolderName.value == it.folderName
                                )
                            }
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
            AddNewFolderDialogBox(
                AddNewFolderDialogBoxParam(
                    shouldDialogBoxAppear = isCreateANewFolderIconClicked,
                    newFolderData = { folderName, folderID ->
                        selectedFolderName.value = folderName
                        CollectionsScreenVM.selectedFolderData.value.id = folderID
                    },
                    onCreated = {
                        coroutineScope.launch {
                            if (btmModalSheetState.isVisible) {
                                btmModalSheetState.hide()
                            }
                        }.invokeOnCompletion {
                            isDropDownMenuIconClicked.value = false
                        }
                    },
                    parentFolderID = parentFolderID,
                    currentFolderID = CollectionsScreenVM.selectedFolderData.value.id
                )
            )
        }
    }
}