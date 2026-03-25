package com.sakethh.linkora.domain.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity
data class RefreshLink(
    @PrimaryKey
    val refreshedLinkId: Long
)
