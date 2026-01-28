package com.sakethh.linkora.domain

import com.sakethh.linkora.preferences.AppPreferences

data class LinkSaveConfig(
    val forceAutoDetectTitle: Boolean,
    val forceSaveWithoutRetrievingData: Boolean,
    val skipSavingIfExists: Boolean = AppPreferences.skipSavingExistingLink.value,
    val forceSaveIfRetrievalFails: Boolean = AppPreferences.forceSaveIfRetrievalFails.value
) {
    companion object {
        fun forceSaveWithoutRetrieving(): LinkSaveConfig {
            return LinkSaveConfig(
                forceAutoDetectTitle = false, forceSaveWithoutRetrievingData = true
            )
        }
    }
}
