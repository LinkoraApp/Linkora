package com.sakethh.linkora.domain.model

import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import kotlinx.serialization.json.Json

data class FlatChildFolderData(
    val itemType: String,
    val lastModified: Long,
    val folderName: String? = null,
    val folderNote: String? = null,
    val folderParentId: Long? = null,
    val folderLocalId: Long? = null,
    val folderRemoteId: Long? = null,
    val folderIsArchived: Boolean? = null,
    val linkType: LinkType? = null,
    val linkLocalId: Long? = null,
    val linkRemoteId: Long? = null,
    val linkTitle: String? = null,
    val linkUrl: String? = null,
    val linkHost: String? = null,
    val linkImgUrl: String? = null,
    val linkNote: String? = null,
    val linkIdOfLinkedFolder: Long? = null,
    val linkUserAgent: String? = null,
    val linkMediaType: MediaType? = null,
    val linkTagsJson: String? = null,
) {
    val asFolder: Folder by lazy {
        Folder(
            name = folderName!!,
            note = folderNote!!,
            parentFolderId = folderParentId,
            localId = folderLocalId!!,
            remoteId = folderRemoteId,
            isArchived = folderIsArchived!!,
            lastModified = lastModified,
        )
    }

    val asLinkTagsPair: LinkTagsPair by lazy {
        val link =
            Link(
                linkType = linkType!!,
                localId = linkLocalId!!,
                remoteId = linkRemoteId,
                title = linkTitle!!,
                url = linkUrl!!,
                host = linkHost!!,
                imgURL = linkImgUrl!!,
                note = linkNote!!,
                idOfLinkedFolder = linkIdOfLinkedFolder,
                userAgent = linkUserAgent,
                mediaType = linkMediaType!!,
                lastModified = lastModified,
            )
        val tags =
            if (linkTagsJson.isNullOrBlank()) {
                emptyList()
            } else {
                Json.decodeFromString<List<Tag>>(linkTagsJson)
            }

        LinkTagsPair(link = link, tags = tags)
    }
}
