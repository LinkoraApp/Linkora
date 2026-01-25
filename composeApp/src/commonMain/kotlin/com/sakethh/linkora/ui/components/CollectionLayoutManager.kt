package com.sakethh.linkora.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.model.FlatChildFolderData
import com.sakethh.linkora.domain.model.FlatSearchResult
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.PageKey
import com.sakethh.linkora.ui.components.folder.FolderComponent
import com.sakethh.linkora.ui.components.link.GridViewLinkComponent
import com.sakethh.linkora.ui.components.link.ListViewLinkComponent
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.domain.ScreenType
import com.sakethh.linkora.ui.domain.model.FolderComponentParam
import com.sakethh.linkora.ui.domain.model.LinkComponentParam
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Constants
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollectionLayoutManager(
    screenType: ScreenType,
    flatChildFolderDataState: PaginationState<Map<PageKey, List<FlatChildFolderData>>>?,
    linksTagsPairsState: PaginationState<Map<PageKey, List<LinkTagsPair>>>?,
    flatSearchResultState: PaginationState<Map<PageKey, List<FlatSearchResult>>>?,
    paddingValues: PaddingValues,
    folderMoreIconClick: (folder: Folder) -> Unit,
    tagMoreIconClick: (tag: Tag) -> Unit,
    onFolderClick: (folder: Folder) -> Unit,
    linkMoreIconClick: (linkTagsPair: LinkTagsPair) -> Unit,
    onLinkClick: (linkTagsPair: LinkTagsPair) -> Unit,
    onTagClick: (tag: Tag) -> Unit,
    onAttachedTagClick: (tag: Tag) -> Unit,
    isCurrentlyInDetailsView: (folder: Folder) -> Boolean,
    emptyDataText: String = "",
    nestedScrollConnection: NestedScrollConnection?,
    onRetrieveNextPage: () -> Unit,
    onFirstVisibleItemIndexChange: (PageKey) -> Unit,
) {
    val listLayoutState =
        rememberLazyListState()

    val gridLayoutState = rememberLazyGridState()
    val staggeredGridLayoutState = rememberLazyStaggeredGridState()

    val linkComponentParam: (linkTagsPair: LinkTagsPair) -> LinkComponentParam = { linkTagsPair ->
        LinkComponentParam(
            link = linkTagsPair.link,
            isSelectionModeEnabled = CollectionsScreenVM.isSelectionEnabled,
            onMoreIconClick = {
                linkMoreIconClick(linkTagsPair)
            },
            onLinkClick = {
                if (!CollectionsScreenVM.isSelectionEnabled.value) {
                    onLinkClick(linkTagsPair)
                } else {
                    if (CollectionsScreenVM.selectedLinkTagPairsViaLongClick.contains(linkTagsPair)) {
                        CollectionsScreenVM.selectedLinkTagPairsViaLongClick.remove(linkTagsPair)
                    } else {
                        CollectionsScreenVM.selectedLinkTagPairsViaLongClick.add(linkTagsPair)
                    }
                }
            },
            onForceOpenInExternalBrowserClicked = {

            },
            isItemSelected = mutableStateOf(
                CollectionsScreenVM.selectedLinkTagPairsViaLongClick.contains(
                    linkTagsPair
                )
            ),
            onLongClick = {
                if (!CollectionsScreenVM.isSelectionEnabled.value) {
                    CollectionsScreenVM.isSelectionEnabled.value = true
                    CollectionsScreenVM.selectedLinkTagPairsViaLongClick.add(linkTagsPair)
                }
            },
            tags = linkTagsPair.tags,
            onTagClick = {
                onAttachedTagClick(it)
            })
    }
    val bottomSpacing = remember {
        mutableStateOf(250.dp)
    }

    val isDataEmpty by rememberSaveable(
        linksTagsPairsState,
        flatChildFolderDataState,
        flatSearchResultState
    ) {
        mutableStateOf(
            when (screenType) {
                ScreenType.LINKS_ONLY -> {
                    linksTagsPairsState?.data?.isEmpty() == true || linksTagsPairsState?.data?.values?.first()
                        ?.isEmpty() == true
                }

                ScreenType.FOLDERS_AND_LINKS -> {
                    flatChildFolderDataState?.data?.isEmpty() == true || flatChildFolderDataState?.data?.values?.first()
                        ?.isEmpty() == true
                }

                ScreenType.TAGS_FOLDERS_LINKS -> {
                    flatSearchResultState?.data?.isEmpty() == true || flatSearchResultState?.data?.values?.first()
                        ?.isEmpty() == true
                }
            }
        )
    }

    val pagesCompleted by rememberSaveable(
        linksTagsPairsState,
        flatChildFolderDataState,
        flatSearchResultState
    ) {
        mutableStateOf(
            when (screenType) {
                ScreenType.LINKS_ONLY -> linksTagsPairsState?.pagesCompleted == true && !linksTagsPairsState.isRetrieving

                ScreenType.FOLDERS_AND_LINKS -> flatChildFolderDataState?.pagesCompleted == true && !flatChildFolderDataState.isRetrieving

                ScreenType.TAGS_FOLDERS_LINKS -> flatSearchResultState?.pagesCompleted == true && !flatSearchResultState.isRetrieving
            }
        )
    }

    val showLoading by rememberSaveable(
        linksTagsPairsState,
        flatChildFolderDataState,
        flatSearchResultState
    ) {
        mutableStateOf(
            !pagesCompleted
            // The moment it tries to show the indicator, the data gets loaded; we need to display this consistently to indicate that the data has indeed been loaded
            /* when (screenType) {
                    ScreenType.LINKS_ONLY -> {
                        linksTagsPairsState?.isRetrieving ?: false
                    }

                    ScreenType.FOLDERS_AND_LINKS -> flatChildFolderDataState?.isRetrieving ?: false
                    ScreenType.TAGS_FOLDERS_LINKS -> flatSearchResultState?.isRetrieving ?: false
                }*/
        )
    }

    val loadingIndicatorModifier = retain(isDataEmpty) {
        Modifier.padding(top = 25.dp)
            .fillMaxWidth()
            .then(
                if (!isDataEmpty) Modifier.padding(
                    start = 15.dp,
                    end = 15.dp,
                    bottom = 150.dp
                ) else Modifier.fillMaxHeight()
            )
    }

    val folderComponentParam: (folder: Folder) -> FolderComponentParam = {
        FolderComponentParam(
            name = it.name,
            note = it.note,
            onClick = { ->
                if (CollectionsScreenVM.selectedFoldersViaLongClick.contains(it)) {
                    return@FolderComponentParam
                }
                onFolderClick(it)
            },
            onLongClick = { ->
                if (CollectionsScreenVM.isSelectionEnabled.value.not()) {
                    CollectionsScreenVM.isSelectionEnabled.value = true
                    CollectionsScreenVM.selectedFoldersViaLongClick.add(it)
                }
            },
            onMoreIconClick = { ->
                folderMoreIconClick(it)
            },
            isCurrentlyInDetailsView = mutableStateOf(isCurrentlyInDetailsView(it)),
            showMoreIcon = mutableStateOf(true),
            isSelectedForSelection = mutableStateOf(
                CollectionsScreenVM.isSelectionEnabled.value && CollectionsScreenVM.selectedFoldersViaLongClick.contains(
                    it
                )
            ),
            showCheckBox = CollectionsScreenVM.isSelectionEnabled,
            onCheckBoxChanged = { bool ->
                if (bool) {
                    CollectionsScreenVM.selectedFoldersViaLongClick.add(it)
                } else {
                    CollectionsScreenVM.selectedFoldersViaLongClick.remove(
                        it
                    )
                }
            })
    }
    val tagComponentParam: (tag: Tag) -> FolderComponentParam = {
        FolderComponentParam(
            name = it.name,
            note = "",
            onClick = { ->
                onTagClick(it)
            },
            onLongClick = { },
            onMoreIconClick = { ->
                tagMoreIconClick(it)
            },
            isCurrentlyInDetailsView = mutableStateOf(true),
            showMoreIcon = mutableStateOf(true),
            isSelectedForSelection = mutableStateOf(false),
            showCheckBox = mutableStateOf(false),
            onCheckBoxChanged = {},
            leadingIcon = Icons.Default.Tag
        )
    }


    val isAtTheEnd = retain {
        derivedStateOf {
            when (AppPreferences.selectedLinkLayout.value) {
                Layout.TITLE_ONLY_LIST_VIEW.name, Layout.REGULAR_LIST_VIEW.name -> {
                    val lastVisibleItem = listLayoutState.layoutInfo.visibleItemsInfo.lastOrNull()
                    (lastVisibleItem?.index
                        ?: 0) >= listLayoutState.layoutInfo.totalItemsCount - Constants.TRIGGER_THRESHOLD_AT_THE_END
                }

                Layout.GRID_VIEW.name -> {
                    val lastVisibleItem = gridLayoutState.layoutInfo.visibleItemsInfo.lastOrNull()
                    (lastVisibleItem?.index
                        ?: 0) >= gridLayoutState.layoutInfo.totalItemsCount - Constants.TRIGGER_THRESHOLD_AT_THE_END
                }

                else -> {
                    val lastVisibleItem =
                        staggeredGridLayoutState.layoutInfo.visibleItemsInfo.lastOrNull()
                    (lastVisibleItem?.index
                        ?: 0) >= staggeredGridLayoutState.layoutInfo.totalItemsCount - Constants.TRIGGER_THRESHOLD_AT_THE_END
                }
            }
        }
    }
    LaunchedEffect(isAtTheEnd.value) {
        onRetrieveNextPage()
    }
    when (AppPreferences.selectedLinkLayout.value) {
        Layout.TITLE_ONLY_LIST_VIEW.name, Layout.REGULAR_LIST_VIEW.name -> {

            LazyColumn(
                modifier = Modifier.then(
                    if (nestedScrollConnection != null) Modifier.nestedScroll(
                        nestedScrollConnection
                    ) else Modifier
                ).padding(paddingValues).fillMaxSize(),
                state = listLayoutState
            ) {

                if (screenType == ScreenType.TAGS_FOLDERS_LINKS) {
                    flatSearchResultState?.data?.forEach { (pageKey, searchResults) ->
                        items(searchResults, key = {
                            "CollectionLayoutManager-LazyColumn-flatSearchResultState-P$pageKey-ID-L${it.linkLocalId}-F${it.folderLocalId}-T${it.tagLocalId}"
                        }) {
                            when (it.itemType) {
                                Constants.TAG -> {
                                    FolderComponent(folderComponentParam = tagComponentParam(it.asTag))
                                }

                                Constants.FOLDER -> {
                                    FolderComponent(folderComponentParam = folderComponentParam(it.asFolder))
                                }

                                else -> {
                                    ListViewLinkComponent(
                                        linkComponentParam = linkComponentParam(it.asLinkTagsPair),
                                        titleOnlyView = AppPreferences.selectedLinkLayout.value == Layout.TITLE_ONLY_LIST_VIEW.name,
                                        onShare = {
                                            LinkoraSDK.getInstance().nativeUtils.onShare(it)
                                        })
                                }
                            }
                        }
                    }
                }

                if (screenType == ScreenType.FOLDERS_AND_LINKS) {
                    flatChildFolderDataState?.data?.forEach { (pageKey, flatChildFolderData) ->
                        items(flatChildFolderData, key = {
                            "CollectionLayoutManager-LazyColumn-flatChildFolderDataState-P$pageKey-ID${it.linkLocalId}${it.folderLocalId}"
                        }) {
                            if (it.itemType == Constants.FOLDER) {
                                FolderComponent(folderComponentParam = folderComponentParam(it.asFolder))
                            } else {
                                ListViewLinkComponent(
                                    linkComponentParam = linkComponentParam(it.asLinkTagsPair),
                                    titleOnlyView = AppPreferences.selectedLinkLayout.value == Layout.TITLE_ONLY_LIST_VIEW.name,
                                    onShare = {
                                        LinkoraSDK.getInstance().nativeUtils.onShare(it)
                                    })
                            }
                        }
                    }
                }

                if (screenType == ScreenType.LINKS_ONLY) {
                    linksTagsPairsState?.data?.forEach { (pageKey, linkTagsPair) ->
                        items(items = linkTagsPair, key = {
                            "CollectionLayoutManager-LazyColumn-linksTagsPairs-P$pageKey-ID" + it.link.localId
                        }) {
                            ListViewLinkComponent(
                                linkComponentParam = linkComponentParam(it),
                                titleOnlyView = AppPreferences.selectedLinkLayout.value == Layout.TITLE_ONLY_LIST_VIEW.name,
                                onShare = {
                                    LinkoraSDK.getInstance().nativeUtils.onShare(it)
                                })
                        }
                    }
                }
                item {
                    AnimatedContent(targetState = showLoading) {
                        if (it) {
                            Box(
                                modifier = loadingIndicatorModifier,
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        } else if (isDataEmpty) {
                            DataEmptyScreen(text = emptyDataText)
                        }
                    }
                }
                item {
                    AnimatedVisibility(enter = fadeIn(), exit = fadeOut(), visible = !showLoading) {
                        Spacer(Modifier.height(bottomSpacing.value))
                    }
                }
            }

            LaunchedEffect(Unit) {
                snapshotFlow {
                    listLayoutState.firstVisibleItemIndex
                }.debounce(500).distinctUntilChanged().collectLatest {
                    onFirstVisibleItemIndexChange(it.toLong())
                }
            }
        }

        Layout.GRID_VIEW.name -> {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                modifier = Modifier.padding(paddingValues).fillMaxSize().then(
                    if (nestedScrollConnection != null) Modifier.nestedScroll(
                        nestedScrollConnection
                    ) else Modifier
                ),
                state = gridLayoutState
            ) {
                if (screenType == ScreenType.TAGS_FOLDERS_LINKS) {
                    flatSearchResultState?.data?.forEach { (pageKey, searchResults) ->
                        items(items = searchResults, span = {
                            if (it.itemType != Constants.LINK) GridItemSpan(
                                this.maxLineSpan
                            ) else GridItemSpan(
                                1
                            )
                        }, key = {
                            "CollectionLayoutManager-LazyVerticalGrid-flatSearchResultState-P$pageKey-ID-L${it.linkLocalId}-F${it.folderLocalId}-T${it.tagLocalId}"
                        }) {
                            when (it.itemType) {
                                Constants.TAG -> {
                                    FolderComponent(folderComponentParam = tagComponentParam(it.asTag))
                                }

                                Constants.FOLDER -> {
                                    FolderComponent(folderComponentParam = folderComponentParam(it.asFolder))
                                }

                                else -> {
                                    GridViewLinkComponent(
                                        linkComponentParam = linkComponentParam(it.asLinkTagsPair),
                                        forStaggeredView = false
                                    )
                                }
                            }
                        }
                    }
                }

                if (screenType == ScreenType.FOLDERS_AND_LINKS) {
                    flatChildFolderDataState?.data?.forEach { (pageIndex, folders) ->
                        items(items = folders, span = {
                            if (it.itemType == Constants.FOLDER) GridItemSpan(this.maxLineSpan) else GridItemSpan(
                                1
                            )
                        }, key = {
                            "CollectionLayoutManager-LazyVerticalGrid-flatChildFolderDataState-P$pageIndex-ID${it.linkLocalId}${it.folderLocalId}"
                        }) {
                            if (it.itemType == Constants.FOLDER) {
                                FolderComponent(folderComponentParam = folderComponentParam(it.asFolder))
                            } else {
                                GridViewLinkComponent(
                                    linkComponentParam = linkComponentParam(it.asLinkTagsPair),
                                    forStaggeredView = AppPreferences.selectedLinkLayout.value == Layout.STAGGERED_VIEW.name
                                )
                            }
                        }
                    }
                }

                if (isDataEmpty) {
                    item(span = {
                        GridItemSpan(this.maxLineSpan)
                    }) {
                        Spacer(Modifier.height(10.dp))
                    }
                }

                if (screenType == ScreenType.LINKS_ONLY) {
                    linksTagsPairsState?.data?.forEach { (pageKey, linkTagsPairs) ->
                        items(linkTagsPairs, key = {
                            "LazyVerticalGrid-linksTagsPairs-P$pageKey-ID" + it.link.localId
                        }) {
                            GridViewLinkComponent(
                                linkComponentParam = linkComponentParam(it),
                                forStaggeredView = AppPreferences.selectedLinkLayout.value == Layout.STAGGERED_VIEW.name
                            )
                        }
                    }
                }

                item(span = {
                    GridItemSpan(this.maxLineSpan)
                }) {
                    AnimatedContent(targetState = showLoading) {
                        if (it) {
                            Box(
                                modifier = loadingIndicatorModifier,
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        } else if (isDataEmpty) {
                            DataEmptyScreen(text = emptyDataText)
                        }
                    }
                }

                item(span = {
                    GridItemSpan(this.maxLineSpan)
                }) {
                    AnimatedVisibility(
                        enter = fadeIn(),
                        exit = fadeOut(),
                        visible = pagesCompleted
                    ) {
                        Spacer(Modifier.height(bottomSpacing.value))
                    }
                }
            }
            LaunchedEffect(Unit) {
                snapshotFlow {
                    gridLayoutState.firstVisibleItemIndex
                }.debounce(500).distinctUntilChanged().collectLatest {
                    onFirstVisibleItemIndexChange(it.toLong())
                }
            }
        }

        Layout.STAGGERED_VIEW.name -> {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                modifier = Modifier.padding(paddingValues).fillMaxSize().then(
                    if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier
                ),
                state = staggeredGridLayoutState
            ) {
                if (screenType == ScreenType.TAGS_FOLDERS_LINKS) {
                    flatSearchResultState?.data?.forEach { (pageKey, searchResults) ->
                        items(items = searchResults, span = {
                            if (it.itemType != Constants.LINK) {
                                StaggeredGridItemSpan.FullLine
                            } else {
                                StaggeredGridItemSpan.SingleLane
                            }
                        }, key = {
                            "CollectionLayoutManager-LazyVerticalGrid-flatSearchResultState-P$pageKey-ID-L${it.linkLocalId}-F${it.folderLocalId}-T${it.tagLocalId}"
                        }) {
                            when (it.itemType) {
                                Constants.TAG -> {
                                    FolderComponent(folderComponentParam = tagComponentParam(it.asTag))
                                }

                                Constants.FOLDER -> {
                                    FolderComponent(folderComponentParam = folderComponentParam(it.asFolder))
                                }

                                else -> {
                                    GridViewLinkComponent(
                                        linkComponentParam = linkComponentParam(it.asLinkTagsPair),
                                        forStaggeredView = false
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isDataEmpty) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(10.dp))
                    }
                }

                if (screenType == ScreenType.FOLDERS_AND_LINKS) {
                    flatChildFolderDataState?.data?.forEach { (pageKey, folders) ->
                        items(items = folders, span = {
                            if (it.itemType != Constants.LINK) {
                                StaggeredGridItemSpan.FullLine
                            } else {
                                StaggeredGridItemSpan.SingleLane
                            }
                        }, key = {
                            "CollectionLayoutManager-LazyVerticalStaggeredGrid-flatChildFolderDataState-P$pageKey-ID${it.linkLocalId}${it.folderLocalId}"
                        }) {
                            if (it.itemType == Constants.FOLDER) {
                                FolderComponent(folderComponentParam = folderComponentParam(it.asFolder))
                            } else {
                                GridViewLinkComponent(
                                    linkComponentParam = linkComponentParam(it.asLinkTagsPair),
                                    forStaggeredView = AppPreferences.selectedLinkLayout.value == Layout.STAGGERED_VIEW.name
                                )
                            }
                        }
                    }
                }

                if (isDataEmpty) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
                if (screenType == ScreenType.LINKS_ONLY) {
                    linksTagsPairsState?.data?.forEach { (pageKey, linkTagsPairs) ->
                        items(linkTagsPairs, key = {
                            "LazyVerticalStaggeredGrid-linksTagsPairs-P$pageKey-ID" + it.link.localId
                        }) {
                            GridViewLinkComponent(
                                linkComponentParam = linkComponentParam(it),
                                forStaggeredView = AppPreferences.selectedLinkLayout.value == Layout.STAGGERED_VIEW.name
                            )
                        }
                    }
                }

                item(span = StaggeredGridItemSpan.FullLine) {
                    AnimatedContent(targetState = showLoading) {
                        if (it) {
                            Box(
                                modifier = loadingIndicatorModifier,
                                contentAlignment = Alignment.Center
                            ) {
                                ContainedLoadingIndicator()
                            }
                        } else if (isDataEmpty) {
                            DataEmptyScreen(text = emptyDataText)
                        }
                    }
                }

                if (pagesCompleted) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(bottomSpacing.value))
                    }
                }
            }

            LaunchedEffect(Unit) {
                snapshotFlow {
                    staggeredGridLayoutState.firstVisibleItemIndex
                }.debounce(500).distinctUntilChanged().collectLatest {
                    onFirstVisibleItemIndexChange(it.toLong())
                }
            }
        }
    }
}