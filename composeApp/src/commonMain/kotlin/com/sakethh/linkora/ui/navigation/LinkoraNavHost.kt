package com.sakethh.linkora.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sakethh.linkora.di.SpecificPanelManagerVMFactory
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.domain.CurrentFABContext
import com.sakethh.linkora.ui.screens.collections.CollectionScreenParams
import com.sakethh.linkora.ui.screens.collections.CollectionsScreen
import com.sakethh.linkora.ui.screens.home.HomeScreen
import com.sakethh.linkora.ui.screens.home.panels.PanelsManagerScreen
import com.sakethh.linkora.ui.screens.home.panels.SpecificPanelManagerScreen
import com.sakethh.linkora.ui.screens.home.panels.SpecificPanelManagerScreenParam
import com.sakethh.linkora.ui.screens.home.panels.SpecificPanelManagerScreenVM
import com.sakethh.linkora.ui.screens.onboarding.OnboardingSlidesScreen
import com.sakethh.linkora.ui.screens.search.SearchScreen
import com.sakethh.linkora.ui.screens.settings.SettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.AboutLibrariesScreen
import com.sakethh.linkora.ui.screens.settings.section.AcknowledgementScreen
import com.sakethh.linkora.ui.screens.settings.section.AdvancedSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.GeneralSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.LanguageSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.LayoutSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.ThemeSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.about.AboutScreen
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreen
import com.sakethh.linkora.ui.screens.settings.section.data.snapshots.SnapshotsScreen
import com.sakethh.linkora.ui.screens.settings.section.data.sync.ServerSetupScreen

@Composable
fun LinkoraNavHost(
    preferences: AppPreferences,
    startDestination: Navigation.Root,
    onOnboardingComplete: () -> Unit,
    currentFABContext: (CurrentFABContext) -> Unit,
    collectionScreenParams: CollectionScreenParams,
    // An event can be pushed to the UIEvent flow instead of hoisting like this, but fine
    forceSearchActive: Boolean,
    cancelForceSearchActive: () -> Unit
) {
    val localNavController = LocalNavController.current

    val specificPanelManagerScreenVM: SpecificPanelManagerScreenVM = linkoraViewModel(
        factory = SpecificPanelManagerVMFactory.create(
            onAndroidMobile = Platform.Android.onMobile(),
            currentBackStackEntryFlow = localNavController.currentBackStackEntryFlow
        )
    )

    NavHost(
        navController = localNavController, startDestination = startDestination,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() },
    ) {
        composable<Navigation.Root.HomeScreen> {
            HomeScreen(currentFABContext)
        }
        composable<Navigation.Root.SearchScreen> {
            SearchScreen(
                currentFABContext = currentFABContext,
                forceActiveSearch = forceSearchActive,
                cancelForceSearchActive = cancelForceSearchActive
            )
        }
        composable<Navigation.Root.CollectionsScreen> {
            CollectionsScreen(
                collectionScreenParams = collectionScreenParams,
                currentFABContext = currentFABContext,
                preferences = preferences
            )
        }
        composable<Navigation.Root.SettingsScreen> {
            SettingsScreen(currentFABContext)
        }
        composable<Navigation.Settings.ThemeSettingsScreen> {
            ThemeSettingsScreen()
        }
        composable<Navigation.Settings.GeneralSettingsScreen> {
            GeneralSettingsScreen()
        }
        composable<Navigation.Settings.LayoutSettingsScreen> {
            LayoutSettingsScreen()
        }
        composable<Navigation.Settings.DataSettingsScreen> {
            DataSettingsScreen()
        }
        composable<Navigation.Settings.Data.ServerSetupScreen> {
            ServerSetupScreen()
        }
        composable<Navigation.Settings.LanguageSettingsScreen> {
            LanguageSettingsScreen()
        }
        composable<Navigation.Home.PanelsManagerScreen> {
            PanelsManagerScreen(
                currentFABContext,
                specificPanelManagerScreenParam = SpecificPanelManagerScreenParam(
                    foldersOfTheSelectedPanel = specificPanelManagerScreenVM.foldersOfTheSelectedPanel,
                    foldersToIncludeInPanel = specificPanelManagerScreenVM.foldersToIncludeInPanel,
                    foldersSearchQuery = specificPanelManagerScreenVM.foldersSearchQuery.value,
                    performAction = specificPanelManagerScreenVM::performAction
                ),
                performAction = specificPanelManagerScreenVM::performAction
            )
        }
        composable<Navigation.Home.SpecificPanelManagerScreen> {
            SpecificPanelManagerScreen(
                specificPanelManagerScreenParam = SpecificPanelManagerScreenParam(
                    foldersOfTheSelectedPanel = specificPanelManagerScreenVM.foldersOfTheSelectedPanel,
                    foldersToIncludeInPanel = specificPanelManagerScreenVM.foldersToIncludeInPanel,
                    foldersSearchQuery = specificPanelManagerScreenVM.foldersSearchQuery.value,
                    performAction = specificPanelManagerScreenVM::performAction
                )
            )
        }
        composable<Navigation.Settings.AboutScreen> {
            AboutScreen()
        }
        composable<Navigation.Settings.AcknowledgementScreen> {
            AcknowledgementScreen()
        }
        composable<Navigation.Settings.AboutLibraries> {
            AboutLibrariesScreen()
        }
        composable<Navigation.Settings.AdvancedSettingsScreen> {
            AdvancedSettingsScreen()
        }
        composable<Navigation.Settings.Data.SnapshotsScreen> {
            SnapshotsScreen()
        }
        composable<Navigation.Root.OnboardingSlidesScreen> {
            OnboardingSlidesScreen(
                onOnboardingComplete = onOnboardingComplete,
                preferences = preferences,
                currentFABContext = currentFABContext
            )
        }
    }
}