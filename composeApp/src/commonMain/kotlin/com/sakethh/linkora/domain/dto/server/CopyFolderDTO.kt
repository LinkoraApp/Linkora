package com.sakethh.linkora.domain.dto.server

import kotlinx.serialization.Serializable

@Serializable
data class CopyFolderDTO(
    val currentFolder: CurrentFolder,
    val links: List<FolderLink>,
)

@Serializable
data class CurrentFolder(
    val newlyCopiedLocalId: Long,
    val parentOfNewlyCopiedLocalId: Long,
    val sourceRemoteId: Long,
    val sourceRemoteParentId: Long?,
    val isRootFolderForTheDestination: Boolean,
)

typealias FolderLink = CurrentFolder
