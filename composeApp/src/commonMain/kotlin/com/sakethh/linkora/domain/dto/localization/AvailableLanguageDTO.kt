package com.sakethh.linkora.domain.dto.localization

import kotlinx.serialization.Serializable

@Serializable
data class AvailableLanguageDTO(
    val languageCode: String,
    val localizedName: String,
    val localizedStringsCount: Int,
)
