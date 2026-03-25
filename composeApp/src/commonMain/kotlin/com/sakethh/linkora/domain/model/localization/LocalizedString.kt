package com.sakethh.linkora.domain.model.localization

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity("localized_strings")
data class LocalizedString(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val languageCode: String,
    val stringName: String,
    val stringValue: String
)