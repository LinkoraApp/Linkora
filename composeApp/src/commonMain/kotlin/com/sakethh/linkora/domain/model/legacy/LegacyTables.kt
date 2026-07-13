package com.sakethh.linkora.domain.model.legacy

import kotlinx.serialization.Serializable

@Serializable
data class LinksTable(
    val id: Long = 0,
    val title: String,
    val webURL: String,
    val baseURL: String,
    val imgURL: String,
    val infoForSaving: String,
    val isLinkedWithSavedLinks: Boolean,
    val isLinkedWithFolders: Boolean,
    val keyOfLinkedFolderV10: Long? = null,
    val keyOfLinkedFolder: String? = null,
    val isLinkedWithImpFolder: Boolean,
    val keyOfImpLinkedFolder: String,
    val keyOfImpLinkedFolderV10: Long? = null,
    val isLinkedWithArchivedFolder: Boolean,
    val keyOfArchiveLinkedFolderV10: Long? = null,
    val keyOfArchiveLinkedFolder: String? = null,
    val userAgent: String? = null,
)

@Serializable
data class FoldersTable(
    val folderName: String,
    val infoForSaving: String,
    val id: Long = 0,
    val parentFolderID: Long? = null,
    val childFolderIDs: List<Long>? = null,
    val isFolderArchived: Boolean = false,
    val isMarkedAsImportant: Boolean = false,
)

@Serializable
data class ArchivedLinks(
    val title: String,
    val webURL: String,
    val baseURL: String,
    val imgURL: String,
    val infoForSaving: String,
    val userAgent: String? = null,
    val id: Long = 0,
)

@Serializable
data class ArchivedFolders(
    val archiveFolderName: String,
    val infoForSaving: String,
    val id: Long = 0,
)

@Serializable
data class ImportantLinks(
    val title: String,
    val webURL: String,
    val baseURL: String,
    val imgURL: String,
    val infoForSaving: String,
    val userAgent: String? = null,
    val id: Long = 0,
)

@Serializable
data class RecentlyVisited(
    val id: Long = 0,
    val title: String,
    val webURL: String,
    val baseURL: String,
    val imgURL: String,
    val infoForSaving: String,
    val userAgent: String? = null,
)

@Serializable
data class PanelFolder(
    val id: Long = 0,
    val folderId: Long,
    val panelPosition: Long,
    val folderName: String,
    val connectedPanelId: Long,
)

@Serializable
data class Panel(
    val panelId: Long = 0,
    val panelName: String,
)
