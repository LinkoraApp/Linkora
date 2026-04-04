package com.sakethh.linkora.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
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
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sakethh.linkora.di.APPVMAssistedFactory
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.tag.Tag
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
import com.sakethh.linkora.ui.screens.collections.CollectionScreenParams
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.screens.collections.components.RenameTagComponent
import com.sakethh.linkora.ui.screens.collections.components.TagDeletionConfirmation
import com.sakethh.linkora.ui.screens.collections.components.TagMenu
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.booleanPreferencesKey
import com.sakethh.linkora.utils.currentSavedServerConfig
import com.sakethh.linkora.utils.host
import com.sakethh.linkora.utils.ifServerConfigured
import com.sakethh.linkora.utils.inRootScreen
import com.sakethh.linkora.utils.isServerConfigured
import com.sakethh.linkora.utils.supportsWideDisplay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Suppress("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    modifier: Modifier = Modifier
) {
    val onAndroidMobile = Platform.Android.onMobile()
    val appVM: AppVM =
        linkoraViewModel(factory = APPVMAssistedFactory.createForApp(LocalDensity.current))
    val preferences by appVM.preferencesAsFlow.collectAsStateWithLifecycle()
    val createTagBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTagDeletionConfirmation by rememberSaveable {
        mutableStateOf(false)
    }
    var showTagRenameComponent by rememberSaveable {
        mutableStateOf(false)
    }
    val tagMenuBtmSheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val collectionsScreenVM: CollectionsScreenVM = linkoraViewModel()
    val rootRouteList = retain {
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
        AnimatedVisibility(
            visible = (appVM.onBoardingCompleted.value || platform == Platform.Web) && supportsWideDisplay(),
            exit = slideOutHorizontally(targetOffsetX = { -it }),
            enter = slideInHorizontally(initialOffsetX = { -it })
        ) {
            DesktopNavigationRail(
                rootRouteList = rootRouteList,
                currentRoute = currentRoute,
                isDataSyncingFromPullRefresh = isDataSyncingFromPullRefresh,
                onNavigate = {},
                isPerformingStartupSync = appVM.isPerformingStartupSync,
                getLastSyncedTime = {
                    appVM.getLastSyncedTime()
                },
                isAnySnapshotOngoing = appVM.isAnySnapshotOngoing,
                preferences = preferences,
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
                    preferences = preferences,
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
                SnackbarHost(appVM.snackbarHostState) { snackbarData ->
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(7.5.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(15.dp),
                                ).padding(15.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = snackbarData.visuals.message,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSecondary,
                                fontSize = 18.sp,
                                maxLines = 3,
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = if (preferences.isServerConfigured()) {
                    Modifier.pullToRefresh(
                        isRefreshing = isDataSyncingFromPullRefresh.value,
                        state = pullToRefreshState,
                        enabled = rememberSaveable(preferences.serverBaseUrl) {
                            preferences.serverBaseUrl.isNotBlank() && onAndroidMobile
                        },
                        onRefresh = {
                            if (appVM.isPerformingStartupSync || isDataSyncingFromPullRefresh.value) {
                                coroutineScope.pushUIEvent(UIEvent.Type.ShowSnackbar("A sync process is already in progress"))
                                return@pullToRefresh
                            }
                            appVM.saveServerConnectionAndSync(
                                serverConnection = preferences.currentSavedServerConfig(),
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
                } else Modifier
            ) {

                LinkoraNavHost(
                    startDestination = appVM.startDestination,
                    onOnboardingComplete = appVM::markOnboardingComplete,
                    currentFABContext = {
                        appVM.updateFABContext(it)
                    },
                    forceSearchActive = forceSearchActive,
                    cancelForceSearchActive = {
                        forceSearchActive = false
                    },
                    preferences = preferences,
                    collectionScreenParams = CollectionScreenParams(
                        collectionPagerState = collectionsScreenVM.collectionPagerState,
                        rootRegularFolders = collectionsScreenVM.rootRegularFolders,
                        allTags = collectionsScreenVM.allTags,
                        currentCollectionSource = collectionsScreenVM.currentCollectionSource,
                        performAction = collectionsScreenVM::performAction,
                        onRetrieveNextRegularRootFolderPage = collectionsScreenVM::retrieveNextBatchOfRegularRootFolders,
                        onRegularRootFolderFirstVisibleItemIndexChange = collectionsScreenVM::updateStartingIndexForRegularRootFoldersPaginator,
                        onRetrieveNextTagsPage = collectionsScreenVM::retrieveNextBatchOfTags,
                        onTagsFirstVisibleItemIndexChange = collectionsScreenVM::updateStartingIndexForTagsPaginator,
                    )
                )
                if (preferences.isServerConfigured()) {
                    Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isDataSyncingFromPullRefresh.value,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                }
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
                    preferences = preferences,
                    addNewLinkDialogParams = AddNewLinkDialogParams(
                        onDismiss = {
                            appVM.showAddLinkDialog = false
                        },
                        currentFolder = if ((inRootScreen == true && onAndroidMobile) || currentFABContext.currentFolder?.localId == Constants.ALL_LINKS_ID) null else currentFABContext.currentFolder,
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
                    preferences = preferences, menuBtmSheetParam = MenuBtmSheetParam(
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
                            preferences.ifServerConfigured {
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
                            preferences.ifServerConfigured {
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
                        onForceLaunchInAnExternalBrowser = {
                            collectionsScreenVM.addANewLink(
                                link = appVM.selectedLinkTagsForMenuBtmSheet.link.copy(
                                    linkType = LinkType.HISTORY_LINK, localId = 0
                                ),
                                linkSaveConfig = LinkSaveConfig(
                                    forceAutoDetectTitle = false,
                                    forceSaveWithoutRetrievingData = true,
                                    useProxy = preferences.useProxy,
                                    skipSavingIfExists = preferences.skipSavingExistingLink,
                                    forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails
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
                            preferences.ifServerConfigured {
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
                            localNavController.navigate(
                                Navigation.Collection.CollectionDetailScreen(
                                    Json.encodeToString(
                                        collectionDetailPaneInfo
                                    )
                                )
                            )
                            hideMenuSheet()
                        },
                        onRefresh = { refreshLinkType ->
                            preferences.ifServerConfigured {
                                showProgressBarDuringRemoteSave.value = true
                            }
                            collectionsScreenVM.refreshLinkMetadata(
                                refreshLinkType = refreshLinkType,
                                appVM.selectedLinkTagsForMenuBtmSheet.link,
                                onCompletion = {
                                    showProgressBarDuringRemoteSave.value = false
                                    hideMenuSheet()
                                })
                        })
                )
            }
            if (appVM.showDeleteDialogBox) {
                DeleteFolderOrLinkDialog(
                    preferences = preferences,
                    deleteFolderOrLinkDialogParam = DeleteFolderOrLinkDialogParam(
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
                    onSave = { newTitle: String, newNote: String, newImageUrl: String, newUrl: String, selectedTags: List<Tag>, onCompletion: () -> Unit ->
                        if (appVM.menuBtmSheetFor is MenuBtmSheetType.Link) {
                            collectionsScreenVM.updateLink(updatedLinkTagsPair = appVM.selectedLinkTagsForMenuBtmSheet.run {
                                copy(
                                    link = link.copy(
                                        url = newUrl.trim(),
                                        title = newTitle.trim(),
                                        imgURL = newImageUrl.trim(),
                                        note = newNote.trim(),
                                        host = newUrl.host(throwOnException = false)
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
                    existingUrl = appVM.selectedLinkTagsForMenuBtmSheet.link.url,
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

            val syncServerSurveyBtmSheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true, confirmValueChange = { sheetValue ->
                    if (sheetValue == SheetValue.Hidden) {
                        !preferences.showSyncServerSurveyNotice
                    } else {
                        true
                    }
                })
            var showSyncServerNotice by rememberSaveable {
                mutableStateOf(preferences.showSyncServerSurveyNotice)
            }
            if (preferences.isServerConfigured() && platform != Platform.Web && showSyncServerNotice) {
                ModalBottomSheet(
                    sheetState = syncServerSurveyBtmSheetState, onDismissRequest = {}) {
                    Column(
                        modifier = Modifier.padding(
                            start = 15.dp, end = 15.dp, bottom = 7.5.dp
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.height(7.5.dp))
                        Text(
                            text = "Hey, you're using the sync server. I'm considering rewriting it in Rust since the current Java setup uses around 300 MB just sitting idle, and Rust gets that down to ~5 MB. " + "Your input would help me figure out if it's worth the time.",
                            fontSize = 16.sp,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Button(
                            onClick = {
                                appVM.updatePreference(
                                    key = booleanPreferencesKey(AppPreferences.SHOW_SYNC_SERVER_SURVEY_NOTICE.key),
                                    newValue = false,
                                    onCompletion = {
                                        coroutineScope.launch {
                                            syncServerSurveyBtmSheetState.hide()
                                        }.invokeOnCompletion {
                                            showSyncServerNotice = false
                                            localUriHandler.openUri("https://forms.gle/75ww25CLQqZuSSuZ6")
                                        }
                                    })
                            },
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .pressScaleEffect().fillMaxWidth()
                        ) {
                            Text(
                                text = "Take the survey",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                appVM.updatePreference(
                                    key = booleanPreferencesKey(AppPreferences.SHOW_SYNC_SERVER_SURVEY_NOTICE.key),
                                    newValue = false,
                                    onCompletion = {
                                        coroutineScope.launch {
                                            syncServerSurveyBtmSheetState.hide()
                                        }.invokeOnCompletion {
                                            showSyncServerNotice = false
                                        }
                                    })
                            },
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .pressScaleEffect().fillMaxWidth()
                        ) {
                            Text(
                                text = "Not Interested",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}