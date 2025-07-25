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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.rememberLocalizedString
import com.sakethh.linkora.common.utils.then
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.components.folder.FolderComponent
import com.sakethh.linkora.ui.components.link.GridViewLinkUIComponent
import com.sakethh.linkora.ui.components.link.LinkListItemComposable
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.domain.model.FolderComponentParam
import com.sakethh.linkora.ui.domain.model.LinkUIComponentParam
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM

@Composable
fun CollectionLayoutManager(
    folders: List<Folder>,
    links: List<Link>,
    paddingValues: PaddingValues,
    folderMoreIconClick: (folder: Folder) -> Unit,
    onFolderClick: (folder: Folder) -> Unit,
    linkMoreIconClick: (link: Link) -> Unit,
    onLinkClick: (link: Link) -> Unit,
    isCurrentlyInDetailsView: (folder: Folder) -> Boolean,
    emptyDataText: String = "",
    nestedScrollConnection: NestedScrollConnection?
) {
    val linkUIComponentParam: (link: Link) -> LinkUIComponentParam = {
        LinkUIComponentParam(
            link = it,
            isSelectionModeEnabled = CollectionsScreenVM.isSelectionEnabled,
            onMoreIconClick = {
                linkMoreIconClick(it)
            },
            onLinkClick = {
                if (CollectionsScreenVM.isSelectionEnabled.value.not()) {
                    onLinkClick(it)
                } else {
                    if (CollectionsScreenVM.selectedLinksViaLongClick.contains(it)) {
                        CollectionsScreenVM.selectedLinksViaLongClick.remove(it)
                    } else {
                        CollectionsScreenVM.selectedLinksViaLongClick.add(it)
                    }
                }
            },
            onForceOpenInExternalBrowserClicked = {

            },
            isItemSelected = mutableStateOf(
                CollectionsScreenVM.selectedLinksViaLongClick.contains(
                    it
                )
            ),
            onLongClick = {
                if (CollectionsScreenVM.isSelectionEnabled.value.not()) {
                    CollectionsScreenVM.isSelectionEnabled.value = true
                    CollectionsScreenVM.selectedLinksViaLongClick.add(it)
                }
            })
    }
    val bottomSpacing = remember {
        mutableStateOf(250.dp)
    }
    val folderComponentParam: (folder: Folder) -> FolderComponentParam = {
        FolderComponentParam(
            folder = it,
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

    when (AppPreferences.currentlySelectedLinkLayout.value) {
        Layout.TITLE_ONLY_LIST_VIEW.name, Layout.REGULAR_LIST_VIEW.name -> {

            LazyColumn(modifier = Modifier.then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier).padding(paddingValues).fillMaxSize()) {
                items(folders) {
                    FolderComponent(folderComponentParam = folderComponentParam(it))
                }

                items(items = links) {
                    LinkListItemComposable(
                        linkUIComponentParam = linkUIComponentParam(it),
                        forTitleOnlyView = AppPreferences.currentlySelectedLinkLayout.value == Layout.TITLE_ONLY_LIST_VIEW.name
                    )
                }
                if ((folders.isEmpty() && links.isEmpty()) || (folders.isNotEmpty() && links.isEmpty())) {
                    item {
                        val text = emptyDataText.ifBlank {
                            when {
                                folders.isEmpty() && links.isEmpty() -> Localization.Key.NoFoldersOrLinksFound.rememberLocalizedString()
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
                modifier = Modifier.padding(paddingValues).fillMaxSize()
                    .then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier)
            ) {
                items(items = folders, span = {
                    GridItemSpan(this.maxLineSpan)
                }) {
                    FolderComponent(folderComponentParam = folderComponentParam(it))
                }

                if (folders.isNotEmpty()) {
                    item(span = {
                        GridItemSpan(this.maxLineSpan)
                    }) {
                        Spacer(Modifier.height(10.dp))
                    }
                }

                items(links) {
                    GridViewLinkUIComponent(
                        linkUIComponentParam = linkUIComponentParam(it),
                        forStaggeredView = AppPreferences.currentlySelectedLinkLayout.value == Layout.STAGGERED_VIEW.name
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
                modifier = Modifier.padding(paddingValues).fillMaxSize().then(if (nestedScrollConnection != null) Modifier.nestedScroll(nestedScrollConnection) else Modifier)
            ) {
                items(items = folders, span = { StaggeredGridItemSpan.Companion.FullLine }) {
                    FolderComponent(folderComponentParam = folderComponentParam(it))
                }

                if (folders.isNotEmpty()) {
                    item(span = StaggeredGridItemSpan.Companion.FullLine) {
                        Spacer(Modifier.height(10.dp))
                    }
                }

                items(links) {
                    GridViewLinkUIComponent(
                        linkUIComponentParam = linkUIComponentParam(it),
                        forStaggeredView = AppPreferences.currentlySelectedLinkLayout.value == Layout.STAGGERED_VIEW.name
                    )
                }

                item(span = StaggeredGridItemSpan.Companion.FullLine) {
                    Spacer(Modifier.height(bottomSpacing.value))
                }
            }
        }
    }
}