package com.sakethh.linkora.domain.dto.twitter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaExtended(
    @SerialName("thumbnail_url") val thumbnailUrl: String,
    val type: String, // video || image
    val url: String,
)
