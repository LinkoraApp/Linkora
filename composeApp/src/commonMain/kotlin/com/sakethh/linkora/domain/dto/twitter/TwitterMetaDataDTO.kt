package com.sakethh.linkora.domain.dto.twitter

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TwitterMetaDataDTO(
    val hasMedia: Boolean,
    @SerialName("media_extended") val media: List<MediaExtended>,
    val text: String,
    @SerialName("user_profile_image_url") val userPfp: String,
    val tweetURL: String,
)
