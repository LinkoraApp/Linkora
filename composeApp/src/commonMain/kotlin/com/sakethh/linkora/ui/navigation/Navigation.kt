package com.sakethh.linkora.ui.navigation

import com.sakethh.linkora.common.Localization
import kotlinx.serialization.Serializable

@Serializable
sealed interface Navigation {

    @Serializable
    sealed interface Root {

        @Serializable
        data object OnboardingSlidesScreen : Root

        @Serializable
        data object HomeScreen : Root {
            override fun toString(): String = Localization.getLocalizedString(Localization.Key.Home)
        }

        @Serializable
        data object SearchScreen : Root {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.Search)
        }

        @Serializable
        data object CollectionsScreen : Root {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.Collections)
        }

        @Serializable
        data object SettingsScreen : Root {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.Settings)
        }
    }

    @Serializable
    sealed interface Settings : Navigation {

        @Serializable
        data object ThemeSettingsScreen : Settings {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.Theme)
        }

        @Serializable
        data object GeneralSettingsScreen : Settings {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.General)
        }

        @Serializable
        data object AdvancedSettingsScreen : Settings {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.Advanced)
        }

        @Serializable
        data object LayoutSettingsScreen : Settings {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.Layout)
        }

        @Serializable
        data object LanguageSettingsScreen : Settings {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.Language)
        }

        @Serializable
        data object DataSettingsScreen : Settings {
            override fun toString(): String = Localization.getLocalizedString(Localization.Key.Data)
        }

        @Serializable
        data object AboutSettingsScreen : Settings {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.About)
        }

        @Serializable
        data object AcknowledgementSettingsScreen : Settings {
            override fun toString(): String =
                Localization.getLocalizedString(Localization.Key.Acknowledgments)
        }

        @Serializable
        sealed interface Data : Settings {
            @Serializable
            data object ServerSetupScreen : Data {
                override fun toString(): String =
                    Localization.getLocalizedString(Localization.Key.LinkoraServerSetup)
            }
        }

        @Serializable
        data object RemindersSettingsScreen : Data {
            override fun toString(): String = TODO()
        }
    }

    @Serializable
    sealed interface Home : Navigation {
        @Serializable
        data object PanelsManagerScreen : Home {
            override fun toString(): String {
                return Localization.getLocalizedString(Localization.Key.Panels)
            }
        }

        @Serializable
        data object SpecificPanelManagerScreen : Home
    }

    @Serializable
    sealed interface Collection {
        @Serializable
        data object CollectionDetailPane : Collection {
            override fun toString(): String {
                return Localization.getLocalizedString(Localization.Key.CollectionDetailPane)
            }
        }
    }
}