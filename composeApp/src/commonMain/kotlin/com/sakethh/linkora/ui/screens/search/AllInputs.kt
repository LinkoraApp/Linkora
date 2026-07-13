package com.sakethh.linkora.ui.screens.search

import com.sakethh.linkora.domain.FolderType
import com.sakethh.linkora.domain.LinkType

data class AllInputs(
    val query: String,
    val sortingType: String,
    val appliedFolderFilters: List<FolderType>,
    val appliedLinkFilters: List<LinkType>,
    val isTagFilterApplied: Boolean,
)
