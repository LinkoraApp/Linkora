package com.sakethh.linkora.domain.dto.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Asset(
    @SerialName("browser_download_url") val directDownloadURL: String,
)
