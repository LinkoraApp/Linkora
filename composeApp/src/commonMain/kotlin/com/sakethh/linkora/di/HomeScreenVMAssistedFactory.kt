package com.sakethh.linkora.di

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakethh.linkora.ui.screens.home.HomeScreenVM

object HomeScreenVMAssistedFactory {
    fun createForPanelsManagerScreen() = viewModelFactory {
        initializer {
            HomeScreenVM(
                localLinksRepo = DependencyContainer.localLinksRepo,
                localPanelsRepo = DependencyContainer.localPanelsRepo,
                triggerCollectionOfPanels = true,
                triggerCollectionOfPanelFolders = false,
                preferencesRepository = DependencyContainer.preferencesRepo,
                localDatabaseUtilsRepo = DependencyContainer.localDatabaseUtilsImpl
            )
        }
    }

    fun createForHomeScreen() = viewModelFactory {
        initializer {
            HomeScreenVM(
                localDatabaseUtilsRepo = DependencyContainer.localDatabaseUtilsImpl,
                localPanelsRepo = DependencyContainer.localPanelsRepo,
                localLinksRepo = DependencyContainer.localLinksRepo,
                preferencesRepository = DependencyContainer.preferencesRepo,
            )
        }
    }
}