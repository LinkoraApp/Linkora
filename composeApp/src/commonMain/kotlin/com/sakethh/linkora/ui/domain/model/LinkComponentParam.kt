package com.sakethh.linkora.ui.domain.model

import androidx.compose.runtime.MutableState
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.model.link.Link

data class LinkComponentParam(
    val link: Link,
    val tags: List<Tag>?,
    val isSelectionModeEnabled: MutableState<Boolean>,
    val onMoreIconClick: () -> Unit,
    val onLinkClick: () -> Unit,
    val onForceOpenInExternalBrowserClicked: () -> Unit,
    val isItemSelected: MutableState<Boolean>,
    val onLongClick: () -> Unit,
    val onTagClick: (tag: Tag) -> Unit
)
