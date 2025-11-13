@file:Suppress("UNCHECKED_CAST")

package com.sakethh.linkora.di

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakethh.linkora.ui.components.sorting.SortingBtmSheetVM
import com.sakethh.linkora.ui.screens.search.SearchScreenVM
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.section.LanguageSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.about.AboutSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.sync.ServerManagementViewModel
import kotlin.reflect.KClass

@Composable
inline fun <reified T : ViewModel> linkoraViewModel(factory: ViewModelProvider.Factory = LinkoraViewModelFactory): T =
    viewModel(modelClass = T::class, factory = factory)

object LinkoraViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        return when (modelClass) {

            SortingBtmSheetVM::class -> SortingBtmSheetVM(
                preferencesRepository = DependencyContainer.preferencesRepo,
                nativeUtils = LinkoraSDK.getInstance().nativeUtils,
                permissionManager = LinkoraSDK.getInstance().permissionManager,
            )

            SearchScreenVM::class -> SearchScreenVM(
                localFoldersRepo = DependencyContainer.localFoldersRepo,
                localLinksRepo = DependencyContainer.localLinksRepo,
                localTagsRepo = DependencyContainer.localTagsRepo
            )

            SettingsScreenViewModel::class -> SettingsScreenViewModel(
                preferencesRepository = DependencyContainer.preferencesRepo,
                nativeUtils = LinkoraSDK.getInstance().nativeUtils,
                permissionManager = LinkoraSDK.getInstance().permissionManager
            )

            LanguageSettingsScreenVM::class -> DependencyContainer.localizationRepo.let {
                LanguageSettingsScreenVM(it, it)
            }

            AboutSettingsScreenVM::class -> AboutSettingsScreenVM(
                localLinksRepo = DependencyContainer.localLinksRepo,
                gitHubReleasesRepo = DependencyContainer.gitHubReleasesRepo
            )

            ServerManagementViewModel::class -> ServerManagementViewModel(
                DependencyContainer.networkRepo,
                DependencyContainer.preferencesRepo,
                DependencyContainer.remoteSyncRepo,
                fileManager = LinkoraSDK.getInstance().fileManager,
                permissionManager = LinkoraSDK.getInstance().permissionManager,
            )

            DataSettingsScreenVM::class -> DataSettingsScreenVM(
                exportDataRepo = DependencyContainer.exportDataRepo,
                importDataRepo = DependencyContainer.importDataRepo,
                linksRepo = DependencyContainer.localLinksRepo,
                preferencesRepository = DependencyContainer.preferencesRepo,
                remoteSyncRepo = DependencyContainer.remoteSyncRepo,
                nativeUtils = LinkoraSDK.getInstance().nativeUtils,
                fileManager = LinkoraSDK.getInstance().fileManager,
                permissionManager = LinkoraSDK.getInstance().permissionManager,
            )

            else -> error("Not sure how to create an instance of ${modelClass.simpleName}, maybe it's available in *AssistedFactory")
        } as T
    }
}