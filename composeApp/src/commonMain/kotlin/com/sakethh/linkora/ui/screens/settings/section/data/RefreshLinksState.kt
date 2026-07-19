package com.sakethh.linkora.ui.screens.settings.section.data

data class RefreshLinksState(
    val isInRefreshingState: Boolean,
    val currentIteration: Int,
    val total: Int,
)
