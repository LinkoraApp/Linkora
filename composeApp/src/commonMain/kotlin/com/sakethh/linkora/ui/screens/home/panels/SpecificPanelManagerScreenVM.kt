package com.sakethh.linkora.ui.screens.home.panels

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.longPreferencesKey
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import com.sakethh.linkora.utils.replaceFirstPlaceHolderWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

class SpecificPanelManagerScreenVM(
    private val localFoldersRepo: LocalFoldersRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val preferencesRepository: PreferencesRepository,
    currentBackStackEntryFlow: Flow<NavBackStackEntry>,
    onAndroidMobile: Boolean
) : ViewModel() {
    private val _foldersToIncludeInPanel = MutableStateFlow(emptyList<Folder>())
    val foldersToIncludeInPanel = _foldersToIncludeInPanel.asStateFlow()

    private val _foldersOfTheSelectedPanel = MutableStateFlow(emptyList<PanelFolder>())
    val foldersOfTheSelectedPanel = _foldersOfTheSelectedPanel.asStateFlow()

    var foldersSearchQuery = mutableStateOf("")
        private set

    fun updateFoldersSearchQuery(query: String) {
        foldersSearchQuery.value = query
    }

    companion object {
        private val _selectedPanel = mutableStateOf(Panel(localId = 0L, panelName = ""))
        val selectedPanel = _selectedPanel

        fun updateSelectedPanel(panel: Panel) {
            _selectedPanel.value = panel
        }
    }

    init {
        val panelRoutes =
            listOf(Navigation.Home.PanelsManagerScreen, Navigation.Home.SpecificPanelManagerScreen)

        viewModelScope.launch {
            currentBackStackEntryFlow.transform { navBackStackEntry ->
                if ((onAndroidMobile && navBackStackEntry.destination.hasRoute(
                        panelRoutes[1]::class
                    )) || (!onAndroidMobile && panelRoutes.any {
                        navBackStackEntry.destination.hasRoute(it::class)
                    })
                ) {
                    emit(Unit)
                } else {
                    linkoraLog("specificPanelManagerScreenDataJob?.cancel()")
                    specificPanelManagerScreenDataJob?.cancel()
                    // We don't emit anything here because we don't want the emission
                    // or the other side effects that `updateSpecificPanelManagerScreenData` triggers.
                    // This logic exists purely by choice. just because I can do it
                    // doesn't mean i should... but i'm doing it anyway.
                    // but anyway, you get the idea.
                    // A clearer approach would be to apply conditions directly in the collection
                }
            }.collectLatest {
                linkoraLog("updateSpecificPanelManagerScreenData()")
                updateSpecificPanelManagerScreenData()
            }
        }
    }

    fun performAction(panelAction: PanelsAction) {
        when (panelAction) {
            is PanelsAction.AddANewAPanel -> addANewAPanel(
                panel = panelAction.panel, onCompletion = panelAction.onCompletion
            )

            is PanelsAction.AddANewFolderInAPanel -> addANewFolderInAPanel(
                panelFolder = panelAction.panelFolder
            )

            is PanelsAction.DeleteAPanel -> deleteAPanel(
                panelId = panelAction.panelId, onCompletion = panelAction.onCompletion
            )

            is PanelsAction.RemoveAFolderFromPanel -> removeAFolderFromAPanel(
                panelId = panelAction.panelId, folderId = panelAction.folderId
            )

            is PanelsAction.RenameAPanel -> renameAPanel(
                panelId = panelAction.panelId,
                newName = panelAction.newName,
                onCompletion = panelAction.onCompletion
            )

            is PanelsAction.UpdateFoldersSearchQuery -> updateFoldersSearchQuery(
                panelAction.query
            )
        }
    }

    private var specificPanelManagerScreenDataJob: Job? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun updateSpecificPanelManagerScreenData() {

        specificPanelManagerScreenDataJob?.cancel()

        specificPanelManagerScreenDataJob = viewModelScope.launch {
            combine(localFoldersRepo.getAllFoldersAsFlow(), snapshotFlow {
                _selectedPanel.value
            }.flatMapLatest { selectedPanel ->
                localPanelsRepo.getAllTheFoldersFromAPanel(selectedPanel.localId)
            }, snapshotFlow {
                foldersSearchQuery.value
            }) { allFolders, foldersOfTheSelectedPanel, foldersSearchQuery ->
                PanelFolderMetaInfo(foldersToIncludeInPanel = allFolders.filter {
                    it.localId !in foldersOfTheSelectedPanel.map { it.folderId } && !it.isArchived
                }.run {
                    if (foldersSearchQuery.isEmpty()) {
                        this
                    } else {
                        filter {
                            it.name.lowercase().contains(foldersSearchQuery.lowercase())
                        }
                    }
                }, foldersOfTheSelectedPanel = foldersOfTheSelectedPanel)
            }.collectLatest { (foldersToIncludeInPanel, foldersOfTheSelectedPanel) ->
                linkoraLog(
                    """
                    foldersToIncludeInPanel: ${foldersToIncludeInPanel.map { it.name }}
                    foldersOfTheSelectedPanel: ${foldersOfTheSelectedPanel.map { it.folderName }}
                """.trimIndent()
                )
                _foldersToIncludeInPanel.emit(foldersToIncludeInPanel)
                _foldersOfTheSelectedPanel.emit(foldersOfTheSelectedPanel)
            }
        }
    }

    fun addANewFolderInAPanel(panelFolder: PanelFolder) {
        viewModelScope.launch {
            localPanelsRepo.addANewFolderInAPanel(panelFolder).collectLatest {
                it.onSuccess {
                    if (it.isRemoteExecutionSuccessful.not()) {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(message = it.getRemoteOnlyFailureMsg()))
                    }
                }
                it.pushSnackbarOnFailure()
            }
        }
    }

    fun addANewAPanel(panel: Panel, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localPanelsRepo.addANewPanel(panel).collectLatest {
                it.onSuccess {
                    pushUIEvent(
                        UIEvent.Type.ShowSnackbar(
                            message = Localization.Key.PanelCreatedSuccessfully.getLocalizedString()
                                .replaceFirstPlaceHolderWith(panel.panelName) + it.getRemoteOnlyFailureMsg()
                        )
                    )
                }.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun deleteAPanel(panelId: Long, onCompletion: () -> Unit) {
        viewModelScope.launch {
            if (preferencesRepository.readPreferenceValue(longPreferencesKey(AppPreferenceType.LAST_SELECTED_PANEL_ID.name)) == panelId) {
                preferencesRepository.changePreferenceValue(
                    preferenceKey = longPreferencesKey(
                        AppPreferenceType.LAST_SELECTED_PANEL_ID.name
                    ), newValue = Constants.DEFAULT_PANELS_ID
                )
            }
            localPanelsRepo.deleteAPanel(panelId).collectLatest {
                it.onSuccess {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(message = Localization.Key.DeletedPanelSuccessfully.getLocalizedString() + it.getRemoteOnlyFailureMsg()))
                }.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun renameAPanel(panelId: Long, newName: String, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localPanelsRepo.updateAPanelName(newName, panelId).collectLatest {
                it.onSuccess {
                    pushUIEvent(
                        UIEvent.Type.ShowSnackbar(
                            message = Localization.Key.UpdatedThePanelNameSuccessfully.getLocalizedString()
                                .replaceFirstPlaceHolderWith(newName) + it.getRemoteOnlyFailureMsg()
                        )
                    )
                }.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun removeAFolderFromAPanel(
        panelId: Long, folderId: Long
    ) {
        viewModelScope.launch {
            localPanelsRepo.deleteAFolderFromAPanel(panelId, folderId).collectLatest {
                it.onSuccess {
                    if (it.isRemoteExecutionSuccessful.not()) {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(it.getRemoteOnlyFailureMsg()))
                    }
                }
                it.pushSnackbarOnFailure()
            }
        }
    }
}

private data class PanelFolderMetaInfo(
    val foldersToIncludeInPanel: List<Folder>, val foldersOfTheSelectedPanel: List<PanelFolder>
)