package com.sakethh.linkora.ui.screens.search

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.FolderType
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.asLocalizedString
import com.sakethh.linkora.domain.asMenuBtmSheetType
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.CollectionLayoutManager
import com.sakethh.linkora.ui.components.SortingIconButton
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.ui.domain.CurrentFABContext
import com.sakethh.linkora.ui.domain.ScreenType
import com.sakethh.linkora.ui.domain.model.CollectionDetailPaneInfo
import com.sakethh.linkora.ui.domain.model.CollectionType
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    currentFABContext: (CurrentFABContext) -> Unit,
    forceActiveSearch: Boolean,
    cancelForceSearchActive: () -> Unit,
) {
    val searchScreenVM: SearchScreenVM = linkoraViewModel()
    val preferences by searchScreenVM.preferencesAsFlow.collectAsStateWithLifecycle()
    val searchBarFocusRequester = retain {
        FocusRequester()
    }
    LaunchedEffect(Unit) {
        currentFABContext(CurrentFABContext.ROOT)
    }
    DisposableEffect(Unit) {
        onDispose {
            if (forceActiveSearch) {
                searchBarFocusRequester.freeFocus()
            }
            cancelForceSearchActive()
        }
    }
    LaunchedEffect(forceActiveSearch) {
        if (forceActiveSearch) {
            searchBarFocusRequester.requestFocus()
            searchScreenVM.isSearchActive.value = true
        }
    }
    LaunchedEffect(!searchScreenVM.isSearchActive.value) {
        cancelForceSearchActive()
    }
    val historyLinkTagsPairsState by searchScreenVM.historyLinkTagsPairsState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val localUriHandler = LocalUriHandler.current
    val navController = LocalNavController.current
    val searchBarPadding =
        animateDpAsState(if (!searchScreenVM.isSearchActive.value) 15.dp else 0.dp)
    val searchResultsState by searchScreenVM.searchResultsState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        ProvideTextStyle(MaterialTheme.typography.titleSmall) {
            SearchBar(
                query = searchScreenVM.searchQuery.value,
                onQueryChange = {
                    searchScreenVM.updateSearchQuery(it)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                placeholder = {
                    Text(
                        text = Localization.Key.SearchTitlesToFindLinksAndFolders.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.basicMarquee(),
                        maxLines = 1
                    )
                },
                modifier = Modifier.focusRequester(searchBarFocusRequester).animateContentSize()
                    .padding(searchBarPadding.value).fillMaxWidth().wrapContentHeight(),
                trailingIcon = {
                    Row {
                        if (searchScreenVM.isSearchActive.value) {
                            SortingIconButton()
                            IconButton(
                                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                    .pressScaleEffect(), onClick = {
                                    if (searchScreenVM.searchQuery.value == "") {
                                        searchScreenVM.isSearchActive.value = false
                                        cancelForceSearchActive()
                                    } else {
                                        searchScreenVM.updateSearchQuery("")
                                    }
                                }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    }
                },
                onSearch = {},
                active = searchScreenVM.isSearchActive.value,
                onActiveChange = {
                    searchScreenVM.updateSearchActiveState(it)
                }) {
                if (searchScreenVM.searchQuery.value.isBlank()) {
                    DataEmptyScreen(text = Localization.Key.SearchInLinkora.rememberLocalizedString())
                } else {
                    Column {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            FolderType.entries.forEach {
                                key(it.name) {
                                    FilterChip(
                                        text = it.asLocalizedString(),
                                        isSelected = searchScreenVM.appliedFolderFilters.contains(
                                            it
                                        ),
                                        onClick = {
                                            searchScreenVM.toggleFolderFilter(it)
                                        })
                                }
                            }
                            LinkType.entries.forEach {
                                key(it.name) {
                                    FilterChip(
                                        text = it.asLocalizedString(),
                                        isSelected = searchScreenVM.appliedLinkFilters.contains(
                                            it
                                        ),
                                        onClick = {
                                            searchScreenVM.toggleLinkFilter(it)
                                        })
                                }
                            }
                            FilterChip(
                                text = Localization.Key.Tags.rememberLocalizedString(),
                                isSelected = searchScreenVM.appliedTagFiltering,
                                onClick = {
                                    searchScreenVM.toggleTagFilter()
                                })
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        CollectionLayoutManager(
                            screenType = ScreenType.TAGS_FOLDERS_LINKS,
                            emptyDataText = Localization.Key.NoSearchResults.rememberLocalizedString(),
                            flatChildFolderDataState = null,
                            linksTagsPairsState = null,
                            flatSearchResultState = searchResultsState,
                            paddingValues = PaddingValues(0.dp),
                            folderMoreIconClick = {
                                coroutineScope.pushUIEvent(
                                    UIEvent.Type.ShowMenuBtmSheet(
                                        menuBtmSheetFor = if (it.isArchived) MenuBtmSheetType.Folder.ArchiveFolder else MenuBtmSheetType.Folder.RegularFolder,
                                        selectedLinkForMenuBtmSheet = null,
                                        selectedFolderForMenuBtmSheet = it
                                    )
                                )
                            },
                            onFolderClick = { folder ->
                                val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                    currentFolder = folder,
                                    currentTag = null,
                                    collectionType = CollectionType.FOLDER
                                )
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                    value = Json.encodeToString(collectionDetailPaneInfo)
                                )
                                navController.navigate(Navigation.Collection.MobileCollectionDetailScreen)
                            },
                            linkMoreIconClick = {
                                coroutineScope.pushUIEvent(
                                    UIEvent.Type.ShowMenuBtmSheet(
                                        menuBtmSheetFor = it.link.linkType.asMenuBtmSheetType(),
                                        selectedLinkForMenuBtmSheet = it,
                                        selectedFolderForMenuBtmSheet = null
                                    )
                                )
                            },
                            onLinkClick = {
                                localUriHandler.openUri(it.link.url)
                                searchScreenVM.addANewLinkToHistory(
                                    link = it.link.copy(
                                        linkType = LinkType.HISTORY_LINK, localId = 0
                                    ), tagIds = it.tags.map { it.localId })
                            },
                            isCurrentlyInDetailsView = {
                                false
                            },
                            nestedScrollConnection = null,
                            onAttachedTagClick = {
                                val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                    currentFolder = null,
                                    currentTag = it,
                                    collectionType = CollectionType.TAG,
                                )
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                    value = Json.encodeToString(
                                        collectionDetailPaneInfo
                                    )
                                )
                                navController.navigate(
                                    Navigation.Collection.MobileCollectionDetailScreen
                                )
                            },
                            onTagClick = {
                                val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                    currentFolder = null,
                                    currentTag = it,
                                    collectionType = CollectionType.TAG,
                                )
                                navController.currentBackStackEntry?.savedStateHandle?.set(
                                    key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                    value = Json.encodeToString(
                                        collectionDetailPaneInfo
                                    )
                                )
                                navController.navigate(
                                    Navigation.Collection.MobileCollectionDetailScreen
                                )
                            },
                            tagMoreIconClick = {
                                coroutineScope.pushUIEvent(
                                    UIEvent.Type.ShowTagMenuBtmSheet(
                                        selectedTag = it
                                    )
                                )
                            },
                            onRetrieveNextPage = searchScreenVM::retrieveNextSearchPage,
                            preferences = preferences,
                            onFirstVisibleItemIndexChange = searchScreenVM::updateFirstVisibleIndexOfSearchPaginator
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = Localization.rememberLocalizedString(Localization.Key.History),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 15.dp)
            )
            Row {
                SortingIconButton()
                Spacer(modifier = Modifier.width(5.dp))
            }
        }
        CollectionLayoutManager(
            screenType = ScreenType.LINKS_ONLY,
            emptyDataText = Localization.Key.NoHistoryFound.rememberLocalizedString(),
            flatChildFolderDataState = null,
            linksTagsPairsState = historyLinkTagsPairsState,
            paddingValues = PaddingValues(0.dp),
            folderMoreIconClick = {},
            onFolderClick = {},
            linkMoreIconClick = {
                coroutineScope.pushUIEvent(
                    UIEvent.Type.ShowMenuBtmSheet(
                        menuBtmSheetFor = MenuBtmSheetType.Link.HistoryLink,
                        selectedLinkForMenuBtmSheet = it,
                        selectedFolderForMenuBtmSheet = null
                    )
                )
            },
            onLinkClick = {
                localUriHandler.openUri(it.link.url)
                searchScreenVM.addANewLinkToHistory(
                    link = it.link.copy(linkType = LinkType.HISTORY_LINK, localId = 0),
                    tagIds = it.tags.map { it.localId })
            },
            isCurrentlyInDetailsView = {
                false
            },
            nestedScrollConnection = null,
            onAttachedTagClick = {
                val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                    currentFolder = null,
                    currentTag = it,
                    collectionType = CollectionType.TAG,
                )
                navController.currentBackStackEntry?.savedStateHandle?.set(
                    key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                    value = Json.encodeToString(
                        collectionDetailPaneInfo
                    )
                )
                navController.navigate(
                    Navigation.Collection.MobileCollectionDetailScreen
                )
            },
            tagMoreIconClick = {},
            onTagClick = {},
            onRetrieveNextPage = {
                searchScreenVM.retrieveNextBatchOfHistoryLinks()
            },
            onFirstVisibleItemIndexChange = searchScreenVM::updateStartingIndexForHistoryPaginator,
            flatSearchResultState = null,
            preferences = preferences
        )
    }
}

@Composable
fun FilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.animateContentSize()
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        FilterChip(
            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
            selected = isSelected,
            onClick = onClick,
            label = {
                Text(
                    text = text, style = MaterialTheme.typography.titleSmall
                )
            },
            leadingIcon = {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check, contentDescription = null
                    )
                }
            })
    }
}