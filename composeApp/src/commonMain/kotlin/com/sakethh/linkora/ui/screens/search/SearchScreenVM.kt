package com.sakethh.linkora.ui.screens.search

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.common.utils.ifNot
import com.sakethh.linkora.common.utils.pushSnackbarOnFailure
import com.sakethh.linkora.domain.FolderType
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SearchScreenVM(
    private val localFoldersRepo: LocalFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
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

    private val _linkQueryResults = MutableStateFlow(emptyList<Link>())
    val linkQueryResults = _linkQueryResults.asStateFlow()

    private val _folderQueryResults = MutableStateFlow(emptyList<Folder>())
    val folderQueryResults = _folderQueryResults.asStateFlow()

    private val _appliedLinkFilters = mutableStateListOf<LinkType>()
    val appliedLinkFilters = _appliedLinkFilters

    private val _availableLinkFilters = mutableStateListOf<LinkType>()
    val availableLinkFilters = _availableLinkFilters

    private val _appliedFolderFilters = mutableStateListOf<FolderType>()
    val appliedFolderFilters = _appliedFolderFilters

    private val _availableFolderFilters = mutableStateListOf<FolderType>()
    val availableFolderFilters = _availableFolderFilters

    fun toggleLinkFilter(filter: LinkType) {
        if (_appliedLinkFilters.contains(filter).not()) {
            _appliedLinkFilters.add(filter)
        } else {
            _appliedLinkFilters.remove(filter)
        }
    }

    fun toggleFolderFilter(filter: FolderType) {
        if (_appliedFolderFilters.contains(filter).not()) {
            _appliedFolderFilters.add(filter)
        } else {
            _appliedFolderFilters.remove(filter)
        }
    }

    init {
        viewModelScope.launch {
            combine(
                snapshotFlow { _searchQuery.value },
                snapshotFlow { AppPreferences.selectedSortingTypeType.value },
                snapshotFlow { _appliedFolderFilters.toList() },
                snapshotFlow { _appliedLinkFilters.toList() }) { query, _, _, _ -> query }.collectLatest { query ->

                if (query.isBlank()) {
                    _linkQueryResults.emit(emptyList())
                    _folderQueryResults.emit(emptyList())
                    return@collectLatest
                }

                launch {
                    _availableLinkFilters.clear()

                    localLinksRepo.search(query, AppPreferences.selectedSortingTypeType.value)
                        .collectLatest { result ->
                            result.onSuccess { success ->
                                _availableLinkFilters.addAll(success.data.map { it.linkType }
                                    .distinct())

                                val filteredResults = success.data.filter {
                                    _appliedLinkFilters.isEmpty() || it.linkType in _appliedLinkFilters
                                }

                                _linkQueryResults.emit(filteredResults)
                            }
                        }
                }

                launch {
                    _availableFolderFilters.clear()
                    localFoldersRepo.search(query, AppPreferences.selectedSortingTypeType.value)
                        .collectLatest { result ->
                            result.onSuccess { success ->
                                _availableFolderFilters.addAll(success.data.map {
                                    if (it.isArchived) {
                                        FolderType.ARCHIVE_FOLDER
                                    } else {
                                        FolderType.REGULAR_FOLDER
                                    }
                                }.distinct())

                                val filteredResults = success.data.filter {
                                    _appliedFolderFilters.isEmpty() || if (it.isArchived) {
                                        FolderType.ARCHIVE_FOLDER
                                    } else {
                                        FolderType.REGULAR_FOLDER
                                    } in _appliedFolderFilters
                                }
                                _folderQueryResults.emit(filteredResults)
                            }.pushSnackbarOnFailure()
                        }
                }
            }
        }

    }

    fun addANewLinkToHistory(link: Link) {
        viewModelScope.launch {
            localLinksRepo.addANewLink(
                link = link.copy(
                    linkType = LinkType.HISTORY_LINK,
                    idOfLinkedFolder = null,
                ), linkSaveConfig = LinkSaveConfig(
                    forceAutoDetectTitle = false, forceSaveWithoutRetrievingData = true
                )
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

    private val _links = MutableStateFlow(emptyList<Link>())
    val links = _links.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                snapshotFlow {
                    AppPreferences.selectedSortingTypeType.value
                },
                snapshotFlow {
                    AppPreferences.forceShuffleLinks.value
                },
            ) { selectedSortingType, forceShuffleLinks ->
                forceShuffleLinks to selectedSortingType
            }.collectLatest { (forceShuffleLinks, selectedSortingType) ->
                localLinksRepo.getSortedLinks(linkType = LinkType.HISTORY_LINK, selectedSortingType)
                    .collectLatest {
                        it.onSuccess {
                            _links.apply {
                                if (forceShuffleLinks) {
                                    emit(it.data.shuffled())
                                } else {
                                    emit(it.data)
                                }
                            }
                        }.pushSnackbarOnFailure()
                    }
            }
        }
    }
}