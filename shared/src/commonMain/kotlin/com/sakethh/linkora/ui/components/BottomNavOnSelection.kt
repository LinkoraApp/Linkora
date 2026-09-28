package com.sakethh.linkora.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.ui.LocalFabController
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.menu.QuickActionItem
import com.sakethh.linkora.ui.domain.AppAction
import com.sakethh.linkora.ui.domain.TransferActionType
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.theme.PreviewTheme
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.bottomNavPaddingAcrossPlatforms
import com.sakethh.linkora.utils.defaultFolderIds
import com.sakethh.linkora.utils.highlightOnFocused
import com.sakethh.linkora.utils.replaceActual

@Composable
fun BottomNavOnSelection(
    progressBarVisible: Boolean,
    showLoadingProgressBarOnTransferAction: () -> Unit,
    hideLoadingProgressBarOnTransferAction: () -> Unit,
    transferActionType: TransferActionType,
    changeTransferActionType: (TransferActionType) -> Unit,
    selectedAndInRoot: MutableState<Boolean>,
    performAction: (AppAction) -> Unit,
) {
    val localizedStrings = LocalizedStrings.current
    val coroutineScope = rememberCoroutineScope()
    val localNavController = LocalNavController.current
    val currentBackStackEntryState by localNavController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntryState?.destination
    val platform = LocalPlatform.current
    val showUnArchiveBtn by remember {
        derivedStateOf {
            CollectionsScreenVM.selectedFoldersViaLongClick.any {
                it.isArchived
            } || CollectionsScreenVM.selectedLinkTagPairsViaLongClick.any {
                it.link.linkType == LinkType.ARCHIVE_LINK
            }
        }
    }
    val showArchiveBtn by remember {
        derivedStateOf {
            CollectionsScreenVM.selectedLinkTagPairsViaLongClick.any {
                it.link.linkType != LinkType.ARCHIVE_LINK
            } || CollectionsScreenVM.selectedFoldersViaLongClick.any {
                !it.isArchived
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize()
            .background(NavigationBarDefaults.containerColor).navigationBarsPadding(),
    ) {
        HorizontalDivider()
        Spacer(modifier = Modifier.height(5.dp))
        if (progressBarVisible) {
            Text(
                text = if (transferActionType == TransferActionType.COPY) {
                    localizedStrings.Copying
                } else {
                    localizedStrings.Moving
                },
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.sp,
                modifier = Modifier.padding(
                    start = 15.dp,
                    bottom = 10.dp,
                    top = 5.dp,
                ),
            )
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp))
            Spacer(Modifier.bottomNavPaddingAcrossPlatforms())
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                onClick = {
                    CollectionsScreenVM.clearAllSelections()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                )
            }
            Column {
                Text(
                    text = (if (CollectionsScreenVM.selectedLinkTagPairsViaLongClick.size == 1) {
                        localizedStrings.Selected1Link
                    } else {
                        localizedStrings.SelectedLinksCount.replaceActual(
                            CollectionsScreenVM.selectedLinkTagPairsViaLongClick.size.toString(),
                        )
                    }),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = if (CollectionsScreenVM.selectedFoldersViaLongClick.size == 1) {
                        localizedStrings.Selected1Folder
                    } else {
                        localizedStrings.SelectedFoldersCount.replaceActual(
                            CollectionsScreenVM.selectedFoldersViaLongClick.size.toString(),
                        )
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        val currentFolder =
            LocalFabController.current.fabState.collectAsStateWithLifecycle().value.currentFolder
        val showPasteButton =
            transferActionType != TransferActionType.NONE && currentFolder != null && currentFolder.localId > 0
        if (!(CollectionsScreenVM.selectedFoldersViaLongClick.isNotEmpty() && currentFolder?.localId in defaultFolderIds().dropWhile {
                it == Constants.ARCHIVE_ID
            })) {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (showPasteButton) {
                    QuickActionItem(
                        iconSize = 24.dp,
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier.padding(7.5.dp).fillMaxWidth(),
                        onClick = {
                            if (transferActionType == TransferActionType.COPY) {
                                performAction(
                                    AppAction.CopySelectedItems(
                                        folderId = currentFolder.localId,
                                        onStart = showLoadingProgressBarOnTransferAction,
                                        onCompletion = hideLoadingProgressBarOnTransferAction,
                                    ),
                                )
                            } else {
                                performAction(
                                    AppAction.MoveSelectedItems(
                                        folderId = currentFolder.localId,
                                        onStart = showLoadingProgressBarOnTransferAction,
                                        onCompletion = hideLoadingProgressBarOnTransferAction,
                                    ),
                                )
                            }
                        },
                        text = localizedStrings.Paste,
                        icon = Icons.Default.ContentPaste
                    )
                    return@Column
                }
                if (transferActionType != TransferActionType.NONE) {
                    return@Column
                }
                Spacer(modifier = Modifier.height(7.5.dp))
                Row(modifier = Modifier.padding(start = 7.5.dp, end = 7.5.dp).fillMaxWidth()) {
                    if (showArchiveBtn) {
                        QuickActionItem(
                            iconSize = 24.dp,
                            shape = RoundedCornerShape(
                                topStart = 25.dp,
                                bottomStart = 25.dp,
                                topEnd = if (showUnArchiveBtn || platform is Platform.Android) 5.dp else 25.dp,
                                bottomEnd = if (showUnArchiveBtn || platform is Platform.Android) 5.dp else 25.dp
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                performAction(
                                    AppAction.ArchiveSelectedItems(
                                        onStart = showLoadingProgressBarOnTransferAction,
                                        onCompletion = hideLoadingProgressBarOnTransferAction,
                                    ),
                                )
                            },
                            text = localizedStrings.Archive,
                            icon = Icons.Default.Archive
                        )
                    }
                    if (showUnArchiveBtn) {
                        QuickActionItem(
                            iconSize = 24.dp,
                            shape = RoundedCornerShape(
                                topStart = if (showArchiveBtn) 5.dp else 25.dp,
                                bottomStart = if (showArchiveBtn) 5.dp else 25.dp,
                                topEnd = if (platform is Platform.Android) 5.dp else 25.dp,
                                bottomEnd = if (platform is Platform.Android) 5.dp else 25.dp
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                performAction(
                                    AppAction.MarkSelectedItemsAsRegular(
                                        onStart = showLoadingProgressBarOnTransferAction,
                                        onCompletion = hideLoadingProgressBarOnTransferAction,
                                    ),
                                )
                            },
                            text = localizedStrings.UnArchive,
                            icon = Icons.Default.Unarchive
                        )
                    }
                    if (platform is Platform.Android) {
                        QuickActionItem(
                            iconSize = 24.dp,
                            shape = RoundedCornerShape(
                                topStart = if (showArchiveBtn || showUnArchiveBtn) 5.dp else 25.dp,
                                bottomStart = if (showArchiveBtn || showUnArchiveBtn) 5.dp else 25.dp,
                                topEnd = 25.dp,
                                bottomEnd = 25.dp
                            ),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                LinkoraSDK.getInstance().nativeUtils.onShare(
                                    CollectionsScreenVM.selectedLinkTagPairsViaLongClick.joinToString(
                                        "\n"
                                    ) {
                                        it.link.url
                                    },
                                )
                            },
                            text = localizedStrings.Share,
                            icon = Icons.Default.Share
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(7.5.dp)
                        .fillMaxWidth()
                ) {
                    QuickActionItem(
                        iconSize = 24.dp,
                        shape = RoundedCornerShape(
                            topStart = 25.dp,
                            bottomStart = 25.dp,
                            topEnd = 5.dp,
                            bottomEnd = 5.dp
                        ),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            changeTransferActionType(TransferActionType.COPY)
                        },
                        text = localizedStrings.Copy,
                        icon = Icons.Default.CopyAll
                    )

                    QuickActionItem(
                        iconSize = 24.dp,
                        shape = RoundedCornerShape(5.dp),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            coroutineScope.pushUIEvent(UIEvent.Type.ShowDeleteDialogBox)
                        },
                        text = localizedStrings.Delete,
                        icon = Icons.Default.Delete
                    )

                    QuickActionItem(
                        iconSize = 24.dp,
                        shape = RoundedCornerShape(
                            topStart = 5.dp,
                            bottomStart = 5.dp,
                            topEnd = 25.dp,
                            bottomEnd = 25.dp
                        ),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            changeTransferActionType(TransferActionType.MOVE)
                        },
                        text = localizedStrings.Move,
                        icon = Icons.AutoMirrored.Filled.DriveFileMove
                    )
                }
            }
        }
        if (transferActionType != TransferActionType.NONE) {
            Text(
                text = if (transferActionType == TransferActionType.COPY) {
                    localizedStrings.NavigateAndCopyDesc
                } else {
                    localizedStrings.NavigateAndMoveDesc
                },
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 15.dp, end = 15.dp),
            )
        }
        val showNavigateToCollectionScreen =
            selectedAndInRoot.value && currentRoute?.hasRoute(Navigation.Root.CollectionsScreen::class) != true
        if (CollectionsScreenVM.selectedFoldersViaLongClick.isNotEmpty() && CollectionsScreenVM.selectedFoldersViaLongClick.any {
                it.parentFolderId != null
            }) {
            Button(
                onClick = {
                    performAction(
                        AppAction.MarkSelectedFoldersAsRoot(
                            onStart = showLoadingProgressBarOnTransferAction,
                            onCompletion = hideLoadingProgressBarOnTransferAction,
                        ),
                    )
                },
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                    .padding(
                        start = 15.dp,
                        end = 15.dp,
                        top = 5.dp,
                    ).highlightOnFocused(shape = ButtonDefaults.shape).pressScaleEffect(),
            ) {
                Text(
                    text = localizedStrings.MarkSelectedFoldersAsRoot,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
        if (showNavigateToCollectionScreen) {
            Button(
                onClick = {
                    localNavController.navigate(Navigation.Root.CollectionsScreen) {
                        /*
                        we need to pop all the stuff, otherwise, the collections screen would
                        appear on top of the search screen. This happens because saving and
                        restoring is taking place in the
                        [com.sakethh.linkora.ui.components.MobileBottomNavBarKt] navigation
                        component, leading us back to the collections screen instead of
                        navigating to the search screen when we press the search item in the
                        bottom nav bar
                         */
                        popUpTo(localNavController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                    .padding(
                        start = 15.dp,
                        end = 15.dp,
                        top = 5.dp,
                        bottom = 5.dp,
                    ).highlightOnFocused(shape = ButtonDefaults.shape),
            ) {
                Text(
                    text = localizedStrings.NavigateToCollectionsScreen,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@Composable
@Preview
private fun BottomNavOnSelectionPreview() {
    PreviewTheme(platform = Platform.Desktop) {
        BottomNavOnSelection(
            progressBarVisible = false,
            showLoadingProgressBarOnTransferAction = {},
            hideLoadingProgressBarOnTransferAction = {},
            transferActionType = TransferActionType.NONE,
            changeTransferActionType = {},
            selectedAndInRoot = mutableStateOf(false),
            performAction = {}
        )
    }
}
