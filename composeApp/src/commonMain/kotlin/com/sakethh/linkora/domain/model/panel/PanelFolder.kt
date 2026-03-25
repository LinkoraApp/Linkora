package com.sakethh.linkora.domain.model.panel

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.sakethh.linkora.utils.getSystemEpochSeconds
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
@Entity(tableName = "panel_folder")
data class PanelFolder(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val remoteId: Long? = null,
    val folderId: Long,
    val panelPosition: Long,
    val folderName: String,
    val connectedPanelId: Long,
    val lastModified: Long = getSystemEpochSeconds()
)