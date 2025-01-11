package com.sakethh.linkora.ui.screens.home

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.utils.Constants
import com.sakethh.linkora.common.utils.getLocalizedString
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.repository.local.PanelsRepo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeScreenVM(
    private val panelsRepo: PanelsRepo,
    triggerCollectionOfPanels: Boolean = true,
    triggerCollectionOfPanelFolders: Boolean = true
) : ViewModel() {
    val currentPhaseOfTheDay = mutableStateOf("")

    private val _panels = MutableStateFlow(emptyList<Panel>())
    val panels = _panels.asStateFlow()

    private val _panelFolders = MutableStateFlow(emptyList<PanelFolder>())
    val panelFolders = _panelFolders.asStateFlow()

    private val defaultPanelFolders = listOf(
        PanelFolder(
            folderId = Constants.SAVED_LINKS_ID,
            folderName = Localization.Key.SavedLinks.getLocalizedString(),
            connectedPanelId = Constants.DEFAULT_PANELS_ID,
            panelPosition = 0
        ),
        PanelFolder(
            folderId = Constants.IMPORTANT_LINKS_ID,
            folderName = Localization.Key.ImportantLinks.getLocalizedString(),
            connectedPanelId = Constants.DEFAULT_PANELS_ID,
            panelPosition = 0
        ),
    )

    private val defaultPanel = Panel(
        panelName = Localization.Key.Default.getLocalizedString(),
        panelId = Constants.DEFAULT_PANELS_ID
    )

    fun defaultPanel(): Panel {
        return defaultPanel
    }

    private var panelFoldersJob: Job? = null

    fun updatePanelFolders(panelId: Long) {
        panelFoldersJob?.cancel()

        panelFoldersJob = viewModelScope.launch {

            if (panelId == Constants.DEFAULT_PANELS_ID) {
                _panelFolders.emit(defaultPanelFolders)
                return@launch
            }

            panelsRepo.getAllTheFoldersFromAPanel(panelId).collectLatest {
                _panelFolders.emit(it)
            }

        }
    }

    init {
        if (triggerCollectionOfPanels) {
            viewModelScope.launch {
                panelsRepo.getAllThePanels().collectLatest {
                    _panels.emit(listOf(defaultPanel) + it)
                }
            }
        }
        if (triggerCollectionOfPanelFolders) {
            viewModelScope.launch {
                updatePanelFolders(Constants.DEFAULT_PANELS_ID)
            }
        }
    }

    init {
        currentPhaseOfTheDay.value = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> {
                Localization.Key.GoodMorning.getLocalizedString()
            }

            in 12..15 -> {
                Localization.Key.GoodAfternoon.getLocalizedString()
            }

            in 16..23 -> {
                Localization.Key.GoodEvening.getLocalizedString()
            }

            else -> {
                Localization.Key.HeyHi.getLocalizedString()
            }
        }
    }
}