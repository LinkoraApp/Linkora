package com.sakethh.linkora.domain

import com.sakethh.linkora.Localization
import com.sakethh.linkora.utils.getLocalizedString
import kotlinx.serialization.Serializable

@Serializable
enum class SnapshotFormat(
    val localizedValue: String,
    val id: Int,
) {
    HTML(localizedValue = "HTML", id = 0),
    JSON(localizedValue = "JSON", id = 1),
    BOTH(id = 2, localizedValue = Localization.Key.Both.getLocalizedString()),
}
