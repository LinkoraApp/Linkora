package com.sakethh.linkora.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.model.FlatChildFolderData
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.repository.local.LocalDatabaseUtilsRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.PageKey
import com.sakethh.linkora.ui.Paginator
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.asStateInWhileSubscribed
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.shuffleLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableMap
import java.util.Calendar
import java.util.TreeMap

class HomeScreenVM(
    private val localLinksRepo: LocalLinksRepo,
    private val localDatabaseUtilsRepo: LocalDatabaseUtilsRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val preferencesRepository: PreferencesRepository,
    triggerCollectionOfPanels: Boolean = true,
    private val triggerCollectionOfPanelFolders: Boolean = true,
) : ViewModel() {
    val currentPhaseOfTheDay = mutableStateOf("")


    // TODO: migrate to the pagination
    private val _existingPanels = MutableStateFlow(emptyList<Panel>())
    val existingPanels = _existingPanels.asStateFlow()

    // TODO: migrate to the pagination
    private val _activePanelAssociatedPanelFolders = MutableStateFlow(emptyList<PanelFolder>())
    val activePanelAssociatedPanelFolders = _activePanelAssociatedPanelFolders.asStateFlow()

    private val _panelFoldersDataFlat =
        MutableStateFlow<TreeMap<Long, PaginationState<Map<PageKey, List<FlatChildFolderData>>>>>(
            value = TreeMap()
        )

    val panelFoldersDataFlat = _panelFoldersDataFlat.asStateInWhileSubscribed(
        initialValue = TreeMap<Long, PaginationState<Map<PageKey, List<FlatChildFolderData>>>>().toImmutableMap()
    )

    private val panelFolderPaginators = mutableMapOf<Long, Paginator<FlatChildFolderData>>()

    fun retrieveNextPage(folderId: Long) {
        viewModelScope.launch {
            panelFolderPaginators[folderId]?.retrieveNextBatch()
        }
    }

    fun onFirstVisibleItemIndexChange(folderId: Long, itemIndex: Long) {
        viewModelScope.launch {
            panelFolderPaginators[folderId]?.updateFirstVisibleItemIndex(itemIndex)
        }
    }

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

    private fun defaultPanel(): Panel {
        return Panel(
            panelName = Localization.Key.Default.getLocalizedString(),
            localId = Constants.DEFAULT_PANELS_ID
        )
    }


    private val appPreferencesCombined = combine(snapshotFlow {
        AppPreferences.forceShuffleLinks.value
    }, snapshotFlow {
        AppPreferences.selectedSortingType.value
    }) { shuffleLinks, sortingType ->
        Pair(shuffleLinks, sortingType)
    }

    private var _activePanelAssociatedFoldersJob: Job? = null

    fun updatePanelFolders(panel: Panel) {
        _activePanelAssociatedFoldersJob?.cancel()

        selectedPanelData = panel

        _activePanelAssociatedFoldersJob = viewModelScope.launch(Dispatchers.Default) {
            preferencesRepository.changePreferenceValue(
                preferenceKey = longPreferencesKey(
                    AppPreferenceType.LAST_SELECTED_PANEL_ID.name
                ), newValue = panel.localId
            )
        }
    }

    var selectedPanelData by mutableStateOf<Panel?>(null)

    private suspend fun freeUpPanelFolderPaginators() {
        _panelFoldersDataFlat.emit(TreeMap())
        for ((_, paginator) in panelFolderPaginators) {
            paginator.cancelAndReset()
        }
        panelFolderPaginators.clear()
    }

    init {

        viewModelScope.launch {
            selectedPanelData = preferencesRepository.readPreferenceValue(
                longPreferencesKey(
                    AppPreferenceType.LAST_SELECTED_PANEL_ID.name
                )
            ).let {
                try {
                    if (it === null || it == Constants.DEFAULT_PANELS_ID) throw Exception()
                    localPanelsRepo.getPanel(it)
                } catch (_: Exception) {
                    defaultPanel()
                }
            }
        }

        if (triggerCollectionOfPanels) {
            viewModelScope.launch {
                localPanelsRepo.getAllThePanels()
                    .collectLatest {
                        _existingPanels.emit(listOf(defaultPanel()) + it)
                    }
            }

            viewModelScope.launch {
                snapshotFlow {
                    selectedPanelData
                }.transform { if (it?.localId != null) emit(it) }.flatMapLatest {
                    if (triggerCollectionOfPanelFolders) {
                        if (Constants.DEFAULT_PANELS_ID == it.localId) {
                            flowOf(defaultPanelFolders)
                        } else {
                            localPanelsRepo.getAllTheFoldersFromAPanel(it.localId)
                        }
                    } else {
                        emptyFlow()
                    }
                }.distinctUntilChanged().collectLatest { panelFolders ->
                    freeUpPanelFolderPaginators()
                    _activePanelAssociatedPanelFolders.emit(panelFolders)
                }
            }

            viewModelScope.launch {
                var lastSortingType: String? = null
                var lastShuffleLinks: Boolean? = null

                combine(
                    appPreferencesCombined,
                    _activePanelAssociatedPanelFolders
                ) { (shuffleLinks, sortingType), activePanelFolders ->
                    Triple(shuffleLinks, sortingType, activePanelFolders)
                }
                    .distinctUntilChanged()
                    .collectLatest { (shuffleLinks, sortingType, activePanelFolders) ->
                        val isSortChanged = lastSortingType != sortingType
                        val isShuffleLinksChanged = lastShuffleLinks != shuffleLinks
                        lastSortingType = sortingType
                        lastShuffleLinks = shuffleLinks

                        if (isSortChanged || isShuffleLinksChanged) {
                            freeUpPanelFolderPaginators()
                        }
                        val activeFolderIds = activePanelFolders.map { it.folderId }.toSet()

                        val panelFoldersDataIterator = panelFolderPaginators.iterator()
                        while (panelFoldersDataIterator.hasNext()) {
                            val (id, paginator) = panelFoldersDataIterator.next()
                            if (id !in activeFolderIds) {
                                paginator.cancelAndReset()
                                panelFoldersDataIterator.remove()

                                _panelFoldersDataFlat.update {
                                    val updated = TreeMap(it)
                                    updated.remove(id)
                                    updated
                                }
                            }
                        }

                        activePanelFolders.forEach { panelFolder ->
                            val folderKey = panelFolder.folderId

                            if (panelFolderPaginators.containsKey(folderKey)) {
                                return@forEach
                            }

                            panelFolderPaginators[folderKey] = Paginator(
                                coroutineScope = viewModelScope,
                                onRetrieve = { pageStartIndex ->
                                    pageStartIndex to when (panelFolder.folderId) {
                                        Constants.SAVED_LINKS_ID -> localDatabaseUtilsRepo.getChildFolderData(
                                            linkType = LinkType.SAVED_LINK,
                                            parentFolderId = Constants.SAVED_LINKS_ID,
                                            sortOption = sortingType,
                                            pageSize = Constants.PAGE_SIZE,
                                            startIndex = pageStartIndex
                                        ).run {
                                            if (shuffleLinks) this.shuffleLinks() else this
                                        }

                                        Constants.IMPORTANT_LINKS_ID -> localDatabaseUtilsRepo.getChildFolderData(
                                            linkType = LinkType.IMPORTANT_LINK,
                                            parentFolderId = Constants.IMPORTANT_LINKS_ID,
                                            sortOption = sortingType,
                                            pageSize = Constants.PAGE_SIZE,
                                            startIndex = pageStartIndex
                                        ).run {
                                            if (shuffleLinks) this.shuffleLinks() else this
                                        }

                                        else -> localDatabaseUtilsRepo.getChildFolderData(
                                            parentFolderId = panelFolder.folderId,
                                            linkType = LinkType.FOLDER_LINK,
                                            sortOption = sortingType,
                                            pageSize = Constants.PAGE_SIZE,
                                            startIndex = pageStartIndex
                                        ).run {
                                            if (shuffleLinks) this.shuffleLinks() else this
                                        }
                                    }
                                },
                                onRetrieved = { currentPageKey, data ->
                                    _panelFoldersDataFlat.update { currentState ->
                                        val updatedFoldersData = TreeMap(currentState)

                                        val updatedPaginationData =
                                            TreeMap(
                                                updatedFoldersData[folderKey]?.data ?: emptyMap()
                                            )
                                        updatedPaginationData[currentPageKey] = data.map {
                                            it.second
                                        }

                                        updatedFoldersData[folderKey] = PaginationState(
                                            isRetrieving = true,
                                            errorOccurred = false,
                                            errorMessage = null,
                                            pagesCompleted = false,
                                            data = updatedPaginationData
                                        )
                                        updatedFoldersData
                                    }
                                },
                                onError = { errorMsg ->
                                    _panelFoldersDataFlat.update {
                                        val updatedData = TreeMap(it)
                                        updatedData[folderKey] = updatedData[folderKey]?.copy(
                                            isRetrieving = false,
                                            errorOccurred = true,
                                            errorMessage = errorMsg,
                                            pagesCompleted = false,
                                        ) ?: PaginationState.retrieving()
                                        updatedData
                                    }
                                },
                                onRetrieving = {
                                    _panelFoldersDataFlat.update {
                                        val updatedData = TreeMap(it)
                                        updatedData[folderKey] = updatedData[folderKey]?.copy(
                                            isRetrieving = true,
                                            errorOccurred = false,
                                            errorMessage = null,
                                            pagesCompleted = false,
                                        ) ?: PaginationState.retrieving()
                                        updatedData
                                    }
                                },
                                onPagesFinished = {
                                    _panelFoldersDataFlat.update {
                                        val updatedData = TreeMap(it)
                                        updatedData[folderKey] = updatedData[folderKey]?.copy(
                                            isRetrieving = false,
                                            errorOccurred = false,
                                            errorMessage = null,
                                            pagesCompleted = true,
                                        ) ?: PaginationState.retrieving()
                                        updatedData
                                    }
                                }
                            ).also {
                                it.retrieveNextBatch()
                            }
                        }
                    }
            }
        }
    }

    fun addLinkToHistory(
        link: Link,
        selectedTags: List<Tag>?,
    ) {
        viewModelScope.launch {
            localLinksRepo.addANewLink(
                link = link.copy(
                    linkType = LinkType.HISTORY_LINK, localId = 0
                ), selectedTagIds = selectedTags?.map {
                    it.localId
                }, linkSaveConfig = LinkSaveConfig(
                    forceAutoDetectTitle = false, forceSaveWithoutRetrievingData = true
                )
            ).collect()
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