package com.sakethh.linkora.ui.domain.model

import androidx.compose.runtime.Stable
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class LinkTagsPair(
    val link: Link,
    val tags: List<Tag>,
)
