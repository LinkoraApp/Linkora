package com.sakethh.linkora

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.AndroidUIEvent
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.ifNot
import com.sakethh.linkora.utils.ifTrue
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainVM(
    preferencesRepository: PreferencesRepository = DependencyContainer.preferencesRepo,
    launchAction: (Action) -> Unit,
) : ViewModel() {
    val preferencesAsFlow = preferencesRepository.preferencesAsFlow
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
                            pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.StoragePermissionIsRequired.getLocalizedString()))
                            launchAction(Action.LaunchWriteExternalStoragePermission)
                        }

                        is AndroidUIEvent.Type.StoragePermissionGrantedForAndBelowQ -> {
                            it.isGranted.ifNot {
                                pushUIEvent(UIEvent.Type.ShowSnackbar(message = Localization.Key.StoragePermissionIsRequired.getLocalizedString()))
                            }.ifTrue {
                                pushUIEvent(UIEvent.Type.ShowSnackbar(message = Localization.Key.PermissionGranted.getLocalizedString()))
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
                                pushUIEvent(UIEvent.Type.ShowSnackbar(message = Localization.Key.NotificationPermissionIsRequired.getLocalizedString()))
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