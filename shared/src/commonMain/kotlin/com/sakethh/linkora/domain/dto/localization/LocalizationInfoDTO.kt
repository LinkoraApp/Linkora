package com.sakethh.linkora.domain.dto.localization

import kotlinx.serialization.Serializable

@Serializable
data class LocalizationInfoDTO(
    val availableLanguages: List<AvailableLanguageDTO>,
    val totalAvailableLanguages: Int,
    val totalStrings: Int,
    val lastUpdatedOn: String,
)
