package com.sakethh.linkora.ui.components.link


import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.baseUrl
import com.sakethh.linkora.common.utils.getVideoPlatformBaseUrls
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.ui.components.CoilImage
import com.sakethh.linkora.ui.domain.model.LinkUIComponentParam
import com.sakethh.linkora.ui.utils.fadedEdges
import com.sakethh.linkora.ui.utils.pulsateEffect

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridViewLinkUIComponent(
    linkUIComponentParam: LinkUIComponentParam,
    forStaggeredView: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxWidth()
            .then(if (!forStaggeredView) Modifier.wrapContentHeight() else Modifier)
            .combinedClickable(onClick = {
                linkUIComponentParam.onLinkClick()
            }, interactionSource = remember {
                MutableInteractionSource()
            }, indication = null, onLongClick = {
                linkUIComponentParam.onLongClick()
            }).pulsateEffect().padding(4.dp).clip(RoundedCornerShape(5.dp)).then(
                if (AppPreferences.enableBorderForNonListViews.value) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(0.5f),
                    shape = RoundedCornerShape(5.dp)
                )
                else Modifier
            ).then(
                if (linkUIComponentParam.isSelectionModeEnabled.value) Modifier.background(
                    colorScheme.primaryContainer
                ) else Modifier
            ).animateContentSize().then(modifier)
    ) {
        if (linkUIComponentParam.isItemSelected.value) {
            Box(
                Modifier.fillMaxWidth().height(150.dp)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = colorScheme.onPrimary)
            }
        } else if (linkUIComponentParam.link.imgURL.trim().isNotBlank()) {
            Box(modifier = if (forStaggeredView) Modifier.fillMaxSize() else Modifier.height(150.dp)) {
                CoilImage(
                    modifier = Modifier.fillMaxSize().then(
                        if (AppPreferences.enableFadedEdgeForNonListViews.value) Modifier.fadedEdges(
                            colorScheme
                        ) else Modifier
                    ),
                    imgURL = linkUIComponentParam.link.imgURL,
                    contentScale = if (linkUIComponentParam.link.imgURL.startsWith("https://pbs.twimg.com/profile_images/") || !AppPreferences.isShelfMinimizedInHomeScreen.value || !forStaggeredView) ContentScale.Crop else ContentScale.Fit,
                    userAgent = linkUIComponentParam.link.userAgent
                        ?: AppPreferences.primaryJsoupUserAgent.value
                )
                if (AppPreferences.showVideoTagOnUIIfApplicable.value && (linkUIComponentParam.link.mediaType == MediaType.VIDEO || linkUIComponentParam.link.url.baseUrl(
                        throwOnException = false
                    ) in getVideoPlatformBaseUrls())
                ) {
                    Text(
                        text = MediaType.VIDEO.name,
                        modifier = Modifier.padding(
                            start = if (AppPreferences.enableBorderForNonListViews.value || AppPreferences.enableBorderForNonListViews.value.not()) 10.dp else 0.dp,
                        )
                            .padding(bottom = if (AppPreferences.enableBaseURLForLinkViews.value || (!AppPreferences.enableBaseURLForLinkViews.value && !AppPreferences.enableTitleForNonListViews.value)) 10.dp else 0.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondary.copy(0.25f),
                                shape = RoundedCornerShape(5.dp)
                            ).padding(5.dp).align(Alignment.BottomStart),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!AppPreferences.enableBaseURLForLinkViews.value && !AppPreferences.enableTitleForNonListViews.value) {
                    Icon(
                        Icons.Default.MoreVert,
                        null,
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .clip(RoundedCornerShape(topStart = 10.dp)).clickable {
                                linkUIComponentParam.onMoreIconClick()
                            }.padding(10.dp)
                    )
                }
            }
        }
        if (AppPreferences.enableTitleForNonListViews.value) {
            Text(
                text = linkUIComponentParam.link.title,
                modifier = Modifier.padding(
                    start = if (AppPreferences.enableBorderForNonListViews.value) 10.dp else 0.dp,
                    top = 10.dp,
                    end = 10.dp,
                    bottom = if (linkUIComponentParam.isSelectionModeEnabled.value) 10.dp else 0.dp
                ),
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }
        if (!linkUIComponentParam.isSelectionModeEnabled.value && AppPreferences.enableBaseURLForLinkViews.value) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(
                    top = 10.dp,
                    bottom = 10.dp,
                    end = if (AppPreferences.enableBorderForNonListViews.value) 5.dp else 0.dp
                )
            ) {
                Box(Modifier.fillMaxWidth(0.75f)) {
                    Text(
                        text = linkUIComponentParam.link.url.baseUrl(throwOnException = false),
                        modifier = Modifier.padding(
                            start = if (AppPreferences.enableBorderForNonListViews.value) 10.dp else 0.dp,
                        ).background(
                            color = MaterialTheme.colorScheme.primary.copy(0.25f),
                            shape = RoundedCornerShape(5.dp)
                        ).padding(5.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Default.MoreVert, null, modifier = Modifier.clickable {
                        linkUIComponentParam.onMoreIconClick()
                    })
            }
        } else if (!AppPreferences.enableBaseURLForLinkViews.value && AppPreferences.enableTitleForNonListViews.value) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Icon(
                    Icons.Default.MoreVert,
                    null,
                    modifier = Modifier.clip(RoundedCornerShape(topStart = 10.dp)).clickable {
                        linkUIComponentParam.onMoreIconClick()
                    }.padding(10.dp)
                )
            }
        }
    }
}