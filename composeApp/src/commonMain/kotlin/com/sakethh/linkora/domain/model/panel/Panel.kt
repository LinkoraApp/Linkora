package com.sakethh.linkora.domain.model.panel

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.sakethh.linkora.utils.getSystemEpochSeconds
import kotlinx.serialization.Serializable

@Serializable
@Entity("panel")
data class Panel(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val panelName: String,
    val remoteId: Long? = null,
    val lastModified: Long = getSystemEpochSeconds()
)