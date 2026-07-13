package com.sakethh.linkora.ui.domain.model

import com.sakethh.linkora.ui.domain.SortingType

data class SortingBtmSheet(
    val sortingName: String,
    val onClick: () -> Unit,
    val sortingType: SortingType,
)
