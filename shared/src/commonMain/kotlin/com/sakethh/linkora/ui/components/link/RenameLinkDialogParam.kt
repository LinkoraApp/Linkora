package com.sakethh.linkora.ui.components.link

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.LastSeenId
import com.sakethh.linkora.ui.LastSeenString
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.tag.TagSelectionComponent
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import com.sakethh.linkora.utils.highlightOnFocused

@Stable
data class RenameLinkDialogParam
@OptIn(ExperimentalMaterial3Api::class)
constructor(
    val showDialogBox: Boolean,
    val sheetState: SheetState,
    val onHide: () -> Unit,
    val onSave: (
        newTitle: String,
        newNote: String,
        newImageUrl: String,
        newUrl: String,
        selectedTags: List<Tag>,
        onCompletion: () -> Unit,
    ) -> Unit,
    val existingTitle: String,
    val existingNote: String,
    val existingImageUrl: String,
    val existingUrl: String,
    val allTags: State<PaginationState<Map<Pair<LastSeenId, LastSeenString>, List<Tag>>>>,
    val selectedTags: List<Tag>,
    val onRetrieveNextTagsPage: () -> Unit,
    val onFirstVisibleIndexChange: (Long) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameLinkDialog(renameLinkDialogParam: RenameLinkDialogParam) {
    val localizedStrings = LocalizedStrings.current
    val coroutineScope = rememberCoroutineScope()

    if (renameLinkDialogParam.showDialogBox) {
        var selectedTags by rememberDeserializableMutableObject {
            mutableStateOf(renameLinkDialogParam.selectedTags)
        }
        var newTitle by rememberSaveable(renameLinkDialogParam.existingTitle) {
            mutableStateOf(renameLinkDialogParam.existingTitle)
        }
        var newNote by rememberSaveable(renameLinkDialogParam.existingNote) {
            mutableStateOf(renameLinkDialogParam.existingNote)
        }
        var newImageURL by rememberSaveable(renameLinkDialogParam.existingImageUrl) {
            mutableStateOf(renameLinkDialogParam.existingImageUrl)
        }
        var newUrl by rememberSaveable(renameLinkDialogParam.existingUrl) {
            mutableStateOf(renameLinkDialogParam.existingUrl)
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
                        text = localizedStrings.ChangeLinkData,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth(if (!Platform.Android.onMobile()) 0.85f else 1f),
                    )
                    if (!Platform.Android.onMobile() && !showProgressBar) {
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                            onClick = renameLinkDialogParam.onHide,
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    label = {
                        Text(
                            text = localizedStrings.NewTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = newTitle,
                    onValueChange = { newTitle = it },
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
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    label = {
                        Text(
                            text = localizedStrings.NewImgURLLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = newImageURL,
                    onValueChange = { newImageURL = it },
                    modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp),
                    readOnly = showProgressBar,
                )
                Spacer(modifier = Modifier.height(5.dp))
                OutlinedTextField(
                    label = {
                        Text(
                            text = localizedStrings.NewURLLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = newUrl,
                    onValueChange = { newUrl = it },
                    modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp),
                    readOnly = showProgressBar,
                )

                if (!showProgressBar) {
                    Text(
                        text = localizedStrings.AttachTags,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 15.dp, top = 15.dp, bottom = 10.dp),
                    )
                    TagSelectionComponent(
                        showCreateNewTagSheet = { coroutineScope.pushUIEvent(UIEvent.Type.ShowCreateTagBtmSheet) },
                        paddingValues = PaddingValues(start = 15.dp, end = 15.dp),
                        allTags = renameLinkDialogParam.allTags.value,
                        selectedTags = selectedTags,
                        onTagClick = { currTag ->
                            if (selectedTags.contains(currTag)) {
                                selectedTags = selectedTags.filterNot { currTag == it }
                            } else {
                                selectedTags += currTag
                            }
                        },
                        onRetrieveNextTagsPage = renameLinkDialogParam.onRetrieveNextTagsPage,
                        onFirstVisibleIndexChange = {
                            renameLinkDialogParam.onFirstVisibleIndexChange(
                                it.toLong(),
                            )
                        },
                    )
                }

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
                        renameLinkDialogParam.onSave(
                            newTitle,
                            newNote,
                            newImageURL,
                            newUrl,
                            selectedTags,
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
                    onClick = renameLinkDialogParam.onHide,
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
                sheetState = renameLinkDialogParam.sheetState,
                modifier = Modifier.imePadding(),
                properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
                onDismissRequest = { if (!showProgressBar) renameLinkDialogParam.onHide() },
            ) { content() }
        } else {
            BasicAlertDialog(
                modifier =
                    Modifier
                        .wrapContentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AlertDialogDefaults.containerColor),
                properties = DialogProperties(usePlatformDefaultWidth = false),
                onDismissRequest = { if (!showProgressBar) renameLinkDialogParam.onHide() },
            ) { content() }
        }
    }
}
