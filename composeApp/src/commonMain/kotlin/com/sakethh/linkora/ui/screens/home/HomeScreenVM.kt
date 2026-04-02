package com.sakethh.linkora.ui.screens.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.AppPreferences
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
import com.sakethh.linkora.ui.LastSeenId
import com.sakethh.linkora.ui.LastSeenString
import com.sakethh.linkora.ui.Paginator
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.asStateInWhileSubscribed
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.longPreferencesKey
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HomeScreenVM(
    private val localLinksRepo: LocalLinksRepo,
    private val localDatabaseUtilsRepo: LocalDatabaseUtilsRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val preferencesRepository: PreferencesRepository,
    triggerCollectionOfPanels: Boolean = true,
    private val triggerCollectionOfPanelFolders: Boolean = true,
) : ViewModel() {
    val currentPhaseOfTheDay = mutableStateOf("")

    val preferencesAsFlow = preferencesRepository.preferencesAsFlow

    // TODO: migrate to the pagination
    private val _existingPanels = MutableStateFlow(emptyList<Panel>())
    val existingPanels = _existingPanels.asStateFlow()

    // TODO: migrate to the pagination
    private val _activePanelAssociatedPanelFolders = MutableStateFlow(emptyList<PanelFolder>())
    val activePanelAssociatedPanelFolders = _activePanelAssociatedPanelFolders.asStateFlow()

    private val _panelFoldersDataFlat =
        MutableStateFlow<Map<Long, PaginationState<Map<Pair<LastSeenId, LastSeenString>, List<FlatChildFolderData>>>>>(
            value = emptyMap()
        )

    val panelFoldersDataFlat = _panelFoldersDataFlat.asStateInWhileSubscribed(
        initialValue = emptyMap()
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


    private val appPreferencesCombined = preferencesAsFlow.map {
        Pair(it.forceShuffleLinks, it.selectedSortingType)
    }

    private var _activePanelAssociatedFoldersJob: Job? = null

    fun updatePanelFolders(panel: Panel) {
        _activePanelAssociatedFoldersJob?.cancel()

        selectedPanelData = panel

        _activePanelAssociatedFoldersJob = viewModelScope.launch(Dispatchers.Default) {
            preferencesRepository.changePreferenceValue(
                preferenceKey = longPreferencesKey(
                    AppPreferences.LAST_SELECTED_PANEL_ID.key
                ), newValue = panel.localId
            )
        }
    }

    var selectedPanelData by mutableStateOf<Panel?>(null)

    private suspend fun freeUpPanelFolderPaginators() {
        _panelFoldersDataFlat.emit(LinkedHashMap())
        for ((_, paginator) in panelFolderPaginators) {
            paginator.cancelAndReset()
        }
        panelFolderPaginators.clear()
    }

    init {

        viewModelScope.launch {
            selectedPanelData = preferencesRepository.readPreferenceValue(
                longPreferencesKey(
                    AppPreferences.LAST_SELECTED_PANEL_ID.key
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
                                    val updated = LinkedHashMap(it)
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
                                onRetrieve = { _, lastSeenString ->
                                    val parts = lastSeenString?.split("|")
                                    val lastTypeOrder = parts?.getOrNull(0)?.toIntOrNull() ?: -1
                                    val lastSortStr = parts?.getOrNull(1) ?: ""
                                    val lastId = parts?.getOrNull(2)?.toLongOrNull()
                                        ?: Constants.EMPTY_LAST_SEEN_ID

                                    val (targetLinkType, targetParentId) = when (panelFolder.folderId) {
                                        Constants.SAVED_LINKS_ID -> LinkType.SAVED_LINK to Constants.SAVED_LINKS_ID
                                        Constants.IMPORTANT_LINKS_ID -> LinkType.IMPORTANT_LINK to Constants.IMPORTANT_LINKS_ID
                                        else -> LinkType.FOLDER_LINK to panelFolder.folderId
                                    }

                                    localDatabaseUtilsRepo.getChildFolderData(
                                        parentFolderId = targetParentId,
                                        linkType = targetLinkType,
                                        sortOption = sortingType,
                                        pageSize = Constants.PAGE_SIZE,
                                        lastTypeOrder = lastTypeOrder,
                                        lastSortStr = lastSortStr,
                                        lastId = lastId
                                    )
                                },
                                onRetrieved = { currentKey, retrievedData ->
                                    val lastItem = retrievedData.lastOrNull()
                                    val nextId =
                                        lastItem?.let { it.folderLocalId ?: it.linkLocalId }
                                            ?: Constants.EMPTY_LAST_SEEN_ID

                                    val nextString = lastItem?.let { item ->
                                        val typeOrder = if (item.itemType == "FOLDER") 0 else 1
                                        val sortStr = item.folderName ?: item.linkTitle ?: ""
                                        val sortId = item.folderLocalId ?: item.linkLocalId ?: 0L
                                        "$typeOrder|$sortStr|$sortId"
                                    } ?: ""

                                    val dataToDisplay =
                                        if (shuffleLinks) retrievedData.shuffled() else retrievedData

                                    _panelFoldersDataFlat.update { currentState ->
                                        val updatedOuterMap = LinkedHashMap(currentState)

                                        val existingState = updatedOuterMap[panelFolder.folderId]
                                            ?: PaginationState(
                                                isRetrieving = false,
                                                errorOccurred = false,
                                                errorMessage = null,
                                                pagesCompleted = false,
                                                data = emptyMap()
                                            )

                                        val updatedInnerData = LinkedHashMap(existingState.data)
                                        updatedInnerData[currentKey] = dataToDisplay

                                        updatedOuterMap[panelFolder.folderId] = existingState.copy(
                                            isRetrieving = false,
                                            pagesCompleted = retrievedData.isEmpty() || retrievedData.size < Constants.PAGE_SIZE,
                                            data = updatedInnerData
                                        )
                                        updatedOuterMap
                                    }

                                    nextId to nextString
                                },
                                onError = { error ->
                                    _panelFoldersDataFlat.update { state ->
                                        val map = LinkedHashMap(state)
                                        val folderState = map[panelFolder.folderId]
                                            ?: PaginationState(
                                                data = emptyMap(),
                                                isRetrieving = false,
                                                errorOccurred = false,
                                                errorMessage = null,
                                                pagesCompleted = false
                                            )
                                        map[panelFolder.folderId] = folderState.copy(
                                            isRetrieving = false,
                                            errorOccurred = true,
                                            errorMessage = error
                                        )
                                        map
                                    }
                                },
                                onRetrieving = {
                                    _panelFoldersDataFlat.update { state ->
                                        val map = LinkedHashMap(state)
                                        val folderState = map[panelFolder.folderId]
                                            ?: PaginationState(
                                                data = emptyMap(),
                                                isRetrieving = false,
                                                errorOccurred = false,
                                                errorMessage = null,
                                                pagesCompleted = false
                                            )
                                        map[panelFolder.folderId] =
                                            folderState.copy(isRetrieving = true)
                                        map
                                    }
                                },
                                onPagesFinished = {
                                    _panelFoldersDataFlat.update { state ->
                                        val map = LinkedHashMap(state)
                                        val folderState = map[panelFolder.folderId]
                                            ?: PaginationState(
                                                data = emptyMap(),
                                                isRetrieving = false,
                                                errorOccurred = false,
                                                errorMessage = null,
                                                pagesCompleted = false
                                            )
                                        map[panelFolder.folderId] =
                                            folderState.copy(pagesCompleted = true)
                                        map
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
        val preferences = preferencesRepository.getPreferences()
        viewModelScope.launch {
            localLinksRepo.addANewLink(
                link = link.copy(
                    linkType = LinkType.HISTORY_LINK, localId = 0
                ), selectedTagIds = selectedTags?.map {
                    it.localId
                }, linkSaveConfig = LinkSaveConfig(
                    forceAutoDetectTitle = false, forceSaveWithoutRetrievingData = true,
                    useProxy = preferences.useProxy,
                    skipSavingIfExists = preferences.skipSavingExistingLink,
                    forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails,
                )
            ).collect()
        }
    }

    init {
        val currentHour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour

        currentPhaseOfTheDay.value = when (currentHour) {
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