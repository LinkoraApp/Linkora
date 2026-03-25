package com.sakethh.linkora.ui.screens.collections

import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.domain.AddANewLinkDialogBoxAction

sealed interface CollectionPaneAction {
    data class ToggleAllLinksFilter(val filter: LinkType) : CollectionPaneAction
    data class OnFirstVisibleItemIndexChangeOfLinkTagsPair(val index: Long): CollectionPaneAction
    data object RetrieveNextLinksPage: CollectionPaneAction
    data object RetrieveNextRootArchivedFolderPage: CollectionPaneAction

    data class OnFirstVisibleItemIndexChangeOfRootArchivedFolders(val index: Long): CollectionPaneAction

    data class AddANewLink(
        val link: Link,
        val linkSaveConfig: LinkSaveConfig,
        val onCompletion: () -> Unit,
        val pushSnackbarOnSuccess: Boolean,
        val selectedTags: List<Tag>
    ) : CollectionPaneAction
}