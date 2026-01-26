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
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.Paginator
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.asStateInWhileSubscribed
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.hexadCombine
import com.sakethh.linkora.utils.ifNot
import com.sakethh.linkora.utils.onError
import com.sakethh.linkora.utils.onPagesFinished
import com.sakethh.linkora.utils.onRetrieved
import com.sakethh.linkora.utils.onRetrieving
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import com.sakethh.linkora.utils.shuffleLinks
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
    private val localTagsRepo: LocalTagsRepo
) : ViewModel() {

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

    private val searchResultsPaginator = Paginator(
        onRetrieve = { pageStartIndex -> // we trigger this entire block when things change from init, must be explicitly handled
            val currentQuery = _searchQuery.value
            if (currentQuery.isBlank()) {
                return@Paginator pageStartIndex to flowOf(Result.Success(emptyList()))
            }

            val currentSort =
                AppPreferences.selectedSortingType.value

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

            val activeLinkTypeFilters = if (appliedLinkFilters.isNotEmpty()) {
                appliedLinkFilters.map { it.name }
            } else {
                listOf("")
            }

            pageStartIndex to localDatabaseUtilsRepo.search(
                query = currentQuery.trim(),
                sortOption = currentSort,
                pageSize = Constants.PAGE_SIZE,
                startIndex = pageStartIndex,
                shouldShowTags = shouldShowTags,
                shouldShowFolders = shouldShowFolders,
                includeArchivedFolders = includeArchivedFolders,
                includeRegularFolders = includeRegularFolders,
                shouldShowLinks = shouldShowLinks,
                isLinkTypeFilterActive = isLinkTypeFilterActive,
                activeLinkTypeFilters = activeLinkTypeFilters,
                assignPath = true
            ).run {
                if (AppPreferences.forceShuffleLinks.value) shuffleLinks() else this
            }
        },
        onRetrieved = _searchResultsState::onRetrieved,
        onError = _searchResultsState::onError,
        onRetrieving = _searchResultsState::onRetrieving,
        onPagesFinished = _searchResultsState::onPagesFinished,
        coroutineScope = viewModelScope
    )

    init {
        viewModelScope.launch {
            hexadCombine(
                snapshotFlow { _searchQuery.value },
                snapshotFlow { AppPreferences.selectedSortingType.value },
                snapshotFlow { AppPreferences.forceShuffleLinks.value },
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
            localLinksRepo.addANewLink(
                link = link.copy(
                    linkType = LinkType.HISTORY_LINK,
                    idOfLinkedFolder = null,
                ), linkSaveConfig = LinkSaveConfig(
                    forceAutoDetectTitle = false, forceSaveWithoutRetrievingData = true,

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

    private val historyLinksPaginator = Paginator(
        onRetrieve = { nextPageStartIndex ->
            nextPageStartIndex to combine(
                snapshotFlow {
                    AppPreferences.selectedSortingType.value
                },
                snapshotFlow {
                    AppPreferences.forceShuffleLinks.value
                },
            ) { selectedSortingType, forceShuffleLinks ->
                forceShuffleLinks to selectedSortingType
            }.flatMapLatest { (forceShuffleLinks, selectedSortingType) ->
                localLinksRepo.getLinks(
                    linkType = LinkType.HISTORY_LINK,
                    selectedSortingType,
                    pageSize = Constants.PAGE_SIZE,
                    startIndex = nextPageStartIndex
                )
                    .flatMapLatest {
                        when (it) {
                            is Result.Failure -> flowOf(Result.Failure(it.message))
                            is Result.Loading -> flowOf(Result.Loading())
                            is Result.Success -> {
                                val allLinks =
                                    if (forceShuffleLinks) it.data.shuffled() else it.data
                                val linkIds = allLinks.map { it.localId }
                                localTagsRepo.getTagsForLinks(linkIds).map { tagsForLinks ->
                                    allLinks.map {
                                        LinkTagsPair(
                                            link = it,
                                            tags = tagsForLinks[it.localId] ?: emptyList()
                                        )
                                    }
                                }.map {
                                    Result.Success(it)
                                }
                            }
                        }
                    }
            }
        },
        onRetrieved = _historyLinkTagsPairsState::onRetrieved,
        onError = _historyLinkTagsPairsState::onError,
        onRetrieving = _historyLinkTagsPairsState::onRetrieving,
        onPagesFinished = _historyLinkTagsPairsState::onPagesFinished,
        coroutineScope = viewModelScope
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