package com.sakethh.linkora.domain.model.panel

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity("panel")
data class Panel(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val panelName: String,
    val remoteId: Long? = null
)