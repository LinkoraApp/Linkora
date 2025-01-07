package com.sakethh.linkora.ui.screens.collections

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DatasetLinked
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakethh.linkora.Platform
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.utils.Constants
import com.sakethh.linkora.common.utils.rememberLocalizedString
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.ui.components.AddANewFolderDialogBox
import com.sakethh.linkora.ui.components.AddANewLinkDialogBox
import com.sakethh.linkora.ui.components.AddItemFABParam
import com.sakethh.linkora.ui.components.AddItemFab
import com.sakethh.linkora.ui.components.DataDialogBoxType
import com.sakethh.linkora.ui.components.DeleteDialogBox
import com.sakethh.linkora.ui.components.DeleteDialogBoxParam
import com.sakethh.linkora.ui.components.RenameDialogBox
import com.sakethh.linkora.ui.components.RenameDialogBoxParam
import com.sakethh.linkora.ui.components.folder.FolderComponent
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetParam
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetUI
import com.sakethh.linkora.ui.components.menu.MenuItemType
import com.sakethh.linkora.ui.domain.ScreenType
import com.sakethh.linkora.ui.domain.model.AddNewFolderDialogBoxParam
import com.sakethh.linkora.ui.domain.model.FolderComponentParam
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.utils.genericViewModelFactory
import com.sakethh.linkora.ui.utils.pulsateEffect
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import com.sakethh.platform
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
@Composable
fun CollectionsScreen() {
    val collectionsScreenVM = viewModel<CollectionsScreenVM>(factory = genericViewModelFactory {
        CollectionsScreenVM(DependencyContainer.localFoldersRepo.value)
    })
    val rootFolders = collectionsScreenVM.rootFolders.collectAsStateWithLifecycle()
    val shouldRenameDialogBoxBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldDeleteDialogBoxBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    val btmModalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedFolder = rememberDeserializableMutableObject {
        mutableStateOf(
            Folder(
                name = "", note = "", parentFolderId = null, id = 0L, isArchived = false
            )
        )
    }
    val btmModalSheetStateForSavingLinks = rememberModalBottomSheetState()
    val shouldMenuBtmModalSheetBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val isMainFabRotated = rememberSaveable {
        mutableStateOf(false)
    }
    val rotationAnimation = remember {
        Animatable(0f)
    }
    val shouldScreenTransparencyDecreasedBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val areFoldersSelectable = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldShowAddLinkDialog = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldShowNewFolderDialog = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldSortingBottomSheetAppear = rememberSaveable {
        mutableStateOf(false)
    }
    val sortingBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shouldBtmSheetForNewLinkAdditionBeEnabled = rememberSaveable {
        mutableStateOf(false)
    }

    val listDetailPaneNavigator = rememberListDetailPaneScaffoldNavigator<Folder>()
    Scaffold(
        floatingActionButton = {
            AddItemFab(
                AddItemFABParam(
                    newLinkBottomModalSheetState = btmModalSheetStateForSavingLinks,
                    shouldBtmSheetForNewLinkAdditionBeEnabled = shouldBtmSheetForNewLinkAdditionBeEnabled,
                    shouldScreenTransparencyDecreasedBoxVisible = shouldScreenTransparencyDecreasedBoxVisible,
                    shouldDialogForNewFolderAppear = shouldShowNewFolderDialog,
                    shouldDialogForNewLinkAppear = shouldShowAddLinkDialog,
                    isMainFabRotated = isMainFabRotated,
                    rotationAnimation = rotationAnimation,
                    inASpecificScreen = false
                )
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        topBar = {
            Column {
                TopAppBar(actions = {}, navigationIcon = {}, title = {
                    Text(
                        text = Navigation.Root.CollectionsScreen.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 18.sp
                    )
                })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.25f))
            }
        }) { padding ->
        ListDetailPaneScaffold(
            modifier = Modifier.padding(padding).fillMaxSize(),
            directive = listDetailPaneNavigator.scaffoldDirective,
            value = listDetailPaneNavigator.scaffoldValue,
            listPane = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        DefaultFolderComponent(
                            name = Localization.rememberLocalizedString(Localization.Key.AllLinks),
                            icon = Icons.Outlined.DatasetLinked,
                            onClick = {
                                listDetailPaneNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail, Folder(
                                        name = Localization.getLocalizedString(Localization.Key.AllLinks),
                                        id = Constants.ALL_LINKS_ID,
                                        note = "",
                                        parentFolderId = null
                                    )
                                )
                            },
                            listDetailPaneNavigator.currentDestination?.content?.id == Constants.ALL_LINKS_ID
                        )
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                top = 15.dp, start = 25.dp, end = 5.dp
                            ),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(0.25f)
                        )
                    }
                    item {
                        DefaultFolderComponent(
                            name = Localization.rememberLocalizedString(Localization.Key.SavedLinks),
                            icon = Icons.Outlined.Link,
                            onClick = { ->
                                listDetailPaneNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail, Folder(
                                        name = Localization.getLocalizedString(Localization.Key.SavedLinks),
                                        id = Constants.SAVED_LINKS_ID,
                                        note = "",
                                        parentFolderId = null
                                    )
                                )
                            },
                            listDetailPaneNavigator.currentDestination?.content?.id == Constants.SAVED_LINKS_ID
                        )
                    }
                    item {
                        DefaultFolderComponent(
                            name = Localization.rememberLocalizedString(Localization.Key.ImportantLinks),
                            icon = Icons.Outlined.StarOutline,
                            onClick = { ->
                                listDetailPaneNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail, Folder(
                                        name = Localization.getLocalizedString(Localization.Key.ImportantLinks),
                                        id = Constants.IMPORTANT_LINKS_ID,
                                        note = "",
                                        parentFolderId = null
                                    )
                                )
                            },
                            isSelected = listDetailPaneNavigator.currentDestination?.content?.id == Constants.IMPORTANT_LINKS_ID
                        )
                    }
                    item {
                        DefaultFolderComponent(
                            name = Localization.rememberLocalizedString(Localization.Key.Archive),
                            icon = Icons.Outlined.Archive,
                            onClick = { ->
                                listDetailPaneNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail, Folder(
                                        name = Localization.getLocalizedString(Localization.Key.Archive),
                                        id = Constants.ARCHIVE_ID,
                                        note = "",
                                        parentFolderId = null
                                    )
                                )
                            },
                            isSelected = listDetailPaneNavigator.currentDestination?.content?.id == Constants.ARCHIVE_ID
                        )

                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = 20.dp, top = 15.dp, bottom = 10.dp
                            ),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(0.25f)
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = Localization.rememberLocalizedString(Localization.Key.Folders),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(start = 15.dp)
                            )
                            IconButton(modifier = Modifier.pulsateEffect(), onClick = {

                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                    items(rootFolders.value) { folder ->
                        FolderComponent(
                            FolderComponentParam(
                                folder = folder,
                                onClick = { ->
                                    listDetailPaneNavigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail, folder
                                    )
                                },
                                onLongClick = { -> },
                                onMoreIconClick = { ->
                                    selectedFolder.value = folder
                                    shouldMenuBtmModalSheetBeVisible.value = true
                                },
                                isCurrentlyInDetailsView = remember(listDetailPaneNavigator.currentDestination?.content?.id) {
                                    mutableStateOf(listDetailPaneNavigator.currentDestination?.content?.id == folder.id)
                                },
                                showMoreIcon = rememberSaveable {
                                    mutableStateOf(true)
                                })
                        )
                    }
                    item {
                        Spacer(Modifier.height(150.dp))
                    }
                }
            },
            detailPane = {
                Row(modifier = Modifier.fillMaxSize()) {
                    VerticalDivider()
                    if (listDetailPaneNavigator.currentDestination?.content == null) {
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
                            folder = listDetailPaneNavigator.currentDestination?.content!!,
                            paneNavigator = listDetailPaneNavigator
                        )
                    }
                }
            })
        if (shouldScreenTransparencyDecreasedBoxVisible.value) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(0.95f)).clickable {
                        shouldScreenTransparencyDecreasedBoxVisible.value = false
                        coroutineScope.launch {
                            awaitAll(async {
                                rotationAnimation.animateTo(
                                    -360f, animationSpec = tween(300)
                                )
                            }, async { isMainFabRotated.value = false })
                        }.invokeOnCompletion {
                            coroutineScope.launch {
                                rotationAnimation.snapTo(0f)
                            }
                        }
                    })
        }
    }

    AddANewLinkDialogBox(
        shouldBeVisible = shouldShowAddLinkDialog,
        screenType = ScreenType.ROOT_SCREEN,
        currentFolder = null
    )

    AddANewFolderDialogBox(
        AddNewFolderDialogBoxParam(
            shouldBeVisible = shouldShowNewFolderDialog,
            inAChildFolderScreen = listDetailPaneNavigator.currentDestination?.content?.id != null && listDetailPaneNavigator.currentDestination?.content?.id!! > 0,
            onFolderCreateClick = { folderName, folderNote, onCompletion ->
                collectionsScreenVM.insertANewFolder(
                    folder = Folder(
                        name = folderName,
                        note = folderNote,
                        parentFolderId = if ((listDetailPaneNavigator.currentDestination?.content?.id
                                ?: 0) > 0
                        ) listDetailPaneNavigator.currentDestination?.content?.id else null
                    ),
                    ignoreFolderAlreadyExistsThrowable = false,
                    onCompletion = onCompletion
                )
            },
            thisFolder = listDetailPaneNavigator.currentDestination?.content
        )
    )
    MenuBtmSheetUI(
        menuBtmSheetParam = MenuBtmSheetParam(
            onMove = {

            }, onCopy = {

            },
            btmModalSheetState = btmModalSheetState,
            shouldBtmModalSheetBeVisible = shouldMenuBtmModalSheetBeVisible,
            btmSheetFor = MenuItemType.FOLDER,
            onDelete = {
                shouldDeleteDialogBoxBeVisible.value = true
            }, onRename = {
                shouldRenameDialogBoxBeVisible.value = true
            }, onArchive = {
                collectionsScreenVM.archiveAFolder(selectedFolder.value)
            }, noteForSaving = selectedFolder.value.note, onDeleteNote = {
                collectionsScreenVM.deleteTheNote(selectedFolder.value)
            },
            linkTitle = "", folderName = selectedFolder.value.name,
            imgLink = "",
            onRefreshClick = {},
            webUrl = "", forceBrowserLaunch = { },
            showQuickActions = rememberSaveable { mutableStateOf(false) },
            shouldTransferringOptionShouldBeVisible = true,
            imgUserAgent = ""
        )
    )
    DeleteDialogBox(
        DeleteDialogBoxParam(
            shouldDeleteDialogBoxBeVisible, DataDialogBoxType.FOLDER, onDeleteClick = {
                collectionsScreenVM.deleteAFolder(selectedFolder.value)
            })
    )
    RenameDialogBox(
        RenameDialogBoxParam(
            onNoteChangeClick = {
                collectionsScreenVM.updateFolderNote(selectedFolder.value.id, newNote = it)
                shouldRenameDialogBoxBeVisible.value = false
            },
            shouldDialogBoxAppear = shouldRenameDialogBoxBeVisible,
            existingFolderName = selectedFolder.value.name,
            onBothTitleAndNoteChangeClick = { title, note ->
                collectionsScreenVM.updateFolderNote(
                    selectedFolder.value.id, newNote = note, pushSnackbarOnSuccess = false
                )
                collectionsScreenVM.updateFolderName(
                    folder = selectedFolder.value,
                    newName = title,
                    ignoreFolderAlreadyExistsThrowable = true
                )
                shouldRenameDialogBoxBeVisible.value = false
            },
            existingTitle = selectedFolder.value.name,
            existingNote = selectedFolder.value.note
        )
    )
}


@Composable
private fun DefaultFolderComponent(
    name: String, icon: ImageVector, onClick: () -> Unit, isSelected: Boolean
) {
    Card(
        modifier = Modifier.padding(
            end = if (platform() == Platform.Android.Mobile) 20.dp else 0.dp,
            start = 20.dp,
            top = 15.dp
        ).wrapContentHeight().fillMaxWidth().clickable(interactionSource = remember {
            MutableInteractionSource()
        }, indication = null, onClick = {
            onClick()
        }).pulsateEffect().then(
            if (isSelected) Modifier.border(
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