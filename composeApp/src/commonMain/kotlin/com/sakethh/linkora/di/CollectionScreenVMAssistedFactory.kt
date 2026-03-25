package com.sakethh.linkora.di

import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavController
import com.sakethh.linkora.ui.screens.collections.CollectionDetailPaneVM
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM

object CollectionScreenVMAssistedFactory {

    fun createForApp() = viewModelFactory {
        initializer {
            CollectionsScreenVM(
                localFoldersRepo = DependencyContainer.localFoldersRepo,
                localLinksRepo = DependencyContainer.localLinksRepo,
                localTagsRepo = DependencyContainer.localTagsRepo,
                preferencesRepo = DependencyContainer.preferencesRepo,
            )
        }
    }

    // This shouldn't be in the common codebase since it's specific to Android,
    // but the object name seems to conflict when redeclared with the same name.
    // For now, let's keep it here.
    fun createForIntentActivity() = viewModelFactory {
        initializer {
            CollectionsScreenVM(
                localFoldersRepo = DependencyContainer.localFoldersRepo,
                localLinksRepo = DependencyContainer.localLinksRepo,
                localTagsRepo = DependencyContainer.localTagsRepo,
            )
        }
    }
}