package com.sakethh.linkora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.PageKey
import com.sakethh.linkora.ui.components.folder.SelectableFolderUIComponent
import com.sakethh.linkora.ui.domain.AddANewLinkDialogBoxAction
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.domain.model.AddNewFolderDialogBoxParam
import com.sakethh.linkora.ui.domain.model.AddNewLinkDialogParams
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.LoadingScreen
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.defaultFolderIds
import com.sakethh.linkora.utils.defaultImpLinksFolder
import com.sakethh.linkora.utils.defaultSavedLinksFolder
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import com.sakethh.linkora.utils.rememberLocalizedString
import com.sakethh.linkora.utils.replaceFirstPlaceHolderWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddANewLinkDialogBox(
    addNewLinkDialogParams: AddNewLinkDialogParams
) {
    val isDataExtractingForTheLink = rememberSaveable {
        mutableStateOf(false)
    }

    val isAutoDetectTitleEnabled = rememberSaveable {
        mutableStateOf(AppPreferences.isAutoDetectTitleForLinksEnabled.value)
    }
    val isForceSaveWithoutFetchingMetaDataEnabled = rememberSaveable {
        mutableStateOf(AppPreferences.forceSaveWithoutFetchingAnyMetaData.value)
    }
    val addTheFolderInRoot = rememberSaveable {
        mutableStateOf(false)
    }
    val isChildFoldersBottomSheetExpanded = rememberSaveable {
        mutableStateOf(false)
    }
    val btmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isDropDownMenuIconClicked = rememberSaveable {
        mutableStateOf(false)
    }
    LaunchedEffect(isDataExtractingForTheLink.value) {
        if (isDataExtractingForTheLink.value) {
            isDropDownMenuIconClicked.value = false
        }
    }
    val linkTextFieldValue = rememberSaveable {
        mutableStateOf(addNewLinkDialogParams.url)
    }
    val titleTextFieldValue = rememberSaveable {
        mutableStateOf("")
    }
    val noteTextFieldValue = rememberSaveable {
        mutableStateOf("")
    }
    val imgUrlTextFieldValue = rememberSaveable {
        mutableStateOf("")
    }

    val allTags = addNewLinkDialogParams.allTags.collectAsStateWithLifecycle().value

    val lazyRowState = rememberLazyListState()
    val content: ComposableContent = {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (platform() is Platform.Android.Mobile) BottomSheetDefaults.ContainerColor else MaterialTheme.colorScheme.surface
        ) {
            if (platform() == Platform.Android.Mobile) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                ) {
                    TopPartOfAddANewLinkDialogBox(
                        isDataExtractingForTheLink = isDataExtractingForTheLink.value,
                        linkTextFieldValue = linkTextFieldValue,
                        titleTextFieldValue = titleTextFieldValue,
                        noteTextFieldValue = noteTextFieldValue,
                        isAutoDetectTitleEnabled = isAutoDetectTitleEnabled,
                        isForceSaveWithoutFetchingMetaDataEnabled = isForceSaveWithoutFetchingMetaDataEnabled,
                        imgUrlTextFieldValue = imgUrlTextFieldValue,
                        currentFolder = addNewLinkDialogParams.currentFolder
                    )
                    BottomPartOfAddANewLinkDialogBox(
                        onDismiss = addNewLinkDialogParams.onDismiss,
                        isDataExtractingForTheLink = isDataExtractingForTheLink,
                        linkTextFieldValue = linkTextFieldValue,
                        titleTextFieldValue = titleTextFieldValue,
                        noteTextFieldValue = noteTextFieldValue,
                        isAutoDetectTitleEnabled = isAutoDetectTitleEnabled,
                        isForceSaveWithoutFetchingMetaDataEnabled = isForceSaveWithoutFetchingMetaDataEnabled,
                        isDropDownMenuIconClicked = isDropDownMenuIconClicked,
                        showChildFoldersBtmSheet = isChildFoldersBottomSheetExpanded,
                        childFoldersBtmSheetState = btmSheetState,
                        lazyRowState = lazyRowState,
                        addTheFolderInRoot = addTheFolderInRoot,
                        currentFolder = addNewLinkDialogParams.currentFolder,
                        allTags = allTags,
                        selectedTags = addNewLinkDialogParams.selectedTags,
                        foldersSearchQuery = addNewLinkDialogParams.foldersSearchQuery,
                        foldersSearchQueryResult = addNewLinkDialogParams.foldersSearchQueryResult,
                        performAction = addNewLinkDialogParams.performAction,
                        rootRegularFolders = addNewLinkDialogParams.rootRegularFolders,
                        imgUrlTextFieldValue = imgUrlTextFieldValue
                    )
                    Spacer(Modifier.height(50.dp))
                }
            } else {
                Box(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.animateContentSize().fillMaxSize()
                            .navigationBarsPadding()
                    ) {
                        TopPartOfAddANewLinkDialogBox(
                            isDataExtractingForTheLink = isDataExtractingForTheLink.value,
                            linkTextFieldValue = linkTextFieldValue,
                            titleTextFieldValue = titleTextFieldValue,
                            noteTextFieldValue = noteTextFieldValue,
                            isAutoDetectTitleEnabled = isAutoDetectTitleEnabled,
                            isForceSaveWithoutFetchingMetaDataEnabled = isForceSaveWithoutFetchingMetaDataEnabled,
                            currentFolder = addNewLinkDialogParams.currentFolder,
                            imgUrlTextFieldValue = imgUrlTextFieldValue
                        )
                        VerticalDivider(
                            modifier = Modifier.padding(
                                start = 20.dp, end = 20.dp
                            ), color = LocalContentColor.current.copy(0.01f), thickness = 1.dp
                        )
                        BottomPartOfAddANewLinkDialogBox(
                            onDismiss = addNewLinkDialogParams.onDismiss,
                            isDataExtractingForTheLink = isDataExtractingForTheLink,
                            linkTextFieldValue = linkTextFieldValue,
                            titleTextFieldValue = titleTextFieldValue,
                            noteTextFieldValue = noteTextFieldValue,
                            isAutoDetectTitleEnabled = isAutoDetectTitleEnabled,
                            isForceSaveWithoutFetchingMetaDataEnabled = isForceSaveWithoutFetchingMetaDataEnabled,
                            isDropDownMenuIconClicked = isDropDownMenuIconClicked,
                            showChildFoldersBtmSheet = isChildFoldersBottomSheetExpanded,
                            childFoldersBtmSheetState = btmSheetState,
                            lazyRowState = lazyRowState,
                            addTheFolderInRoot = addTheFolderInRoot,
                            currentFolder = addNewLinkDialogParams.currentFolder,
                            allTags = allTags,
                            selectedTags = addNewLinkDialogParams.selectedTags,
                            foldersSearchQuery = addNewLinkDialogParams.foldersSearchQuery,
                            foldersSearchQueryResult = addNewLinkDialogParams.foldersSearchQueryResult,
                            performAction = addNewLinkDialogParams.performAction,
                            rootRegularFolders = addNewLinkDialogParams.rootRegularFolders,
                            imgUrlTextFieldValue = imgUrlTextFieldValue
                        )
                    }
                    if (!isDataExtractingForTheLink.value) {
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .align(Alignment.TopEnd).padding(15.dp),
                            onClick = addNewLinkDialogParams.onDismiss
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close, contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
    val addANewLinkBtmSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = {
            it != SheetValue.Hidden
        })
    val coroutineScope = rememberCoroutineScope()
    if (platform() !is Platform.Android.Mobile) {
        BasicAlertDialog(
            onDismissRequest = {
                if (!isDataExtractingForTheLink.value) {
                    addNewLinkDialogParams.onDismiss()
                }
            },
            modifier = Modifier.fillMaxSize(0.9f).background(AlertDialogDefaults.containerColor),
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            content()
        }
    } else {
        ModalBottomSheet(sheetState = addANewLinkBtmSheetState, onDismissRequest = {
            if (!isDataExtractingForTheLink.value) {
                coroutineScope.launch {
                    addANewLinkBtmSheetState.hide()
                }.invokeOnCompletion {
                    addNewLinkDialogParams.onDismiss()
                }
            }
        }) {
            content()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AddANewLinkDialogBox.cancelCollectionOfChildFolders()
        }
    }
}

