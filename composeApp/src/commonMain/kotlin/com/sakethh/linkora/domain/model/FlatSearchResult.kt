package com.sakethh.linkora.domain.model

import androidx.room.Ignore
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import kotlinx.serialization.json.Json

data class FlatSearchResult(
    val itemType: String,

    val tagLocalId: Long? = null,
    val tagRemoteId: Long? = null,
    val tagName: String? = null,
    val tagLastModified: Long? = null,

    val folderName: String? = null,
    val folderNote: String? = null,
    val folderParentId: Long? = null,
    val folderLocalId: Long? = null,
    val folderRemoteId: Long? = null,
    val folderIsArchived: Boolean? = null,
    val folderLastModified: Long? = null,

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
    val linkLastModified: Long? = null,

    val linkTagsJson: String? = null
) {

    @Ignore
    @Transient
    var path: List<Folder>? = null


    val asTag: Tag by lazy {
        Tag(
            localId = tagLocalId!!, remoteId = tagRemoteId,
            name = tagName!!, lastModified = tagLastModified!!
        )
    }

    val asFolder: Folder by lazy {
        Folder(
            name = folderName!!, note = folderNote!!, parentFolderId = folderParentId,
            localId = folderLocalId!!, remoteId = folderRemoteId,
            isArchived = folderIsArchived!!, lastModified = folderLastModified!!
        ).also {
            it.path = path
        }
    }

    val asLinkTagsPair: LinkTagsPair by lazy {
        val link = Link(
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
            lastModified = linkLastModified!!
        )
        val tags = if (linkTagsJson.isNullOrBlank() || linkTagsJson == "[]") emptyList()
        else Json.decodeFromString<List<Tag>>(linkTagsJson)

        LinkTagsPair(link = link.also {
            it.path = path
        }, tags = tags)
    }
}