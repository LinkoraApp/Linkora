package com.sakethh.linkora.ui.components.link

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.components.CoilImage
import com.sakethh.linkora.ui.domain.model.LinkComponentParam
import com.sakethh.linkora.ui.screens.collections.components.ItemDivider
import com.sakethh.linkora.ui.utils.pressScaleEffect

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListViewLinkComponent(
    linkComponentParam: LinkComponentParam,
    titleOnlyView: Boolean,
    modifier: Modifier = Modifier,
    imageAlignment: Alignment = Alignment.Center,
    onShare: (url: String) -> Unit
) {
    val localClipBoardManager = LocalClipboardManager.current
    LocalUriHandler.current
    val platform = LocalPlatform.current
    Column(
        modifier = Modifier.background(
            if (linkComponentParam.isItemSelected.value) MaterialTheme.colorScheme.primary.copy(
                0.25f
            ) else Color.Transparent
        ).pointerHoverIcon(icon = PointerIcon.Hand).combinedClickable(interactionSource = remember {
            MutableInteractionSource()
        }, indication = null, onClick = {
            linkComponentParam.onLinkClick()
        }, onLongClick = {
            linkComponentParam.onLongClick()
        }).padding(start = 15.dp, top = 15.dp).fillMaxWidth().wrapContentHeight().pressScaleEffect()
            .animateContentSize().then(modifier), verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 15.dp).wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = linkComponentParam.link.title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(if (!linkComponentParam.isSelectionModeEnabled.value && titleOnlyView) 1f else 0.65f)
                    .padding(end = 15.dp),
                maxLines = 4,
                lineHeight = 20.sp,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
            )
            if (!linkComponentParam.isItemSelected.value && !titleOnlyView) {
                if (linkComponentParam.link.imgURL.isNotEmpty()) {
                    CoilImage(
                        modifier = Modifier.width(95.dp).height(60.dp)
                            .clip(RoundedCornerShape(15.dp)),
                        imgURL = linkComponentParam.link.imgURL,
                        userAgent = linkComponentParam.link.userAgent
                            ?: AppPreferences.primaryJsoupUserAgent.value,
                        alignment = imageAlignment
                    )
                } else {
                    Box(
                        modifier = Modifier.width(95.dp).height(60.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            tint = MaterialTheme.colorScheme.onPrimary,
                            imageVector = Icons.Rounded.ImageNotSupported,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else if (linkComponentParam.isItemSelected.value) {
                Box(
                    modifier = Modifier.width(95.dp).height(60.dp).clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        tint = MaterialTheme.colorScheme.onPrimary,
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        if (linkComponentParam.link.note.isNotBlank() && AppPreferences.showNoteInListViewLayout.value) {
            Text(
                modifier = Modifier.padding(
                    end = 15.dp, top = 10.dp
                ),
                text = linkComponentParam.link.note,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.75f)
            )
        }
        if (linkComponentParam.tags != null) {
            TagsRow(tags = linkComponentParam.tags, onTagClick = {
                linkComponentParam.onTagClick(it)
            })
        }

        if (AppPreferences.showHostInLinkListView.value) {
            Text(
                modifier = Modifier.padding(
                    top = if (linkComponentParam.tags != null) 5.dp else 15.dp,
                    end = 15.dp,
                    bottom = if (linkComponentParam.isSelectionModeEnabled.value) 15.dp else 0.dp
                ).background(
                    color = MaterialTheme.colorScheme.primary.copy(0.1f),
                    shape = RoundedCornerShape(5.dp)
                ).padding(5.dp),
                text = linkComponentParam.link.baseURL.replace("www.", "").replace("http://", "")
                    .replace("https://", ""),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                if (!linkComponentParam.isSelectionModeEnabled.value) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(modifier=Modifier.pointerHoverIcon(icon = PointerIcon.Hand),onClick = {
                            localClipBoardManager.setText(
                                AnnotatedString(linkComponentParam.link.url)
                            )
                        }) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy, contentDescription = null
                            )
                        }
                        if (platform is Platform.Android) {
                            IconButton(modifier=Modifier.pointerHoverIcon(icon = PointerIcon.Hand),onClick = {
                                onShare(linkComponentParam.link.url)
                            }) {
                                Icon(imageVector = Icons.Outlined.Share, contentDescription = null)
                            }
                        }
                        IconButton(modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand), onClick = {
                            linkComponentParam.onMoreIconClick()
                        }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert, contentDescription = null
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = linkComponentParam.isSelectionModeEnabled.value.not(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ItemDivider(
                colorOpacity = 0.25f,
                color = MaterialTheme.colorScheme.outline,
                paddingValues = PaddingValues(top = 2.5.dp, bottom = 2.5.dp, end = 10.dp)
            )
        }
    }
}

@Composable
fun TagsRow(
    modifier: Modifier = Modifier.padding(top = 5.dp, end = 15.dp).fillMaxWidth(),
    tags: List<Tag>,
    onTagClick: (Tag) -> Unit,
    chipColorOpacity: Float = 0.5f
) {
    LazyRow(
        modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tags) { tag ->
            AssistChip(
                colors = AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(chipColorOpacity)
            ), border = AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = MaterialTheme.colorScheme.secondaryContainer.copy(chipColorOpacity)
            ), onClick = {
                onTagClick(tag)
            }, label = {
                Text(
                    text = tag.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }, leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }, modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand))
        }
    }
}