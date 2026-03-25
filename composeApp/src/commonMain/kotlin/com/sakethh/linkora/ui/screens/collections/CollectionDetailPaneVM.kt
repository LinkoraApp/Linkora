package com.sakethh.linkora.ui.screens.collections

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.FlatChildFolderData
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalDatabaseUtilsRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.LastSeenId
import com.sakethh.linkora.ui.LastSeenString
import com.sakethh.linkora.ui.Paginator
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.domain.model.CollectionDetailPaneInfo
import com.sakethh.linkora.ui.domain.model.CollectionType
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushLocalizedSnackbar
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.asStateInWhileSubscribed
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.onError
import com.sakethh.linkora.utils.onPagesFinished
import com.sakethh.linkora.utils.onRetrieved
import com.sakethh.linkora.utils.onRetrieving
import com.sakethh.linkora.utils.shuffleLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

class CollectionDetailPaneVM(
    private val localFoldersRepo: LocalFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
    private val localTagsRepo: LocalTagsRepo,
    private val localDatabaseUtilsRepo: LocalDatabaseUtilsRepo,
    val collectionDetailPaneInfo: CollectionDetailPaneInfo
) : ViewModel() {

    private val _linkTagsPairsState = MutableStateFlow(
        value = PaginationState.retrieving<List<LinkTagsPair>>()
    )

    val linkTagsPairsState = _linkTagsPairsState.asStateInWhileSubscribed(
        initialValue = PaginationState.retrieving()
    )

    enum class LinkTagsPairPaginatorType {
        LinksAssociatedWithATag,
        FolderBased
    }

    val linkTagsPairPaginatorType
        get() = run {
            if (collectionDetailPaneInfo.collectionType == CollectionType.TAG && collectionDetailPaneInfo.currentTag != null) {
                LinkTagsPairPaginatorType.LinksAssociatedWithATag
            } else {
                LinkTagsPairPaginatorType.FolderBased
            }
        }

    private val currentInstanceLinkType = when (collectionDetailPaneInfo.currentFolder?.localId) {
        Constants.SAVED_LINKS_ID -> {
            LinkType.SAVED_LINK
        }

        Constants.IMPORTANT_LINKS_ID -> {
            LinkType.IMPORTANT_LINK
        }

        Constants.ARCHIVE_ID -> {
            LinkType.ARCHIVE_LINK
        }

        Constants.HISTORY_ID -> {
            LinkType.HISTORY_LINK
        }

        else -> {
            LinkType.FOLDER_LINK
        }
    }

    fun performAction(collectionPaneAction: CollectionPaneAction) = when (collectionPaneAction) {
        is CollectionPaneAction.ToggleAllLinksFilter -> toggleAllLinksFilter(collectionPaneAction.filter)
        is CollectionPaneAction.OnFirstVisibleItemIndexChangeOfLinkTagsPair -> {
            if (collectionDetailPaneInfo.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                updateAllLinksPaginatorFirstVisibleIndex(collectionPaneAction.index)
            } else {
                updateLinkTagsPaginatorFirstVisibleIndex(
                    collectionPaneAction.index
                )
            }
        }

        CollectionPaneAction.RetrieveNextLinksPage -> {
            if (collectionDetailPaneInfo.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                retrieveNextAllLinksPage()
            } else {
                retrieveNextLinksPage()
            }
        }


        is CollectionPaneAction.OnFirstVisibleItemIndexChangeOfRootArchivedFolders -> updateStartingIndexForArchivedRootFoldersPaginator(
            collectionPaneAction.index
        )

        CollectionPaneAction.RetrieveNextRootArchivedFolderPage -> retrieveNextBatchOfArchivedRootFolders()
        is CollectionPaneAction.AddANewLink ->
            viewModelScope.launch {
                localLinksRepo.addANewLink(
                    link = collectionPaneAction.link,
                    selectedTagIds = collectionPaneAction.selectedTags.map {
                        it.localId
                    },
                    linkSaveConfig = collectionPaneAction.linkSaveConfig
                ).collectLatest {
                    it.onSuccess {
                        collectionPaneAction.onCompletion()
                        if (collectionPaneAction.pushSnackbarOnSuccess) {
                            Localization.Key.SavedTheLink.pushLocalizedSnackbar(append = it.getRemoteOnlyFailureMsg())
                        }
                    }.onFailure {
                        collectionPaneAction.onCompletion()
                        UIEvent.pushUIEvent(UIEvent.Type.ShowSnackbar(it))
                    }
                }
            }

    }

    fun retrieveNextBatchOfArchivedRootFolders() {
        viewModelScope.launch {
            archiveRootFoldersPaginator.retrieveNextBatch()
        }
    }

    fun updateStartingIndexForArchivedRootFoldersPaginator(newIndex: Long) {
        viewModelScope.launch {
            archiveRootFoldersPaginator.updateFirstVisibleItemIndex(newIndex)
        }
    }

    private val appPreferencesCombined = combine(snapshotFlow {
        AppPreferences.forceShuffleLinks.value
    }, snapshotFlow {
        AppPreferences.selectedSortingType.value
    }) { shuffleLinks, sortingType ->
        Pair(shuffleLinks, sortingType)
    }

    val sortingType get() = AppPreferences.selectedSortingType.value
    val shuffleLinks get() = AppPreferences.forceShuffleLinks.value

    // localDatabaseUtilsRepo#getChildFolderData supports this directly, since it directly queries and returns the result. This can be replaced with it, but this should be fine.
    fun Flow<Result<List<Link>>>.mapToLinkTagsPair(): Flow<Result<List<LinkTagsPair>>> {
        return flatMapLatest { result ->
            when (result) {
                is Result.Failure -> flowOf(Result.Failure(result.message))
                is Result.Loading -> flowOf(Result.Loading())
                is Result.Success -> {
                    val linksIds = result.data.map { it.localId }
                    localTagsRepo.getTagsForLinks(linksIds).map { tagsMap ->
                        result.data.map { link ->
                            LinkTagsPair(
                                link = link, tags = tagsMap[link.localId] ?: emptyList()
                            )
                        }
                    }.flatMapLatest {
                        flowOf(Result.Success(it))
                    }
                }
            }
        }
    }

    private val _rootArchiveFolders = MutableStateFlow(
        PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = emptyMap<Pair<LastSeenId, LastSeenString>, List<Folder>>()
        )
    )
    val rootArchiveFolders = _rootArchiveFolders.asStateInWhileSubscribed(
        initialValue = PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = emptyMap()
        )
    )

    private val archiveRootFoldersPaginator = Paginator(
        coroutineScope = viewModelScope,
        onRetrieve = { lastSeenId, lastSeenString ->
            localFoldersRepo.getRootFolders(
                sortingType,
                isArchived = true,
                pageSize = Constants.PAGE_SIZE,
                lastSeenId = lastSeenId,
                lastSeenName = lastSeenString
            )
        },
        onRetrieved = { currentKey, retrievedData ->
            _rootArchiveFolders.onRetrieved(
                currentKey = currentKey,
                data = retrievedData,
                shouldShuffle = AppPreferences.forceShuffleLinks.value,
                idSelector = { it.localId },
                stringSelector = { it.name })
        },
        onError = _rootArchiveFolders::onError,
        onRetrieving = _rootArchiveFolders::onRetrieving,
        onPagesFinished = _rootArchiveFolders::onPagesFinished
    )

    private val linkTagsPairPaginator = Paginator(
        coroutineScope = viewModelScope,
        onRetrieve = { lastSeenId, lastSeenString ->
            if (linkTagsPairPaginatorType == LinkTagsPairPaginatorType.LinksAssociatedWithATag) {
                localLinksRepo.getLinks(
                    tagId = collectionDetailPaneInfo.currentTag?.localId
                        ?: error("collectionDetailPaneInfo.currentTag?.localId is null"),
                    sortOption = sortingType,
                    pageSize = Constants.PAGE_SIZE,
                    lastSeenTitle = lastSeenString,
                    lastSeenId = lastSeenId
                ).run {
                    if (shuffleLinks) shuffleLinks() else this
                }.mapToLinkTagsPair()
            } else {
                localLinksRepo.getLinks(
                    linkType = currentInstanceLinkType,
                    parentFolderId = collectionDetailPaneInfo.currentFolder?.localId
                        ?: error("collectionDetailPaneInfo.currentFolder?.localId is null"),
                    sortOption = sortingType,
                    pageSize = Constants.PAGE_SIZE,
                    lastSeenId = lastSeenId,
                    lastSeenTitle = lastSeenString
                ).run {
                    if (shuffleLinks) shuffleLinks() else this
                }.mapToLinkTagsPair()
            }
        },
        onRetrieved = { currentKey, retrievedData ->
            _linkTagsPairsState.onRetrieved(
                currentKey = currentKey,
                data = retrievedData,
                shouldShuffle = AppPreferences.forceShuffleLinks.value,
                idSelector = { it.link.localId },
                stringSelector = { it.link.title })
        },
        onError = _linkTagsPairsState::onError,
        onRetrieving = _linkTagsPairsState::onRetrieving,
        onPagesFinished = _linkTagsPairsState::onPagesFinished
    )


    private val _childFoldersFlat = MutableStateFlow(
        value = PaginationState.retrieving<List<FlatChildFolderData>>()
    )
    val childFoldersFlat = _childFoldersFlat.asStateInWhileSubscribed(
        initialValue = PaginationState.retrieving()
    )

    private val childFoldersFlatPaginator = Paginator(
        coroutineScope = viewModelScope,
        onRetrieve = { _, lastSeenString ->
            val parts = lastSeenString?.split("|")

            val lastTypeOrder = parts?.getOrNull(0)?.toIntOrNull() ?: -1
            val lastSortStr = parts?.getOrNull(1) ?: ""
            val lastId = parts?.getOrNull(2)?.toLongOrNull() ?: Constants.EMPTY_LAST_SEEN_ID

            localDatabaseUtilsRepo.getChildFolderData(
                parentFolderId = collectionDetailPaneInfo.currentFolder?.localId
                    ?: error("childFoldersPaginator: Parent ID is null"),
                linkType = LinkType.FOLDER_LINK,
                sortOption = sortingType,
                pageSize = Constants.PAGE_SIZE,
                lastTypeOrder = lastTypeOrder,
                lastSortStr = lastSortStr,
                lastId = lastId
            )
        },
        onRetrieved = { currentKey, retrievedData ->
            _childFoldersFlat.onRetrieved(
                currentKey = currentKey,
                data = retrievedData,
                shouldShuffle = AppPreferences.forceShuffleLinks.value,

                idSelector = { item ->
                    item.folderLocalId ?: item.linkLocalId ?: 0L
                },

                stringSelector = { item ->
                    val typeOrder = if (item.itemType == "FOLDER") 0 else 1

                    val sortStr = item.folderName ?: item.linkTitle ?: ""

                    val sortId = item.folderLocalId ?: item.linkLocalId ?: 0L

                    "$typeOrder|$sortStr|$sortId"
                })
        },
        onError = _childFoldersFlat::onError,
        onRetrieving = _childFoldersFlat::onRetrieving,
        onPagesFinished = _childFoldersFlat::onPagesFinished
    )


    private fun updateLinkTagsPaginatorFirstVisibleIndex(index: Long) {
        viewModelScope.launch {
            if (collectionDetailPaneInfo.currentFolder != null && collectionDetailPaneInfo.currentFolder.localId >= 0) {
                childFoldersFlatPaginator.updateFirstVisibleItemIndex(index)
            } else {
                linkTagsPairPaginator.updateFirstVisibleItemIndex(index)
            }
        }
    }

    private fun updateAllLinksPaginatorFirstVisibleIndex(index: Long) {
        viewModelScope.launch {
            allLinksPaginator.updateFirstVisibleItemIndex(index)
        }
    }

    private fun retrieveNextLinksPage() {
        viewModelScope.launch {
            if (collectionDetailPaneInfo.currentFolder != null && collectionDetailPaneInfo.currentFolder.localId >= 0) {
                childFoldersFlatPaginator.retrieveNextBatch()
            } else {
                linkTagsPairPaginator.retrieveNextBatch()
            }
        }
    }

    private fun retrieveNextAllLinksPage() {
        viewModelScope.launch {
            allLinksPaginator.retrieveNextBatch()
        }
    }


    private val _appliedFiltersForAllLinks = mutableStateListOf<LinkType>()
    val appliedFiltersForAllLinks = _appliedFiltersForAllLinks

    fun toggleAllLinksFilter(filter: LinkType) {
        if (_appliedFiltersForAllLinks.contains(filter).not()) {
            _appliedFiltersForAllLinks.add(filter)
        } else {
            _appliedFiltersForAllLinks.remove(filter)
        }
    }

    private val allLinksPaginator = Paginator(
        coroutineScope = viewModelScope,
        onRetrieve = { lastSeenId, lastSeenString ->
            localLinksRepo.getAllLinks(
                applyLinkFilters = appliedFiltersForAllLinks.isNotEmpty(),
                activeLinkFilters = appliedFiltersForAllLinks.toList().map { it.name },
                sortOption = sortingType,
                pageSize = Constants.PAGE_SIZE,
                lastSeenId = lastSeenId,
                lastSeenName = lastSeenString
            ).run {
                if (shuffleLinks) shuffleLinks() else this
            }.mapToLinkTagsPair()
        },
        onRetrieved = { currentKey, retrievedData ->
            _linkTagsPairsState.onRetrieved(
                currentKey = currentKey,
                data = retrievedData,
                shouldShuffle = AppPreferences.forceShuffleLinks.value,
                idSelector = { it.link.localId },
                stringSelector = { it.link.title })
        },
        onError = _linkTagsPairsState::onError,
        onRetrieving = _linkTagsPairsState::onRetrieving,
        onPagesFinished = _linkTagsPairsState::onPagesFinished
    )


    init {
        viewModelScope.launch {
            launch {
                if (collectionDetailPaneInfo.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                    _linkTagsPairsState.emit(PaginationState.retrieving())
                    retrieveNextAllLinksPage()
                    return@launch
                }

                if (collectionDetailPaneInfo.currentFolder?.localId == Constants.ARCHIVE_ID) {
                    _rootArchiveFolders.emit(PaginationState.retrievingOnEmpty())
                    archiveRootFoldersPaginator.retrieveNextBatch()
                    return@launch
                }

                _linkTagsPairsState.emit(PaginationState.retrieving())
                emptyCollectableChildFolders()
                retrieveNextLinksPage()
            }

            launch {
                snapshotFlow {
                    _appliedFiltersForAllLinks.toList()
                }.transform {
                    if (collectionDetailPaneInfo.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                        emit(it)
                    }
                }.collectLatest {
                    allLinksPaginator.cancelAndReset()
                    _linkTagsPairsState.emit(PaginationState.retrievingOnEmpty())
                    retrieveNextAllLinksPage()
                }
            }

            launch {
                var lastSortingType = AppPreferences.selectedSortingType.value
                appPreferencesCombined.collectLatest { (shuffleLinks, sortingType) ->
                    val isSortingTypeChanged = if (sortingType == lastSortingType) {
                        false
                    } else {
                        lastSortingType = sortingType
                        true
                    }

                    if (isSortingTypeChanged) {
                        linkTagsPairPaginator.cancelAndReset()
                        allLinksPaginator.cancelAndReset()
                        childFoldersFlatPaginator.cancelAndReset()
                        _linkTagsPairsState.emit(PaginationState.retrievingOnEmpty())
                        archiveRootFoldersPaginator.cancelAndReset()

                        if (collectionDetailPaneInfo.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                            retrieveNextAllLinksPage()
                            return@collectLatest
                        }

                        emptyCollectableChildFolders()
                        retrieveNextLinksPage()
                    }
                }
            }
        }
    }


    private fun emptyCollectableChildFolders() {
        viewModelScope.launch(Dispatchers.Main) {
            _childFoldersFlat.emit(PaginationState.retrieving())
        }
    }
}