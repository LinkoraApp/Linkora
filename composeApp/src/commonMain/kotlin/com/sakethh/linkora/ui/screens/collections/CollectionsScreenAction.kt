package com.sakethh.linkora.ui.screens.collections

import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag

sealed interface CollectionsScreenAction {
    data class AddANewLink(
        val link: Link,
        val linkSaveConfig: LinkSaveConfig,
        val onCompletion: () -> Unit,
        val pushSnackbarOnSuccess: Boolean,
        val selectedTags: List<Tag>
    ) : CollectionsScreenAction

}