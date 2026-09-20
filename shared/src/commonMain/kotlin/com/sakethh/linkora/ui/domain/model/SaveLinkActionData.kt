package com.sakethh.linkora.ui.domain.model

data class SaveLinkActionData(
    val forceSaveWithoutFetchingAnyMetaData: Boolean,
    val isAutoDetectTitleEnabled: Boolean,
    val linkTextFieldValue: String,
    val titleTextFieldValue: String,
    val noteTextFieldValue: String,
    val selectedFolderName: String,
    val selectedFolderID: Long,
)
