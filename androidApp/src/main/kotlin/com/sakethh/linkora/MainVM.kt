package com.sakethh.linkora

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.repository.LocalizationRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.AndroidUIEvent
import com.sakethh.linkora.utils.ifNot
import com.sakethh.linkora.utils.ifTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainVM(
    preferencesRepository: PreferencesRepository = DependencyContainer.preferencesRepo,
    localizationRepo: LocalizationRepo.Local = DependencyContainer.localizationRepo,
    launchAction: (Action) -> Unit,
) : ViewModel() {
    val preferencesAsFlow = preferencesRepository.currentPreferencesStateAsFlow
    val localizedStrings = localizationRepo.localizedStrings

    /** it's this or blocking the main thread for a couple of milliseconds in [LinkoraApp] */
    val shouldKeepSplashScreen = flow {
        DependencyContainer.preferencesRepo.readAllPreferencesFromDisk()
        val preferences = DependencyContainer.preferencesRepo.getCurrentPreferencesState()
        DependencyContainer.localizationRepo.loadLanguage(preferences.preferredAppLanguageCode)

        if (
            DependencyContainer.preferencesRepo.readPreferenceFromDisk(
                AppPreferences.DID_AUTO_FORCE_STRICT_FOLDER_IDS_ON_DEFAULT_FOLDERS,
            ) == null
        ) {
            DependencyContainer.localLinksRepo
                .forceSetDefaultFolderToInternalIds(viaSocket = false)
                .collect()
            DependencyContainer.preferencesRepo.writePreferenceToDisk(
                preference = AppPreferences.DID_AUTO_FORCE_STRICT_FOLDER_IDS_ON_DEFAULT_FOLDERS,
                newValue = true,
            )
            linkoraLog("applied DID_AUTO_FORCE_STRICT_FOLDER_IDS_ON_DEFAULT_FOLDERS")
        }

        emit(false)
    }
        .catch { emit(false) }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = true,
        )

    init {
        viewModelScope.launch {
            launch {
                UIEvent.uiEvents.collect {
                    if (it is UIEvent.Type.MinimizeTheApp) {
                        launchAction(Action.Minimize)
                    }
                }
            }

            launch {
                AndroidUIEvent.androidUIEventChannel.collect {
                    when (it) {
                        is AndroidUIEvent.Type.ShowRuntimePermissionForStorage -> {
                            pushUIEvent(
                                UIEvent.Type.ShowSnackbar(
                                    localizedStrings.value.StoragePermissionIsRequired,
                                ),
                            )
                            launchAction(Action.LaunchWriteExternalStoragePermission)
                        }

                        is AndroidUIEvent.Type.StoragePermissionGrantedForAndBelowQ -> {
                            it.isGranted
                                .ifNot {
                                    pushUIEvent(
                                        UIEvent.Type.ShowSnackbar(
                                            message =
                                                localizedStrings.value.StoragePermissionIsRequired,
                                        ),
                                    )
                                }
                                .ifTrue {
                                    pushUIEvent(
                                        UIEvent.Type.ShowSnackbar(
                                            message = localizedStrings.value.PermissionGranted,
                                        ),
                                    )
                                }
                        }

                        is AndroidUIEvent.Type.ImportAFile -> {
                            launchAction(Action.LaunchFileImport(it.fileType))
                        }

                        is AndroidUIEvent.Type.ShowRuntimePermissionForNotifications -> {
                            if (Build.VERSION.SDK_INT > 32) {
                                launchAction(Action.ShowNotificationPermissionDialog)
                            }
                        }

                        is AndroidUIEvent.Type.NotificationPermissionState -> {
                            it.isGranted.ifNot {
                                pushUIEvent(
                                    UIEvent.Type.ShowSnackbar(
                                        message =
                                            localizedStrings.value.NotificationPermissionIsRequired,
                                    ),
                                )
                            }
                        }

                        is AndroidUIEvent.Type.PickADirectory -> {
                            launchAction(Action.LaunchDirectoryPicker)
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
