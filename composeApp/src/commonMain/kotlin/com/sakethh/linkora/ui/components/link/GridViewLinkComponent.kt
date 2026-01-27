package com.sakethh.linkora.ui.components.link


import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
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
                    contentScale = if (linkComponentParam.link.imgURL.startsWith("https://pbs.twimg.com/profile_images/") || !forStaggeredView) ContentScale.Crop else ContentScale.Fit,
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

        val showTitle by rememberSaveable(
            AppPreferences.showTitleInLinkGridView.value,
            linkComponentParam.link.title.isNotBlank()
        ) {
            mutableStateOf(
                AppPreferences.showTitleInLinkGridView.value && linkComponentParam.link.title.isNotBlank()
            )
        }
        val showNote by rememberSaveable(
            AppPreferences.showNoteInLinkView.value,
            linkComponentParam.link.note.isNotBlank()
        ) {
            mutableStateOf(
                AppPreferences.showNoteInLinkView.value && linkComponentParam.link.note.isNotBlank()
            )
        }
        val showDate by rememberSaveable(
            AppPreferences.showDateInLinkView,
            linkComponentParam.link.date != null
        ) {
            mutableStateOf(
                AppPreferences.showDateInLinkView && linkComponentParam.link.date != null
            )
        }
        val showTags by
        rememberSaveable(AppPreferences.showTagsInLinkView, linkComponentParam.tags != null) {
            mutableStateOf(
                AppPreferences.showTagsInLinkView && linkComponentParam.tags != null
            )
        }
        val showHost by rememberSaveable(
            !linkComponentParam.isSelectionModeEnabled.value,
            AppPreferences.showHostInLinkListView.value
        ) {
            mutableStateOf(
                !linkComponentParam.isSelectionModeEnabled.value && AppPreferences.showHostInLinkListView.value
            )
        }
        if (showTitle) {
            Text(
                text = linkComponentParam.link.title,
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = 10.dp,
                    end = 10.dp,
                ),
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                maxLines = 3
            )
        }

        if (showNote) {
            Text(
                text = linkComponentParam.link.note,
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = 10.dp,
                    end = 10.dp,
                ),
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.75f),
                maxLines = 3
            )
        }

        if (showDate) {
            Text(
                modifier = Modifier.padding(
                    end = 15.dp, top = 10.dp, start = 10.dp
                ),
                text = linkComponentParam.link.date ?: "",
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.45.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(0.65f)
            )
        }

        if (showTags) {
            Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp).fillMaxWidth()) {
                TagsRow(
                    modifier = Modifier.fillMaxWidth(),
                    tags = linkComponentParam.tags ?: emptyList(),
                    onTagClick = {
                        linkComponentParam.onTagClick(it)
                    })
            }
        }

        val foldersPath = retain {
            linkComponentParam.link.path
        }

        if (linkComponentParam.showPath && !foldersPath.isNullOrEmpty()) {
            Text(
                text = "Folder Path",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 5.dp, start = 10.dp)
            )
            FoldersRow(
                modifier = Modifier.fillMaxWidth().padding(
                    start = 10.dp,
                ), folders = foldersPath, onFolderClick = { linkComponentParam.onFolderClick(it) })
        }

        if (showHost) {
            Text(
                text = linkComponentParam.link.url.host(throwOnException = false),
                modifier = Modifier.padding(
                    start = 10.dp,
                    top = if (!AppPreferences.showTagsInLinkView) 10.dp else 5.dp,
                    end = 10.dp,
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
        val addTopSpacing by retain {
            derivedStateOf {
                showTitle || showNote || showDate || showHost
            }
        }
        if (!AppPreferences.showMenuOnGridLinkClick) {
            Box(
                modifier = Modifier.padding(top = if (addTopSpacing) 10.dp else 0.dp)
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
        } else if (addTopSpacing) {
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}