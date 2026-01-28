package com.sakethh.linkora.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sakethh.linkora.di.APPVMAssistedFactory
import com.sakethh.linkora.di.CollectionScreenVMAssistedFactory
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.preferences.AppPreferences.serverBaseUrl
import com.sakethh.linkora.ui.components.AddANewFolderDialogBox
import com.sakethh.linkora.ui.components.AddANewLinkDialogBox
import com.sakethh.linkora.ui.components.AddItemFABParam
import com.sakethh.linkora.ui.components.AddItemFab
import com.sakethh.linkora.ui.components.BottomNavOnSelection
import com.sakethh.linkora.ui.components.CreateATagBtmSheet
import com.sakethh.linkora.ui.components.DeleteDialogBoxType
import com.sakethh.linkora.ui.components.DeleteFolderOrLinkDialog
import com.sakethh.linkora.ui.components.DeleteFolderOrLinkDialogParam
import com.sakethh.linkora.ui.components.DesktopNavigationRail
import com.sakethh.linkora.ui.components.MobileBottomNavBar
import com.sakethh.linkora.ui.components.RenameFolderOrLinkDialog
import com.sakethh.linkora.ui.components.RenameFolderOrLinkDialogParam
import com.sakethh.linkora.ui.components.menu.MenuBtmSheet
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetParam
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.ui.components.menu.menuBtmSheetFolderEntries
import com.sakethh.linkora.ui.components.sorting.SortingBottomSheet
import com.sakethh.linkora.ui.components.sorting.SortingBottomSheetParam
import com.sakethh.linkora.ui.domain.FABContext
import com.sakethh.linkora.ui.domain.SortingBtmSheetType
import com.sakethh.linkora.ui.domain.TransferActionType
import com.sakethh.linkora.ui.domain.model.AddNewFolderDialogBoxParam
import com.sakethh.linkora.ui.domain.model.AddNewLinkDialogParams
import com.sakethh.linkora.ui.domain.model.CollectionDetailPaneInfo
import com.sakethh.linkora.ui.domain.model.CollectionType
import com.sakethh.linkora.ui.navigation.LinkoraNavHost
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.CollectionDetailPaneParams
import com.sakethh.linkora.ui.screens.collections.CollectionScreenParams
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.screens.collections.components.RenameTagComponent
import com.sakethh.linkora.ui.screens.collections.components.TagDeletionConfirmation
import com.sakethh.linkora.ui.screens.collections.components.TagMenu
import com.sakethh.linkora.ui.utils.rememberDeserializableObject
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.currentSavedServerConfig
import com.sakethh.linkora.utils.ifServerConfigured
import com.sakethh.linkora.utils.inRootScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    modifier: Modifier = Modifier
) {
    val appVM: AppVM =
        linkoraViewModel(factory = APPVMAssistedFactory.createForApp(LocalDensity.current))
    val createTagBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTagDeletionConfirmation by rememberSaveable {
        mutableStateOf(false)
    }
    var showTagRenameComponent by rememberSaveable {
        mutableStateOf(false)
    }
    val tagMenuBtmSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val collectionsScreenVM: CollectionsScreenVM =
        linkoraViewModel(factory = CollectionScreenVMAssistedFactory.createForApp(platform = platform()))
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
    val currentBackStackEntryState by localNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntryState?.destination

    val rotationAnimatable = remember {
        Animatable(0f)
    }
    val isReducedTransparencyBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    val platform = LocalPlatform.current

    val isDataSyncingFromPullRefresh = rememberSaveable {
        mutableStateOf(false)
    }
    val pullToRefreshState = rememberPullToRefreshState()
    LaunchedEffect(AppVM.isMainFabRotated.value) {
        if (!AppVM.isMainFabRotated.value) {
            isReducedTransparencyBoxVisible.value = false
            rotationAnimatable.animateTo(
                -180f, animationSpec = tween(500)
            )
        }
    }
    val currentFABContext by appVM.currentContextOfFAB
    var forceSearchActive by rememberSaveable {
        mutableStateOf(false)
    }
    Row(modifier = Modifier.fillMaxSize().then(modifier)) {
        if (appVM.onBoardingCompleted.value && (platform() == Platform.Desktop || platform() == Platform.Android.Tablet)) {
            DesktopNavigationRail(
                rootRouteList = rootRouteList,
                currentRoute = currentRoute,
                isDataSyncingFromPullRefresh = isDataSyncingFromPullRefresh,
                onNavigate = {
                    collectionsScreenVM.clearDetailPaneHistory()
                },
                isPerformingStartupSync = appVM.isPerformingStartupSync,
                getLastSyncedTime = {
                    appVM.getLastSyncedTime()
                },
                isAnySnapshotOngoing = appVM.isAnySnapshotOngoing,
                performAction = appVM::performAppAction
            )
        }
        var showLoadingProgressBarOnTransferAction by rememberSaveable {
            mutableStateOf(false)
        }
        val selectedAndInRoot = rememberSaveable(inRootScreen, appVM.transferActionType.value) {
            mutableStateOf((inRootScreen == true) && (appVM.transferActionType.value != TransferActionType.NONE))
        }

        Scaffold(
            bottomBar = {
                Box(modifier = Modifier.animateContentSize()) {
                    if (CollectionsScreenVM.isSelectionEnabled.value) {
                        BottomNavOnSelection(
                            showLoadingProgressBarOnTransferAction = {
                            showLoadingProgressBarOnTransferAction = true
                        },
                            hideLoadingProgressBarOnTransferAction = {
                                showLoadingProgressBarOnTransferAction = false
                            },
                            selectedAndInRoot = selectedAndInRoot,
                            currentRoute = currentRoute,
                            progressBarVisible = showLoadingProgressBarOnTransferAction,
                            currentFABContext = currentFABContext,
                            transferActionType = appVM.transferActionType.value,
                            changeTransferActionType = {
                                appVM.transferActionType.value = it
                            },
                            performAction = appVM::performAppAction
                        )
                    }
                }
                MobileBottomNavBar(
                    rootRouteList = rootRouteList,
                    isPerformingStartupSync = appVM.isPerformingStartupSync,
                    inRootScreen = inRootScreen,
                    navDestination = currentRoute,
                    onDoubleTap = { navigationRoot ->
                        forceSearchActive = navigationRoot is Navigation.Root.SearchScreen
                    })
            },
            floatingActionButton = {
                AnimatedVisibility(
                    enter = fadeIn(),
                    exit = fadeOut(),
                    visible = currentRoute?.hasRoute<Navigation.Root.SettingsScreen>() == false && !currentRoute.hasRoute<Navigation.Home.PanelsManagerScreen>() && currentFABContext.fabContext != FABContext.HIDE && !CollectionsScreenVM.isSelectionEnabled.value,
                ) {
                    if (currentFABContext.fabContext == FABContext.ADD_LINK_IN_FOLDER) {
                        FloatingActionButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                            onClick = {
                                appVM.showAddLinkDialog = true
                            }) {
                            Icon(
                                imageVector = Icons.Default.AddLink, contentDescription = null
                            )
                        }
                        return@AnimatedVisibility
                    }
                    AddItemFab(
                        AddItemFABParam(
                            isReducedTransparencyBoxVisible = isReducedTransparencyBoxVisible.value,
                            onShowDialogForNewFolder = {
                                appVM.showNewFolderDialog = true
                            },
                            onShowAddLinkDialog = {
                                appVM.showAddLinkDialog = true
                            },
                            isMainFabRotated = AppVM.isMainFabRotated.value,
                            rotationAnimatable = rotationAnimatable,
                            inASpecificScreen = false,
                            onCreateATagClick = {
                                appVM.showBtmSheetForNewTagAddition = true
                            },
                            hideReducedTransparencyBox = {
                                isReducedTransparencyBoxVisible.value = false
                            },
                            undoMainFabRotation = {
                                AppVM.isMainFabRotated.value = false
                            },
                            showReducedTransparencyBox = {
                                isReducedTransparencyBoxVisible.value = true
                            },
                            rotateMainFab = {
                                AppVM.isMainFabRotated.value = true
                            })
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(appVM.snackbarHostState, snackbar = {
                    Snackbar(
                        it,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                })
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier.pullToRefresh(
                    isRefreshing = isDataSyncingFromPullRefresh.value,
                    state = pullToRefreshState,
                    enabled = rememberSaveable(serverBaseUrl.value) {
                        serverBaseUrl.value.isNotBlank() && platform == Platform.Android.Mobile
                    },
                    onRefresh = {
                        appVM.saveServerConnectionAndSync(
                            serverConnection = currentSavedServerConfig(),
                            timeStampAfter = {
                                appVM.getLastSyncedTime()
                            },
                            onSyncStart = {
                                isDataSyncingFromPullRefresh.value = true
                            },
                            onCompletion = {
                                isDataSyncingFromPullRefresh.value = false
                            })
                    })
            ) {
                LinkoraNavHost(
                    startDestination = appVM.startDestination.value,
                    onOnboardingComplete = appVM::markOnboardingComplete,
                    currentFABContext = {
                        appVM.updateFABContext(it)
                    },
                    forceSearchActive = forceSearchActive,
                    cancelForceSearchActive = {
                        forceSearchActive = false
                    },
                    collectionScreenParams = CollectionScreenParams(
                        isPaneSelected = collectionsScreenVM.isPaneSelected,
                        rootRegularFolders = collectionsScreenVM.rootRegularFolders,
                        allTags = collectionsScreenVM.allTags,
                        peekPaneHistory = collectionsScreenVM.peekPaneHistory,
                        currentCollectionSource = collectionsScreenVM.currentCollectionSource,
                        performAction = collectionsScreenVM::performAction,
                        onRetrieveNextRegularRootFolderPage = collectionsScreenVM::retrieveNextBatchOfRegularRootFolders,
                        onArchivedRootFolderFirstVisibleItemIndexChange = collectionsScreenVM::updateStartingIndexForArchivedRootFoldersPaginator,
                        onRegularRootFolderFirstVisibleItemIndexChange = collectionsScreenVM::updateStartingIndexForRegularRootFoldersPaginator,
                        onRetrieveNextArchivedRootFolderPage = collectionsScreenVM::retrieveNextBatchOfArchivedRootFolders,
                        onRetrieveNextTagsPage = collectionsScreenVM::retrieveNextBatchOfTags,
                        onTagsFirstVisibleItemIndexChange = collectionsScreenVM::updateStartingIndexForTagsPaginator,
                    ),
                    collectionDetailPaneParams = CollectionDetailPaneParams(
                        linkTagsPairs = collectionsScreenVM.linkTagsPairsState,
                        childFoldersFlat = collectionsScreenVM.childFoldersFlat,
                        rootArchiveFolders = collectionsScreenVM.rootArchiveFolders,
                        collectionDetailPaneInfo = collectionsScreenVM.collectionDetailPaneInfo,
                        peekPaneHistory = collectionsScreenVM.peekPaneHistory,
                        appliedFiltersForAllLinks = collectionsScreenVM.appliedFiltersForAllLinks,
                        performAction = collectionsScreenVM::performAction
                    )
                )
                Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isDataSyncingFromPullRefresh.value,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
            AnimatedVisibility(
                visible = isReducedTransparencyBoxVisible.value, enter = fadeIn(), exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(0.95f)).clickable {
                            AppVM.isMainFabRotated.value = false
                        })
            }
            if (appVM.showAddLinkDialog) {
                AddANewLinkDialogBox(
                    addNewLinkDialogParams = AddNewLinkDialogParams(
                        onDismiss = {
                            appVM.showAddLinkDialog = false
                        },
                        currentFolder = if ((inRootScreen == true && platform is Platform.Android.Mobile) || currentFABContext.currentFolder?.localId == Constants.ALL_LINKS_ID) null else currentFABContext.currentFolder,
                        allTags = collectionsScreenVM.allTags,
                        selectedTags = collectionsScreenVM.selectedTags,
                        foldersSearchQuery = collectionsScreenVM.foldersSearchQuery,
                        foldersSearchQueryResult = collectionsScreenVM.foldersSearchQueryResult,
                        rootRegularFolders = collectionsScreenVM.rootRegularFolders,
                        performAction = collectionsScreenVM::performAction,
                    ),
                )
            }

            if (appVM.showNewFolderDialog) {
                AddANewFolderDialogBox(
                    AddNewFolderDialogBoxParam(
                        onDismiss = {
                            appVM.showNewFolderDialog = false
                        },
                        inCollectionDetailPane = currentFABContext.currentFolder != null,
                        onFolderCreateClick = { folderName, folderNote, onCompletion ->
                            if (menuBtmSheetFolderEntries().contains(appVM.menuBtmSheetFor)) {
                                collectionsScreenVM.insertANewFolder(
                                    folder = Folder(
                                        name = folderName,
                                        note = folderNote,
                                        parentFolderId = currentFABContext.currentFolder?.run {
                                            if (this.localId > 0) this.localId else null
                                        }),
                                    ignoreFolderAlreadyExistsThrowable = false,
                                    onCompletion = onCompletion
                                )
                            }
                        },
                        currentFolder = currentFABContext.currentFolder
                    )
                )
            }
            val localUriHandler = LocalUriHandler.current
            val showProgressBarDuringRemoteSave = rememberSaveable {
                mutableStateOf(false)
            }
            val hideMenuSheet: () -> Unit = {
                coroutineScope.launch {
                    appVM.menuBtmSheetState.hide()
                }.invokeOnCompletion {
                    appVM.showMenuSheet = false
                }
            }
            if (appVM.showMenuSheet) {
                MenuBtmSheet(
                    menuBtmSheetParam = MenuBtmSheetParam(
                        onDismiss = {
                            appVM.showMenuSheet = false
                        },
                        btmModalSheetState = appVM.menuBtmSheetState,
                        menuBtmSheetFor = appVM.menuBtmSheetFor,
                        onDelete = {
                            appVM.showDeleteDialogBox = true
                        },
                        onRename = {
                            appVM.showRenameDialogBox = true
                        },
                        onArchive = {
                            ifServerConfigured {
                                showProgressBarDuringRemoteSave.value = true
                            }
                            if (menuBtmSheetFolderEntries().contains(appVM.menuBtmSheetFor)) {
                                collectionsScreenVM.archiveAFolder(
                                    appVM.selectedFolderForMenuBtmSheet, onCompletion = {
                                        showProgressBarDuringRemoteSave.value = false
                                        hideMenuSheet()
                                    })
                            } else {
                                collectionsScreenVM.archiveALink(
                                    appVM.selectedLinkTagsForMenuBtmSheet.link, onCompletion = {
                                        showProgressBarDuringRemoteSave.value = false
                                        hideMenuSheet()
                                    })
                            }
                        },
                        onDeleteNote = {
                            ifServerConfigured {
                                showProgressBarDuringRemoteSave.value = true
                            }
                            if (menuBtmSheetFolderEntries().contains(appVM.menuBtmSheetFor)) {
                                collectionsScreenVM.deleteTheNote(
                                    appVM.selectedFolderForMenuBtmSheet, onCompletion = {
                                        showProgressBarDuringRemoteSave.value = false
                                        hideMenuSheet()
                                    })
                            } else {
                                collectionsScreenVM.deleteTheNote(
                                    appVM.selectedLinkTagsForMenuBtmSheet.link, onCompletion = {
                                        showProgressBarDuringRemoteSave.value = false
                                        hideMenuSheet()
                                    })
                            }
                        },
                        onRefreshClick = {
                            ifServerConfigured {
                                showProgressBarDuringRemoteSave.value = true
                            }
                            collectionsScreenVM.refreshLinkMetadata(
                                appVM.selectedLinkTagsForMenuBtmSheet.link, onCompletion = {
                                    showProgressBarDuringRemoteSave.value = false
                                    hideMenuSheet()
                                })
                        },
                        onForceLaunchInAnExternalBrowser = {
                            collectionsScreenVM.addANewLink(
                                link = appVM.selectedLinkTagsForMenuBtmSheet.link.copy(
                                    linkType = LinkType.HISTORY_LINK, localId = 0
                                ),
                                linkSaveConfig = LinkSaveConfig(
                                    forceAutoDetectTitle = false,
                                    forceSaveWithoutRetrievingData = true
                                ),
                                onCompletion = {},
                                pushSnackbarOnSuccess = false,
                                selectedTags = appVM.selectedLinkTagsForMenuBtmSheet.tags
                            )
                            localUriHandler.openUri(appVM.selectedLinkTagsForMenuBtmSheet.link.url)
                        },
                        onShare = {
                            LinkoraSDK.getInstance().nativeUtils.onShare(it)
                        },
                        showQuickActions = rememberSaveable { mutableStateOf(false) },
                        shouldTransferringOptionShouldBeVisible = true,
                        linkTagsPair = appVM.selectedLinkTagsForMenuBtmSheet,
                        folder = appVM.selectedFolderForMenuBtmSheet,
                        onAddToImportantLinks = {
                            ifServerConfigured {
                                showProgressBarDuringRemoteSave.value = true
                            }
                            collectionsScreenVM.markALinkAsImp(
                                appVM.selectedLinkTagsForMenuBtmSheet.link,
                                onCompletion = {
                                    showProgressBarDuringRemoteSave.value = false
                                    hideMenuSheet()
                                },
                                tagIds = appVM.selectedLinkTagsForMenuBtmSheet.tags.map { it.localId })
                        },
                        shouldShowArchiveOption = {
                            appVM.selectedLinkTagsForMenuBtmSheet.link.linkType == LinkType.ARCHIVE_LINK
                        },
                        showProgressBarDuringRemoteSave = showProgressBarDuringRemoteSave,
                        onTagClick = {
                            val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                currentFolder = null,
                                currentTag = it,
                                collectionType = CollectionType.TAG,
                            )
                            localNavController.currentBackStackEntry?.savedStateHandle?.set(
                                key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                                value = Json.encodeToString(
                                    collectionDetailPaneInfo
                                )
                            )
                            localNavController.navigate(
                                Navigation.Collection.MobileCollectionDetailScreen
                            )
                            hideMenuSheet()
                        })
                )
            }
            if (appVM.showDeleteDialogBox) {
                DeleteFolderOrLinkDialog(
                    DeleteFolderOrLinkDialogParam(
                        onDismiss = {
                            appVM.showDeleteDialogBox = false
                        },
                        if (CollectionsScreenVM.isSelectionEnabled.value) DeleteDialogBoxType.SELECTED_DATA else if (menuBtmSheetFolderEntries().contains(
                                appVM.menuBtmSheetFor
                            )
                        ) {
                            DeleteDialogBoxType.FOLDER
                        } else DeleteDialogBoxType.LINK,
                        onDeleteClick = { onCompletion, _ ->
                            if (CollectionsScreenVM.isSelectionEnabled.value) {
                                appVM.deleteSelectedItems(onStart = {}, onCompletion)
                                return@DeleteFolderOrLinkDialogParam
                            }
                            if (menuBtmSheetFolderEntries().contains(appVM.menuBtmSheetFor)) {
                                collectionsScreenVM.deleteAFolder(
                                    appVM.selectedFolderForMenuBtmSheet, onCompletion = {
                                        hideMenuSheet()
                                        onCompletion()
                                    })
                            } else {
                                collectionsScreenVM.deleteALink(
                                    appVM.selectedLinkTagsForMenuBtmSheet.link, onCompletion = {
                                        hideMenuSheet()
                                        onCompletion()
                                    })
                            }
                        })
                )
            }
            val renameDialogSheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            )
            val slideDownAndHideRenameSheet: () -> Unit = {
                coroutineScope.launch {
                    renameDialogSheetState.hide()
                }.invokeOnCompletion {
                    appVM.showRenameDialogBox = false
                }
            }
            val allTags = collectionsScreenVM.allTags.collectAsStateWithLifecycle()
            RenameFolderOrLinkDialog(
                renameFolderOrLinkDialogParam = RenameFolderOrLinkDialogParam(
                    selectedTags = appVM.selectedLinkTagsForMenuBtmSheet.tags,
                    allTags = allTags,
                    onSave = { newTitle: String, newNote: String, newImageUrl: String, selectedTags: List<Tag>, onCompletion: () -> Unit ->
                        if (appVM.menuBtmSheetFor is MenuBtmSheetType.Link) {
                            collectionsScreenVM.updateLink(updatedLinkTagsPair = appVM.selectedLinkTagsForMenuBtmSheet.run {
                                copy(
                                    link = link.copy(
                                        title = newTitle, imgURL = newImageUrl, note = newNote
                                    ), tags = selectedTags
                                )
                            }, onCompletion = {
                                slideDownAndHideRenameSheet()
                                onCompletion()
                            })
                        } else {
                            collectionsScreenVM.updateFolder(
                                newFolderData = appVM.selectedFolderForMenuBtmSheet.copy(
                                    name = newTitle, note = newNote
                                ), onCompletion = {
                                    slideDownAndHideRenameSheet()
                                    onCompletion()
                                })
                        }
                    },
                    showDialogBox = appVM.showRenameDialogBox,
                    existingFolderName = appVM.selectedFolderForMenuBtmSheet.name,
                    existingTitle = if (menuBtmSheetFolderEntries().contains(appVM.menuBtmSheetFor)) appVM.selectedFolderForMenuBtmSheet.name else appVM.selectedLinkTagsForMenuBtmSheet.link.title,
                    existingNote = if (menuBtmSheetFolderEntries().contains(appVM.menuBtmSheetFor)) appVM.selectedFolderForMenuBtmSheet.note else appVM.selectedLinkTagsForMenuBtmSheet.link.note,
                    existingImageUrl = appVM.selectedLinkTagsForMenuBtmSheet.link.imgURL,
                    onHide = slideDownAndHideRenameSheet,
                    sheetState = renameDialogSheetState,
                    dialogBoxFor = appVM.menuBtmSheetFor,
                    onRetrieveNextTagsPage = collectionsScreenVM::retrieveNextBatchOfTags,
                    onFirstVisibleIndexChange = collectionsScreenVM::updateStartingIndexForTagsPaginator,
                )
            )

            if (appVM.showSortingBtmSheet) {
                SortingBottomSheet(
                    SortingBottomSheetParam(
                        onDismiss = {
                        appVM.showSortingBtmSheet = false
                    },
                        onSelected = { sortingPreferences, _, _ -> },
                        bottomModalSheetState = appVM.sortingBtmSheetState,
                        sortingBtmSheetType = SortingBtmSheetType.COLLECTIONS_SCREEN,
                        showFoldersSelection = rememberSaveable {
                            mutableStateOf(false)
                        },
                        showLinksSelection = rememberSaveable {
                            mutableStateOf(false)
                        })
                )
            }
            CreateATagBtmSheet(
                sheetState = createTagBtmSheetState,
                showBtmSheet = appVM.showBtmSheetForNewTagAddition,
                onCancel = {
                    coroutineScope.launch {
                        createTagBtmSheetState.hide()
                    }.invokeOnCompletion {
                        appVM.showBtmSheetForNewTagAddition = false
                    }
                },
                onCreateClick = { tagName ->
                    collectionsScreenVM.createATag(tagName = tagName, onCompletion = {
                        coroutineScope.launch {
                            createTagBtmSheetState.hide()
                        }.invokeOnCompletion {
                            appVM.showBtmSheetForNewTagAddition = false
                        }
                    })
                })

            TagMenu(showMenu = appVM.showMenuForTag, sheetState = tagMenuBtmSheet, onHide = {
                appVM.showMenuForTag = false
            }, tag = appVM.selectedTagForBtmTagSheet, onRename = {
                appVM.showMenuForTag = false
                showTagRenameComponent = true
            }, onDelete = {
                appVM.showMenuForTag = false
                showTagDeletionConfirmation = true
            })

            TagDeletionConfirmation(showConfirmation = showTagDeletionConfirmation, onHide = {
                showTagDeletionConfirmation = false
            }, onDelete = {
                collectionsScreenVM.deleteATag(
                    tagId = appVM.selectedTagForBtmTagSheet.localId, onCompletion = {
                        appVM.showMenuForTag = false
                        showTagDeletionConfirmation = false
                    })
            })
            val tagRenameBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            RenameTagComponent(
                sheetState = tagRenameBtmSheetState,
                showComponent = showTagRenameComponent,
                existingName = appVM.selectedTagForBtmTagSheet.name,
                onHide = {
                    showTagRenameComponent = false
                },
                onSave = { newName ->
                    collectionsScreenVM.renameATag(
                        localId = appVM.selectedTagForBtmTagSheet.localId,
                        newName = newName,
                        onCompletion = {
                            coroutineScope.launch {
                                tagRenameBtmSheetState.hide()
                            }.invokeOnCompletion {
                                showTagRenameComponent = false
                            }
                        })
                })
        }
    }
}