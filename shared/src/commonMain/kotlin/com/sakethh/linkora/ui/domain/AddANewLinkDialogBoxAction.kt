package com.sakethh.linkora.ui.domain

import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag

sealed interface AddANewLinkDialogBoxAction {
    data class UnSelectATag(
        val tag: Tag,
    ) : AddANewLinkDialogBoxAction

    data class SelectATag(
        val tag: Tag,
    ) : AddANewLinkDialogBoxAction

    data object ClearSelectedTags : AddANewLinkDialogBoxAction

    data class AddANewLink(
        val link: Link,
        val linkSaveConfig: LinkSaveConfig,
        val onCompletion: () -> Unit,
        val pushSnackbarOnSuccess: Boolean,
        val selectedTags: List<Tag>,
    ) : AddANewLinkDialogBoxAction

    data class UpdateFoldersSearchQuery(
        val string: String,
    ) : AddANewLinkDialogBoxAction

    data class CreateATag(
        val tagName: String,
        val onCompletion: () -> Unit,
    ) : AddANewLinkDialogBoxAction

    data class InsertANewFolder(
        val folder: Folder,
        val onCompletion: () -> Unit,
    ) : AddANewLinkDialogBoxAction

    data object OnRetrieveNextTagsPage : AddANewLinkDialogBoxAction

    data class OnFirstVisibleIndexChangeOfTags(
        val index: Long,
    ) : AddANewLinkDialogBoxAction

    data object OnRetrieveNextRegularRootPage : AddANewLinkDialogBoxAction

    data class OnFirstVisibleIndexChangeOfRootFolders(
        val index: Long,
    ) : AddANewLinkDialogBoxAction
}
