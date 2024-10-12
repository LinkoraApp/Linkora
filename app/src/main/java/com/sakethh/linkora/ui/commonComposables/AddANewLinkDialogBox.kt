package com.sakethh.linkora.ui.commonComposables

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.LocalizedStrings
import com.sakethh.linkora.data.RequestResult
import com.sakethh.linkora.data.local.site_specific_user_agent.SiteSpecificUserAgentRepo
import com.sakethh.linkora.ui.commonComposables.viewmodels.commonBtmSheets.AddANewLinkDialogBoxVM
import com.sakethh.linkora.ui.commonComposables.viewmodels.commonBtmSheets.ShelfBtmSheetVM
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.screens.collections.specific.SpecificScreenType
import com.sakethh.linkora.ui.screens.settings.SettingsPreference
import com.sakethh.linkora.ui.theme.LinkoraTheme
import com.sakethh.linkora.utils.isAValidURL
import com.sakethh.linkora.utils.linkoraLog
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class
)
@Composable
fun AddANewLinkDialogBox(
    shouldDialogBoxAppear: MutableState<Boolean>,
    screenType: SpecificScreenType,
    onSaveClick: (isAutoDetectSelected: Boolean, webURL: String, title: String, note: String, selectedDefaultFolder: String?, selectedNonDefaultFolderID: Long?) -> Unit,
    isDataExtractingForTheLink: Boolean,
    onFolderCreateClick: (folderName: String, folderNote: String) -> Unit,
) {
    val addANewLinkDialogBoxVM: AddANewLinkDialogBoxVM = hiltViewModel()
    val parentFoldersData = addANewLinkDialogBoxVM.foldersRepo.getAllRootFolders().collectAsState(
        initial = emptyList()
    )
    val isDropDownMenuIconClicked = rememberSaveable {
        mutableStateOf(false)
    }
    val isAutoDetectTitleEnabled = rememberSaveable {
        mutableStateOf(SettingsPreference.isAutoDetectTitleForLinksEnabled.value)
    }
    val isCreateANewFolderIconClicked = rememberSaveable {
        mutableStateOf(false)
    }
    if (isDataExtractingForTheLink) {
        isDropDownMenuIconClicked.value = false
    }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as Activity
    val intent = activity.intent
    val intentData = rememberSaveable(inputs = arrayOf(intent)) {
        mutableStateOf(intent)
    }
    val isChildFoldersBottomSheetExpanded = mutableStateOf(false)
    val btmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(key1 = Unit) {
        AddANewLinkDialogBox.currentUserAgent.value = SettingsPreference.primaryJsoupUserAgent.value
        awaitAll(async {
            if (screenType == SpecificScreenType.INTENT_ACTIVITY) {
                this.launch {
                    SettingsPreference.readAllPreferencesValues(context)
                }
            }
        })
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    if (shouldDialogBoxAppear.value) {
        val linkTextFieldValue = if (screenType == SpecificScreenType.INTENT_ACTIVITY) {
            rememberSaveable(
                inputs = arrayOf(
                    intentData.value?.getStringExtra(
                        Intent.EXTRA_TEXT
                    ).toString()
                )
            ) {
                mutableStateOf(intentData.value?.getStringExtra(Intent.EXTRA_TEXT).toString())
            }
        } else {
            rememberSaveable {
                mutableStateOf("")
            }
        }
        LaunchedEffect(Unit) {
            lifecycleOwner.lifecycle.currentStateFlow.collectLatest {
                when (it) {
                    Lifecycle.State.DESTROYED -> {}
                    Lifecycle.State.INITIALIZED -> {

                    }

                    Lifecycle.State.CREATED -> {}
                    Lifecycle.State.STARTED -> {}
                    Lifecycle.State.RESUMED -> {
                        linkoraLog(it.name)
                        AddANewLinkDialogBox.updateUserAgent(
                            linkTextFieldValue.value,
                            context
                        )
                    }
                }
            }
        }
        val titleTextFieldValue = rememberSaveable {
            mutableStateOf("")
        }
        val noteTextFieldValue = rememberSaveable {
            mutableStateOf("")
        }
        val selectedFolderName = rememberSaveable(LocalizedStrings.savedLinks.value) {
            mutableStateOf(LocalizedStrings.savedLinks.value)
        }
        val selectedFolderID = rememberSaveable {
            mutableLongStateOf(-1)
        }
        val childFolders =
            addANewLinkDialogBoxVM.childFolders.collectAsStateWithLifecycle()
        LinkoraTheme {
            BasicAlertDialog(
                onDismissRequest = {
                    if (!isDataExtractingForTheLink) {
                        shouldDialogBoxAppear.value = false
                    }
                }, modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AlertDialogDefaults.containerColor),
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .animateContentSize()
                            .fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        item {
                            Text(
                                text =
                                when (screenType) {
                                    SpecificScreenType.IMPORTANT_LINKS_SCREEN -> LocalizedStrings.addANewLinkInImportantLinks.value
                                    SpecificScreenType.SAVED_LINKS_SCREEN -> LocalizedStrings.addANewLinkInSavedLinks.value
                                    SpecificScreenType.SPECIFIC_FOLDER_LINKS_SCREEN -> LocalizedStrings.addANewLinkIn.value
                                    else -> LocalizedStrings.addANewLink.value
                                } + if (screenType == SpecificScreenType.SPECIFIC_FOLDER_LINKS_SCREEN) " \"${CollectionsScreenVM.currentClickedFolderData.value.folderName}\"" else "",
                                color = AlertDialogDefaults.titleContentColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 22.sp,
                                modifier = Modifier.padding(
                                    start = 20.dp,
                                    top = 30.dp,
                                    end = 20.dp
                                ),
                                lineHeight = 28.sp
                            )
                        }
                        item {
                            OutlinedTextField(readOnly = isDataExtractingForTheLink,
                                modifier = Modifier
                                    .padding(
                                        start = 20.dp, end = 20.dp, top = 20.dp
                                    )
                                    .fillMaxWidth(),
                                label = {
                                    Text(
                                        text = LocalizedStrings.linkAddress.value,
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
                                    AddANewLinkDialogBox.updateUserAgent(
                                        linkTextFieldValue.value,
                                        context
                                    )
                                })
                        }
                        item {
                            Box(modifier = Modifier.animateContentSize()) {
                                if (!SettingsPreference.isAutoDetectTitleForLinksEnabled.value && !isAutoDetectTitleEnabled.value) {
                                    OutlinedTextField(readOnly = isDataExtractingForTheLink,
                                        modifier = Modifier
                                            .padding(
                                                start = 20.dp, end = 20.dp, top = 15.dp
                                            )
                                            .fillMaxWidth(),
                                        label = {
                                            Text(
                                                text = LocalizedStrings.titleForTheLink.value,
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
                            }
                        }

                        item {
                            OutlinedTextField(readOnly = isDataExtractingForTheLink,
                                modifier = Modifier
                                    .padding(
                                        start = 20.dp, end = 20.dp, top = 15.dp
                                    )
                                    .fillMaxWidth(),
                                label = {
                                    Text(
                                        text = LocalizedStrings.noteForSavingTheLink.value,
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
                        }
                        item {
                            if (SettingsPreference.isAutoDetectTitleForLinksEnabled.value) {
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
                                            Icon(
                                                imageVector = Icons.Outlined.Info,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .padding(
                                                        start = 10.dp, end = 10.dp
                                                    )
                                            )
                                        }
                                        Text(
                                            text = LocalizedStrings.titleWillBeAutomaticallyDetected.value,
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
                        }
                        item {
                            if (screenType == SpecificScreenType.ROOT_SCREEN || screenType == SpecificScreenType.INTENT_ACTIVITY) {
                                Text(
                                    text = LocalizedStrings.addIn.value,
                                    color = contentColorFor(backgroundColor = AlertDialogDefaults.containerColor),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 18.sp,
                                    modifier = Modifier
                                        .padding(start = 20.dp, top = 20.dp, end = 20.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                                ) {
                                    FilledTonalButton(modifier = Modifier
                                        .pulsateEffect()
                                        .fillMaxWidth(0.8f),
                                        onClick = {
                                            if (!isDataExtractingForTheLink) {
                                                isDropDownMenuIconClicked.value =
                                                    !isDropDownMenuIconClicked.value
                                                addANewLinkDialogBoxVM.subFoldersList.clear()
                                            }
                                        }) {
                                        Text(
                                            text = selectedFolderName.value,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontSize = 18.sp,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(5.dp))
                                    FilledTonalIconButton(
                                        modifier = Modifier.pulsateEffect(
                                            0.75f
                                        ), onClick = {
                                            if (!isDataExtractingForTheLink) {
                                                isDropDownMenuIconClicked.value =
                                                    !isDropDownMenuIconClicked.value
                                                addANewLinkDialogBoxVM.subFoldersList.clear()
                                            }
                                        }) {
                                        Icon(
                                            imageVector = if (isDropDownMenuIconClicked.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }

                        if (isDropDownMenuIconClicked.value) {
                            item {
                                SelectableFolderUIComponent(
                                    onClick = {
                                        isDropDownMenuIconClicked.value = false
                                        selectedFolderName.value = LocalizedStrings.savedLinks.value
                                        selectedFolderID.longValue = -1
                                    },
                                    folderName = LocalizedStrings.savedLinks.value,
                                    imageVector = Icons.Outlined.Link,
                                    isComponentSelected = selectedFolderID.longValue == (-1).toLong()
                                )
                            }
                            item {
                                SelectableFolderUIComponent(
                                    onClick = {
                                        selectedFolderName.value = LocalizedStrings.importantLinks.value
                                        isDropDownMenuIconClicked.value = false
                                        selectedFolderID.longValue = -2
                                    },
                                    folderName = LocalizedStrings.importantLinks.value,
                                    imageVector = Icons.Outlined.StarOutline,
                                    isComponentSelected = selectedFolderID.longValue == (-2).toLong()
                                )
                            }
                            items(parentFoldersData.value) {
                                FolderSelectorComponent(
                                    onItemClick = {
                                        selectedFolderName.value = it.folderName
                                        selectedFolderID.longValue = it.id
                                        isDropDownMenuIconClicked.value = false
                                    },
                                    isCurrentFolderSelected = mutableStateOf(it.id == selectedFolderID.longValue),
                                    folderName = it.folderName,
                                    onSubDirectoryIconClick = {
                                        addANewLinkDialogBoxVM.changeParentFolderId(it.id)
                                        addANewLinkDialogBoxVM.subFoldersList.add(it)
                                        isChildFoldersBottomSheetExpanded.value = true
                                        coroutineScope.launch {
                                            btmSheetState.expand()
                                        }
                                        selectedFolderName.value = it.folderName
                                        selectedFolderID.longValue = it.id
                                    }
                                )
                            }
                            if (!isDropDownMenuIconClicked.value) {
                                item {
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                            }
                        }
                        item {
                            if (!SettingsPreference.isAutoDetectTitleForLinksEnabled.value) {
                                Row(
                                    modifier = Modifier
                                        .padding(top = 20.dp)
                                        .fillMaxWidth()
                                        .clickable {
                                            if (!isDataExtractingForTheLink) {
                                                isAutoDetectTitleEnabled.value =
                                                    !isAutoDetectTitleEnabled.value
                                            }
                                        }
                                        .padding(
                                            start = 10.dp, end = 20.dp
                                        ), verticalAlignment = Alignment.CenterVertically
                                ) {
                                    androidx.compose.material3.Checkbox(enabled = !isDataExtractingForTheLink,
                                        checked = isAutoDetectTitleEnabled.value,
                                        onCheckedChange = {
                                            isAutoDetectTitleEnabled.value = it
                                        })
                                    Text(
                                        text = LocalizedStrings.forceAutoDetectTitle.value,
                                        color = contentColorFor(backgroundColor = AlertDialogDefaults.containerColor),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                        item {
                            if (!isDataExtractingForTheLink) {
                                OutlinedButton(colors = ButtonDefaults.outlinedButtonColors(),
                                    border = BorderStroke(
                                        width = 1.dp, color = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier
                                        .padding(
                                            end = 20.dp, top = 10.dp, start = 20.dp
                                        )
                                        .fillMaxWidth()
                                        .pulsateEffect(),
                                    onClick = {
                                        shouldDialogBoxAppear.value = false
                                    }) {
                                    Text(
                                        text = LocalizedStrings.cancel.value,
                                        color = MaterialTheme.colorScheme.secondary,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontSize = 16.sp
                                    )
                                }
                                Button(
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier
                                        .padding(
                                            end = 20.dp,
                                            top = 10.dp,
                                            start = 20.dp
                                        )
                                        .fillMaxWidth()
                                        .pulsateEffect(),
                                    onClick = {
                                        RequestResult.isThisFirstRequest = true
                                        onSaveClick(
                                            isAutoDetectTitleEnabled.value,
                                            linkTextFieldValue.value,
                                            titleTextFieldValue.value,
                                            noteTextFieldValue.value,
                                            selectedFolderName.value,
                                            selectedFolderID.longValue
                                        )
                                    }) {
                                    Text(
                                        text = LocalizedStrings.save.value,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontSize = 16.sp
                                    )
                                }
                                if (isAValidURL(linkTextFieldValue.value)) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(20.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(0.25f)
                                    )
                                    Card(
                                        border = BorderStroke(
                                            1.dp,
                                            contentColorFor(MaterialTheme.colorScheme.surface)
                                        ),
                                        colors = CardDefaults.cardColors(containerColor = AlertDialogDefaults.containerColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, end = 20.dp)
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
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .padding(
                                                            start = 10.dp, end = 10.dp
                                                        )
                                                )
                                            }
                                            Text(
                                                text = buildAnnotatedString {
                                                    append("User agent ")
                                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                                        append(AddANewLinkDialogBox.currentUserAgent.value)
                                                    }
                                                    append(" will be used to retrieve metadata.")
                                                },
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
                            } else {
                                Spacer(modifier = Modifier.height(30.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 20.dp, end = 20.dp)
                                )
                                if (RequestResult.isThisFirstRequest.not()) {
                                    Spacer(modifier = Modifier.height(15.dp))
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
                                                Icon(
                                                    imageVector = Icons.Outlined.Warning,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .padding(
                                                            start = 10.dp, end = 10.dp
                                                        )
                                                )
                                            }
                                            Text(
                                                text = "Initial request has been failed",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp,
                                                textAlign = TextAlign.Start,
                                                modifier = Modifier
                                                    .padding(end = 10.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Card(
                                        border = BorderStroke(
                                            1.dp,
                                            contentColorFor(MaterialTheme.colorScheme.surface)
                                        ),
                                        colors = CardDefaults.cardColors(containerColor = AlertDialogDefaults.containerColor),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, end = 20.dp)
                                    ) {
                                        Text(
                                            modifier = Modifier
                                                .padding(
                                                    end = 10.dp,
                                                    top = 10.dp,
                                                    start = 10.dp,
                                                    bottom = 10.dp
                                                )
                                                .fillMaxWidth(),
                                            text = buildAnnotatedString {
                                                appendInlineContent(id = "infoIcon")
                                                append("  Retrying metadata retrieval with secondary user agent:\n")
                                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                                    append(SettingsPreference.secondaryJsoupUserAgent.value)
                                                }
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            inlineContent = mapOf("infoIcon" to InlineTextContent(
                                                Placeholder(
                                                    20.sp,
                                                    20.sp,
                                                    PlaceholderVerticalAlign.TextCenter
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Info,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentDescription = ""
                                                )
                                            })
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(20.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(0.25f)
                            )
                        }
                    }
                }
                if (isChildFoldersBottomSheetExpanded.value) {
                    ModalBottomSheet(sheetState = btmSheetState, onDismissRequest = {
                        addANewLinkDialogBoxVM.subFoldersList.clear()
                        isChildFoldersBottomSheetExpanded.value = false
                    }) {
                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            stickyHeader {
                                Column {
                                    TopAppBar(title = {
                                        Text(
                                            text = addANewLinkDialogBoxVM.subFoldersList.last().folderName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontSize = 24.sp
                                        )
                                    })
                                    LazyRow(
                                        modifier = Modifier.padding(
                                            start = 15.dp,
                                            end = 15.dp,
                                            bottom = 15.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        item {
                                            Text(
                                                text = "/",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontSize = 16.sp
                                            )
                                        }
                                        item {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                                contentDescription = ""
                                            )
                                        }
                                        itemsIndexed(addANewLinkDialogBoxVM.subFoldersList) { index, subFolder ->
                                            Text(
                                                text = subFolder.folderName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontSize = 16.sp,
                                                modifier = Modifier.clickable {
                                                    if (addANewLinkDialogBoxVM.subFoldersList.indexOf(
                                                            subFolder
                                                        ) != addANewLinkDialogBoxVM.subFoldersList.lastIndex
                                                    ) {
                                                        addANewLinkDialogBoxVM.subFoldersList.subList(
                                                            index + 1,
                                                            addANewLinkDialogBoxVM.subFoldersList.lastIndex + 1
                                                        ).clear()
                                                        addANewLinkDialogBoxVM.changeParentFolderId(
                                                            subFolder.id
                                                        )
                                                    }
                                                }
                                            )
                                            if (subFolder.id != addANewLinkDialogBoxVM.subFoldersList.last().id) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                                    contentDescription = ""
                                                )
                                                Text(
                                                    text = ShelfBtmSheetVM.selectedShelfData.shelfName,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontSize = 16.sp
                                                )
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = LocalContentColor.current.copy(0.25f))
                                    Spacer(modifier = Modifier.height(15.dp))
                                }
                            }
                            if (childFolders.value.isNotEmpty()) {
                                items(childFolders.value) {
                                    FolderSelectorComponent(
                                        onItemClick = {
                                            selectedFolderName.value = it.folderName
                                            selectedFolderID.longValue = it.id
                                            isDropDownMenuIconClicked.value = false
                                            addANewLinkDialogBoxVM.subFoldersList.clear()
                                            coroutineScope.launch {
                                                btmSheetState.hide()
                                            }
                                            isChildFoldersBottomSheetExpanded.value = false
                                        },
                                        isCurrentFolderSelected = mutableStateOf(it.id == selectedFolderID.longValue),
                                        folderName = it.folderName,
                                        onSubDirectoryIconClick = {
                                            selectedFolderName.value = it.folderName
                                            addANewLinkDialogBoxVM.subFoldersList.add(it)
                                            addANewLinkDialogBoxVM.changeParentFolderId(it.id)
                                            selectedFolderID.longValue = it.id
                                        }
                                    )
                                }
                            } else {
                                item {
                                    Text(
                                        text = LocalizedStrings.thisFolderHasNoSubfolders.value,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 24.sp,
                                        lineHeight = 36.sp,
                                        textAlign = TextAlign.Start,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(15.dp)
                                    )
                                }
                                item {
                                    Button(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(15.dp),
                                        onClick = {
                                            isDropDownMenuIconClicked.value = false
                                            addANewLinkDialogBoxVM.subFoldersList.clear()
                                            coroutineScope.launch {
                                                btmSheetState.hide()
                                            }
                                            isChildFoldersBottomSheetExpanded.value = false
                                        }) {
                                        Text(
                                            text = LocalizedStrings.saveInThisFolder.value,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }
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
                        selectedFolderID.longValue = folderID
                    },
                    onCreated = {
                        isDropDownMenuIconClicked.value = false
                    },
                    inAChildFolderScreen = screenType == SpecificScreenType.SPECIFIC_FOLDER_LINKS_SCREEN,
                    onFolderCreateClick = { folderName, folderNote ->
                        onFolderCreateClick(folderName, folderNote)
                    }
                )
            )
        }
    }
}

@Composable
private fun FolderSelectorComponent(
    onItemClick: () -> Unit,
    isCurrentFolderSelected: MutableState<Boolean>,
    folderName: String,
    onSubDirectoryIconClick: () -> Unit
) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable {
            onItemClick()
        }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                tint = if (isCurrentFolderSelected.value) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        top = 0.dp
                    )
                    .size(28.dp)
            )
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrentFolderSelected.value) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isCurrentFolderSelected.value) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                    }
                    IconButton(onClick = {
                        onSubDirectoryIconClick()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.SubdirectoryArrowRight,
                            contentDescription = null
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
        }
        Text(
            text = folderName,
            color = if (isCurrentFolderSelected.value) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            maxLines = 1, modifier = Modifier
                .padding(
                    start = 20.dp, end = 20.dp
                ),
            overflow = TextOverflow.Ellipsis
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(0.1f)
        )
    }
}

// not updating both AddANewLinkDialogBoxVM, loll; this will either be removed or just hang out here as an object

object AddANewLinkDialogBox : ViewModel() {

    val currentUserAgent = mutableStateOf(SettingsPreference.primaryJsoupUserAgent.value)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SiteSpecificUserAgentEntryPoint {
        fun siteSpecificUserAgentRepo(): SiteSpecificUserAgentRepo
    }


    fun updateUserAgent(linkAddress: String, context: Context) {
        val siteSpecificUserAgentRepo =
            EntryPoints.get(context.applicationContext, SiteSpecificUserAgentEntryPoint::class.java)
                .siteSpecificUserAgentRepo()
        viewModelScope.launch {
            if (isAValidURL(linkAddress)) {
                val domain = linkAddress.split("/")[2].replace("www.", "").replace("http://", "")
                    .replace("https://", "")
                currentUserAgent.value =
                    if (siteSpecificUserAgentRepo.doesDomainExistPartially(domain)) {
                        siteSpecificUserAgentRepo.getUserAgentByPartialDomain(domain)
                    } else {
                        SettingsPreference.primaryJsoupUserAgent.value
                    }
            }
        }
    }
}