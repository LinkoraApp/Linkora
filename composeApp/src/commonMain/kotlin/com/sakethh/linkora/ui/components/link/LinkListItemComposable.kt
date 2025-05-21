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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ImageNotSupported
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.components.CoilImage
import com.sakethh.linkora.ui.domain.model.LinkUIComponentParam
import com.sakethh.linkora.ui.screens.collections.ItemDivider
import com.sakethh.linkora.ui.utils.pulsateEffect
import com.sakethh.onShare

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinkListItemComposable(
    linkUIComponentParam: LinkUIComponentParam,
    forTitleOnlyView: Boolean,
    modifier: Modifier = Modifier,
    imageAlignment: Alignment = Alignment.Center
) {
    val localClipBoardManager = LocalClipboardManager.current
    LocalUriHandler.current
    val platform = LocalPlatform.current
    Column(
        modifier = Modifier.background(
            if (linkUIComponentParam.isItemSelected.value) MaterialTheme.colorScheme.primary.copy(
                0.25f
            ) else Color.Transparent
        ).combinedClickable(interactionSource = remember {
            MutableInteractionSource()
        }, indication = null, onClick = {
            linkUIComponentParam.onLinkClick()
        }, onLongClick = {
            linkUIComponentParam.onLongClick()
        }).padding(start = 15.dp, top = 15.dp).fillMaxWidth().wrapContentHeight().pulsateEffect()
            .animateContentSize().then(modifier), verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 15.dp).wrapContentHeight(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = linkUIComponentParam.link.title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(if (!linkUIComponentParam.isSelectionModeEnabled.value && forTitleOnlyView) 1f else 0.65f)
                    .padding(end = 15.dp),
                maxLines = 4,
                lineHeight = 20.sp,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
            )
            if (!linkUIComponentParam.isItemSelected.value && !forTitleOnlyView) {
                if (linkUIComponentParam.link.imgURL.isNotEmpty()) {
                    CoilImage(
                        modifier = Modifier.width(95.dp).height(60.dp)
                            .clip(RoundedCornerShape(15.dp)),
                        imgURL = linkUIComponentParam.link.imgURL,
                        userAgent = linkUIComponentParam.link.userAgent
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
            } else if (linkUIComponentParam.isItemSelected.value) {
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
        if (linkUIComponentParam.link.note.isNotBlank() && AppPreferences.showNoteInListViewLayout.value) {
            Text(
                modifier = Modifier.padding(
                    end = 15.dp, top = 10.dp
                ),
                text = linkUIComponentParam.link.note,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 3,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.75f)
            )
        }

        if (AppPreferences.enableBaseURLForLinkViews.value) {
            Text(
                modifier = Modifier.padding(
                    top = 15.dp,
                    end = 15.dp,
                    bottom = if (linkUIComponentParam.isSelectionModeEnabled.value) 15.dp else 0.dp
                ).background(
                    color = MaterialTheme.colorScheme.primary.copy(0.1f),
                    shape = RoundedCornerShape(5.dp)
                ).padding(5.dp),
                text = linkUIComponentParam.link.baseURL.replace("www.", "").replace("http://", "")
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
        if (linkUIComponentParam.showQuickOptions) {
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    if (!linkUIComponentParam.isSelectionModeEnabled.value) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            /*IconButton(onClick = {
                                linkUIComponentParam.onForceOpenInExternalBrowserClicked()
                                localURIHandler.openUri(linkUIComponentParam.link.url)
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInBrowser,
                                    contentDescription = null
                                )
                            }*/
                            IconButton(onClick = {
                                localClipBoardManager.setText(
                                    AnnotatedString(linkUIComponentParam.link.url)
                                )
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null
                                )
                            }
                            if (platform is Platform.Android) {
                                IconButton(onClick = {
                                    onShare(url = linkUIComponentParam.link.url)
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = null
                                    )
                                }
                            }
                            IconButton(onClick = {
                                linkUIComponentParam.onMoreIconClick()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert, contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        } else {
            return
        }
        AnimatedVisibility(
            visible = linkUIComponentParam.isSelectionModeEnabled.value.not(),
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