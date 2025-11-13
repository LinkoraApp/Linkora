package com.sakethh.linkora.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.components.folder.FolderComponent
import com.sakethh.linkora.ui.components.link.GridViewLinkComponent
import com.sakethh.linkora.ui.components.link.ListViewLinkComponent
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.domain.model.FolderComponentParam
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.domain.model.LinkComponentParam
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.utils.rememberLocalizedString

@Composable
fun CollectionLayoutManager(
    folders: List<Folder>,
    linksTagsPairs: List<LinkTagsPair>,
    tags: List<Tag>,
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
    nestedScrollConnection: NestedScrollConnection?
) {
    val linkComponentParam: (linkTagsPair: LinkTagsPair) -> LinkComponentParam =
        { linkTagsPair ->
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
            onLongClick = { -> },
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

    when (AppPreferences.selectedLinkLayout.value) {
        Layout.TITLE_ONLY_LIST_VIEW.name, Layout.REGULAR_LIST_VIEW.name -> {

            LazyColumn(
                modifier = Modifier.then(
                    if (nestedScrollConnection != null) Modifier.nestedScroll(
                        nestedScrollConnection
                    ) else Modifier
                ).padding(paddingValues).fillMaxSize()
            ) {
                items(folders) {
                    FolderComponent(folderComponentParam = folderComponentParam(it))
                }

                items(tags) {
                    FolderComponent(folderComponentParam = tagComponentParam(it))
                }

                items(items = linksTagsPairs) {
                    ListViewLinkComponent(
                        linkComponentParam = linkComponentParam(it),
                        titleOnlyView = AppPreferences.selectedLinkLayout.value == Layout.TITLE_ONLY_LIST_VIEW.name,
                        onShare = {
                            LinkoraSDK.getInstance().nativeUtils.onShare(it)
                        })
                }
                if ((folders.isEmpty() && linksTagsPairs.isEmpty()) || (folders.isNotEmpty() && linksTagsPairs.isEmpty())) {
                    item {
                        val text = emptyDataText.ifBlank {
                            when {
                                folders.isEmpty() && linksTagsPairs.isEmpty() -> Localization.Key.NoFoldersOrLinksFound.rememberLocalizedString()
                                else -> Localization.Key.FoldersExistsButNotLinks.rememberLocalizedString()
                            }
                        }
                        DataEmptyScreen(text = text)
                    }
                }
                item {
                    Spacer(Modifier.height(bottomSpacing.value))
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
                )
            ) {
                items(items = folders, span = {
                    GridItemSpan(this.maxLineSpan)
                }) {
                    FolderComponent(folderComponentParam = folderComponentParam(it))
                }
                items(items = tags, span = {
                    GridItemSpan(this.maxLineSpan)
                }) {
                    FolderComponent(folderComponentParam = tagComponentParam(it))
                }

                if (folders.isNotEmpty()) {
                    item(span = {
                        GridItemSpan(this.maxLineSpan)
                    }) {
                        Spacer(Modifier.height(10.dp))
                    }
                }

                items(linksTagsPairs) {
                    GridViewLinkComponent(
                        linkComponentParam = linkComponentParam(it),
                        forStaggeredView = AppPreferences.selectedLinkLayout.value == Layout.STAGGERED_VIEW.name
                    )
                }
                item(span = {
                    GridItemSpan(this.maxLineSpan)
                }) {
                    Spacer(Modifier.height(bottomSpacing.value))
                }
            }
        }

        Layout.STAGGERED_VIEW.name -> {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                modifier = Modifier.padding(paddingValues).fillMaxSize().then(
                    if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier
                )
            ) {
                items(items = folders, span = { StaggeredGridItemSpan.FullLine }) {
                    FolderComponent(folderComponentParam = folderComponentParam(it))
                }

                if (folders.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(10.dp))
                    }
                }

                items(items = tags, span = { StaggeredGridItemSpan.FullLine }) {
                    FolderComponent(folderComponentParam = tagComponentParam(it))
                }
                if (tags.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(10.dp))
                    }
                }
                items(linksTagsPairs) {
                    GridViewLinkComponent(
                        linkComponentParam = linkComponentParam(it),
                        forStaggeredView = AppPreferences.selectedLinkLayout.value == Layout.STAGGERED_VIEW.name
                    )
                }

                item(span = StaggeredGridItemSpan.FullLine) {
                    Spacer(Modifier.height(bottomSpacing.value))
                }
            }
        }
    }
}