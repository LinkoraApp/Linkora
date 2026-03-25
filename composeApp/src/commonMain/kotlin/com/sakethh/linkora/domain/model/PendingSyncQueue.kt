package com.sakethh.linkora.domain.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity("pending_sync_queue")
data class PendingSyncQueue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operation: String,
    val payload: String
)
