package com.sakethh.linkora.ui.components.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.bottomNavPaddingAcrossPlatforms
import com.sakethh.linkora.utils.fillMaxWidthWithPadding
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MenuBtmSheet(
    menuBtmSheetParam: MenuBtmSheetParam
) {
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(
        menuBtmSheetParam.showProgressBarDuringRemoteSave.value,
        menuBtmSheetParam.btmModalSheetState.isVisible
    ) {
        if (menuBtmSheetParam.showProgressBarDuringRemoteSave.value && !menuBtmSheetParam.btmModalSheetState.isVisible) {
            menuBtmSheetParam.btmModalSheetState.expand()
        }
    }
    val showNote = rememberSaveable(menuBtmSheetParam.showNote.value) {
        mutableStateOf(menuBtmSheetParam.showNote.value)
    }
    val platform = platform()
    val localClipboard = LocalClipboardManager.current
    val currentFolder = remember(menuBtmSheetParam.folder) {
        menuBtmSheetParam.folder
    }
    val hideContent: () -> Unit = {
        coroutineScope.launch {
            if (menuBtmSheetParam.btmModalSheetState.isVisible) {
                menuBtmSheetParam.btmModalSheetState.hide()
            }
        }.invokeOnCompletion {
            menuBtmSheetParam.onDismiss()
        }
    }

    val quickActions: ComposableContent = {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            QuickActionItem(
                shape = RoundedCornerShape(
                    topStart = 20.dp, topEnd = 8.dp, bottomStart = 20.dp, bottomEnd = 8.dp
                ),
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).weight(1f)
                    .pressScaleEffect(),
                onClick = {
                    menuBtmSheetParam.onForceLaunchInAnExternalBrowser()
                    hideContent()
                },
                text = Localization.Key.Open.rememberLocalizedString(),
                icon = Icons.Default.OpenInNew
            )
            val lastItemShape = RoundedCornerShape(
                topStart = 8.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 20.dp
            )
            QuickActionItem(
                shape = if (platform is Platform.Android.Mobile) RoundedCornerShape(8.dp) else lastItemShape,
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).weight(1f)
                    .pressScaleEffect(),
                onClick = {
                    localClipboard.setText(
                        AnnotatedString(
                            text = menuBtmSheetParam.linkTagsPair?.link?.url ?: ""
                        )
                    )
                    hideContent()
                },
                text = Localization.Key.Copy.rememberLocalizedString(),
                icon = Icons.Default.CopyAll
            )

            if (platform is Platform.Android.Mobile) {
                QuickActionItem(
                    shape = lastItemShape,
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).weight(1f)
                        .pressScaleEffect(),
                    onClick = {
                        menuBtmSheetParam.onShare(menuBtmSheetParam.linkTagsPair!!.link.url)
                        hideContent()
                    },
                    text = Localization.Key.Share.rememberLocalizedString(),
                    icon = Icons.Default.Share
                )
            }
        }
    }

    var showRefreshOptions by rememberSaveable {
        mutableStateOf(false)
    }

    val commonContent: ComposableContent = {
        Column(
            modifier = if (platform is Platform.Android.Mobile) Modifier else Modifier.verticalScroll(
                rememberScrollState()
            )
        ) {
            if (platform is Platform.Android.Mobile) {
                IndividualMenuComponent(
                    onClick = {
                        coroutineScope.launch {
                            if (menuBtmSheetParam.btmModalSheetState.isVisible) {
                                menuBtmSheetParam.btmModalSheetState.hide()
                            }
                        }.invokeOnCompletion {
                            showNote.value = true
                            coroutineScope.launch {
                                menuBtmSheetParam.btmModalSheetState.show()
                            }
                        }
                    },
                    elementName = Localization.Key.ViewNote.rememberLocalizedString(),
                    elementImageVector = Icons.AutoMirrored.Outlined.TextSnippet
                )
            }
            IndividualMenuComponent(
                onClick = {
                    hideContent()
                    menuBtmSheetParam.onRename()
                },
                elementName = Localization.Key.Edit.rememberLocalizedString(),
                elementImageVector = Icons.Outlined.Edit
            )
            if (menuBtmSheetLinkEntries().contains(menuBtmSheetParam.menuBtmSheetFor)) {
                Row(
                    modifier = Modifier
                        .pointerHoverIcon(icon = PointerIcon.Hand)
                        .combinedClickable(
                            interactionSource = null, indication = null,
                            onClick = {
                                showRefreshOptions = !showRefreshOptions
                            })
                        .pressScaleEffect()
                        .padding(end = 10.dp)
                        .wrapContentHeight()
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .padding(10.dp),
                        onClick = {
                            showRefreshOptions = !showRefreshOptions
                        },
                        colors = IconButtonDefaults.filledTonalIconButtonColors()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null
                        )
                    }
                    Text(
                        text = "Refresh",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AnimatedVisibility(
                    visible = showRefreshOptions,
                    modifier = Modifier.padding(start = 30.dp).fillMaxWidth()
                ) {
                    Column {
                        RefreshMenuComponent(
                            label = "Refresh Title",
                            onClick = {
                                menuBtmSheetParam.onRefresh(RefreshLinkType.Title)
                                hideContent()
                            }
                        )
                        RefreshMenuComponent(
                            label = "Refresh Image",
                            onClick = {
                                menuBtmSheetParam.onRefresh(RefreshLinkType.Image)
                                hideContent()
                            }
                        )
                        RefreshMenuComponent(
                            label = Localization.Key.RefreshImageAndTitle.rememberLocalizedString(),
                            onClick = {
                                menuBtmSheetParam.onRefresh(RefreshLinkType.Both)
                                hideContent()
                            }
                        )
                    }
                }
            }

            if (menuBtmSheetLinkEntries().contains(menuBtmSheetParam.menuBtmSheetFor)) {
                val markedAsImportant =
                    menuBtmSheetParam.linkTagsPair!!.link.linkType == LinkType.IMPORTANT_LINK

                IndividualMenuComponent(
                    onClick = {
                        menuBtmSheetParam.onAddToImportantLinks?.let { it() }
                        hideContent()
                    },
                    elementName = if (!markedAsImportant) Localization.Key.MarkALinkAsImpLink.getLocalizedString() else Localization.Key.RemoveALinkFromImpLink.getLocalizedString(),
                    elementImageVector = if (markedAsImportant) Icons.Outlined.DeleteForever else Icons.Outlined.StarOutline
                )
            }

            val isArchived = if (menuBtmSheetParam.menuBtmSheetFor is MenuBtmSheetType.Folder) {
                currentFolder!!.isArchived
            } else {
                menuBtmSheetParam.linkTagsPair!!.link.linkType == LinkType.ARCHIVE_LINK
            }
            val inChildFolder = currentFolder != null && currentFolder.parentFolderId != null

            if (!inChildFolder) {
                IndividualMenuComponent(
                    onClick = {
                        menuBtmSheetParam.onArchive()
                        hideContent()
                    },
                    elementName = if (isArchived) Localization.Key.UnArchive.getLocalizedString() else Localization.Key.Archive.getLocalizedString(),
                    elementImageVector = if (isArchived) Icons.Outlined.Unarchive else Icons.Outlined.Archive
                )
            }

            if (menuBtmSheetLinkEntries().contains(menuBtmSheetParam.menuBtmSheetFor) && menuBtmSheetParam.linkTagsPair!!.link.note.isNotBlank() || menuBtmSheetFolderEntries().contains(
                    menuBtmSheetParam.menuBtmSheetFor
                ) && currentFolder!!.note.isNotBlank()
            ) {
                IndividualMenuComponent(
                    onClick = {
                        menuBtmSheetParam.onDeleteNote()
                        hideContent()
                    },
                    elementName = Localization.Key.DeleteTheNote.rememberLocalizedString(),
                    elementImageVector = Icons.Outlined.Delete
                )
            }
            if (menuBtmSheetParam.menuBtmSheetFor != MenuBtmSheetType.Link.ImportantLink) {
                IndividualMenuComponent(
                    onClick = {
                        menuBtmSheetParam.onDelete()
                        coroutineScope.launch {
                            if (menuBtmSheetParam.btmModalSheetState.isVisible) {
                                menuBtmSheetParam.btmModalSheetState.hide()
                            }
                        }.invokeOnCompletion {
                            menuBtmSheetParam.onDismiss()
                        }
                    },
                    elementName = if (menuBtmSheetParam.menuBtmSheetFor == MenuBtmSheetType.Folder.RegularFolder) Localization.Key.DeleteTheFolder.rememberLocalizedString() else Localization.Key.DeleteTheLink.rememberLocalizedString(),
                    elementImageVector = if (menuBtmSheetParam.menuBtmSheetFor == MenuBtmSheetType.Folder.RegularFolder) Icons.Outlined.FolderDelete else Icons.Outlined.DeleteForever
                )
            }
            if ((platform == Platform.Android.Mobile && AppPreferences.selectedLinkLayout.value in listOf(
                    Layout.STAGGERED_VIEW.name, Layout.GRID_VIEW.name
                ) && menuBtmSheetParam.menuBtmSheetFor in menuBtmSheetLinkEntries()) || (platform !is Platform.Android.Mobile && menuBtmSheetParam.menuBtmSheetFor in menuBtmSheetLinkEntries())
            ) {
                quickActions()
            }
            if (platform !is Platform.Android.Mobile) {
                Spacer(Modifier.height(15.dp))
            }
        }
    }
    ModalBottomSheet(
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = menuBtmSheetParam.showProgressBarDuringRemoteSave.value.not()),
        onDismissRequest = {
            if (menuBtmSheetParam.showProgressBarDuringRemoteSave.value) return@ModalBottomSheet
            hideContent()
        },
        dragHandle = {
            if (platform !is Platform.Android.Mobile) {
                BottomSheetDefaults.DragHandle()
            }
        },
        sheetState = menuBtmSheetParam.btmModalSheetState,
        shape = if (menuBtmSheetParam.showProgressBarDuringRemoteSave.value && platform() is Platform.Android.Mobile) RectangleShape else BottomSheetDefaults.ExpandedShape
    ) {
        if (menuBtmSheetParam.showProgressBarDuringRemoteSave.value) {
            Column(
                modifier = Modifier.fillMaxWidthWithPadding().bottomNavPaddingAcrossPlatforms()
            ) {
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = Localization.rememberLocalizedString(Localization.Key.UpdatingChangesOnRemoteServer),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            return@ModalBottomSheet
        }
        if (platform is Platform.Android.Mobile) {
            MobileMenu(
                menuBtmSheetParam,
                menuBtmSheetParam.linkTagsPair!!,
                currentFolder,
                showNote,
                commonContent
            )
        } else {
            NonMobileMenu(
                menuBtmSheetParam, menuBtmSheetParam.linkTagsPair!!, currentFolder, commonContent
            )
        }
    }
}



