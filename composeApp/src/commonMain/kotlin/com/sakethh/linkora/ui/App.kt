package com.sakethh.linkora.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BackupTable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.Constants
import com.sakethh.linkora.common.utils.bottomNavPaddingAcrossPlatforms
import com.sakethh.linkora.common.utils.defaultFolderIds
import com.sakethh.linkora.common.utils.ifNot
import com.sakethh.linkora.common.utils.inRootScreen
import com.sakethh.linkora.common.utils.initializeIfServerConfigured
import com.sakethh.linkora.common.utils.isNotNull
import com.sakethh.linkora.common.utils.rememberLocalizedString
import com.sakethh.linkora.common.utils.replaceFirstPlaceHolderWith
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.components.AddANewFolderDialogBox
import com.sakethh.linkora.ui.components.AddANewLinkDialogBox
import com.sakethh.linkora.ui.components.AddItemFABParam
import com.sakethh.linkora.ui.components.AddItemFab
import com.sakethh.linkora.ui.components.DeleteDialogBox
import com.sakethh.linkora.ui.components.DeleteDialogBoxParam
import com.sakethh.linkora.ui.components.DeleteDialogBoxType
import com.sakethh.linkora.ui.components.ManageReminderBtmSheet
import com.sakethh.linkora.ui.components.RenameDialogBox
import com.sakethh.linkora.ui.components.RenameDialogBoxParam
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetParam
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetUI
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetVM
import com.sakethh.linkora.ui.components.menu.menuBtmSheetFolderEntries
import com.sakethh.linkora.ui.components.sorting.SortingBottomSheetParam
import com.sakethh.linkora.ui.components.sorting.SortingBottomSheetUI
import com.sakethh.linkora.ui.domain.ScreenType
import com.sakethh.linkora.ui.domain.SortingBtmSheetType
import com.sakethh.linkora.ui.domain.TransferActionType
import com.sakethh.linkora.ui.domain.model.AddNewFolderDialogBoxParam
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.CollectionDetailPane
import com.sakethh.linkora.ui.screens.collections.CollectionsScreen
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.screens.home.HomeScreen
import com.sakethh.linkora.ui.screens.home.panels.PanelsManagerScreen
import com.sakethh.linkora.ui.screens.home.panels.SpecificPanelManagerScreen
import com.sakethh.linkora.ui.screens.onboarding.OnboardingSlidesScreen
import com.sakethh.linkora.ui.screens.search.SearchScreen
import com.sakethh.linkora.ui.screens.settings.SettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.AcknowledgementSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.AdvancedSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.LanguageSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.LayoutSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.ThemeSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.about.AboutSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.data.sync.ServerSetupScreen
import com.sakethh.linkora.ui.screens.settings.section.general.GeneralSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.general.reminders.RemindersSettingsScreen
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.genericViewModelFactory
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import com.sakethh.linkora.ui.utils.rememberDeserializableObject
import com.sakethh.platform
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    modifier: Modifier = Modifier
) {
    val appVM = viewModel<AppVM>(factory = genericViewModelFactory {
        AppVM(
            remoteSyncRepo = DependencyContainer.remoteSyncRepo.value,
            preferencesRepository = DependencyContainer.preferencesRepo.value,
            networkRepo = DependencyContainer.networkRepo.value,
            linksRepo = DependencyContainer.localLinksRepo.value,
            foldersRepo = DependencyContainer.localFoldersRepo.value,
            localMultiActionRepo = DependencyContainer.localMultiActionRepo.value,
            localPanelsRepo = DependencyContainer.localPanelsRepo.value,
            exportDataRepo = DependencyContainer.exportDataRepo.value,
            reminderRepo = DependencyContainer.remindersRepo.value,
        )
    })
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    val showRenameDialogBox = rememberSaveable {
        mutableStateOf(false)
    }
    val showDeleteDialogBox = rememberSaveable {
        mutableStateOf(false)
    }
    val menuBtmModalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selectedFolderForMenuBtmSheet = rememberDeserializableMutableObject {
        mutableStateOf(
            Folder(
                name = "", note = "", parentFolderId = null, localId = 0L, isArchived = false
            )
        )
    }
    val selectedLinkForMenuBtmSheet = rememberDeserializableMutableObject {
        mutableStateOf(
            Link(
                linkType = LinkType.SAVED_LINK,
                localId = 0L,
                title = "",
                url = "",
                baseURL = "",
                imgURL = "",
                note = "",
                idOfLinkedFolder = null,
                userAgent = null
            )
        )
    }
    val menuBtmModalSheetVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldShowAddLinkDialog = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldShowNewFolderDialog = rememberSaveable {
        mutableStateOf(false)
    }
    val sortingBottomSheetVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val sortingBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val menuBtmSheetFor: MutableState<MenuBtmSheetType> = rememberDeserializableMutableObject {
        mutableStateOf(MenuBtmSheetType.Folder.RegularFolder)
    }

    val menuBtmSheetVM: MenuBtmSheetVM = viewModel(factory = genericViewModelFactory {
        MenuBtmSheetVM(DependencyContainer.localLinksRepo.value)
    })

    LaunchedEffect(Unit) {
        UIEvent.uiEvents.collectLatest { eventType ->
            when (eventType) {
                is UIEvent.Type.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = eventType.message)
                }

                is UIEvent.Type.ShowAddANewFolderDialogBox -> shouldShowNewFolderDialog.value = true
                is UIEvent.Type.ShowAddANewLinkDialogBox -> shouldShowAddLinkDialog.value = true
                is UIEvent.Type.ShowDeleteDialogBox -> showDeleteDialogBox.value = true

                is UIEvent.Type.ShowMenuBtmSheetUI -> {
                    menuBtmSheetFor.value = eventType.menuBtmSheetFor
                    if (eventType.selectedFolderForMenuBtmSheet.isNotNull()) {
                        menuBtmSheetVM.updateArchiveFolderCardData(eventType.selectedFolderForMenuBtmSheet!!.isArchived)
                        selectedFolderForMenuBtmSheet.value =
                            eventType.selectedFolderForMenuBtmSheet
                    }
                    if (eventType.selectedLinkForMenuBtmSheet.isNotNull()) {
                        menuBtmSheetVM.updateImpLinkInfo(eventType.selectedLinkForMenuBtmSheet!!.url)
                        menuBtmSheetVM.updateArchiveLinkInfo(eventType.selectedLinkForMenuBtmSheet.url)
                        selectedLinkForMenuBtmSheet.value = eventType.selectedLinkForMenuBtmSheet
                    }
                    menuBtmModalSheetVisible.value = true
                    this.launch {
                        menuBtmModalSheetState.show()
                    }
                }

                is UIEvent.Type.ShowRenameDialogBox -> showRenameDialogBox.value = true

                is UIEvent.Type.ShowSortingBtmSheetUI -> {
                    sortingBottomSheetVisible.value = true
                    this.launch {
                        sortingBtmSheetState.show()
                    }
                }

                else -> Unit
            }
        }
    }
    val collectionsScreenVM = viewModel<CollectionsScreenVM>(factory = genericViewModelFactory {
        CollectionsScreenVM(
            DependencyContainer.localFoldersRepo.value, DependencyContainer.localLinksRepo.value
        )
    })
    val rootRouteList = rememberDeserializableObject {
        listOf(
            Navigation.Root.HomeScreen,
            Navigation.Root.SearchScreen,
            Navigation.Root.CollectionsScreen,
            Navigation.Root.SettingsScreen,
        )
    }
    val localNavController = LocalNavController.current
    val inRootScreen = localNavController.inRootScreen(includeSettingsScreen = true)
    val currentBackStackEntryState = localNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntryState.value?.destination
    val standardBottomSheet =
        rememberStandardBottomSheetState(skipHiddenState = false, initialValue = SheetValue.Hidden)
    val scaffoldSheetState =
        rememberBottomSheetScaffoldState(bottomSheetState = standardBottomSheet)

    val rotationAnimation = remember {
        Animatable(0f)
    }
    val isReducedTransparencyBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val showBtmSheetForNewLinkAddition = rememberSaveable {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    val showAddingLinkOrFoldersFAB = rememberSaveable {
        mutableStateOf(false)
    }
    val platform = LocalPlatform.current
    LaunchedEffect(
        key1 = currentBackStackEntryState.value,
        key2 = CollectionsScreenVM.collectionDetailPaneInfo.value,
        key3 = CollectionsScreenVM.isSelectionEnabled.value
    ) {
        launch {
            if (rootRouteList.any {
                    currentBackStackEntryState.value?.destination?.hasRoute(it::class) == true
                } && CollectionsScreenVM.isSelectionEnabled.value.not()) {
                scaffoldSheetState.bottomSheetState.expand()
            } else {
                scaffoldSheetState.bottomSheetState.hide()
            }
        }
        launch {
            if (platform is Platform.Android.Mobile && currentBackStackEntryState.value?.destination?.hasRoute(
                    Navigation.Root.CollectionsScreen::class
                ) == true
            ) {
                CollectionsScreenVM.resetCollectionDetailPaneInfo()
            }
            if (currentBackStackEntryState.value?.destination?.hasRoute(Navigation.Root.CollectionsScreen::class) == true && platform !is Platform.Android.Mobile && CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId == Constants.ARCHIVE_ID) {
                showAddingLinkOrFoldersFAB.value = false
            }
            showAddingLinkOrFoldersFAB.value = listOf(
                Navigation.Root.HomeScreen,
                Navigation.Root.SearchScreen,
                Navigation.Root.CollectionsScreen,
                Navigation.Collection.CollectionDetailPane
            ).any {
                currentBackStackEntryState.value?.destination?.hasRoute(it::class) == true
            } && CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId != Constants.ARCHIVE_ID
        }
    }
    LaunchedEffect(AppVM.isMainFabRotated.value) {
        if (AppVM.isMainFabRotated.value.not()) {
            isReducedTransparencyBoxVisible.value = false
            rotationAnimation.animateTo(
                -180f, animationSpec = tween(500)
            )
        }
    }
    Row(modifier = Modifier.fillMaxSize().then(modifier)) {
        if (appVM.onBoardingCompleted.value && (platform() == Platform.Desktop || platform() == Platform.Android.Tablet)) {
            Row {
                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        rootRouteList.forEach { navRouteItem ->
                            if (AppPreferences.isHomeScreenEnabled.value.not() && navRouteItem.toString() == Navigation.Root.HomeScreen.toString()) return@forEach

                            val isSelected = currentRoute?.hasRoute(navRouteItem::class) == true
                            NavigationRailItem(
                                modifier = Modifier.padding(
                                start = 15.dp, end = 15.dp, top = 15.dp
                            ), selected = isSelected, onClick = {
                                if (currentRoute?.hasRoute(navRouteItem::class) == false) {
                                    CollectionsScreenVM.resetCollectionDetailPaneInfo()
                                    localNavController.navigate(navRouteItem)
                                }
                            }, icon = {
                                Icon(
                                    imageVector = if (isSelected) {
                                        when (navRouteItem) {
                                            Navigation.Root.HomeScreen -> Icons.Filled.Home
                                            Navigation.Root.SearchScreen -> Icons.Filled.Search
                                            Navigation.Root.CollectionsScreen -> Icons.Filled.Folder
                                            Navigation.Root.SettingsScreen -> Icons.Filled.Settings
                                            else -> return@NavigationRailItem
                                        }
                                    } else {
                                        when (navRouteItem) {
                                            Navigation.Root.HomeScreen -> Icons.Outlined.Home
                                            Navigation.Root.SearchScreen -> Icons.Outlined.Search
                                            Navigation.Root.CollectionsScreen -> Icons.Outlined.Folder
                                            Navigation.Root.SettingsScreen -> Icons.Outlined.Settings
                                            else -> return@NavigationRailItem
                                        }
                                    }, contentDescription = null
                                )
                            }, label = {
                                Text(
                                    text = navRouteItem.toString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            })
                        }
                    }
                    Box(
                        Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter
                    ) {
                        if (platform() !is Platform.Android.Mobile && appVM.isPerformingStartupSync.value) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.align(Alignment.BottomCenter)
                                    .padding(bottom = 90.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync, contentDescription = null
                                )
                                CircularProgressIndicator()
                            }
                        }

                        if (AppPreferences.areSnapshotsEnabled.value && platform == Platform.Desktop) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.align(Alignment.BottomCenter)
                                    .padding(bottom = 30.dp)
                                    .alpha(if (platform() !is Platform.Android.Mobile && appVM.isAnySnapshotOngoing.value) 1f else 0.25f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BackupTable,
                                    contentDescription = null
                                )

                                if (appVM.isAnySnapshotOngoing.value) CircularProgressIndicator()
                            }
                        }
                    }
                }
                VerticalDivider()
            }
        }
        val showLoadingProgressBarOnTransferAction = rememberSaveable {
            mutableStateOf(false)
        }
        val selectedAndInRoot = rememberSaveable(inRootScreen, appVM.transferActionType.value) {
            mutableStateOf((inRootScreen == true) && (appVM.transferActionType.value != TransferActionType.NONE))
        }
        Scaffold(
            bottomBar = {
            Box(modifier = Modifier.animateContentSize()) {
                if (CollectionsScreenVM.isSelectionEnabled.value) {
                    Column(
                        modifier = Modifier.fillMaxWidth().animateContentSize()
                            .background(if (inRootScreen == true) BottomAppBarDefaults.containerColor else TopAppBarDefaults.topAppBarColors().containerColor)
                    ) {
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(5.dp))
                        if (showLoadingProgressBarOnTransferAction.value) {
                            Text(
                                text = if (appVM.transferActionType.value == TransferActionType.COPY) {
                                    Localization.Key.Copying.rememberLocalizedString()
                                } else {
                                    Localization.Key.Moving.rememberLocalizedString()
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(
                                    start = 15.dp, bottom = 10.dp, top = 5.dp
                                )
                            )
                            LinearProgressIndicator(
                                Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp)
                            )
                            Spacer(Modifier.bottomNavPaddingAcrossPlatforms())
                            return@Column
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = {
                                CollectionsScreenVM.clearAllSelections()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Close, contentDescription = null
                                )
                            }
                            Column {
                                Text(
                                    text = Localization.Key.SelectedLinksCount.rememberLocalizedString()
                                        .replaceFirstPlaceHolderWith(CollectionsScreenVM.selectedLinksViaLongClick.size.toString()),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = Localization.Key.SelectedFoldersCount.rememberLocalizedString()
                                        .replaceFirstPlaceHolderWith(CollectionsScreenVM.selectedFoldersViaLongClick.size.toString()),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                        val showPasteButton =
                            appVM.transferActionType.value != TransferActionType.NONE && (selectedAndInRoot.value.not() || CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder != null) && CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId != Constants.ALL_LINKS_ID
                        if ((CollectionsScreenVM.selectedFoldersViaLongClick.isNotEmpty() && CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId in defaultFolderIds().dropWhile {
                                it == Constants.ARCHIVE_ID
                            }).not()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if ((appVM.transferActionType.value == TransferActionType.NONE && showPasteButton.not()) || (appVM.transferActionType.value != TransferActionType.NONE && showPasteButton)) {
                                    Text(
                                        text = Localization.Key.MultiActionsLabel.rememberLocalizedString(),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(start = 15.dp)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.animateContentSize()
                                ) {
                                    if (showPasteButton) {
                                        IconButton(onClick = {
                                            if (CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder == null) {
                                                return@IconButton
                                            }
                                            if (appVM.transferActionType.value == TransferActionType.COPY) {
                                                appVM.copySelectedItems(
                                                    folderId = CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId!!,
                                                    onStart = {
                                                        showLoadingProgressBarOnTransferAction.value =
                                                            true
                                                    },
                                                    onCompletion = {
                                                        showLoadingProgressBarOnTransferAction.value =
                                                            false
                                                    })
                                            } else {
                                                appVM.moveSelectedItems(
                                                    folderId = CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId!!,
                                                    onStart = {
                                                        showLoadingProgressBarOnTransferAction.value =
                                                            true
                                                    },
                                                    onCompletion = {
                                                        showLoadingProgressBarOnTransferAction.value =
                                                            false
                                                    })
                                            }
                                        }, modifier = Modifier.padding(end = 6.5.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = null
                                            )
                                        }
                                        return@Row
                                    }
                                    if (appVM.transferActionType.value != TransferActionType.NONE) {
                                        return@Row
                                    }
                                    IconButton(onClick = {
                                        coroutineScope.pushUIEvent(UIEvent.Type.ShowDeleteDialogBox)
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null
                                        )
                                    }
                                    if (CollectionsScreenVM.selectedLinksViaLongClick.any {
                                            it.linkType == LinkType.ARCHIVE_LINK
                                        }
                                            .not() || CollectionsScreenVM.selectedFoldersViaLongClick.any {
                                            it.isArchived.not()
                                        }) {
                                        IconButton(onClick = {
                                            appVM.archiveSelectedItems(onStart = {
                                                showLoadingProgressBarOnTransferAction.value = true
                                            }, onCompletion = {
                                                showLoadingProgressBarOnTransferAction.value = false
                                            })
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    if (CollectionsScreenVM.selectedFoldersViaLongClick.any {
                                            it.isArchived
                                        } || CollectionsScreenVM.selectedLinksViaLongClick.any {
                                            it.linkType == LinkType.ARCHIVE_LINK
                                        }) {
                                        IconButton(onClick = {
                                            appVM.markSelectedItemsAsRegular(onStart = {
                                                showLoadingProgressBarOnTransferAction.value = true
                                            }, onCompletion = {
                                                showLoadingProgressBarOnTransferAction.value = false
                                            })
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.Unarchive,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        appVM.transferActionType.value = TransferActionType.COPY
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.CopyAll,
                                            contentDescription = null
                                        )
                                    }
                                    IconButton(onClick = {
                                        appVM.transferActionType.value = TransferActionType.MOVE
                                    }) {
                                        Icon(
                                            imageVector = Icons.Outlined.DriveFileMove,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                        if (appVM.transferActionType.value != TransferActionType.NONE) {
                            Text(
                                text = if (appVM.transferActionType.value == TransferActionType.COPY) Localization.Key.NavigateAndCopyDesc.rememberLocalizedString() else Localization.Key.NavigateAndMoveDesc.rememberLocalizedString(),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(start = 15.dp, end = 15.dp)
                            )
                        }
                        val showNavigateToCollectionScreen =
                            selectedAndInRoot.value && currentRoute?.hasRoute(Navigation.Root.CollectionsScreen::class) != true && CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId != Constants.ALL_LINKS_ID
                        if (CollectionsScreenVM.selectedFoldersViaLongClick.isNotEmpty() && CollectionsScreenVM.selectedFoldersViaLongClick.any {
                                it.parentFolderId != null
                            }) {
                            Button(
                                onClick = {
                                    appVM.markSelectedFoldersAsRoot(onStart = {
                                        showLoadingProgressBarOnTransferAction.value = true
                                    }, onCompletion = {
                                        showLoadingProgressBarOnTransferAction.value = false
                                    })
                                }, modifier = Modifier.fillMaxWidth().padding(
                                    start = 15.dp,
                                    end = 15.dp,
                                    top = 5.dp,
                                    bottom = if (showNavigateToCollectionScreen.not()) 5.dp else 0.dp
                                )
                            ) {
                                Text(
                                    text = Localization.Key.MarkSelectedFoldersAsRoot.rememberLocalizedString(),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                        if (showNavigateToCollectionScreen) {
                            Button(
                                onClick = {
                                    localNavController.navigate(Navigation.Root.CollectionsScreen)
                                }, modifier = Modifier.fillMaxWidth().padding(
                                    start = 15.dp, end = 15.dp, top = 5.dp, bottom = 5.dp
                                )
                            ) {
                                Text(
                                    text = Localization.Key.NavigateToCollectionsScreen.rememberLocalizedString(),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        }, floatingActionButton = {
            AnimatedVisibility(
                enter = fadeIn(),
                exit = fadeOut(),
                visible = showAddingLinkOrFoldersFAB.value && CollectionsScreenVM.isSelectionEnabled.value.not()
            ) {
                if (CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId in listOf(
                        Constants.SAVED_LINKS_ID,
                        Constants.IMPORTANT_LINKS_ID,
                        Constants.ALL_LINKS_ID
                    )
                ) {
                    FloatingActionButton(
                        modifier = Modifier.padding(
                            bottom = if (platform() == Platform.Android.Mobile && rootRouteList.any {
                                    currentRoute?.hasRoute(it::class) == true
                                }) 82.dp else 0.dp
                        ), onClick = {
                            shouldShowAddLinkDialog.value = true
                        }) {
                        Icon(
                            imageVector = Icons.Default.AddLink, contentDescription = null
                        )
                    }
                    return@AnimatedVisibility
                }
                AddItemFab(
                    AddItemFABParam(
                        showBtmSheetForNewLinkAddition = showBtmSheetForNewLinkAddition,
                        isReducedTransparencyBoxVisible = isReducedTransparencyBoxVisible,
                        showDialogForNewFolder = shouldShowNewFolderDialog,
                        shouldShowAddLinkDialog = shouldShowAddLinkDialog,
                        isMainFabRotated = AppVM.isMainFabRotated,
                        rotationAnimation = rotationAnimation,
                        inASpecificScreen = false
                    )
                )
            }
        }, snackbarHost = {
            SnackbarHost(snackbarHostState, snackbar = {
                Snackbar(
                    it,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            })
        }, modifier = Modifier.fillMaxSize()
        ) {
            BottomSheetScaffold(
                sheetDragHandle = {},
                sheetPeekHeight = 0.dp,
                scaffoldState = scaffoldSheetState,
                sheetSwipeEnabled = false,
                sheetShape = RectangleShape,
                sheetContent = {
                    if (platform == Platform.Android.Mobile) {
                        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                            if (appVM.isPerformingStartupSync.value) {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .windowInsetsPadding(NavigationBarDefaults.windowInsets)
                                    .background(NavigationBarDefaults.containerColor),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rootRouteList.forEach { navRouteItem ->
                                    if (AppPreferences.isHomeScreenEnabled.value.not() && navRouteItem.toString() == Navigation.Root.HomeScreen.toString()) return@forEach

                                    val isSelected =
                                        currentRoute?.hasRoute(navRouteItem::class) == true
                                    NavigationBarItem(selected = isSelected, onClick = {
                                        isSelected.ifNot {
                                            CollectionsScreenVM.resetCollectionDetailPaneInfo()
                                            localNavController.navigate(navRouteItem)
                                        }
                                    }, icon = {
                                        Icon(
                                            imageVector = if (isSelected) {
                                                when (navRouteItem) {
                                                    Navigation.Root.HomeScreen -> Icons.Filled.Home
                                                    Navigation.Root.SearchScreen -> Icons.Filled.Search
                                                    Navigation.Root.CollectionsScreen -> Icons.Filled.Folder
                                                    Navigation.Root.SettingsScreen -> Icons.Filled.Settings
                                                    else -> return@NavigationBarItem
                                                }
                                            } else {
                                                when (navRouteItem) {
                                                    Navigation.Root.HomeScreen -> Icons.Outlined.Home
                                                    Navigation.Root.SearchScreen -> Icons.Outlined.Search
                                                    Navigation.Root.CollectionsScreen -> Icons.Outlined.Folder
                                                    Navigation.Root.SettingsScreen -> Icons.Outlined.Settings
                                                    else -> return@NavigationBarItem
                                                }
                                            }, contentDescription = null
                                        )
                                    }, label = {
                                        Text(
                                            text = navRouteItem.toString(),
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    })
                                }
                            }

                        }
                    }
                }) {
                NavHost(
                    navController = localNavController,
                    startDestination = appVM.startDestination.value
                ) {
                    composable<Navigation.Root.HomeScreen> {
                        HomeScreen()
                    }
                    composable<Navigation.Root.SearchScreen> {
                        SearchScreen()
                    }
                    composable<Navigation.Root.CollectionsScreen> {
                        CollectionsScreen(
                            collectionsScreenVM = collectionsScreenVM
                        )
                    }
                    composable<Navigation.Root.SettingsScreen> {
                        SettingsScreen()
                    }
                    composable<Navigation.Settings.ThemeSettingsScreen> {
                        ThemeSettingsScreen()
                    }
                    composable<Navigation.Settings.GeneralSettingsScreen> {
                        GeneralSettingsScreen()
                    }
                    composable<Navigation.Settings.LayoutSettingsScreen> {
                        LayoutSettingsScreen()
                    }
                    composable<Navigation.Settings.DataSettingsScreen> {
                        DataSettingsScreen()
                    }
                    composable<Navigation.Settings.Data.ServerSetupScreen> {
                        ServerSetupScreen()
                    }
                    composable<Navigation.Settings.LanguageSettingsScreen> {
                        LanguageSettingsScreen()
                    }
                    composable<Navigation.Collection.CollectionDetailPane> {
                        CollectionDetailPane()
                    }
                    composable<Navigation.Home.PanelsManagerScreen> {
                        PanelsManagerScreen()
                    }
                    composable<Navigation.Home.SpecificPanelManagerScreen> {
                        SpecificPanelManagerScreen()
                    }
                    composable<Navigation.Settings.AboutSettingsScreen> {
                        AboutSettingsScreen()
                    }
                    composable<Navigation.Settings.AcknowledgementSettingsScreen> {
                        AcknowledgementSettingsScreen()
                    }
                    composable<Navigation.Settings.AdvancedSettingsScreen> {
                        AdvancedSettingsScreen()
                    }
                    composable<Navigation.Root.OnboardingSlidesScreen> {
                        OnboardingSlidesScreen(onOnboardingComplete = {
                            appVM.markOnboardingComplete()
                        })
                    }
                    composable<Navigation.Settings.General.RemindersSettingsScreen> { navBackStackEntry ->
                        RemindersSettingsScreen()
                    }
                }
            }

            AnimatedVisibility(
                visible = isReducedTransparencyBoxVisible.value, enter = fadeIn(), exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(0.95f)).clickable {
                            AppVM.isMainFabRotated.value = false
                        })
            }
            AddANewLinkDialogBox(
                shouldBeVisible = shouldShowAddLinkDialog,
                screenType = ScreenType.ROOT_SCREEN,
                currentFolder = if ((inRootScreen == true && platform is Platform.Android.Mobile) || CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId == Constants.ALL_LINKS_ID) null else CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder,
                collectionsScreenVM
            )

            AddANewFolderDialogBox(
                AddNewFolderDialogBoxParam(
                    shouldBeVisible = shouldShowNewFolderDialog,
                    inAChildFolderScreen = CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId != null && CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId!! > 0,
                    onFolderCreateClick = { folderName, folderNote, onCompletion ->
                        if (menuBtmSheetFolderEntries().contains(menuBtmSheetFor.value)) {
                            collectionsScreenVM.insertANewFolder(
                                folder = Folder(
                                    name = folderName,
                                    note = folderNote,
                                    parentFolderId = if ((CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId
                                            ?: 0) > 0
                                    ) CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder?.localId else null
                                ), ignoreFolderAlreadyExistsThrowable = false, onCompletion = {
                                    collectionsScreenVM.triggerFoldersSorting()
                                    onCompletion()
                                })
                        }
                    },
                    thisFolder = CollectionsScreenVM.collectionDetailPaneInfo.value.currentFolder
                )
            )
            val localUriHandler = LocalUriHandler.current
            val showProgressBarDuringRemoteSave = rememberSaveable {
                mutableStateOf(false)
            }
            val manageReminderBtmSheetState =
                rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val isManageReminderBtmSheetVisible = rememberSaveable { mutableStateOf(false) }
            MenuBtmSheetUI(
                menuBtmSheetParam = MenuBtmSheetParam(
                    btmModalSheetState = menuBtmModalSheetState,
                    shouldBtmModalSheetBeVisible = menuBtmModalSheetVisible,
                    menuBtmSheetFor = menuBtmSheetFor.value,
                    onDelete = {
                        showDeleteDialogBox.value = true
                    },
                    onRename = {
                        showRenameDialogBox.value = true
                    },
                    onArchive = {
                        initializeIfServerConfigured {
                            showProgressBarDuringRemoteSave.value = true
                        }
                        if (menuBtmSheetFolderEntries().contains(menuBtmSheetFor.value)) {
                            collectionsScreenVM.archiveAFolder(
                                selectedFolderForMenuBtmSheet.value, onCompletion = {
                                    showProgressBarDuringRemoteSave.value = false
                                    coroutineScope.launch {
                                        menuBtmModalSheetState.hide()
                                    }.invokeOnCompletion {
                                        menuBtmModalSheetVisible.value = false
                                    }
                                    collectionsScreenVM.triggerFoldersSorting()
                                })
                        } else {
                            collectionsScreenVM.archiveALink(
                                selectedLinkForMenuBtmSheet.value, onCompletion = {
                                    showProgressBarDuringRemoteSave.value = false
                                    coroutineScope.launch {
                                        menuBtmModalSheetState.hide()
                                    }.invokeOnCompletion {
                                        menuBtmModalSheetVisible.value = false
                                    }
                                    collectionsScreenVM.triggerLinksSorting()
                                })
                        }
                    },
                    onDeleteNote = {
                        initializeIfServerConfigured {
                            showProgressBarDuringRemoteSave.value = true
                        }
                        if (menuBtmSheetFolderEntries().contains(menuBtmSheetFor.value)) {
                            collectionsScreenVM.deleteTheNote(
                                selectedFolderForMenuBtmSheet.value, onCompletion = {
                                    showProgressBarDuringRemoteSave.value = false
                                    coroutineScope.launch {
                                        menuBtmModalSheetState.hide()
                                    }.invokeOnCompletion {
                                        menuBtmModalSheetVisible.value = false
                                    }
                                })
                        } else {
                            collectionsScreenVM.deleteTheNote(
                                selectedLinkForMenuBtmSheet.value, onCompletion = {
                                    showProgressBarDuringRemoteSave.value = false
                                    coroutineScope.launch {
                                        menuBtmModalSheetState.hide()
                                    }.invokeOnCompletion {
                                        menuBtmModalSheetVisible.value = false
                                    }
                                })
                        }
                    },
                    onRefreshClick = {
                        initializeIfServerConfigured {
                            showProgressBarDuringRemoteSave.value = true
                        }
                        collectionsScreenVM.refreshLinkMetadata(
                            selectedLinkForMenuBtmSheet.value, onCompletion = {
                                showProgressBarDuringRemoteSave.value = false
                                coroutineScope.launch {
                                    menuBtmModalSheetState.hide()
                                }.invokeOnCompletion {
                                    menuBtmModalSheetVisible.value = false
                                }
                            })
                    },
                    onForceLaunchInAnExternalBrowser = {
                        localUriHandler.openUri(selectedLinkForMenuBtmSheet.value.url)
                    },
                    showQuickActions = rememberSaveable { mutableStateOf(false) },
                    shouldTransferringOptionShouldBeVisible = true,
                    link = selectedLinkForMenuBtmSheet,
                    folder = selectedFolderForMenuBtmSheet,
                    onAddToImportantLinks = {
                        initializeIfServerConfigured {
                            showProgressBarDuringRemoteSave.value = true
                        }
                        collectionsScreenVM.markALinkAsImp(
                            selectedLinkForMenuBtmSheet.value, onCompletion = {
                                showProgressBarDuringRemoteSave.value = false
                                coroutineScope.launch {
                                    menuBtmModalSheetState.hide()
                                }.invokeOnCompletion {
                                    menuBtmModalSheetVisible.value = false
                                }
                            })
                    },
                    shouldShowArchiveOption = {
                        menuBtmSheetVM.shouldShowArchiveOption(selectedLinkForMenuBtmSheet.value.url)
                    },
                    showProgressBarDuringRemoteSave = showProgressBarDuringRemoteSave,
                    onManageLinkReminders = {
                        isManageReminderBtmSheetVisible.value = true
                        coroutineScope.launch {
                            manageReminderBtmSheetState.show()
                        }
                    })
            )
            DeleteDialogBox(
                DeleteDialogBoxParam(
                    showDeleteDialogBox,
                    if (CollectionsScreenVM.isSelectionEnabled.value) DeleteDialogBoxType.SELECTED_DATA else if (menuBtmSheetFolderEntries().contains(
                            menuBtmSheetFor.value
                        )
                    ) {
                        DeleteDialogBoxType.FOLDER
                    } else DeleteDialogBoxType.LINK,
                    onDeleteClick = { onCompletion, _ ->
                        if (CollectionsScreenVM.isSelectionEnabled.value) {
                            appVM.deleteSelectedItems(onStart = {}, onCompletion)
                            return@DeleteDialogBoxParam
                        }
                        if (menuBtmSheetFolderEntries().contains(menuBtmSheetFor.value)) {
                            collectionsScreenVM.deleteAFolder(
                                selectedFolderForMenuBtmSheet.value, onCompletion = {
                                    coroutineScope.launch {
                                        menuBtmModalSheetState.hide()
                                    }.invokeOnCompletion {
                                        menuBtmModalSheetVisible.value = false
                                    }
                                    collectionsScreenVM.triggerFoldersSorting()
                                    onCompletion()
                                })
                        } else {
                            collectionsScreenVM.deleteALink(
                                selectedLinkForMenuBtmSheet.value, onCompletion = {
                                    coroutineScope.launch {
                                        menuBtmModalSheetState.hide()
                                    }.invokeOnCompletion {
                                        menuBtmModalSheetVisible.value = false
                                    }
                                    collectionsScreenVM.triggerLinksSorting()
                                    onCompletion()
                                })
                        }
                    })
            )
            RenameDialogBox(
                RenameDialogBoxParam(
                    onNoteChangeClick = { newNote, onCompletion ->
                        if (menuBtmSheetFolderEntries().contains(menuBtmSheetFor.value)) {
                            collectionsScreenVM.updateFolderNote(
                                selectedFolderForMenuBtmSheet.value.localId,
                                newNote = newNote,
                                onCompletion = {
                                    onCompletion()
                                    showRenameDialogBox.value = false
                                })
                        } else {
                            collectionsScreenVM.updateLinkNote(
                                selectedLinkForMenuBtmSheet.value.localId,
                                newNote = newNote,
                                onCompletion = {
                                    onCompletion()
                                    showRenameDialogBox.value = false
                                })
                        }
                    },
                    shouldDialogBoxAppear = showRenameDialogBox,
                    existingFolderName = selectedFolderForMenuBtmSheet.value.name,
                    onBothTitleAndNoteChangeClick = { title, note, onCompletion ->
                        if (menuBtmSheetFor.value in menuBtmSheetFolderEntries()) {
                            collectionsScreenVM.updateFolderNote(
                                selectedFolderForMenuBtmSheet.value.localId,
                                newNote = note,
                                pushSnackbarOnSuccess = false,
                                onCompletion = {
                                    onCompletion()
                                    showRenameDialogBox.value = false
                                })
                            collectionsScreenVM.updateFolderName(
                                folder = selectedFolderForMenuBtmSheet.value,
                                newName = title,
                                ignoreFolderAlreadyExistsThrowable = true,
                                onCompletion = {
                                    onCompletion()
                                    collectionsScreenVM.triggerFoldersSorting()
                                    showRenameDialogBox.value = false
                                })
                        } else {
                            collectionsScreenVM.updateLinkNote(
                                linkId = selectedLinkForMenuBtmSheet.value.localId,
                                newNote = note,
                                pushSnackbarOnSuccess = false,
                                onCompletion = {
                                    onCompletion()
                                    showRenameDialogBox.value = false
                                })
                            collectionsScreenVM.updateLinkTitle(
                                linkId = selectedLinkForMenuBtmSheet.value.localId,
                                newTitle = title,
                                onCompletion = {
                                    onCompletion()
                                    collectionsScreenVM.triggerLinksSorting()
                                    showRenameDialogBox.value = false
                                })
                        }
                        coroutineScope.launch {
                            menuBtmModalSheetState.hide()
                        }.invokeOnCompletion {
                            menuBtmModalSheetVisible.value = false
                        }
                    },
                    existingTitle = if (menuBtmSheetFolderEntries().contains(menuBtmSheetFor.value)) selectedFolderForMenuBtmSheet.value.name else selectedLinkForMenuBtmSheet.value.title,
                    existingNote = if (menuBtmSheetFolderEntries().contains(menuBtmSheetFor.value)) selectedFolderForMenuBtmSheet.value.note else selectedLinkForMenuBtmSheet.value.note
                )
            )

            SortingBottomSheetUI(
                SortingBottomSheetParam(
                    shouldBottomSheetBeVisible = sortingBottomSheetVisible,
                    onSelected = { sortingPreferences, _, _ -> },
                    bottomModalSheetState = sortingBtmSheetState,
                    sortingBtmSheetType = SortingBtmSheetType.COLLECTIONS_SCREEN,
                    shouldFoldersSelectionBeVisible = mutableStateOf(false),
                    shouldLinksSelectionBeVisible = mutableStateOf(false)
                )
            )
            ManageReminderBtmSheet(
                isVisible = isManageReminderBtmSheetVisible,
                btmSheetState = manageReminderBtmSheetState,
                link = selectedLinkForMenuBtmSheet.value,
                onSaveClick = { title, description, selectedReminderType, selectedReminderMode, datePickerState, timePickerState,linkView ->
                    appVM.scheduleAReminder(
                        linkId = selectedLinkForMenuBtmSheet.value.localId,
                        title = title,
                        description = description,
                        timePickerState = timePickerState,
                        datePickerState = datePickerState,
                        reminderMode = selectedReminderMode,
                        reminderType = selectedReminderType,
                        linkView = linkView
                    )
                })
        }
    }
}