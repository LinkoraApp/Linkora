package com.sakethh.linkora.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
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
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.PageKey
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.ui.components.menu.menuBtmSheetFolderEntries
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import com.sakethh.linkora.utils.rememberLocalizedString
import com.sakethh.linkora.utils.replaceFirstPlaceHolderWith

@Stable
data class RenameFolderOrLinkDialogParam @OptIn(ExperimentalMaterial3Api::class) constructor(
    val showDialogBox: Boolean,
    val sheetState: SheetState,
    val onHide: () -> Unit,
    val dialogBoxFor: MenuBtmSheetType = MenuBtmSheetType.Folder.RegularFolder,
    val onSave: (newTitle: String, newNote: String, newImageUrl: String, newUrl: String, selectedTags: List<Tag>, onCompletion: () -> Unit) -> Unit,
    val existingFolderName: String?,
    val existingTitle: String,
    val existingNote: String,
    val existingImageUrl: String,
    val existingUrl: String,
    val allTags: State<PaginationState<Map<PageKey, List<Tag>>>>,
    val selectedTags: List<Tag>,
    val onRetrieveNextTagsPage: () -> Unit,
    val onFirstVisibleIndexChange: (Long) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameFolderOrLinkDialog(
    renameFolderOrLinkDialogParam: RenameFolderOrLinkDialogParam
) {
    rememberCoroutineScope()
    if (renameFolderOrLinkDialogParam.showDialogBox) {
        var selectedTags by rememberDeserializableMutableObject {
            mutableStateOf(renameFolderOrLinkDialogParam.selectedTags)
        }
        var newFolderOrTitleName by rememberSaveable(renameFolderOrLinkDialogParam.existingTitle) {
            mutableStateOf(renameFolderOrLinkDialogParam.existingTitle)
        }
        var newNote by rememberSaveable(renameFolderOrLinkDialogParam.existingNote) {
            mutableStateOf(renameFolderOrLinkDialogParam.existingNote)
        }
        var newImageURL by rememberSaveable(renameFolderOrLinkDialogParam.existingImageUrl) {
            mutableStateOf(renameFolderOrLinkDialogParam.existingImageUrl)
        }
        var newUrl by rememberSaveable(renameFolderOrLinkDialogParam.existingUrl) {
            mutableStateOf(renameFolderOrLinkDialogParam.existingUrl)
        }
        var showProgressBar by rememberSaveable {
            mutableStateOf(false)
        }
        val content: ComposableContent = {
            Column(
                Modifier.wrapContentSize().animateContentSize().padding(
                    bottom = 15.dp,
                    top = if (platform() == Platform.Android.Mobile) 0.dp else 15.dp
                )
                    .fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(
                        start = 15.dp,
                        end = 15.dp
                    )
                ) {
                    Text(
                        text = if (menuBtmSheetFolderEntries().contains(
                                renameFolderOrLinkDialogParam.dialogBoxFor
                            ) && renameFolderOrLinkDialogParam.existingFolderName?.isNotBlank() == true
                        ) Localization.Key.RenameFolder.rememberLocalizedString()
                            .replaceFirstPlaceHolderWith(renameFolderOrLinkDialogParam.existingFolderName) else Localization.Key.ChangeLinkData.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(if (platform() != Platform.Android.Mobile) 0.85f else 1f)
                    )

                    if (platform() != Platform.Android.Mobile && !showProgressBar) {
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                            onClick = renameFolderOrLinkDialogParam.onHide
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close, contentDescription = null
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    label = {
                        Text(
                            text = if (menuBtmSheetFolderEntries().contains(
                                    renameFolderOrLinkDialogParam.dialogBoxFor
                                )
                            ) Localization.Key.NewName.rememberLocalizedString()
                            else Localization.Key.NewTitle.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = newFolderOrTitleName,
                    onValueChange = {
                        newFolderOrTitleName = it
                    },
                    modifier = Modifier.fillMaxWidth().padding(
                        start = 15.dp,
                        end = 15.dp
                    ),
                    readOnly = showProgressBar
                )
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    label = {
                        Text(
                            text = Localization.Key.NewNote.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = newNote,
                    onValueChange = {
                        newNote = it
                    },
                    modifier = Modifier.fillMaxWidth().padding(
                        start = 15.dp,
                        end = 15.dp
                    ),
                    readOnly = showProgressBar
                )
                if (renameFolderOrLinkDialogParam.dialogBoxFor is MenuBtmSheetType.Link) {
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        label = {
                            Text(
                                text = "New Image URL",
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 12.sp
                            )
                        },
                        textStyle = MaterialTheme.typography.titleSmall,
                        value = newImageURL,
                        onValueChange = {
                            newImageURL = it
                        },
                        modifier = Modifier.fillMaxWidth().padding(
                            start = 15.dp,
                            end = 15.dp
                        ),
                        readOnly = showProgressBar
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    OutlinedTextField(
                        label = {
                            Text(
                                text = "New URL",
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 12.sp
                            )
                        },
                        textStyle = MaterialTheme.typography.titleSmall,
                        value = newUrl,
                        onValueChange = {
                            newUrl = it
                        },
                        modifier = Modifier.fillMaxWidth().padding(
                            start = 15.dp,
                            end = 15.dp
                        ),
                        readOnly = showProgressBar
                    )
                }

                if (renameFolderOrLinkDialogParam.dialogBoxFor is MenuBtmSheetType.Link && !showProgressBar) {
                    Text(
                        text = Localization.Key.AttachTags.rememberLocalizedString(),
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 15.dp, top = 15.dp, bottom = 10.dp)
                    )
                    TagSelectionComponent(
                        paddingValues = PaddingValues(start = 15.dp, end = 15.dp),
                        allTags = renameFolderOrLinkDialogParam.allTags.value,
                        selectedTags = selectedTags,
                        onTagClick = { currTag ->
                            if (selectedTags.contains(currTag)) {
                                selectedTags = selectedTags.filterNot {
                                    currTag == it
                                }
                            } else {
                                selectedTags += currTag
                            }
                        },
                        onRetrieveNextTagsPage = renameFolderOrLinkDialogParam.onRetrieveNextTagsPage,
                        onFirstVisibleIndexChange = {
                            renameFolderOrLinkDialogParam.onFirstVisibleIndexChange(it.toLong())
                        })
                }

                if (showProgressBar) {

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(
                            start = 15.dp,
                            end = 15.dp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    return@Column
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    modifier = Modifier.padding(
                        start = 15.dp,
                        end = 15.dp
                    ).pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                        .pressScaleEffect(), onClick = {
                        showProgressBar = true
                        renameFolderOrLinkDialogParam.onSave(
                            newFolderOrTitleName, newNote, newImageURL, newUrl, selectedTags, {
                                showProgressBar = true
                            })
                    }) {
                    Text(
                        text = Localization.rememberLocalizedString(Localization.Key.Save),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                OutlinedButton(
                    modifier = Modifier.padding(
                        start = 15.dp,
                        end = 15.dp
                    ).pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                        .pressScaleEffect(),
                    onClick = renameFolderOrLinkDialogParam.onHide
                ) {
                    Text(
                        text = Localization.Key.Cancel.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }
            }
        }
        if (platform() == Platform.Android.Mobile) {
            ModalBottomSheet(
                sheetState = renameFolderOrLinkDialogParam.sheetState,
                modifier = Modifier.imePadding(),
                properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
                onDismissRequest = {
                    if (!showProgressBar) {
                        renameFolderOrLinkDialogParam.onHide()
                    }
                }) {
                content()
            }
        } else {
            BasicAlertDialog(
                modifier = Modifier.then(
                    if (platform() == Platform.Android.Mobile) Modifier.fillMaxSize() else Modifier.wrapContentSize()
                ).clip(RoundedCornerShape(10.dp)).background(AlertDialogDefaults.containerColor),
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = {
                    if (!showProgressBar) {
                        renameFolderOrLinkDialogParam.onHide()
                    }
                }) {
                content()
            }
        }
    }
}