package com.sakethh.linkora.ui.screens.search

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.utils.rememberLocalizedString
import com.sakethh.linkora.domain.asHistoryLinkWithoutId
import com.sakethh.linkora.domain.asLocalizedString
import com.sakethh.linkora.domain.asMenuBtmSheetType
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.CollectionLayoutManager
import com.sakethh.linkora.ui.components.SortingIconButton
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.ui.domain.model.CollectionDetailPaneInfo
import com.sakethh.linkora.ui.domain.model.SearchNavigated
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.genericViewModelFactory
import com.sakethh.linkora.ui.utils.pulsateEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    val searchScreenVM: SearchScreenVM = viewModel(factory = genericViewModelFactory {
        SearchScreenVM(
            DependencyContainer.localFoldersRepo.value, DependencyContainer.localLinksRepo.value
        )
    })

    val historyLinks = searchScreenVM.links.collectAsStateWithLifecycle()
    val searchQueryLinkResults = searchScreenVM.linkQueryResults.collectAsStateWithLifecycle()
    val searchQueryFolderResults = searchScreenVM.folderQueryResults.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val localUriHandler = LocalUriHandler.current
    val navController = LocalNavController.current
    Column(modifier = Modifier.fillMaxSize()) {
        ProvideTextStyle(MaterialTheme.typography.titleSmall) {
            SearchBar(
                query = searchScreenVM.searchQuery.value, onQueryChange = {
                    searchScreenVM.updateSearchQuery(it)
                }, leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                }, placeholder = {
                    Text(
                        text = Localization.Key.SearchTitlesToFindLinksAndFolders.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.basicMarquee(),
                        maxLines = 1
                    )
                }, modifier = Modifier.animateContentSize().padding(
                    if (searchScreenVM.isSearchActive.value.not()) 15.dp else 0.dp
                ).fillMaxWidth().wrapContentHeight(), trailingIcon = {
                    Row {
                        if (searchScreenVM.isSearchActive.value) {
                            SortingIconButton()
                            IconButton(modifier = Modifier.pulsateEffect(), onClick = {
                                if (searchScreenVM.searchQuery.value == "") {
                                    searchScreenVM.isSearchActive.value = false
                                } else {
                                    searchScreenVM.updateSearchQuery("")
                                }
                            }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    }
                }, onSearch = {

                }, active = searchScreenVM.isSearchActive.value, onActiveChange = {
                    searchScreenVM.updateSearchActiveState(it)
                }) {
                if (searchScreenVM.searchQuery.value.isBlank()) {
                    DataEmptyScreen(text = "Search Linkora: Browse through all your saved links and folders.")
                } else {
                    Column {
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            searchScreenVM.availableFolderFilters.forEach {
                                FilterChip(
                                    text = it.asLocalizedString(),
                                    isSelected = searchScreenVM.appliedFolderFilters.contains(
                                        it
                                    ),
                                    onClick = {
                                        searchScreenVM.toggleFolderFilter(it)
                                    })
                            }
                            searchScreenVM.availableLinkFilters.forEach {
                                FilterChip(
                                    text = it.asLocalizedString(),
                                    isSelected = searchScreenVM.appliedLinkFilters.contains(
                                        it
                                    ),
                                    onClick = {
                                        searchScreenVM.toggleLinkFilter(it)
                                    })
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        CollectionLayoutManager(
                            emptyDataText = "Nothing matched your search. Remember, you can search both by title and note. Give it another try!",
                            folders = searchQueryFolderResults.value,
                            links = searchQueryLinkResults.value,
                            isInSelectionMode = mutableStateOf(false),
                            paddingValues = PaddingValues(0.dp),
                            folderMoreIconClick = {
                                coroutineScope.pushUIEvent(
                                    UIEvent.Type.ShowMenuBtmSheetUI(
                                        menuBtmSheetFor = if (it.isArchived) MenuBtmSheetType.Folder.ArchiveFolder else MenuBtmSheetType.Folder.RegularFolder,
                                        selectedLinkForMenuBtmSheet = null,
                                        selectedFolderForMenuBtmSheet = it
                                    )
                                )
                            },
                            onFolderClick = { folder ->
                                val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                    currentFolder = folder,
                                    isAnyCollectionSelected = true,
                                    searchNavigated = SearchNavigated(
                                        navigatedFromSearchScreen = true,
                                        navigatedWithFolderId = folder.localId
                                    )
                                )
                                CollectionsScreenVM.updateCollectionDetailPaneInfo(
                                    collectionDetailPaneInfo
                                )
                                navController.navigate(Navigation.Collection.CollectionDetailPane)
                            },
                            linkMoreIconClick = {
                                coroutineScope.pushUIEvent(
                                    UIEvent.Type.ShowMenuBtmSheetUI(
                                        menuBtmSheetFor = it.linkType.asMenuBtmSheetType(),
                                        selectedLinkForMenuBtmSheet = it,
                                        selectedFolderForMenuBtmSheet = null
                                    )
                                )
                            },
                            onLinkClick = {
                                localUriHandler.openUri(it.url)
                                searchScreenVM.addANewLinkToHistory(
                                    link = it.asHistoryLinkWithoutId()
                                )
                            },
                            isCurrentlyInDetailsView = {
                                false
                            })
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
            SortingIconButton()
        }
        CollectionLayoutManager(
            emptyDataText = "No history found. Your history is clean!",
            folders = emptyList(),
            links = historyLinks.value,
            isInSelectionMode = mutableStateOf(false),
            paddingValues = PaddingValues(0.dp),
            folderMoreIconClick = {},
            onFolderClick = {},
            linkMoreIconClick = {
                coroutineScope.pushUIEvent(
                    UIEvent.Type.ShowMenuBtmSheetUI(
                        menuBtmSheetFor = MenuBtmSheetType.Link.HistoryLink,
                        selectedLinkForMenuBtmSheet = it,
                        selectedFolderForMenuBtmSheet = null
                    )
                )
            },
            onLinkClick = {
                localUriHandler.openUri(it.url)
                searchScreenVM.addANewLinkToHistory(
                    link = it.asHistoryLinkWithoutId()
                )
            },
            isCurrentlyInDetailsView = {
                false
            })
    }
}

@Composable
fun FilterChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.animateContentSize()
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        androidx.compose.material3.FilterChip(selected = isSelected, onClick = onClick, label = {
            Text(
                text = text, style = MaterialTheme.typography.titleSmall
            )
        }, leadingIcon = {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check, contentDescription = null
                )
            }
        })
    }
}