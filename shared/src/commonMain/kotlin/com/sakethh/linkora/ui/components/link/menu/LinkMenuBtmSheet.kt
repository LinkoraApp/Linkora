package com.sakethh.linkora.ui.components.link.menu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusGroup
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.TextSnippet
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Web
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
import androidx.compose.runtime.retain.retain
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
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.menu.IndividualMenuComponent
import com.sakethh.linkora.ui.components.menu.QuickActionItem
import com.sakethh.linkora.ui.components.menu.RefreshMenuComponent
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.bottomNavPaddingAcrossPlatforms
import com.sakethh.linkora.utils.fillMaxWidthWithPadding
import com.sakethh.linkora.utils.highlightOnFocused
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LinkMenuBtmSheet(
    preferences: AppPreferences,
    menuBtmSheetParam: LinkMenuBtmSheetParam,
) {
    val localizedStrings = LocalizedStrings.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(
        menuBtmSheetParam.showProgressBarDuringRemoteSave.value,
        menuBtmSheetParam.btmModalSheetState.isVisible,
    ) {
        if (
            menuBtmSheetParam.showProgressBarDuringRemoteSave.value &&
                !menuBtmSheetParam.btmModalSheetState.isVisible
        ) {
            menuBtmSheetParam.btmModalSheetState.expand()
        }
    }

    var showNote by rememberSaveable {
        mutableStateOf(false)
    }
    val localClipboard = LocalClipboardManager.current
    val currentLink =
        remember(menuBtmSheetParam.linkTagsPair) {
            menuBtmSheetParam.linkTagsPair
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

    val quickActions: ComposableContent = {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 10.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            QuickActionItem(
                shape =
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 8.dp,
                        bottomStart = 20.dp,
                        bottomEnd = 8.dp,
                    ),
                modifier =
                    Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        .weight(1f)
                        .pressScaleEffect(),
                onClick = {
                    menuBtmSheetParam.onForceLaunchInAnExternalBrowser()
                    hideContent()
                },
                text = localizedStrings.Open,
                icon = Icons.AutoMirrored.Filled.OpenInNew,
            )
            val lastItemShape = retain {
                RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 20.dp,
                    bottomStart = 8.dp,
                    bottomEnd = 20.dp,
                )
            }
            QuickActionItem(
                shape = if (onAndroidMobile) RoundedCornerShape(8.dp) else lastItemShape,
                modifier =
                    Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        .weight(1f)
                        .pressScaleEffect(),
                onClick = {
                    localClipboard.setText(AnnotatedString(text = currentLink.link.url))
                    hideContent()
                },
                text = localizedStrings.Copy,
                icon = Icons.Default.CopyAll,
            )
            if (onAndroidMobile) {
                QuickActionItem(
                    shape = lastItemShape,
                    modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .weight(1f)
                            .pressScaleEffect(),
                    onClick = {
                        menuBtmSheetParam.onShare(currentLink.link.url)
                        hideContent()
                    },
                    text = localizedStrings.Share,
                    icon = Icons.Default.Share,
                )
            }
        }
    }

    var showRefreshOptions by rememberSaveable { mutableStateOf(false) }

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

            if (preferences.webCapturesLocation.isNotBlank()) {
                IndividualMenuComponent(
                    onClick = {
                        hideContent()
                        menuBtmSheetParam.onCaptureWebpage(currentLink.link.url)
                    },
                    elementName = localizedStrings.CaptureWebpage,
                    elementImageVector = Icons.Outlined.Download,
                )
                IndividualMenuComponent(
                    onClick = {
                        hideContent()
                        menuBtmSheetParam.onOpenWebCaptureFolder(currentLink.link.url)
                    },
                    elementName = localizedStrings.OpenCapturedFolder,
                    elementImageVector = Icons.Outlined.Web,
                )
            }

            Row(
                modifier =
                    Modifier.highlightOnFocused()
                        .pointerHoverIcon(icon = PointerIcon.Hand)
                        .combinedClickable(
                            interactionSource = null,
                            indication = null,
                            onClick = { showRefreshOptions = !showRefreshOptions },
                        )
                        .pressScaleEffect()
                        .padding(end = 10.dp)
                        .wrapContentHeight()
                        .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(10.dp),
                    onClick = { showRefreshOptions = !showRefreshOptions },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(),
                ) {
                    Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                }
                Text(
                    text = localizedStrings.Refresh,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            AnimatedVisibility(
                visible = showRefreshOptions,
                modifier = Modifier.padding(start = 30.dp).fillMaxWidth(),
            ) {
                Column {
                    RefreshMenuComponent(
                        label = localizedStrings.RefreshTitleLabel,
                        onClick = {
                            menuBtmSheetParam.onRefresh(RefreshLinkType.Title)
                            hideContent()
                        },
                    )
                    RefreshMenuComponent(
                        label = localizedStrings.RefreshImageLabel,
                        onClick = {
                            menuBtmSheetParam.onRefresh(RefreshLinkType.Image)
                            hideContent()
                        },
                    )
                    RefreshMenuComponent(
                        label = localizedStrings.RefreshImageAndTitle,
                        onClick = {
                            menuBtmSheetParam.onRefresh(RefreshLinkType.Both)
                            hideContent()
                        },
                    )
                }
            }

            val markedAsImportant = currentLink.link.linkType == LinkType.IMPORTANT_LINK
            IndividualMenuComponent(
                onClick = {
                    menuBtmSheetParam.onAddToImportantLinks?.let { it() }
                    hideContent()
                },
                elementName =
                    if (!markedAsImportant) localizedStrings.MarkALinkAsImpLink
                    else localizedStrings.RemoveALinkFromImpLink,
                elementImageVector =
                    if (markedAsImportant) Icons.Outlined.DeleteForever
                    else Icons.Outlined.StarOutline,
            )

            val isArchived = currentLink.link.linkType == LinkType.ARCHIVE_LINK

            if (!menuBtmSheetParam.inChildFolder) {
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

            if (currentLink.link.note.isNotBlank()) {
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
                elementName = localizedStrings.DeleteTheLink,
                elementImageVector = Icons.Outlined.DeleteForever,
            )

            if (
                (onAndroidMobile &&
                    preferences.selectedLinkLayout in
                        listOf(
                            Layout.STAGGERED_VIEW.name,
                            Layout.GRID_VIEW.name,
                        )) || !onAndroidMobile
            ) {
                quickActions()
            }

            if (!onAndroidMobile) {
                Spacer(Modifier.height(15.dp))
            }
        }
    }

    ModalBottomSheet(
        sheetGesturesEnabled = !Platform.Android.onTV(),
        properties =
            ModalBottomSheetProperties(
                shouldDismissOnBackPress = !menuBtmSheetParam.showProgressBarDuringRemoteSave.value,
            ),
        onDismissRequest = {
            if (menuBtmSheetParam.showProgressBarDuringRemoteSave.value) return@ModalBottomSheet
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
            LinkMobileMenu(preferences, menuBtmSheetParam, currentLink, showNote, commonContent)
        } else {
            LinkNonMobileMenu(preferences, menuBtmSheetParam, currentLink, commonContent)
        }
    }
}
