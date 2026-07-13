package com.sakethh.linkora.domain

data class LinkSaveConfig(
    val forceAutoDetectTitle: Boolean,
    val forceSaveWithoutRetrievingData: Boolean,
    val useProxy: Boolean,
    val skipSavingIfExists: Boolean,
    val forceSaveIfRetrievalFails: Boolean,
) {
    companion object {
        fun forceSaveWithoutRetrieving(): LinkSaveConfig = LinkSaveConfig(
            forceAutoDetectTitle = false,
            forceSaveWithoutRetrievingData = true,
            useProxy = false,
            skipSavingIfExists = true,
            forceSaveIfRetrievalFails = true,
        )
    }
}