@Composable
private fun TopPartOfAddANewLinkDialogBox(
    isDataExtractingForTheLink: Boolean,
    linkTextFieldValue: MutableState<String>,
    titleTextFieldValue: MutableState<String>,
    noteTextFieldValue: MutableState<String>,
    imgUrlTextFieldValue: MutableState<String>,
    isAutoDetectTitleEnabled: MutableState<Boolean>,
    isForceSaveWithoutFetchingMetaDataEnabled: MutableState<Boolean>,
    currentFolder: Folder?
) {
    val focusRequester = remember {
        FocusRequester()
    }
    Column(
        modifier = Modifier.fillMaxWidth(if (platform() is Platform.Android.Mobile) 1f else 0.5f)
            .then(if (platform() is Platform.Android.Mobile) Modifier else Modifier.fillMaxHeight()),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            color = AlertDialogDefaults.titleContentColor,
            text = when (currentFolder) {
                null -> Localization.rememberLocalizedString(
                    Localization.Key.AddANewLink
                )

                else -> Localization.rememberLocalizedString(Localization.Key.AddANewLinkIn)
                    .replaceFirstPlaceHolderWith(currentFolder.name)
            },
            style = MaterialTheme.typography.titleMedium,
            fontSize = 22.sp,
            modifier = Modifier.padding(
                start = 20.dp, top = 30.dp, end = 20.dp
            ),
            lineHeight = 28.sp
        )

        OutlinedTextField(
            readOnly = isDataExtractingForTheLink,
            modifier = Modifier.padding(
                start = 20.dp, end = 20.dp, top = 20.dp
            ).fillMaxWidth().focusRequester(focusRequester),
            label = {
                Text(
                    text = Localization.rememberLocalizedString(Localization.Key.LinkAddress),
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

        Box(modifier = Modifier.animateContentSize()) {
            if (!AppPreferences.isAutoDetectTitleForLinksEnabled.value && !isAutoDetectTitleEnabled.value) {
                OutlinedTextField(
                    readOnly = isDataExtractingForTheLink,
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp, top = 15.dp
                    ).fillMaxWidth(),
                    label = {
                        Text(
                            text = Localization.rememberLocalizedString(
                                Localization.Key.TitleForTheLink
                            ),
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
        OutlinedTextField(
            readOnly = isDataExtractingForTheLink,
            modifier = Modifier.padding(
                start = 20.dp, end = 20.dp, top = 15.dp
            ).fillMaxWidth(),
            label = {
                Text(
                    text = Localization.rememberLocalizedString(Localization.Key.NoteForSavingTheLink),
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
        OutlinedTextField(
            readOnly = isDataExtractingForTheLink,
            modifier = Modifier.padding(
                start = 20.dp, end = 20.dp, top = 15.dp
            ).fillMaxWidth(),
            label = {
                Text(
                    text = "Image URL for Link",
                    color = AlertDialogDefaults.textContentColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 12.sp
                )
            },
            supportingText = {
                Text(
                    text = "Leave this field empty to automatically retrieve the image URL.",
                    color = AlertDialogDefaults.textContentColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 12.sp
                )
            },
            textStyle = MaterialTheme.typography.titleSmall,
            singleLine = true,
            value = imgUrlTextFieldValue.value,
            onValueChange = {
                imgUrlTextFieldValue.value = it
            })
        if (AppPreferences.isAutoDetectTitleForLinksEnabled.value || AppPreferences.forceSaveWithoutFetchingAnyMetaData.value) {
            InfoCard(
                if (AppPreferences.isAutoDetectTitleForLinksEnabled.value) Localization.rememberLocalizedString(
                    Localization.Key.AutoDetectTitleIsEnabled
                ) else Localization.rememberLocalizedString(
                    Localization.Key.DataRetrievalDisabled
                )
            )
        }
        Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            if (!isForceSaveWithoutFetchingMetaDataEnabled.value && !AppPreferences.isAutoDetectTitleForLinksEnabled.value && !AppPreferences.forceSaveWithoutFetchingAnyMetaData.value) {
                Row(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        .padding(top = if (AppPreferences.isAutoDetectTitleForLinksEnabled.value) 0.dp else 10.dp)
                        .fillMaxWidth().clickable {
                            if (!isDataExtractingForTheLink) {
                                isAutoDetectTitleEnabled.value = !isAutoDetectTitleEnabled.value
                            }
                        }.padding(
                            start = 10.dp, end = 20.dp
                        ), verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        enabled = !isDataExtractingForTheLink,
                        checked = isAutoDetectTitleEnabled.value,
                        onCheckedChange = {
                            isAutoDetectTitleEnabled.value = it
                            if (it) {
                                isForceSaveWithoutFetchingMetaDataEnabled.value = false
                            }
                        })
                    Text(
                        text = Localization.rememberLocalizedString(Localization.Key.ForceAutoDetectTitle),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }
            }
        }

        if (!isAutoDetectTitleEnabled.value && !AppPreferences.isAutoDetectTitleForLinksEnabled.value && !AppPreferences.forceSaveWithoutFetchingAnyMetaData.value) {
            Row(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(top = 10.dp)
                    .fillMaxWidth().clickable {
                        if (!isDataExtractingForTheLink) {
                            isForceSaveWithoutFetchingMetaDataEnabled.value =
                                !isForceSaveWithoutFetchingMetaDataEnabled.value
                        }
                    }.padding(
                        start = 10.dp, end = 20.dp
                    ), verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    enabled = !isDataExtractingForTheLink,
                    checked = isForceSaveWithoutFetchingMetaDataEnabled.value,
                    onCheckedChange = {
                        isForceSaveWithoutFetchingMetaDataEnabled.value = it
                        if (it) {
                            isAutoDetectTitleEnabled.value = false
                        }
                    })
                Text(
                    text = Localization.rememberLocalizedString(Localization.Key.ForceSaveWithoutRetrievingMetadata),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 16.sp
                )
            }
        }
    }
    val platform = platform()
    LaunchedEffect(Unit) {
        if (platform is Platform.Android.Mobile) {
            delay(250)
        }
        focusRequester.requestFocus()
    }
}

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
private fun BottomPartOfAddANewLinkDialogBox(
    onDismiss: () -> Unit,
    isDataExtractingForTheLink: MutableState<Boolean>,
    linkTextFieldValue: MutableState<String>,
    titleTextFieldValue: MutableState<String>,
    noteTextFieldValue: MutableState<String>,
    imgUrlTextFieldValue: MutableState<String>,
    isAutoDetectTitleEnabled: MutableState<Boolean>,
    isForceSaveWithoutFetchingMetaDataEnabled: MutableState<Boolean>,
    isDropDownMenuIconClicked: MutableState<Boolean>,
    showChildFoldersBtmSheet: MutableState<Boolean>,
    childFoldersBtmSheetState: SheetState,
    lazyRowState: LazyListState,
    addTheFolderInRoot: MutableState<Boolean>,
    currentFolder: Folder?,
    rootRegularFolders: StateFlow<PaginationState<Map<PageKey, List<Folder>>>>,
    allTags: PaginationState<Map<PageKey, List<Tag>>>,
    selectedTags: List<Tag>,
    foldersSearchQuery: String,
    foldersSearchQueryResult: StateFlow<List<Folder>>,
    performAction: (AddANewLinkDialogBoxAction) -> Unit,
) {
    val lazyColumnState = rememberLazyListState()

    LaunchedEffect(Unit) {
        launch {
            snapshotFlow {
                lazyColumnState.canScrollForward
            }.debounce(500).distinctUntilChanged().collect {
                if (!it && !allTags.isRetrieving && !allTags.pagesCompleted) {
                    performAction(AddANewLinkDialogBoxAction.OnRetrieveNextRegularRootPage)
                }
            }
        }

        launch {
            snapshotFlow {
                lazyColumnState.firstVisibleItemIndex
            }.debounce(500).distinctUntilChanged().collect {
                performAction(AddANewLinkDialogBoxAction.OnFirstVisibleIndexChangeOfRootFolders(it.toLong()))
            }
        }
    }
    var showBtmSheetForNewTagAddition by rememberSaveable {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    val rootFolders by rootRegularFolders.collectAsStateWithLifecycle()
    val showNewFolderDialog = rememberSaveable {
        mutableStateOf(false)
    }
    var showFolderSearchBtmSheet by rememberSaveable {
        mutableStateOf(false)
    }
    val folderSearchBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val foldersSearchQueryResult by foldersSearchQueryResult.collectAsStateWithLifecycle()
    val selectedFolderForSavingTheLink = rememberDeserializableMutableObject {
        mutableStateOf(
            Folder(
                name = Localization.Key.SavedLinks.getLocalizedString(),
                note = "",
                parentFolderId = null,
                localId = Constants.SAVED_LINKS_ID,
                remoteId = null,
                isArchived = false
            )
        )
    }
    val searchFocusRequester = remember {
        FocusRequester()
    }
    if (isDataExtractingForTheLink.value) {
        if (platform() is Platform.Android.Mobile) {
            Spacer(modifier = Modifier.height(30.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp)
            )
        } else {
            LoadingScreen(PaddingValues())
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().then(
            if (platform() is Platform.Android.Mobile) Modifier else Modifier.verticalScroll(
                rememberScrollState()
            )
        ),
        verticalArrangement = if (platform() is Platform.Android.Mobile) Arrangement.Top else Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                .clickable(onClick = {
                    AppPreferences.showTagsInAddNewLinkDialogBox =
                        !AppPreferences.showTagsInAddNewLinkDialogBox
                }, indication = null, interactionSource = null)
        ) {
            IconButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(
                    start = 5.dp
                ), onClick = {
                    AppPreferences.showTagsInAddNewLinkDialogBox =
                        !AppPreferences.showTagsInAddNewLinkDialogBox
                }) {
                Icon(
                    imageVector = if (AppPreferences.showTagsInAddNewLinkDialogBox) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            Text(
                text = Localization.Key.AttachTags.rememberLocalizedString(),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 18.sp
            )
        }

        AnimatedVisibility(AppPreferences.showTagsInAddNewLinkDialogBox) {
            TagSelectionComponent(
                paddingValues = PaddingValues(start = 15.dp, end = 25.dp),
                allTags = allTags,
                selectedTags = selectedTags,
                onTagClick = {
                    if (selectedTags.contains(it)) {
                        performAction(AddANewLinkDialogBoxAction.UnSelectATag(it))
                    } else {
                        performAction(AddANewLinkDialogBoxAction.SelectATag(it))
                    }
                },
                onRetrieveNextTagsPage = {
                    performAction(AddANewLinkDialogBoxAction.OnRetrieveNextTagsPage)
                },
                onFirstVisibleIndexChange = {
                    performAction(AddANewLinkDialogBoxAction.OnFirstVisibleIndexChangeOfTags(it.toLong()))
                }
            )
        }

        if (currentFolder == null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(
                        start = 5.dp
                    ), onClick = {
                        showFolderSearchBtmSheet = true
                    }) {
                    Icon(
                        imageVector = Icons.Default.Search, contentDescription = null
                    )
                }
                Text(
                    text = Localization.rememberLocalizedString(Localization.Key.AddIn),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 18.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp)
            ) {
                FilledTonalButton(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).pressScaleEffect()
                        .fillMaxWidth(0.8f), onClick = {
                        if (!isDataExtractingForTheLink.value) {
                            isDropDownMenuIconClicked.value = !isDropDownMenuIconClicked.value
                            AddANewLinkDialogBox.subFoldersList.clear()
                        }
                    }) {
                    Text(
                        text = selectedFolderForSavingTheLink.value.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 18.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                FilledTonalIconButton(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).pressScaleEffect(
                        0.75f
                    ), onClick = {
                        if (!isDataExtractingForTheLink.value) {
                            isDropDownMenuIconClicked.value = !isDropDownMenuIconClicked.value
                            AddANewLinkDialogBox.subFoldersList.clear()
                        }
                    }) {
                    Icon(
                        imageVector = if (isDropDownMenuIconClicked.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            if (isDropDownMenuIconClicked.value) {
                LazyColumn(
                    state = lazyColumnState,
                    modifier = Modifier.padding(top = 15.dp, start = 15.dp, end = 15.dp)
                        .heightIn(max = 350.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(25.dp))
                        .background(MaterialTheme.colorScheme.surface).border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(0.15f),
                            shape = RoundedCornerShape(25.dp)
                        ).padding(start = 15.dp, end = 15.dp)
                ) {
                    item {
                        SelectableFolderUIComponent(
                            onClick = {
                                isDropDownMenuIconClicked.value = false
                                selectedFolderForSavingTheLink.value = defaultSavedLinksFolder()
                            },
                            folderName = Localization.rememberLocalizedString(Localization.Key.SavedLinks),
                            imageVector = Icons.Outlined.Link,
                            isComponentSelected = selectedFolderForSavingTheLink.value.localId == Constants.SAVED_LINKS_ID
                        )
                    }
                    item {
                        SelectableFolderUIComponent(
                            onClick = {
                                isDropDownMenuIconClicked.value = false
                                selectedFolderForSavingTheLink.value = defaultImpLinksFolder()
                            },
                            folderName = Localization.rememberLocalizedString(Localization.Key.ImportantLinks),
                            imageVector = Icons.Outlined.StarOutline,
                            isComponentSelected = selectedFolderForSavingTheLink.value.localId == Constants.IMPORTANT_LINKS_ID
                        )
                    }
                    rootFolders.data.forEach { (_, folders) ->
                        items(folders) {
                            FolderSelectorComponent(
                                endSpacing = 0.dp,
                                paddingValues = PaddingValues(),
                                onItemClick = {
                                    isDropDownMenuIconClicked.value = false
                                    selectedFolderForSavingTheLink.value = it
                                },
                                isCurrentFolderSelected = rememberSaveable(it.localId == selectedFolderForSavingTheLink.value.localId) {
                                    mutableStateOf(it.localId == selectedFolderForSavingTheLink.value.localId)
                                },
                                folderName = it.name,
                                onSubDirectoryIconClick = {
                                    AddANewLinkDialogBox.changeParentFolderId(
                                        it.localId, coroutineScope
                                    )
                                    AddANewLinkDialogBox.subFoldersList.add(it)
                                    showChildFoldersBtmSheet.value = true
                                    coroutineScope.launch {
                                        childFoldersBtmSheetState.expand()
                                        try {
                                            if (lazyRowState.layoutInfo.totalItemsCount - 1 < 0) return@launch
                                            lazyRowState.animateScrollToItem(lazyRowState.layoutInfo.totalItemsCount - 1)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    selectedFolderForSavingTheLink.value = it
                                })
                        }
                    }
                    if (!rootFolders.pagesCompleted) {
                        item {
                            Box(
                                modifier = Modifier.padding(top = 15.dp, bottom = 15.dp)
                                    .fillMaxWidth().height(50.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        }
                    }
                }
                if (!isDropDownMenuIconClicked.value) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        if (isDropDownMenuIconClicked.value) {
            Spacer(Modifier.height(10.dp))
        }
        if (currentFolder == null) {
            Button(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(
                    end = 20.dp,
                    start = 20.dp,
                    top = if (isDropDownMenuIconClicked.value) 0.dp else 5.dp
                ).fillMaxWidth().pressScaleEffect(), onClick = {
                    addTheFolderInRoot.value = false
                    showNewFolderDialog.value = true
                }, colors = ButtonDefaults.filledTonalButtonColors()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CreateNewFolder, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = Localization.Key.CreateANewFolder.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 25.dp, end = 25.dp, top = 10.dp, bottom = 10.dp),
            color = DividerDefaults.color.copy(0.25f)
        )
        OutlinedButton(
            colors = ButtonDefaults.outlinedButtonColors(), border = BorderStroke(
                width = 1.dp, color = MaterialTheme.colorScheme.secondary
            ), modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(
                end = 20.dp, start = 20.dp
            ).fillMaxWidth().pressScaleEffect(), onClick = {
                performAction(AddANewLinkDialogBoxAction.ClearSelectedTags)
                onDismiss()
                isForceSaveWithoutFetchingMetaDataEnabled.value = false
            }) {
            Text(
                text = Localization.rememberLocalizedString(Localization.Key.Cancel),
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp
            )
        }
        Button(
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(
                end = 20.dp, top = 10.dp, start = 20.dp
            ).fillMaxWidth().pressScaleEffect(),
            onClick = {
                isDataExtractingForTheLink.value = true
                val linkType = when (currentFolder?.localId
                    ?: selectedFolderForSavingTheLink.value.localId) {
                    Constants.SAVED_LINKS_ID -> LinkType.SAVED_LINK
                    Constants.IMPORTANT_LINKS_ID -> LinkType.IMPORTANT_LINK
                    else -> LinkType.FOLDER_LINK
                }
                performAction(
                    AddANewLinkDialogBoxAction.AddANewLink(
                        link = Link(
                            linkType = linkType,
                            title = titleTextFieldValue.value.trim(),
                            url = linkTextFieldValue.value.trim(),
                            imgURL = imgUrlTextFieldValue.value.trim(),
                            note = noteTextFieldValue.value,
                            idOfLinkedFolder = currentFolder?.localId
                                ?: selectedFolderForSavingTheLink.value.localId,
                            userAgent = AppPreferences.primaryJsoupUserAgent.value
                        ), linkSaveConfig = LinkSaveConfig(
                            forceAutoDetectTitle = isAutoDetectTitleEnabled.value || AppPreferences.isAutoDetectTitleForLinksEnabled.value,
                            forceSaveWithoutRetrievingData = isForceSaveWithoutFetchingMetaDataEnabled.value || AppPreferences.forceSaveWithoutFetchingAnyMetaData.value
                        ), onCompletion = onDismiss, selectedTags = selectedTags,
                        pushSnackbarOnSuccess = true
                    )
                )
            }) {
            Text(
                text = Localization.rememberLocalizedString(Localization.Key.Save),
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp
            )
        }

    }
    val hideSearchBtmSheet: () -> Unit = {
        coroutineScope.launch {
            folderSearchBtmSheetState.hide()
        }.invokeOnCompletion {
            showFolderSearchBtmSheet = false
        }
    }
    if (showFolderSearchBtmSheet) {
        ModalBottomSheet(
            sheetState = folderSearchBtmSheetState, onDismissRequest = hideSearchBtmSheet
        ) {
            Scaffold(topBar = {
                Text(
                    text = Localization.Key.SearchForFolders.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().padding(15.dp)
                )
            }, bottomBar = {
                OutlinedTextField(
                    trailingIcon = {
                        SortingIconButton()
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null)
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = foldersSearchQuery,
                    onValueChange = {
                        performAction(AddANewLinkDialogBoxAction.UpdateFoldersSearchQuery(it))
                    },
                    shape = RoundedCornerShape(25.dp),
                    label = {
                        Text(
                            text = Localization.Key.FolderName.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    placeholder = {
                        Text(
                            text = Localization.Key.SearchForFolders.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                        )
                    },
                    modifier = Modifier.focusRequester(searchFocusRequester)
                        .background(BottomSheetDefaults.ContainerColor)
                        .fillMaxWidth().padding(15.dp)
                )
                LaunchedEffect(Unit) {
                    searchFocusRequester.requestFocus()
                }
            }, containerColor = BottomSheetDefaults.ContainerColor) { paddingValues ->
                LazyColumn(
                    Modifier.addEdgeToEdgeScaffoldPadding(paddingValues).fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    if (foldersSearchQueryResult.isEmpty()) {
                        item {
                            DataEmptyScreen(text = Localization.Key.NoFoldersFound.rememberLocalizedString())
                        }
                    } else {
                        items(items = foldersSearchQueryResult, key = {
                            "${it.name}-${it.note}-${it.localId}"
                        }) {
                            FolderSelectorComponent(
                                onItemClick = {
                                    hideSearchBtmSheet()
                                    selectedFolderForSavingTheLink.value = it
                                },
                                isCurrentFolderSelected = rememberSaveable(it.localId == selectedFolderForSavingTheLink.value.localId) {
                                    mutableStateOf(it.localId == selectedFolderForSavingTheLink.value.localId)
                                },
                                folderName = it.name,
                                onSubDirectoryIconClick = null
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                        }
                    }
                }
            }
        }
    }
    val createTagBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val hideCreateATagBtmSheet: () -> Unit = {
        coroutineScope.launch {
            createTagBtmSheetState.hide()
        }.invokeOnCompletion {
            showBtmSheetForNewTagAddition = false
        }
    }

    CreateATagBtmSheet(
        sheetState = createTagBtmSheetState,
        showBtmSheet = showBtmSheetForNewTagAddition,
        onCancel = hideCreateATagBtmSheet,
        onCreateClick = { tagName ->
            performAction(
                AddANewLinkDialogBoxAction.CreateATag(
                    tagName = tagName,
                    onCompletion = hideCreateATagBtmSheet
                )
            )
        })

    if (showChildFoldersBtmSheet.value) {
        val childFolders = AddANewLinkDialogBox.childFolders.collectAsStateWithLifecycle()
        ModalBottomSheet(sheetState = childFoldersBtmSheetState, onDismissRequest = {
            AddANewLinkDialogBox.subFoldersList.clear()
            showChildFoldersBtmSheet.value = false
        }) {
            LazyColumn(
                Modifier.fillMaxWidth().wrapContentHeight()
            ) {
                stickyHeader {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = try {
                                AddANewLinkDialogBox.subFoldersList.last().name
                            } catch (e: Exception) {
                                e.printStackTrace()
                                ""
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(start = 15.dp, bottom = 5.dp)
                        )
                        LazyRow(
                            state = lazyRowState, modifier = Modifier.padding(
                                start = 15.dp, end = 15.dp, bottom = 15.dp
                            ), verticalAlignment = Alignment.CenterVertically
                        ) {
                            item {
                                Text(
                                    text = "/",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 16.sp
                                )
                            }
                            itemsIndexed(AddANewLinkDialogBox.subFoldersList.toMutableStateList()) { index, subFolder ->
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowRight,
                                    contentDescription = ""
                                )
                                Text(
                                    text = subFolder.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 16.sp,
                                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                        .clickable {
                                            AddANewLinkDialogBox.changeParentFolderId(
                                                subFolder.localId,
                                                coroutineScope
                                            )
                                            selectedFolderForSavingTheLink.value = subFolder
                                            if (AddANewLinkDialogBox.subFoldersList.indexOf(
                                                    subFolder
                                                ) != AddANewLinkDialogBox.subFoldersList.indexOf(
                                                    AddANewLinkDialogBox.subFoldersList.last()
                                                )
                                            ) {
                                                AddANewLinkDialogBox.subFoldersList.removeAll(
                                                    AddANewLinkDialogBox.subFoldersList.toList()
                                                        .subList(
                                                            fromIndex = AddANewLinkDialogBox.subFoldersList.indexOf(
                                                                AddANewLinkDialogBox.subFoldersList.find {
                                                                    it.localId == selectedFolderForSavingTheLink.value.localId
                                                                }) + 1,
                                                            toIndex = AddANewLinkDialogBox.subFoldersList.size
                                                        ).toSet()
                                                )
                                            }
                                        })
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
                                selectedFolderForSavingTheLink.value = it
                                isDropDownMenuIconClicked.value = false
                                AddANewLinkDialogBox.subFoldersList.clear()
                                coroutineScope.launch {
                                    childFoldersBtmSheetState.hide()
                                }
                                showChildFoldersBtmSheet.value = false
                            },
                            isCurrentFolderSelected = rememberSaveable(it.localId == selectedFolderForSavingTheLink.value.localId) {
                                mutableStateOf(it.localId == selectedFolderForSavingTheLink.value.localId)
                            },
                            folderName = it.name,
                            onSubDirectoryIconClick = {
                                selectedFolderForSavingTheLink.value = it
                                AddANewLinkDialogBox.subFoldersList.add(it)
                                AddANewLinkDialogBox.changeParentFolderId(
                                    selectedFolderForSavingTheLink.value.localId,
                                    coroutineScope
                                )
                                coroutineScope.launch {
                                    try {
                                        if (lazyRowState.layoutInfo.totalItemsCount - 1 < 0) return@launch
                                        lazyRowState.animateScrollToItem(lazyRowState.layoutInfo.totalItemsCount - 1)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            })
                    }
                } else {
                    item {
                        Text(
                            text = Localization.Key.ThisFolderHasNoSubfolders.rememberLocalizedString(),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 24.sp,
                            lineHeight = 36.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxSize().padding(15.dp)
                        )
                    }
                    item {
                        FilledTonalButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(
                                top = 5.dp, end = 15.dp, start = 15.dp
                            ).fillMaxWidth().pressScaleEffect(), onClick = {
                                addTheFolderInRoot.value = false
                                showNewFolderDialog.value = true
                            }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreateNewFolder, null)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = Localization.Key.CreateANewFolder.rememberLocalizedString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 16.sp
                                )
                            }
                        }
                        Button(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .fillMaxWidth().padding(start = 15.dp, end = 15.dp), onClick = {
                                isDropDownMenuIconClicked.value = false
                                AddANewLinkDialogBox.subFoldersList.clear()
                                coroutineScope.launch {
                                    childFoldersBtmSheetState.hide()
                                }
                                showChildFoldersBtmSheet.value = false
                            }) {
                            Text(
                                text = Localization.Key.SaveInThisFolder.rememberLocalizedString(),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(5.dp))
                    }
                }
                if (AddANewLinkDialogBox.childFolders.value.isNotEmpty()) {
                    item {
                        FilledTonalButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(
                                end = 20.dp, top = 15.dp, start = 20.dp, bottom = 15.dp
                            ).fillMaxWidth().pressScaleEffect(), onClick = {
                                addTheFolderInRoot.value = false
                                showNewFolderDialog.value = true
                            }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreateNewFolder, null)
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = Localization.Key.CreateANewFolder.rememberLocalizedString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showNewFolderDialog.value) {
        AddANewFolderDialogBox(
            AddNewFolderDialogBoxParam(
                onDismiss = {
                    showNewFolderDialog.value = false
                },
                inCollectionDetailPane = !addTheFolderInRoot.value,
                onFolderCreateClick = { folderName, folderNote, onCompletion ->
                    performAction(
                        AddANewLinkDialogBoxAction.InsertANewFolder(
                            folder = Folder(
                                name = folderName,
                                note = folderNote,
                                parentFolderId = if (addTheFolderInRoot.value || selectedFolderForSavingTheLink.value.localId in defaultFolderIds()) null else selectedFolderForSavingTheLink.value.localId,
                            ),
                            onCompletion = onCompletion,
                            ignoreFolderAlreadyExistsThrowable = true
                        )
                    )
                },
                currentFolder = if (selectedFolderForSavingTheLink.value.localId in defaultFolderIds()) null else selectedFolderForSavingTheLink.value
            )
        )
    }
}


@Composable
private fun FolderSelectorComponent(
    onItemClick: () -> Unit,
    isCurrentFolderSelected: MutableState<Boolean>,
    folderName: String,
    onSubDirectoryIconClick: (() -> Unit)?,
    paddingValues: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp),
    endSpacing: Dp = 20.dp
) {
    Column(
        modifier = Modifier.pressScaleEffect().pointerHoverIcon(icon = PointerIcon.Hand)
            .fillMaxWidth()
            .clickable(indication = null, interactionSource = null, onClick = onItemClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                tint = if (isCurrentFolderSelected.value) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier.padding(paddingValues).size(28.dp)
            )
            Box(
                modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isCurrentFolderSelected.value) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = if (isCurrentFolderSelected.value) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    if (onSubDirectoryIconClick != null) {
                        Spacer(modifier = Modifier.width(20.dp))
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                            onClick = {
                                onSubDirectoryIconClick()
                            }) {
                            Icon(
                                imageVector = Icons.Filled.SubdirectoryArrowRight,
                                contentDescription = null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(endSpacing))
                }
            }
        }
        Text(
            text = folderName,
            color = if (isCurrentFolderSelected.value) MaterialTheme.colorScheme.primary else LocalContentColor.current,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            maxLines = 1,
            modifier = Modifier.padding(
                start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                end = paddingValues.calculateEndPadding(
                    LocalLayoutDirection.current
                )
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

object AddANewLinkDialogBox {
    val subFoldersList = mutableStateListOf<Folder>()

    private val _childFolders = MutableStateFlow(emptyList<Folder>())
    val childFolders = _childFolders.asStateFlow()

    private var changeParentFolderIdJob: Job? = null

    fun cancelCollectionOfChildFolders() = changeParentFolderIdJob?.cancel()

    fun changeParentFolderId(parentFolderId: Long, coroutineScope: CoroutineScope) {
        changeParentFolderIdJob?.cancel()
        linkoraLog(parentFolderId)
        changeParentFolderIdJob = coroutineScope.launch {
            DependencyContainer.localFoldersRepo.getChildFoldersOfThisParentIDAsFlow(
                parentFolderId
            ).cancellable().collectLatest {
                it.onSuccess {
                    _childFolders.emit(it.data)
                }.pushSnackbarOnFailure()
            }
        }
    }
}