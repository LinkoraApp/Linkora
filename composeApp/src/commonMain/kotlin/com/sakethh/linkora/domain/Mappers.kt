package com.sakethh.linkora.domain

import androidx.compose.runtime.Composable
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.dto.server.folder.AddFolderDTO
import com.sakethh.linkora.domain.dto.server.folder.FolderDTO
import com.sakethh.linkora.domain.dto.server.link.AddLinkDTO
import com.sakethh.linkora.domain.dto.server.link.LinkDTO
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.PanelForJSONExportSchema
import com.sakethh.linkora.domain.model.legacy.LegacyExportSchema
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.LinkTagDTO
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.catchAsThrowableAndEmitFailure
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun LinkType.asMenuBtmSheetType(): MenuBtmSheetType.Link = when (this) {
    LinkType.SAVED_LINK -> MenuBtmSheetType.Link.SavedLink
    LinkType.FOLDER_LINK -> MenuBtmSheetType.Link.FolderLink
    LinkType.HISTORY_LINK -> MenuBtmSheetType.Link.HistoryLink
    LinkType.IMPORTANT_LINK -> MenuBtmSheetType.Link.ImportantLink
    LinkType.ARCHIVE_LINK -> MenuBtmSheetType.Link.ArchiveLink
}

fun <T> Flow<T>.mapToResultFlow(): Flow<Result<T>> = this.map {
    Result.Success(it)
}
    .catchAsThrowableAndEmitFailure()

fun Long.asLinkType() = when (this) {
    Constants.SAVED_LINKS_ID -> LinkType.SAVED_LINK

    Constants.HISTORY_ID -> LinkType.HISTORY_LINK

    Constants.IMPORTANT_LINKS_ID -> LinkType.IMPORTANT_LINK

    Constants.ARCHIVE_ID -> LinkType.ARCHIVE_LINK

    else -> {
        LinkType.FOLDER_LINK
    }
}

@Composable
fun LinkType.asLocalizedString(): String {
    val localizedStrings = LocalizedStrings.current
    return when (this) {
        LinkType.SAVED_LINK -> localizedStrings.SavedLinks
        LinkType.FOLDER_LINK -> localizedStrings.FolderLinks
        LinkType.HISTORY_LINK -> localizedStrings.HistoryLinks
        LinkType.IMPORTANT_LINK -> localizedStrings.ImportantLinks
        LinkType.ARCHIVE_LINK -> localizedStrings.ArchiveLinks
    }
}

@Composable
fun FolderType.asLocalizedString(): String {
    val localizedStrings = LocalizedStrings.current
    return when (this) {
        FolderType.REGULAR_FOLDER -> localizedStrings.RegularFolder
        FolderType.ARCHIVE_FOLDER -> localizedStrings.ArchiveFolder
    }
}

fun Folder.asAddFolderDTO(correlation: Correlation): AddFolderDTO = AddFolderDTO(
    name = this.name,
    note = this.note,
    parentFolderId = this.parentFolderId,
    isArchived = this.isArchived,
    eventTimestamp = this.lastModified,
    correlation = correlation,
)

fun Folder.asFolderDTO(
    remoteId: Long,
    remoteParentFolderId: Long?,
    correlation: Correlation,
): FolderDTO = FolderDTO(
    id = remoteId,
    name = this.name,
    note = this.note,
    parentFolderId = remoteParentFolderId,
    isArchived = this.isArchived,
    eventTimestamp = this.lastModified,
    correlation = correlation,
)

fun Link.asAddLinkDTO(
    remoteTagIds: List<Long>,
    correlation: Correlation,
): AddLinkDTO = AddLinkDTO(
    linkType = this.linkType,
    title = this.title,
    url = this.url,
    baseURL = this.host,
    imgURL = this.imgURL,
    note = this.note,
    eventTimestamp = this.lastModified,
    idOfLinkedFolder = this.idOfLinkedFolder,
    userAgent = this.userAgent,
    markedAsImportant = false,
    mediaType = this.mediaType,
    tags = remoteTagIds,
    correlation = correlation,
)

