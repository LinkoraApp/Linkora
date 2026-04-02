package com.sakethh.linkora.ui.screens.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.domain.FolderType
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.FlatSearchResult
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalDatabaseUtilsRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.ui.Paginator
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.Sorting
import com.sakethh.linkora.utils.asStateInWhileSubscribed
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.hexadCombine
import com.sakethh.linkora.utils.ifNot
import com.sakethh.linkora.utils.onError
import com.sakethh.linkora.utils.onPagesFinished
import com.sakethh.linkora.utils.onRetrieved
import com.sakethh.linkora.utils.onRetrieving
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchScreenVM(
    private val localLinksRepo: LocalLinksRepo,
    private val localDatabaseUtilsRepo: LocalDatabaseUtilsRepo,
    private val localTagsRepo: LocalTagsRepo,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    val preferencesAsFlow = preferencesRepository.preferencesAsFlow
    private val _searchQuery = mutableStateOf("")
    val searchQuery = _searchQuery

    private val _isSearchActive = mutableStateOf(false)
    val isSearchActive = _isSearchActive

    private var searchQueryResultsJob: Job? = null

    fun updateSearchActiveState(isActive: Boolean) {
        _isSearchActive.value = isActive
        isActive.ifNot {
            searchQueryResultsJob?.cancel()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _searchResultsState = MutableStateFlow(
        value = PaginationState.retrieving<List<FlatSearchResult>>()
    )

    val searchResultsState =
        _searchResultsState.asStateInWhileSubscribed(initialValue = PaginationState.retrieving())


    private val _appliedLinkFilters = mutableStateListOf<LinkType>()
    val appliedLinkFilters = _appliedLinkFilters

    private val _appliedFolderFilters = mutableStateListOf<FolderType>()
    val appliedFolderFilters = _appliedFolderFilters

    private val _appliedTagFiltering = mutableStateOf(false)
    val appliedTagFiltering by _appliedTagFiltering

    private val searchResultsPaginator = Paginator<FlatSearchResult>(
        coroutineScope = viewModelScope,
        onRetrieve = { lastSeenId, lastSeenString ->
            val currentQuery = _searchQuery.value
            if (currentQuery.isBlank()) {
                return@Paginator flowOf(Result.Success(emptyList()))
            }

            // Protocol: "typeOrder|sortStr|sortNum"
            // TODO: refactor this into a data class
            val parts = lastSeenString?.split("|")
            val lastTypeOrder = parts?.getOrNull(0)?.toIntOrNull() ?: -1
            val lastSortStr = parts?.getOrNull(1) ?: ""
            val lastSortNum = parts?.getOrNull(2)?.toLongOrNull() ?: 0L
            val lastId = lastSeenId ?: Constants.EMPTY_LAST_SEEN_ID

            val currentSort = preferencesRepository.getPreferences().selectedSortingType
            val appliedFolderFilters = _appliedFolderFilters.toList()
            val appliedLinkFilters = _appliedLinkFilters.toList()
            val isTagFilteringApplied = _appliedTagFiltering.value
            val shouldShowTags =
                isTagFilteringApplied || (appliedFolderFilters.isEmpty() && appliedLinkFilters.isEmpty())
            val shouldShowFolders =
                appliedFolderFilters.isNotEmpty() || (!isTagFilteringApplied && appliedLinkFilters.isEmpty())
            val includeArchivedFolders =
                appliedFolderFilters.contains(FolderType.ARCHIVE_FOLDER) || appliedFolderFilters.isEmpty()
            val includeRegularFolders =
                appliedFolderFilters.contains(FolderType.REGULAR_FOLDER) || appliedFolderFilters.isEmpty()
            val shouldShowLinks =
                appliedLinkFilters.isNotEmpty() || (!isTagFilteringApplied && appliedFolderFilters.isEmpty())
            val isLinkTypeFilterActive = appliedLinkFilters.isNotEmpty()
            val activeLinkTypeFilters =
                if (appliedLinkFilters.isNotEmpty()) appliedLinkFilters.map { it.name } else listOf(
                    ""
                )

            localDatabaseUtilsRepo.search(
                query = currentQuery.trim(),
                sortOption = currentSort,
                pageSize = Constants.PAGE_SIZE,
                shouldShowTags = shouldShowTags,
                shouldShowFolders = shouldShowFolders,
                includeArchivedFolders = includeArchivedFolders,
                includeRegularFolders = includeRegularFolders,
                shouldShowLinks = shouldShowLinks,
                isLinkTypeFilterActive = isLinkTypeFilterActive,
                activeLinkTypeFilters = activeLinkTypeFilters,
                lastTypeOrder = lastTypeOrder,
                lastSortStr = lastSortStr,
                lastSortNum = lastSortNum,
                lastId = lastId,
                assignPath = true
            )
        },
        onRetrieved = { currentKey, orderedData ->
            val sortOption = preferencesRepository.getPreferences().selectedSortingType
            _searchResultsState.onRetrieved(
                currentKey = currentKey,
                data = orderedData,
                shouldShuffle = preferencesRepository.getPreferences().forceShuffleLinks,

                idSelector = { item ->
                    when (item.itemType) {
                        Constants.TAG -> item.tagLocalId
                        Constants.FOLDER -> item.folderLocalId
                        else -> item.linkLocalId
                    } ?: 0L
                },
                stringSelector = { item ->

                    val typeOrder = when (item.itemType) {
                        Constants.TAG -> 0
                        Constants.FOLDER -> 1
                        else -> 2
                    }

                    val sortStr =
                        if (sortOption == Sorting.A_TO_Z || sortOption == Sorting.Z_TO_A) {
                            item.tagName ?: item.folderName ?: item.linkTitle ?: ""
                        } else ""

                    val sortNum =
                        if (sortOption == Sorting.NEW_TO_OLD || sortOption == Sorting.OLD_TO_NEW) {
                            item.tagLocalId ?: item.folderLocalId ?: item.linkLocalId ?: 0L
                        } else 0L

                    "$typeOrder|$sortStr|$sortNum"
                }
            )
        },
        onError = _searchResultsState::onError,
        onRetrieving = _searchResultsState::onRetrieving,
        onPagesFinished = _searchResultsState::onPagesFinished
    )

    init {
        viewModelScope.launch {
            hexadCombine(
                snapshotFlow { _searchQuery.value },
                preferencesAsFlow.map {
                    it.selectedSortingType
                },
                preferencesAsFlow.map {
                    it.forceShuffleLinks
                },
                snapshotFlow { _appliedFolderFilters.toList() },
                snapshotFlow { _appliedLinkFilters.toList() },
                snapshotFlow { _appliedTagFiltering.value }) { query, _, _, _, _, _ ->
                query
            }.collectLatest { query ->
                searchResultsPaginator.cancelAndReset()

                _searchResultsState.update {
                    it.copy(isRetrieving = true, data = emptyMap(), pagesCompleted = false)
                }

                if (query.isNotBlank()) {
                    retrieveNextSearchPage() // will always start as a "new instance", since we canceled and reset the state above
                } else {
                    _searchResultsState.update { it.copy(isRetrieving = false) }
                }
            }
        }
    }

    fun retrieveNextSearchPage() {
        viewModelScope.launch {
            searchResultsPaginator.retrieveNextBatch()
        }
    }

    fun updateFirstVisibleIndexOfSearchPaginator(newIndex: Long) {
        viewModelScope.launch {
            searchResultsPaginator.updateFirstVisibleItemIndex(newIndex)
        }
    }

    fun toggleLinkFilter(filter: LinkType) {
        if (_appliedLinkFilters.contains(filter).not()) {
            _appliedLinkFilters.add(filter)
        } else {
            _appliedLinkFilters.remove(filter)
        }
    }

    fun toggleTagFilter() {
        _appliedTagFiltering.value = !_appliedTagFiltering.value
    }

    fun toggleFolderFilter(filter: FolderType) {
        if (!_appliedFolderFilters.contains(filter)) {
            _appliedFolderFilters.add(filter)
        } else {
            _appliedFolderFilters.remove(filter)
        }
    }

    fun addANewLinkToHistory(link: Link, tagIds: List<Long>?) {
        viewModelScope.launch {
            val preferences = preferencesRepository.getPreferences()
            localLinksRepo.addANewLink(
                link = link.copy(
                    linkType = LinkType.HISTORY_LINK,
                    idOfLinkedFolder = null,
                ), linkSaveConfig = LinkSaveConfig(
                    forceAutoDetectTitle = false, forceSaveWithoutRetrievingData = true,
                    useProxy = preferences.useProxy,
                    skipSavingIfExists = preferences.skipSavingExistingLink,
                    forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails,
                ), selectedTagIds = tagIds
            ).collectLatest {
                it.onSuccess {
                    if (it.isRemoteExecutionSuccessful.not()) {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(it.getRemoteOnlyFailureMsg()))
                    }
                }
                it.pushSnackbarOnFailure()
            }
        }
    }

    private val _historyLinkTagsPairsState = MutableStateFlow(
        PaginationState.retrieving<List<LinkTagsPair>>()
    )

    val historyLinkTagsPairsState = _historyLinkTagsPairsState.asStateInWhileSubscribed(
        initialValue = PaginationState.retrieving()
    )

    private val historyLinksPaginator = Paginator<LinkTagsPair>(
        coroutineScope = viewModelScope,
        onRetrieve = { lastSeenId, lastSeenString ->
            combine(
                preferencesAsFlow.map {
                    it.selectedSortingType
                },
                preferencesAsFlow.map {
                    it.forceShuffleLinks
                },
            ) { selectedSortingType, forceShuffleLinks ->
                forceShuffleLinks to selectedSortingType
            }.flatMapLatest { (forceShuffleLinks, selectedSortingType) ->
                localLinksRepo.getLinks(
                    linkType = LinkType.HISTORY_LINK,
                    sortOption = selectedSortingType,
                    pageSize = Constants.PAGE_SIZE,
                    lastSeenId = lastSeenId,
                    lastSeenTitle = lastSeenString
                ).flatMapLatest { result ->
                    when (result) {
                        is Result.Failure -> flowOf(Result.Failure(result.message))
                        is Result.Loading -> flowOf(Result.Loading())
                        is Result.Success -> {
                            // we need the original order to calculate the next cursor
                            val linkIds = result.data.map { it.localId }

                            localTagsRepo.getTagsForLinks(linkIds).map { tagsForLinks ->
                                val pairs = result.data.map { link ->
                                    LinkTagsPair(
                                        link = link,
                                        tags = tagsForLinks[link.localId] ?: emptyList()
                                    )
                                }
                                Result.Success(pairs)
                            }
                        }
                    }
                }
            }
        },
        onRetrieved = { currentKey, retrievedData ->
            _historyLinkTagsPairsState.onRetrieved(
                currentKey = currentKey,
                data = retrievedData,
                shouldShuffle = preferencesRepository.getPreferences().forceShuffleLinks,
                idSelector = { it.link.localId },
                stringSelector = { it.link.title }
            )
        },
        onError = _historyLinkTagsPairsState::onError,
        onRetrieving = _historyLinkTagsPairsState::onRetrieving,
        onPagesFinished = _historyLinkTagsPairsState::onPagesFinished
    )

    fun retrieveNextBatchOfHistoryLinks() {
        viewModelScope.launch {
            historyLinksPaginator.retrieveNextBatch()
        }
    }

    fun updateStartingIndexForHistoryPaginator(newIndex: Long) {
        viewModelScope.launch {
            historyLinksPaginator.updateFirstVisibleItemIndex(newIndex)
        }
    }

    init {
        viewModelScope.launch {
            historyLinksPaginator.retrieveNextBatch()
        }
    }
}