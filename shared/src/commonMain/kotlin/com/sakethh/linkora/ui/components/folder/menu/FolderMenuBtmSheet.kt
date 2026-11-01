package com.sakethh.linkora.ui.components.folder.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.menu.IndividualMenuComponent
import com.sakethh.linkora.utils.bottomNavPaddingAcrossPlatforms
import com.sakethh.linkora.utils.fillMaxWidthWithPadding
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderMenuBtmSheet(
    preferences: AppPreferences,
    menuBtmSheetParam: FolderMenuBtmSheetParam,
) {
    val localizedStrings = LocalizedStrings.current
    val coroutineScope = rememberCoroutineScope()
    var shareInLoadingState by rememberSaveable {
        mutableStateOf(false)
    }
    LaunchedEffect(
        menuBtmSheetParam.showProgressBarDuringRemoteSave.value,
        menuBtmSheetParam.btmModalSheetState.isVisible,
        shareInLoadingState,
    ) {
        if (
            (shareInLoadingState || menuBtmSheetParam.showProgressBarDuringRemoteSave.value) &&
                !menuBtmSheetParam.btmModalSheetState.isVisible
        ) {
            menuBtmSheetParam.btmModalSheetState.expand()
        }
    }

    var showNote by rememberSaveable {
        mutableStateOf(false)
    }
    val currentFolder =
        remember(menuBtmSheetParam.folder) {
            menuBtmSheetParam.folder
        }

    val hideContent: () -> Unit = {
        coroutineScope
            .launch {
                if (menuBtmSheetParam.btmModalSheetState.isVisible) {
                    menuBtmSheetParam.btmModalSheetState.hide()
                }
            }
            .invokeOnCompletion {
                menuBtmSheetParam.onDismiss()
            }
    }

    val onAndroidMobile = Platform.Android.onMobile()
    val commonContent: ComposableContent = {
        Column(
            modifier =
                if (onAndroidMobile) {
                    Modifier
                } else {
                    Modifier.verticalScroll(
                            rememberScrollState(),
                        )
                        .focusGroup()
                },
        ) {
            if (onAndroidMobile) {
                IndividualMenuComponent(
                    onClick = {
                        coroutineScope
                            .launch {
                                if (menuBtmSheetParam.btmModalSheetState.isVisible) {
                                    menuBtmSheetParam.btmModalSheetState.hide()
                                }
                            }
                            .invokeOnCompletion {
                                showNote = true
                                coroutineScope.launch {
                                    menuBtmSheetParam.btmModalSheetState.show()
                                }
                            }
                    },
                    elementName = localizedStrings.ViewNote,
                    elementImageVector = Icons.AutoMirrored.Outlined.TextSnippet,
                )
            }
            IndividualMenuComponent(
                onClick = {
                    hideContent()
                    menuBtmSheetParam.onRename()
                },
                elementName = localizedStrings.Edit,
                elementImageVector = Icons.Outlined.Edit,
            )

            if (LocalPlatform.current is Platform.Android) {
                IndividualMenuComponent(
                    inLoadingState = shareInLoadingState,
                    onClick = {
                        shareInLoadingState = true
                        menuBtmSheetParam.onShare(
                            currentFolder,
                            {
                                shareInLoadingState = false
                                hideContent()
                            },
                        )
                    },
                    elementName = localizedStrings.Share,
                    elementImageVector = Icons.Outlined.Share,
                )
            }

            val isArchived = currentFolder.isArchived
            val inChildFolder = currentFolder.parentFolderId != null

            if (!inChildFolder) {
                IndividualMenuComponent(
                    onClick = {
                        menuBtmSheetParam.onArchive()
                        hideContent()
                    },
                    elementName =
                        if (isArchived) localizedStrings.UnArchive else localizedStrings.Archive,
                    elementImageVector =
                        if (isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                )
            }

            if (currentFolder.note.isNotBlank()) {
                IndividualMenuComponent(
                    onClick = {
                        menuBtmSheetParam.onDeleteNote()
                        hideContent()
                    },
                    elementName = localizedStrings.DeleteTheNote,
                    elementImageVector = Icons.Outlined.Delete,
                )
            }

            IndividualMenuComponent(
                onClick = {
                    menuBtmSheetParam.onDelete()
                    coroutineScope
                        .launch {
                            if (menuBtmSheetParam.btmModalSheetState.isVisible) {
                                menuBtmSheetParam.btmModalSheetState.hide()
                            }
                        }
                        .invokeOnCompletion {
                            menuBtmSheetParam.onDismiss()
                        }
                },
                elementName = localizedStrings.DeleteTheFolder,
                elementImageVector = Icons.Outlined.FolderDelete,
            )

            if (!onAndroidMobile) {
                Spacer(Modifier.height(15.dp))
            }
        }
    }

    ModalBottomSheet(
        sheetGesturesEnabled = !Platform.Android.onTV(),
        properties =
            ModalBottomSheetProperties(
                shouldDismissOnBackPress =
                    !shareInLoadingState &&
                        !menuBtmSheetParam.showProgressBarDuringRemoteSave.value,
            ),
        onDismissRequest = {
            if (shareInLoadingState || menuBtmSheetParam.showProgressBarDuringRemoteSave.value)
                return@ModalBottomSheet
            hideContent()
        },
        dragHandle = {
            if (!onAndroidMobile) {
                BottomSheetDefaults.DragHandle()
            }
        },
        sheetState = menuBtmSheetParam.btmModalSheetState,
        shape =
            if (menuBtmSheetParam.showProgressBarDuringRemoteSave.value && onAndroidMobile) {
                RectangleShape
            } else {
                BottomSheetDefaults.ExpandedShape
            },
    ) {
        if (menuBtmSheetParam.showProgressBarDuringRemoteSave.value) {
            Column(
                modifier = Modifier.fillMaxWidthWithPadding().bottomNavPaddingAcrossPlatforms(),
            ) {
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = localizedStrings.UpdatingChangesOnRemoteServer,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            return@ModalBottomSheet
        }

        if (onAndroidMobile) {
            FolderMobileMenu(preferences, menuBtmSheetParam, currentFolder, showNote, commonContent)
        } else {
            FolderNonMobileMenu(preferences, menuBtmSheetParam, currentFolder, commonContent)
        }
    }
}
