package com.sakethh.linkora.domain.dto.server.folder

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.dto.server.Correlation
import kotlinx.serialization.Serializable

@Serializable
data class UpdateFolderNoteDTO(
    val folderId: Long,
    val newNote: String,
    val eventTimestamp: Long,
    val correlation: Correlation? = null
)