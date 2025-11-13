package com.sakethh.linkora.ui.components.link


import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.components.CoilImage
import com.sakethh.linkora.ui.domain.model.LinkComponentParam
import com.sakethh.linkora.ui.utils.fadedEdges
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.getVideoPlatformBaseUrls
import com.sakethh.linkora.utils.host

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridViewLinkComponent(
    linkComponentParam: LinkComponentParam, forStaggeredView: Boolean, modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = if (linkComponentParam.isSelectionModeEnabled.value) colorScheme.primaryContainer else CardDefaults.cardColors().containerColor),
        modifier = Modifier.fillMaxWidth()
            .then(if (!forStaggeredView) Modifier.wrapContentHeight() else Modifier)
            .pointerHoverIcon(icon = PointerIcon.Hand).combinedClickable(onClick = {
                if (AppPreferences.showMenuOnGridLinkClick && !linkComponentParam.isSelectionModeEnabled.value) {
                    linkComponentParam.onMoreIconClick()
                    return@combinedClickable
                }
                linkComponentParam.onLinkClick()
            }, interactionSource = remember {
                MutableInteractionSource()
            }, indication = null, onLongClick = {
                linkComponentParam.onLongClick()
            }).pressScaleEffect().padding(start = 4.dp, end = 4.dp, top = 4.dp).then(modifier)
            .animateContentSize()
    ) {
        if (linkComponentParam.isItemSelected.value) {
            Box(
                Modifier.fillMaxWidth().height(150.dp)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = colorScheme.onPrimary)
            }
        } else if (linkComponentParam.link.imgURL.trim().isNotBlank()) {
            Box(modifier = if (forStaggeredView) Modifier.fillMaxSize() else Modifier.height(150.dp)) {
                CoilImage(
                    modifier = Modifier.fillMaxSize().then(
                        if (AppPreferences.enableFadedEdgeForNonListViews.value) Modifier.fadedEdges(
                            colorScheme
                        ) else Modifier
                    ),
                    imgURL = linkComponentParam.link.imgURL,
                    contentScale = if (linkComponentParam.link.imgURL.startsWith("https://pbs.twimg.com/profile_images/") || !AppPreferences.isShelfMinimizedInHomeScreen.value || !forStaggeredView) ContentScale.Crop else ContentScale.Fit,
                    userAgent = linkComponentParam.link.userAgent
                        ?: AppPreferences.primaryJsoupUserAgent.value
                )
                if (AppPreferences.showVideoTagOnUIIfApplicable.value && (linkComponentParam.link.mediaType == MediaType.VIDEO || linkComponentParam.link.url.host(
                        throwOnException = false
                    ) in getVideoPlatformBaseUrls())
                ) {
                    Text(
                        text = MediaType.VIDEO.name,
                        modifier = Modifier.padding(
                            start = 10.dp,
                        )
                            .padding(bottom = if (AppPreferences.showHostInLinkListView.value || (!AppPreferences.showHostInLinkListView.value && !AppPreferences.showTitleInLinkGridView.value)) 10.dp else 0.dp)
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
            }
        }
        if (AppPreferences.showTitleInLinkGridView.value) {
            Text(
                text = linkComponentParam.link.title,
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = 10.dp,
                    end = 10.dp,
                    bottom = if (!AppPreferences.showHostInLinkListView.value) 10.dp else 0.dp
                ),
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                maxLines = 3
            )
        }
        if (!linkComponentParam.isSelectionModeEnabled.value && AppPreferences.showHostInLinkListView.value) {
            Text(
                text = linkComponentParam.link.url.host(throwOnException = false),
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = 10.dp,
                    end = 10.dp,
                    bottom = if (AppPreferences.showMenuOnGridLinkClick) 10.dp else 0.dp
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
        if (!AppPreferences.showMenuOnGridLinkClick) {
            Box(
                modifier = Modifier.padding(top = if ((!AppPreferences.showTitleInLinkGridView.value && AppPreferences.showHostInLinkListView.value) || (AppPreferences.showTitleInLinkGridView.value && AppPreferences.showHostInLinkListView.value)) 10.dp else 0.dp)
                    .fillMaxWidth()
                    .background(ButtonDefaults.filledTonalButtonColors().containerColor)
                    .height(36.dp).clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = linkComponentParam.onMoreIconClick
                    ).pointerHoverIcon(PointerIcon.Hand), contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = null)
            }
        }
    }
}