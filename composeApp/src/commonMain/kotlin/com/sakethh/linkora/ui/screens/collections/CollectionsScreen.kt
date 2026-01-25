package com.sakethh.linkora.ui.screens.collections

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DatasetLinked
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.platform.PlatformSpecificBackHandler
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.components.SortingIconButton
import com.sakethh.linkora.ui.components.folder.FolderComponent
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.ui.domain.CurrentFABContext
import com.sakethh.linkora.ui.domain.model.CollectionDetailPaneInfo
import com.sakethh.linkora.ui.domain.model.CollectionType
import com.sakethh.linkora.ui.domain.model.FolderComponentParam
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.LoadingScreen
import com.sakethh.linkora.ui.screens.collections.components.ItemDivider
import com.sakethh.linkora.ui.screens.collections.components.RootCollectionSwitcher
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CollectionsScreen(
    collectionScreenParams: CollectionScreenParams,
    collectionDetailPaneParams: CollectionDetailPaneParams,
    currentFABContext: (CurrentFABContext) -> Unit
) {
    val anyCollectionSelected by collectionScreenParams.isPaneSelected.collectAsStateWithLifecycle()
    val platform = LocalPlatform.current
    LaunchedEffect(Unit) {
        currentFABContext(CurrentFABContext.ROOT)
    }
    LaunchedEffect(anyCollectionSelected) {
        if (!anyCollectionSelected && platform !is Platform.Android.Mobile) {
            currentFABContext(CurrentFABContext.ROOT)
        }
    }
    val rootFolders by collectionScreenParams.rootRegularFolders.collectAsStateWithLifecycle()
    LaunchedEffect(rootFolders) {
        linkoraLog("rootFolders:\n:${rootFolders}")
    }
    val coroutineScope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var isRootContentSwitcherBtmSheetVisible by rememberSaveable {
        mutableStateOf(false)
    }
    val rootContentPagerState =
        rememberPagerState(initialPage = AppPreferences.selectedCollectionSourceId) { 2 }
    val rootContentSwitcherBtmSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(rootContentPagerState.currentPage) {
        AppPreferences.selectedCollectionSourceId = rootContentPagerState.currentPage
        rootContentPagerState.animateScrollToPage(AppPreferences.selectedCollectionSourceId)
    }
    val allTags by collectionScreenParams.allTags.collectAsStateWithLifecycle()
    val paneHistoryPeek by collectionScreenParams.peekPaneHistory.collectAsStateWithLifecycle()
    val rootFoldersListState = rememberLazyListState()
    val tagsListState = rememberLazyListState()

    val isRootFoldersListAtTheEnd = retain {
        derivedStateOf {
            (rootFoldersListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= (rootFoldersListState.layoutInfo.totalItemsCount - Constants.TRIGGER_THRESHOLD_AT_THE_END)
        }
    }
    val isTagsListAtTheEnd = retain {
        derivedStateOf {
            (tagsListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) >= (tagsListState.layoutInfo.totalItemsCount - Constants.TRIGGER_THRESHOLD_AT_THE_END)
        }
    }

    LaunchedEffect(isRootFoldersListAtTheEnd.value) {
        collectionScreenParams.onRetrieveNextRegularRootFolderPage()
    }

    LaunchedEffect(isTagsListAtTheEnd.value) {
        collectionScreenParams.onRetrieveNextTagsPage()
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            rootFoldersListState.firstVisibleItemIndex
        }.debounce(500).distinctUntilChanged().collectLatest {
            collectionScreenParams.onRegularRootFolderFirstVisibleItemIndexChange(it.toLong())
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            tagsListState.firstVisibleItemIndex
        }.debounce(500).distinctUntilChanged().collectLatest {
            collectionScreenParams.onTagsFirstVisibleItemIndexChange(it.toLong())
        }
    }
    val parentScrollState = rememberScrollState()

    // yoinked from SO: https://stackoverflow.com/a/72329873
    val defaultFoldersScrollConnection = retain {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0) {
                    val consumed = parentScrollState.dispatchRawDelta(-available.y)
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (available.y > 0) {
                    val consumed = parentScrollState.dispatchRawDelta(-available.y)
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }
        }
    }


    val isRootFoldersEmpty = rootFolders.data.isEmpty() || rootFolders.data.values.first()
        .isEmpty()


    val isTagsEmpty = allTags.data.isEmpty() || allTags.data.values.first()
        .isEmpty()

    Scaffold(
        floatingActionButtonPosition = FabPosition.End,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        topBar = {
            Column {
                MediumTopAppBar(
                    scrollBehavior = topAppBarScrollBehavior, title = {
                        Text(
                            text = Navigation.Root.CollectionsScreen.toString(),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 22.sp
                        )
                    })
                if (platform !is Platform.Android.Mobile) {
                    HorizontalDivider()
                }
            }
        }) { padding ->
        Row(
            modifier = Modifier.padding(padding).fillMaxSize()
                .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxHeight()
                    .fillMaxWidth(if (platform() is Platform.Android.Mobile) 1f else 0.4f)
            ) {
                val screenHeight = maxHeight
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(parentScrollState)
                ) {
                    DefaultFolderComponent(
                        name = Localization.rememberLocalizedString(Localization.Key.AllLinks),
                        icon = Icons.Outlined.DatasetLinked,
                        onClick = {
                            val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                currentFolder = Folder(
                                    name = Localization.Key.AllLinks.getLocalizedString(),
                                    note = "",
                                    parentFolderId = null,
                                    localId = Constants.ALL_LINKS_ID
                                ),
                                currentTag = null,
                                collectionType = CollectionType.FOLDER,
                            )
                            if (platform is Platform.Android.Mobile) {
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                    value = Json.encodeToString(collectionDetailPaneInfo)
                                )
                                navController.navigate(
                                    Navigation.Collection.MobileCollectionDetailScreen
                                )
                            } else {
                                collectionScreenParams.performAction(
                                    CollectionsAction.PushToDetailPane(
                                        collectionDetailPaneInfo
                                    )
                                )
                                collectionScreenParams.performAction(
                                    CollectionsAction.ClearDetailPaneHistoryUntilLast
                                )
                            }
                        },
                        isSelected = paneHistoryPeek?.currentFolder?.localId == Constants.ALL_LINKS_ID
                    )
                    ItemDivider()
                    DefaultFolderComponent(
                        name = Localization.rememberLocalizedString(Localization.Key.SavedLinks),
                        icon = Icons.Outlined.Link,
                        onClick = {
                            val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                currentFolder = Folder(
                                    name = Localization.Key.SavedLinks.getLocalizedString(),
                                    note = "",
                                    parentFolderId = null,
                                    localId = Constants.SAVED_LINKS_ID
                                ),
                                currentTag = null,
                                collectionType = CollectionType.FOLDER,
                            )
                            if (platform is Platform.Android.Mobile) {
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                    value = Json.encodeToString(collectionDetailPaneInfo)
                                )
                                navController.navigate(
                                    Navigation.Collection.MobileCollectionDetailScreen
                                )
                            } else {
                                collectionScreenParams.performAction(
                                    CollectionsAction.PushToDetailPane(
                                        collectionDetailPaneInfo
                                    )
                                )
                                collectionScreenParams.performAction(
                                    CollectionsAction.ClearDetailPaneHistoryUntilLast
                                )
                            }
                        },
                        isSelected = paneHistoryPeek?.currentFolder?.localId == Constants.SAVED_LINKS_ID
                    )
                    DefaultFolderComponent(
                        name = Localization.rememberLocalizedString(Localization.Key.ImportantLinks),
                        icon = Icons.Outlined.StarOutline,
                        onClick = {
                            val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                currentFolder = Folder(
                                    name = Localization.Key.ImportantLinks.getLocalizedString(),
                                    note = "",
                                    parentFolderId = null,
                                    localId = Constants.IMPORTANT_LINKS_ID
                                ),
                                currentTag = null,
                                collectionType = CollectionType.FOLDER,
                            )
                            if (platform is Platform.Android.Mobile) {
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                    value = Json.encodeToString(collectionDetailPaneInfo)
                                )
                                navController.navigate(
                                    Navigation.Collection.MobileCollectionDetailScreen
                                )
                            } else {
                                collectionScreenParams.performAction(
                                    CollectionsAction.PushToDetailPane(
                                        collectionDetailPaneInfo
                                    )
                                )
                                collectionScreenParams.performAction(
                                    CollectionsAction.ClearDetailPaneHistoryUntilLast
                                )
                            }
                        },
                        isSelected = paneHistoryPeek?.currentFolder?.localId == Constants.IMPORTANT_LINKS_ID
                    )
                    DefaultFolderComponent(
                        name = Localization.rememberLocalizedString(Localization.Key.Archive),
                        icon = Icons.Outlined.Archive,
                        onClick = {
                            val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                currentFolder = Folder(
                                    name = Localization.Key.Archive.getLocalizedString(),
                                    note = "",
                                    parentFolderId = null,
                                    localId = Constants.ARCHIVE_ID
                                ),
                                currentTag = null,
                                collectionType = CollectionType.FOLDER,
                            )
                            if (platform is Platform.Android.Mobile) {
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                    value = Json.encodeToString(collectionDetailPaneInfo)
                                )
                                navController.navigate(
                                    Navigation.Collection.MobileCollectionDetailScreen
                                )
                            } else {
                                collectionScreenParams.performAction(
                                    CollectionsAction.PushToDetailPane(
                                        collectionDetailPaneInfo
                                    )
                                )
                                collectionScreenParams.performAction(
                                    CollectionsAction.ClearDetailPaneHistoryUntilLast
                                )
                            }
                        },
                        isSelected = paneHistoryPeek?.currentFolder?.localId == Constants.ARCHIVE_ID
                    )
                    Spacer(modifier = Modifier.height(15.dp))

                    Row(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .padding(start = 15.dp)
                                .clickable(indication = null, interactionSource = remember {
                                    MutableInteractionSource()
                                }) {
                                    isRootContentSwitcherBtmSheetVisible = true
                                    coroutineScope.launch {
                                        rootContentSwitcherBtmSheetState.show()
                                    }
                                }, verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    isRootContentSwitcherBtmSheetVisible = true
                                    coroutineScope.launch {
                                        rootContentSwitcherBtmSheetState.show()
                                    }
                                },
                                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                    .size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            AnimatedContent(targetState = collectionScreenParams.currentCollectionSource) { currentCollectionSource ->
                                Text(
                                    text = currentCollectionSource,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 20.sp,
                                )
                            }
                        }
                        Row {
                            SortingIconButton()
                            if (platform == Platform.Android.Mobile) {
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                        }
                    }

                    HorizontalPager(
                        state = rootContentPagerState,
                        modifier = Modifier.height(screenHeight).animateContentSize()
                            .fillMaxWidth().nestedScroll(defaultFoldersScrollConnection),
                        verticalAlignment = Alignment.Top,
                    ) { currentPage ->
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = if (currentPage == 0) rootFoldersListState else tagsListState,
                        ) {
                            when (currentPage) {
                                0 -> {

                                    item {
                                        AnimatedVisibility(!rootFolders.isRetrieving && isRootFoldersEmpty) {
                                            DataEmptyScreen(
                                                text = Localization.Key.NoFoldersFound.rememberLocalizedString(),
                                                paddingValues = PaddingValues(
                                                    top = 50.dp, start = 15.dp
                                                )
                                            )
                                        }
                                    }

                                    if (!rootFolders.isRetrieving && isRootFoldersEmpty) {
                                        return@LazyColumn
                                    }

                                    rootFolders.data.forEach { (_, folders) ->
                                        items(folders, key = {
                                            "rootFolders" + it.localId
                                        }) { folder ->
                                            FolderComponent(
                                                FolderComponentParam(
                                                    name = folder.name,
                                                    note = folder.note,
                                                    onClick = {
                                                        if (CollectionsScreenVM.selectedFoldersViaLongClick.contains(
                                                                folder
                                                            )
                                                        ) {
                                                            return@FolderComponentParam
                                                        }
                                                        val collectionDetailPaneInfo =
                                                            CollectionDetailPaneInfo(
                                                                currentFolder = folder,
                                                                currentTag = null,
                                                                collectionType = CollectionType.FOLDER,
                                                            )
                                                        if (platform is Platform.Android.Mobile) {
                                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                                key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                                                value = Json.encodeToString(
                                                                    collectionDetailPaneInfo
                                                                )
                                                            )
                                                            navController.navigate(
                                                                Navigation.Collection.MobileCollectionDetailScreen
                                                            )
                                                        } else {
                                                            collectionScreenParams.performAction(
                                                                CollectionsAction.PushToDetailPane(
                                                                    collectionDetailPaneInfo
                                                                )
                                                            )
                                                            collectionScreenParams.performAction(
                                                                CollectionsAction.ClearDetailPaneHistoryUntilLast
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {
                                                        if (CollectionsScreenVM.isSelectionEnabled.value.not()) {
                                                            CollectionsScreenVM.isSelectionEnabled.value =
                                                                true
                                                            CollectionsScreenVM.selectedFoldersViaLongClick.add(
                                                                folder
                                                            )
                                                        }
                                                    },
                                                    onMoreIconClick = {
                                                        coroutineScope.pushUIEvent(
                                                            UIEvent.Type.ShowMenuBtmSheet(
                                                                menuBtmSheetFor = MenuBtmSheetType.Folder.RegularFolder,
                                                                selectedLinkForMenuBtmSheet = null,
                                                                selectedFolderForMenuBtmSheet = folder
                                                            )
                                                        )
                                                    },
                                                    isCurrentlyInDetailsView = remember(
                                                        paneHistoryPeek?.currentFolder?.localId
                                                    ) {
                                                        mutableStateOf(paneHistoryPeek?.currentFolder?.localId == folder.localId)
                                                    },
                                                    showMoreIcon = rememberSaveable {
                                                        mutableStateOf(true)
                                                    },
                                                    isSelectedForSelection = rememberSaveable(
                                                        CollectionsScreenVM.isSelectionEnabled.value,
                                                        CollectionsScreenVM.selectedFoldersViaLongClick.size
                                                    ) {
                                                        mutableStateOf(
                                                            CollectionsScreenVM.isSelectionEnabled.value && CollectionsScreenVM.selectedFoldersViaLongClick.contains(
                                                                folder
                                                            )
                                                        )
                                                    },
                                                    showCheckBox = CollectionsScreenVM.isSelectionEnabled,
                                                    onCheckBoxChanged = { bool ->
                                                        if (bool) {
                                                            CollectionsScreenVM.selectedFoldersViaLongClick.add(
                                                                folder
                                                            )
                                                        } else {
                                                            CollectionsScreenVM.selectedFoldersViaLongClick.remove(
                                                                folder
                                                            )
                                                        }
                                                    })
                                            )
                                        }
                                    }

                                    item {
                                        AnimatedVisibility(!rootFolders.pagesCompleted) {
                                            LoadingScreen(
                                                paddingValues = PaddingValues(
                                                    start = 15.dp,
                                                    top = 75.dp
                                                )
                                            )
                                        }
                                    }
                                }

                                1 -> {
                                    item {
                                        AnimatedVisibility(!allTags.isRetrieving && isTagsEmpty) {
                                            DataEmptyScreen(text = Localization.Key.NoTagsFound.rememberLocalizedString())
                                        }
                                    }

                                    if (!allTags.isRetrieving && isTagsEmpty) {
                                        return@LazyColumn
                                    }

                                    allTags.data.forEach { (pageKey, tags) ->
                                        items(tags, key = {
                                            "Collection-Screen-Tags-Page:$pageKey+" + "ID:" + it.localId
                                        }) { currentTag ->
                                            FolderComponent(
                                                FolderComponentParam(
                                                    name = currentTag.name,
                                                    note = "",
                                                    leadingIcon = Icons.Default.Tag,
                                                    onClick = {
                                                        val collectionDetailPaneInfo =
                                                            CollectionDetailPaneInfo(
                                                                currentFolder = null,
                                                                currentTag = currentTag,
                                                                collectionType = CollectionType.TAG,
                                                            )
                                                        if (platform is Platform.Android.Mobile) {
                                                            navController.currentBackStackEntry?.savedStateHandle?.set(
                                                                key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                                                value = Json.encodeToString(
                                                                    collectionDetailPaneInfo
                                                                )
                                                            )
                                                            navController.navigate(
                                                                Navigation.Collection.MobileCollectionDetailScreen
                                                            )
                                                        } else {
                                                            collectionScreenParams.performAction(
                                                                CollectionsAction.PushToDetailPane(
                                                                    collectionDetailPaneInfo
                                                                )
                                                            )
                                                            collectionScreenParams.performAction(
                                                                CollectionsAction.ClearDetailPaneHistoryUntilLast
                                                            )
                                                        }
                                                    },
                                                    onLongClick = {},
                                                    onMoreIconClick = {
                                                        coroutineScope.pushUIEvent(
                                                            UIEvent.Type.ShowTagMenuBtmSheet(
                                                                selectedTag = currentTag
                                                            )
                                                        )
                                                    },
                                                    isCurrentlyInDetailsView = rememberSaveable {
                                                        mutableStateOf(false)
                                                    },
                                                    showMoreIcon = rememberSaveable {
                                                        mutableStateOf(true)
                                                    },
                                                    isSelectedForSelection = rememberSaveable {
                                                        mutableStateOf(false)
                                                    },
                                                    showCheckBox = rememberSaveable {
                                                        mutableStateOf(false)
                                                    },
                                                    onCheckBoxChanged = {})
                                            )
                                        }
                                    }

                                    item {
                                        AnimatedVisibility(!allTags.pagesCompleted) {
                                            LoadingScreen(
                                                paddingValues = PaddingValues(
                                                    start = 15.dp,
                                                    top = 75.dp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Spacer(Modifier.height(250.dp))
                            }
                        }
                    }
                }
            }
            if (platform() is Platform.Android.Mobile) return@Row
            VerticalDivider(modifier = Modifier.padding(start = 20.dp))
            if (!anyCollectionSelected) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Localization.Key.SelectACollection.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                CollectionDetailPane(
                    platform = platform,
                    currentFABContext = currentFABContext,
                    collectionDetailPaneParams = collectionDetailPaneParams
                )
            }
        }
    }
    val hideCollectionSwitcher: () -> Unit = {
        coroutineScope.launch {
            rootContentSwitcherBtmSheetState.hide()
        }.invokeOnCompletion {
            isRootContentSwitcherBtmSheetVisible = false
        }
    }
    RootCollectionSwitcher(
        isRootContentSwitcherBtmSheetVisible = isRootContentSwitcherBtmSheetVisible,
        rootContentSwitcherBtmSheetState = rootContentSwitcherBtmSheetState,
        onHide = hideCollectionSwitcher,
        onSourceClick = { currCollectionSourceId ->
            AppPreferences.selectedCollectionSourceId = currCollectionSourceId
            coroutineScope.launch {
                rootContentPagerState.animateScrollToPage(currCollectionSourceId)
            }
            hideCollectionSwitcher()
        })

    PlatformSpecificBackHandler {
        if (CollectionsScreenVM.isSelectionEnabled.value) {
            CollectionsScreenVM.clearAllSelections()
        } else {
            navController.navigateUp()
        }
    }
}

@Composable
private fun DefaultFolderComponent(
    name: String, icon: ImageVector, onClick: () -> Unit, isSelected: Boolean
) {
    Card(
        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(
            end = if (platform() == Platform.Android.Mobile) 15.dp else 0.dp,
            start = 15.dp,
            top = 15.dp
        ).wrapContentHeight().fillMaxWidth().clickable(interactionSource = remember {
            MutableInteractionSource()
        }, indication = null, onClick = {
            onClick()
        }).pressScaleEffect().then(
            if (isSelected && platform() !is Platform.Android.Mobile) Modifier.border(
                width = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CardDefaults.shape
            ) else Modifier
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.padding(20.dp), imageVector = icon, contentDescription = null
            )
            Text(
                text = name, style = MaterialTheme.typography.titleSmall, fontSize = 16.sp
            )
        }
    }
}