package com.sakethh.linkora.di

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavBackStackEntry
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.ui.screens.home.panels.SpecificPanelManagerScreenVM
import kotlinx.coroutines.flow.Flow

object SpecificPanelManagerVMFactory {
    fun create(
        onAndroidMobile: Boolean, currentBackStackEntryFlow: Flow<NavBackStackEntry>
    ) = viewModelFactory {
        initializer {
            SpecificPanelManagerScreenVM(
                localFoldersRepo = DependencyContainer.localFoldersRepo,
                localPanelsRepo = DependencyContainer.localPanelsRepo,
                preferencesRepository = DependencyContainer.preferencesRepo,
                currentBackStackEntryFlow = currentBackStackEntryFlow,
                onAndroidMobile = onAndroidMobile
            )
        }
    }
}