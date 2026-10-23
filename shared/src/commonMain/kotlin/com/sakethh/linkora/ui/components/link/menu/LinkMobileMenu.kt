package com.sakethh.linkora.ui.components.link.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.CoilImage
import com.sakethh.linkora.ui.components.link.TagsRow
import com.sakethh.linkora.ui.components.menu.MenuNonImageHeader
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.screens.collections.components.ItemDivider
import com.sakethh.linkora.ui.utils.EdgeType
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.fadedEdges

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LinkMobileMenu(
    preferences: AppPreferences,
    menuBtmSheetParam: LinkMenuBtmSheetParam,
    currentLinkTagsPair: LinkTagsPair,
    showNote: Boolean,
    commonMenuContent: ComposableContent,
) {
    val localizedStrings = LocalizedStrings.current
    val localClipBoardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val showTags = remember {
        currentLinkTagsPair.tags?.isEmpty() == false &&
            preferences.selectedLinkLayout in
                listOf(
                    Layout.STAGGERED_VIEW.name,
                    Layout.GRID_VIEW.name,
                )
    }

    val hostComponent: ComposableContent = {
        Text(
            modifier =
                Modifier.then(if (!showTags) Modifier else Modifier.padding(start = 10.dp))
                    .background(
                        color =
                            MaterialTheme.colorScheme.secondaryContainer.copy(
                                if (!showTags) 1f else 0.5f,
                            ),
                        shape = RoundedCornerShape(10.dp),
                    )
                    .padding(5.dp),
            text =
                currentLinkTagsPair.link.host
                    .replace("www.", "")
                    .replace("http://", "")
                    .replace("https://", ""),
            style =
                if (!showTags) MaterialTheme.typography.titleLarge
                else MaterialTheme.typography.titleMedium,
            maxLines = 1,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }

    Column(modifier = Modifier.navigationBarsPadding().verticalScroll(rememberScrollState())) {
        if (
            currentLinkTagsPair.link.imgURL.isNotEmpty() &&
                preferences.showAssociatedImageInLinkMenu
        ) {
            Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                CoilImage(
                    modifier =
                        Modifier.height(200.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                            .fadedEdges(MaterialTheme.colorScheme, edgeType = EdgeType.BOTTOM)
                            .fadedEdges(MaterialTheme.colorScheme, edgeType = EdgeType.TOP),
                    imgURL = currentLinkTagsPair.link.imgURL,
                    preferences = preferences,
                    userAgent =
                        currentLinkTagsPair.link.userAgent ?: preferences.primaryJsoupUserAgent,
                )
                Column(
                    modifier =
                        Modifier.align(Alignment.BottomStart)
                            .padding(start = 8.dp, end = 15.dp, top = 15.dp, bottom = 8.dp),
                ) {
                    Text(
                        text = currentLinkTagsPair.link.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 3,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier =
                            Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .clickable(
                                    interactionSource = null,
                                    onClick = {
                                        localClipBoardManager.setText(
                                            AnnotatedString(
                                                currentLinkTagsPair.link.title,
                                            ),
                                        )
                                    },
                                    indication = null,
                                )
                                .padding(end = 20.dp),
                    )
                    if (!showTags) {
                        Spacer(Modifier.height(5.dp))
                        hostComponent()
                    }
                }
            }
        }

        if (
            !preferences.showAssociatedImageInLinkMenu || currentLinkTagsPair.link.imgURL.isEmpty()
        ) {
            MenuNonImageHeader(
                onClick = {
                    localClipBoardManager.setText(AnnotatedString(currentLinkTagsPair.link.title))
                    coroutineScope.pushUIEvent(
                        UIEvent.Type.ShowSnackbar(localizedStrings.CopiedTitleToTheClipboard),
                    )
                },
                leadingIcon = Icons.Default.Link,
                text = currentLinkTagsPair.link.title,
            )
            ItemDivider(
                colorOpacity = 0.25f,
                paddingValues = PaddingValues(start = 15.dp, end = 15.dp),
            )
        }

        if (!showNote) {
            val commonModifier = Modifier.padding(start = 10.dp, end = 10.dp).fillMaxWidth()
            if (showTags) {
                Text(
                    text = localizedStrings.AssociatedTags,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier =
                        commonModifier.padding(
                            top =
                                if (
                                    !preferences.showAssociatedImageInLinkMenu ||
                                        currentLinkTagsPair.link.imgURL.isEmpty()
                                ) {
                                    10.dp
                                } else {
                                    0.dp
                                },
                        ),
                    fontSize = 12.sp,
                )
                TagsRow(
                    modifier = commonModifier,
                    tags = currentLinkTagsPair.tags,
                    onTagClick = { menuBtmSheetParam.onTagClick(it) },
                )
            }
            if (showTags) {
                hostComponent()
            }
            commonMenuContent()
        } else {
            val note = currentLinkTagsPair.link.note
            if (note.isNotEmpty()) {
                Text(
                    text = localizedStrings.SavedNote,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(20.dp),
                )
                Text(
                    text = note,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 20.sp,
                    modifier =
                        Modifier.fillMaxWidth()
                            .pointerHoverIcon(icon = PointerIcon.Hand)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    localClipBoardManager.setText(AnnotatedString(note))
                                    coroutineScope.pushUIEvent(
                                        UIEvent.Type.ShowSnackbar(
                                            localizedStrings.CopiedNoteToTheClipboard,
                                        ),
                                    )
                                },
                            )
                            .padding(start = 20.dp, end = 25.dp),
                    textAlign = TextAlign.Start,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = localizedStrings.NoNoteAdded,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 24.sp,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
