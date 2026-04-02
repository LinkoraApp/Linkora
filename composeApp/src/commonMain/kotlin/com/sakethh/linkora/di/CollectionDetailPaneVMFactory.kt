package com.sakethh.linkora.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakethh.linkora.ui.domain.model.CollectionDetailPaneInfo
import com.sakethh.linkora.ui.screens.collections.CollectionDetailPaneVM

object CollectionDetailPaneVMFactory {

    fun create(collectionDetailPaneInfo: CollectionDetailPaneInfo): ViewModelProvider.Factory {
        return viewModelFactory {
            initializer {
                CollectionDetailPaneVM(
                    localFoldersRepo = DependencyContainer.localFoldersRepo,
                    localLinksRepo = DependencyContainer.localLinksRepo,
                    localTagsRepo = DependencyContainer.localTagsRepo,
                    localDatabaseUtilsRepo = DependencyContainer.localDatabaseUtilsImpl,
                    preferencesRepository = DependencyContainer.preferencesRepo,
                    collectionDetailPaneInfo = collectionDetailPaneInfo,
                )
            }
        }
    }
}