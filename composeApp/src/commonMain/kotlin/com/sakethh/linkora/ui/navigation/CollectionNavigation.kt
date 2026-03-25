package com.sakethh.linkora.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface CollectionNavigation {
    @Serializable
    data object Empty : CollectionNavigation

    @Serializable
    data class Pane(val collectionDetailPaneInfo: String) : CollectionNavigation
}