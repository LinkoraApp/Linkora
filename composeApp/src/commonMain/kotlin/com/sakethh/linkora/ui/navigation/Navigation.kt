package com.sakethh.linkora.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Navigation {
    @Serializable
    sealed interface Root {
        @Serializable
        data object OnboardingSlidesScreen : Root

        @Serializable
        data object HomeScreen : Root

        @Serializable
        data object SearchScreen : Root

        @Serializable
        data object CollectionsScreen : Root

        @Serializable
        data object SettingsScreen : Root
    }

    @Serializable
    sealed interface Settings : Navigation {
        @Serializable
        data object ThemeSettingsScreen : Settings

        @Serializable
        data object GeneralSettingsScreen : Settings

        @Serializable
        data object AdvancedSettingsScreen : Settings

        @Serializable
        data object LayoutSettingsScreen : Settings

        @Serializable
        data object LanguageSettingsScreen : Settings

        @Serializable
        data object DataSettingsScreen : Settings

        @Serializable
        data object AboutScreen : Settings

        @Serializable
        data object AcknowledgementScreen : Settings

        @Serializable
        data object AboutLibraries : Settings

        @Serializable
        sealed interface Data : Settings {
            @Serializable
            data object ServerSetupScreen : Data

            @Serializable
            data object SnapshotsScreen : Data

            @Serializable
            data object WebPageCapturesScreen : Data {
                override fun toString(): String = "Web-page Captures"
            }
        }
    }

    @Serializable
    sealed interface Home : Navigation {
        @Serializable
        data object PanelsManagerScreen : Home

        @Serializable
        data object SpecificPanelManagerScreen : Home
    }

    @Serializable
    sealed interface Collection {
        @Serializable
        data class CollectionDetailScreen(
            val collectionDetailPaneInfo: String,
        ) : Collection
    }
}
