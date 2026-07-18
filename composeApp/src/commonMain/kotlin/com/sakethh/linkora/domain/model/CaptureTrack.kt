package com.sakethh.linkora.domain.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity
data class CaptureTrack(
    @PrimaryKey val capturedLinkId: Long,
)
