package com.sakethh.linkora.di

import androidx.compose.ui.unit.Density
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sakethh.linkora.ui.AppVM

object APPVMAssistedFactory {
    fun createForApp(localDensity: Density) = viewModelFactory {
        initializer {
            AppVM(
                density = localDensity,
                preferencesRepository = DependencyContainer.preferencesRepo,
                linksRepo = DependencyContainer.localLinksRepo,
                foldersRepo = DependencyContainer.localFoldersRepo,
                localMultiActionRepo = DependencyContainer.localMultiActionRepo,
                localPanelsRepo = DependencyContainer.localPanelsRepo,
                permissionManager = LinkoraSDK.getInstance().permissionManager,
                fileManager = LinkoraSDK.getInstance().fileManager,
                snapshotRepo = DependencyContainer.snapshotRepo,
                nativeUtils = LinkoraSDK.getInstance().nativeUtils,
                webCapture = LinkoraSDK.getInstance().webCapture,
                localizationRepo = DependencyContainer.localizationRepo,
                syncSessionService = DependencyContainer.syncSessionService,
            )
        }
    }
}