fun Link.asLinkDTO(
    id: Long,
    remoteLinkTags: List<LinkTagDTO>,
    correlation: Correlation,
): LinkDTO = LinkDTO(
    linkType = this.linkType,
    title = this.title,
    url = this.url,
    baseURL = this.host,
    imgURL = this.imgURL,
    note = this.note,
    idOfLinkedFolder = this.idOfLinkedFolder,
    userAgent = this.userAgent,
    markedAsImportant = false,
    mediaType = this.mediaType,
    id = id,
    eventTimestamp = this.lastModified,
    linkTags = remoteLinkTags,
    correlation = correlation,
)

suspend fun LegacyExportSchema.asJSONExportSchema(userAgent: String): JSONExportSchema = coroutineScope {
        val links = mutableListOf<Link>()
        val folders = mutableListOf<Folder>()
        val panels = mutableListOf<Panel>()
        val panelFolders = mutableListOf<PanelFolder>()
        awaitAll(
            async {
                links.addAll(
                    this@asJSONExportSchema.linksTable.map {
                        Link(
                            linkType =
                                if (it.isLinkedWithSavedLinks) {
                                    LinkType.SAVED_LINK
                                } else {
                                    LinkType.FOLDER_LINK
                                },
                            title = it.title,
                            url = it.webURL,
                            imgURL = it.imgURL,
                            note = it.infoForSaving,
                            idOfLinkedFolder = it.keyOfLinkedFolderV10,
                            localId = it.id,
                            userAgent = userAgent,
                        )
                    },
                )
            },
            async {
                links.addAll(
                    this@asJSONExportSchema.importantLinksTable.map {
                        Link(
                            linkType = LinkType.IMPORTANT_LINK,
                            title = it.title,
                            url = it.webURL,
                            imgURL = it.imgURL,
                            note = it.infoForSaving,
                            idOfLinkedFolder = null,
                            localId = it.id,
                            userAgent = userAgent,
                        )
                    },
                )
            },
            async {
                links.addAll(
                    this@asJSONExportSchema.archivedLinksTable.map {
                        Link(
                            linkType = LinkType.ARCHIVE_LINK,
                            title = it.title,
                            url = it.webURL,
                            imgURL = it.imgURL,
                            note = it.infoForSaving,
                            idOfLinkedFolder = null,
                            localId = it.id,
                            userAgent = userAgent,
                        )
                    },
                )
            },
            async {
                links.addAll(
                    this@asJSONExportSchema.historyLinksTable.map {
                        Link(
                            linkType = LinkType.HISTORY_LINK,
                            title = it.title,
                            url = it.webURL,
                            imgURL = it.imgURL,
                            note = it.infoForSaving,
                            idOfLinkedFolder = null,
                            localId = it.id,
                            userAgent = userAgent,
                        )
                    },
                )
            },
            async {
                folders.addAll(
                    this@asJSONExportSchema.foldersTable.map {
                        Folder(
                            name = it.folderName,
                            note = it.infoForSaving,
                            parentFolderId = it.parentFolderID,
                            isArchived = it.isFolderArchived,
                            localId = it.id,
                        )
                    },
                )
            },
            async {
                panels.addAll(
                    this@asJSONExportSchema.panels.map {
                        Panel(panelName = it.panelName, localId = it.panelId)
                    },
                )
            },
            async {
                panelFolders.addAll(
                    this@asJSONExportSchema.panelFolders.map {
                        PanelFolder(
                            localId = it.id,
                            folderId = it.folderId,
                            panelPosition = it.panelPosition,
                            folderName = it.folderName,
                            connectedPanelId = it.connectedPanelId,
                        )
                    },
                )
            },
        )
        return@coroutineScope JSONExportSchema(
            schemaVersion = this@asJSONExportSchema.schemaVersion,
            links = links.toList(),
            folders = folders.toList(),
            panels =
                PanelForJSONExportSchema(
                    panels = panels.toList(),
                    panelFolders = panelFolders.toList(),
                ),
        )
    }
