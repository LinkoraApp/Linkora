package com.sakethh.linkora.domain.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "snapshot")
data class Snapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, val content: String
)
