package com.sakethh.linkora.domain.dto.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDTOItem(
    val assets: List<Asset>,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("html_url") val releasePageURL: String,
    @SerialName("name") val releaseName: String,
    @SerialName("tag_name") val tagName: String,
)
