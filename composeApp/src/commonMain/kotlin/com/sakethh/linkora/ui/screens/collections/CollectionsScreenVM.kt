package com.sakethh.linkora.ui.screens.collections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.FlatChildFolderData
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalDatabaseUtilsRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.PageKey
import com.sakethh.linkora.ui.Paginator
import com.sakethh.linkora.ui.domain.AddANewLinkDialogBoxAction
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.domain.model.CollectionDetailPaneInfo
import com.sakethh.linkora.ui.domain.model.CollectionType
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushLocalizedSnackbar
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.asStateInWhileSubscribed
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.onError
import com.sakethh.linkora.utils.onPagesFinished
import com.sakethh.linkora.utils.onRetrieved
import com.sakethh.linkora.utils.onRetrieving
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import com.sakethh.linkora.utils.replaceFirstPlaceHolderWith
import com.sakethh.linkora.utils.shuffleLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.internal.toImmutableMap
import java.util.TreeMap

class CollectionsScreenVM(
    private val localFoldersRepo: LocalFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
    private val localTagsRepo: LocalTagsRepo,
    private val localDatabaseUtilsRepo: LocalDatabaseUtilsRepo,
    loadNonArchivedRootFoldersOnInit: Boolean = true,
    loadArchivedRootFoldersOnInit: Boolean = true,
    val collectionDetailPaneInfo: CollectionDetailPaneInfo? = null,
    val platform: Platform,
    preferencesRepo: PreferencesRepository? = null
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
            val collectionInfo = dynamicCollectionDetailPaneInfo
            if (collectionInfo?.collectionType == CollectionType.TAG && collectionInfo.currentTag != null) {
                LinkTagsPairPaginatorType.LinksAssociatedWithATag
            } else {
                LinkTagsPairPaginatorType.FolderBased
            }
        }

    private val currentInstanceLinkType
        get() = when (dynamicCollectionDetailPaneInfo?.currentFolder?.localId) {
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

    private val linkTagsPairPaginator = Paginator(
        coroutineScope = viewModelScope,
        onRetrieve = { nextPageStartIndex ->
            nextPageStartIndex to if (linkTagsPairPaginatorType == LinkTagsPairPaginatorType.LinksAssociatedWithATag) {
                localLinksRepo.getLinks(
                    tagId = dynamicCollectionDetailPaneInfo?.currentTag?.localId
                        ?: error("linkTagsPairPaginator-dynamicCollectionDetailPaneInfo?.currentTag?.localId is null"),
                    sortOption = sortingType,
                    pageSize = Constants.PAGE_SIZE,
                    startIndex = nextPageStartIndex
                ).run {
                    if (shuffleLinks) shuffleLinks() else this
                }.mapToLinkTagsPair()
            } else {
                localLinksRepo.getLinks(
                    linkType = currentInstanceLinkType,
                    parentFolderId = dynamicCollectionDetailPaneInfo?.currentFolder?.localId
                        ?: error("linkTagsPairPaginator-dynamicCollectionDetailPaneInfo?.currentFolder?.localId is null"),
                    sortOption = sortingType,
                    pageSize = Constants.PAGE_SIZE,
                    startIndex = nextPageStartIndex
                ).run {
                    if (shuffleLinks) shuffleLinks() else this
                }.mapToLinkTagsPair()
            }
        },
        onRetrieved = _linkTagsPairsState::onRetrieved,
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
        onRetrieve = { nextPageStartIndex ->
            nextPageStartIndex to localDatabaseUtilsRepo.getChildFolderData(
                parentFolderId = dynamicCollectionDetailPaneInfo?.currentFolder?.localId
                    ?: error("childFoldersPaginator-dynamicCollectionDetailPaneInfo?.currentFolder?.localId is null"),
                sortOption = sortingType,
                pageSize = Constants.PAGE_SIZE,
                startIndex = nextPageStartIndex,
                linkType = LinkType.FOLDER_LINK
            ).run {
                if (shuffleLinks) shuffleLinks() else this
            }
        },
        onRetrieved = _childFoldersFlat::onRetrieved,
        onError = _childFoldersFlat::onError,
        onRetrieving = _childFoldersFlat::onRetrieving,
        onPagesFinished = _childFoldersFlat::onPagesFinished
    )

    companion object {
        val selectedLinkTagPairsViaLongClick = mutableStateListOf<LinkTagsPair>()
        val selectedFoldersViaLongClick = mutableStateListOf<Folder>()
        val isSelectionEnabled = mutableStateOf(false)

        fun clearAllSelections() {
            isSelectionEnabled.value = false
            selectedLinkTagPairsViaLongClick.clear()
            selectedFoldersViaLongClick.clear()
        }
    }

    private val _detailPaneHistory = MutableStateFlow<List<CollectionDetailPaneInfo>>(emptyList())

    val isPaneSelected = _detailPaneHistory.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val peekPaneHistory = _detailPaneHistory.map {
        try {
            it.last()
        } catch (_: Exception) {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val dynamicCollectionDetailPaneInfo: CollectionDetailPaneInfo?
        get() = if (platform is Platform.Android.Mobile) collectionDetailPaneInfo else peekPaneHistory.value

    fun clearDetailPaneHistoryUntilLast() {
        viewModelScope.launch {
            _detailPaneHistory.update {
                listOf(it.last())
            }
        }
    }

    fun clearDetailPaneHistory() {
        viewModelScope.launch {
            _detailPaneHistory.emit(emptyList())
        }
    }

    fun pushToDetailPane(collectionDetailPaneInfo: CollectionDetailPaneInfo) {
        _detailPaneHistory.update {
            it + collectionDetailPaneInfo
        }
    }

    fun popFromDetailPane(): CollectionDetailPaneInfo? {
        _detailPaneHistory.update {
            try {
                it.dropLast(1)
            } catch (_: Exception) {
                emptyList()
            }
        }
        return peekPaneHistory.value
    }

    fun performAction(addANewLinkDialogBoxAction: AddANewLinkDialogBoxAction) =
        when (addANewLinkDialogBoxAction) {
            is AddANewLinkDialogBoxAction.AddANewLink -> addANewLink(
                link = addANewLinkDialogBoxAction.link,
                selectedTags = addANewLinkDialogBoxAction.selectedTags,
                linkSaveConfig = addANewLinkDialogBoxAction.linkSaveConfig,
                onCompletion = addANewLinkDialogBoxAction.onCompletion,
                pushSnackbarOnSuccess = addANewLinkDialogBoxAction.pushSnackbarOnSuccess
            )

            AddANewLinkDialogBoxAction.ClearSelectedTags -> clearSelectedTags()
            is AddANewLinkDialogBoxAction.CreateATag -> createATag(
                tagName = addANewLinkDialogBoxAction.tagName,
                onCompletion = addANewLinkDialogBoxAction.onCompletion
            )

            is AddANewLinkDialogBoxAction.InsertANewFolder -> insertANewFolder(
                folder = addANewLinkDialogBoxAction.folder,
                ignoreFolderAlreadyExistsThrowable = addANewLinkDialogBoxAction.ignoreFolderAlreadyExistsThrowable,
                onCompletion = addANewLinkDialogBoxAction.onCompletion
            )

            is AddANewLinkDialogBoxAction.SelectATag -> selectATag(addANewLinkDialogBoxAction.tag)
            is AddANewLinkDialogBoxAction.UnSelectATag -> unSelectATag(addANewLinkDialogBoxAction.tag)
            is AddANewLinkDialogBoxAction.UpdateFoldersSearchQuery -> foldersSearchQuery =
                addANewLinkDialogBoxAction.string

            is AddANewLinkDialogBoxAction.OnFirstVisibleIndexChangeOfTags -> updateStartingIndexForTagsPaginator(
                addANewLinkDialogBoxAction.index
            )

            AddANewLinkDialogBoxAction.OnRetrieveNextTagsPage -> retrieveNextBatchOfTags()
            is AddANewLinkDialogBoxAction.OnFirstVisibleIndexChangeOfRootFolders -> updateStartingIndexForRegularRootFoldersPaginator(
                addANewLinkDialogBoxAction.index
            )

            AddANewLinkDialogBoxAction.OnRetrieveNextRegularRootPage -> retrieveNextBatchOfRegularRootFolders()
        }

    fun performAction(collectionsAction: CollectionsAction) = when (collectionsAction) {
        is CollectionsAction.AddANewLink -> addANewLink(
            link = collectionsAction.link,
            selectedTags = collectionsAction.selectedTags,
            linkSaveConfig = collectionsAction.linkSaveConfig,
            onCompletion = collectionsAction.onCompletion,
            pushSnackbarOnSuccess = collectionsAction.pushSnackbarOnSuccess
        )

        CollectionsAction.PopFromDetailPane -> popFromDetailPane()
        is CollectionsAction.PushToDetailPane -> pushToDetailPane(collectionsAction.collectionDetailPaneInfo)
        is CollectionsAction.ToggleAllLinksFilter -> toggleAllLinksFilter(collectionsAction.filter)
        CollectionsAction.ClearDetailPaneHistoryUntilLast -> clearDetailPaneHistoryUntilLast()

        is CollectionsAction.OnFirstVisibleItemIndexChangeOfLinkTagsPair -> {
            if (dynamicCollectionDetailPaneInfo?.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                updateAllLinksPaginatorFirstVisibleIndex(collectionsAction.index)
            } else {
                updateLinkTagsPaginatorFirstVisibleIndex(
                    collectionsAction.index
                )
            }
        }

        CollectionsAction.RetrieveNextLinksPage -> {
            if (dynamicCollectionDetailPaneInfo?.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                retrieveNextAllLinksPage()
            } else {
                retrieveNextLinksPage()
            }
        }

        is CollectionsAction.OnFirstVisibleItemIndexChangeOfRootArchivedFolders -> updateStartingIndexForArchivedRootFoldersPaginator(
            collectionsAction.index
        )

        CollectionsAction.RetrieveNextRootArchivedFolderPage -> retrieveNextBatchOfArchivedRootFolders()
    }

    private fun updateLinkTagsPaginatorFirstVisibleIndex(index: Long) {
        val currentCollectionInfo = dynamicCollectionDetailPaneInfo
        viewModelScope.launch {
            if (currentCollectionInfo?.currentFolder?.localId != null && currentCollectionInfo.currentFolder.localId >= 0) {
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
        val currentCollectionInfo = dynamicCollectionDetailPaneInfo
        viewModelScope.launch {
            if (currentCollectionInfo?.currentFolder?.localId != null && currentCollectionInfo.currentFolder.localId >= 0) {
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
        onRetrieve = { nextPageStartIndex ->
            nextPageStartIndex to localLinksRepo.getAllLinks(
                applyLinkFilters = appliedFiltersForAllLinks.isNotEmpty(),
                activeLinkFilters = appliedFiltersForAllLinks.toList().map { it.name },
                sortOption = sortingType,
                pageSize = Constants.PAGE_SIZE,
                startIndex = nextPageStartIndex,
            ).run {
                if (shuffleLinks) shuffleLinks() else this
            }.mapToLinkTagsPair()
        },
        onRetrieved = _linkTagsPairsState::onRetrieved,
        onError = _linkTagsPairsState::onError,
        onRetrieving = _linkTagsPairsState::onRetrieving,
        onPagesFinished = _linkTagsPairsState::onPagesFinished
    )

    private val _rootRegularFolders = MutableStateFlow(
        PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = TreeMap<PageKey, List<Folder>>().toImmutableMap()
        )
    )
    val rootRegularFolders = _rootRegularFolders.asStateInWhileSubscribed(
        initialValue = PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = emptyMap()
        )
    )

    private val _rootArchiveFolders = MutableStateFlow(
        PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = TreeMap<PageKey, List<Folder>>().toImmutableMap()
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

    private val _allTags = MutableStateFlow(
        value = PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = TreeMap<PageKey, List<Tag>>().toImmutableMap()
        )
    )

    val allTags = _allTags.asStateInWhileSubscribed(
        initialValue = PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = emptyMap()
        )
    )

    private val regularRootFoldersPaginator = Paginator(
        coroutineScope = viewModelScope,
        onRetrieve = { nextPageStartIndex ->
            nextPageStartIndex to localFoldersRepo.getRootFolders(
                sortingType,
                isArchived = false,
                pageSize = Constants.PAGE_SIZE,
                startIndex = nextPageStartIndex
            )
        },
        onRetrieved = _rootRegularFolders::onRetrieved,
        onError = _rootRegularFolders::onError,
        onRetrieving = _rootRegularFolders::onRetrieving,
        onPagesFinished = _rootRegularFolders::onPagesFinished
    )

    private val archiveRootFoldersPaginator = Paginator(
        coroutineScope = viewModelScope,
        onRetrieve = { nextPageStartIndex ->
            nextPageStartIndex to localFoldersRepo.getRootFolders(
                sortingType,
                isArchived = true,
                pageSize = Constants.PAGE_SIZE,
                startIndex = nextPageStartIndex
            )
        },
        onRetrieved = _rootArchiveFolders::onRetrieved,
        onError = _rootArchiveFolders::onError,
        onRetrieving = _rootArchiveFolders::onRetrieving,
        onPagesFinished = _rootArchiveFolders::onPagesFinished
    )

    private val tagsPaginator = Paginator(
        coroutineScope = viewModelScope,
        onRetrieve = { nextPageStartIndex ->
            nextPageStartIndex to localTagsRepo.getTags(
                sortingType,
                pageSize = Constants.PAGE_SIZE,
                startIndex = nextPageStartIndex
            )
        },
        onRetrieved = _allTags::onRetrieved,
        onError = _allTags::onError,
        onRetrieving = _allTags::onRetrieving,
        onPagesFinished = _allTags::onPagesFinished
    )


    fun retrieveNextBatchOfRegularRootFolders() {
        viewModelScope.launch {
            regularRootFoldersPaginator.retrieveNextBatch()
        }
    }

    fun retrieveNextBatchOfTags() {
        viewModelScope.launch {
            tagsPaginator.retrieveNextBatch()
        }
    }

    fun updateStartingIndexForRegularRootFoldersPaginator(newIndex: Long) {
        viewModelScope.launch {
            regularRootFoldersPaginator.updateFirstVisibleItemIndex(newIndex)
        }
    }

    fun updateStartingIndexForTagsPaginator(newIndex: Long) {
        viewModelScope.launch {
            tagsPaginator.updateFirstVisibleItemIndex(newIndex)
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

    private val _selectedTags = mutableStateListOf<Tag>()
    val selectedTags: List<Tag> = _selectedTags

    fun selectATag(tag: Tag) {
        if (!_selectedTags.contains(tag)) {
            _selectedTags.add(tag)
        }
    }

    fun unSelectATag(tag: Tag) {
        _selectedTags.remove(tag)
    }

    fun createATag(tagName: String, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localTagsRepo.createATag(Tag(name = tagName)).collect()
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    private val _currentCollectionSource get() = if (AppPreferences.selectedCollectionSourceId == 0) Localization.Key.Folders.getLocalizedString() else Localization.Key.Tags.getLocalizedString()
    var currentCollectionSource by mutableStateOf(_currentCollectionSource)

    var foldersSearchQuery by mutableStateOf("")
    private val _foldersSearchQueryResult = MutableStateFlow(emptyList<Folder>())
    val foldersSearchQueryResult = _foldersSearchQueryResult.asStateFlow()

    init {

        viewModelScope.launch {
            combine(snapshotFlow {
                foldersSearchQuery
            }, snapshotFlow {
                AppPreferences.selectedSortingType.value
            }) { query, sortingType ->
                Pair(query, sortingType)
            }.cancellable().collectLatest { (query, sortingType) ->
                localFoldersRepo.search(query = query, sortOption = sortingType).collectLatest {
                    it.onSuccess {
                        _foldersSearchQueryResult.emit(it.data)
                    }
                }
            }
        }

        if (preferencesRepo != null) {
            viewModelScope.launch {
                snapshotFlow {
                    AppPreferences.selectedCollectionSourceId
                }.cancellable().collectLatest {
                    currentCollectionSource = _currentCollectionSource
                    preferencesRepo.changePreferenceValue(
                        preferenceKey = intPreferencesKey(
                            AppPreferenceType.COLLECTION_SOURCE_ID.name
                        ), newValue = it
                    )
                }
            }

            viewModelScope.launch {
                snapshotFlow {
                    AppPreferences.showTagsInAddNewLinkDialogBox
                }.cancellable().collectLatest {
                    preferencesRepo.changePreferenceValue(
                        preferenceKey = booleanPreferencesKey(
                            AppPreferenceType.SHOW_TAGS_BY_DEFAULT_IN_ADD_LINK.name
                        ), newValue = it
                    )
                }
            }
        }
        viewModelScope.launch {
            tagsPaginator.retrieveNextBatch()
        }

        suspend fun loadRootFolders() {
            _rootArchiveFolders.emit(PaginationState.retrievingOnEmpty())
            _rootRegularFolders.emit(PaginationState.retrievingOnEmpty())

            if (loadNonArchivedRootFoldersOnInit && loadArchivedRootFoldersOnInit) {
                regularRootFoldersPaginator.retrieveNextBatch()
                archiveRootFoldersPaginator.retrieveNextBatch()
            }

            if (loadArchivedRootFoldersOnInit && !loadNonArchivedRootFoldersOnInit) {
                archiveRootFoldersPaginator.retrieveNextBatch()
            }

            if (!loadArchivedRootFoldersOnInit && loadNonArchivedRootFoldersOnInit) {
                regularRootFoldersPaginator.retrieveNextBatch()
            }
        }

        viewModelScope.launch {
            loadRootFolders()
        }
        viewModelScope.launch {

            // android-mobile handing
            if (collectionDetailPaneInfo != null) {

                if (collectionDetailPaneInfo.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                    _linkTagsPairsState.emit(PaginationState.retrieving())
                    retrieveNextAllLinksPage()
                    return@launch
                }

                _linkTagsPairsState.emit(PaginationState.retrieving())
                emptyCollectableChildFolders()
                retrieveNextLinksPage()
            }
        }


        // ==== RESET THE STATE OF PAGINATORS (+HANDLE DESKTOP COLLECTION-DETAIL-PANE) =====

        viewModelScope.launch {

            launch {
                snapshotFlow {
                    _appliedFiltersForAllLinks.toList()
                }.transform {
                    if (dynamicCollectionDetailPaneInfo?.currentFolder?.localId == Constants.ALL_LINKS_ID) {
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

                combine(appPreferencesCombined, peekPaneHistory) { appPreferences, paneHistory ->
                    appPreferences.second to paneHistory
                }
                    .collectLatest { (sortingType, paneHistory) ->

                        val collectionPaneInfo = collectionDetailPaneInfo ?: paneHistory

                        linkTagsPairPaginator.cancelAndReset()
                        allLinksPaginator.cancelAndReset()
                        childFoldersFlatPaginator.cancelAndReset()
                        val isSortingTypeChanged = if (sortingType == lastSortingType) {
                            false
                        } else {
                            lastSortingType = sortingType
                            true
                        }

                        if (isSortingTypeChanged) {
                            tagsPaginator.cancelAndReset()
                            archiveRootFoldersPaginator.cancelAndReset()
                            regularRootFoldersPaginator.cancelAndReset()

                            loadRootFolders()
                            tagsPaginator.retrieveNextBatch()
                        }

                        if (collectionPaneInfo == null) return@collectLatest

                        _linkTagsPairsState.emit(PaginationState.retrievingOnEmpty())

                        if (collectionPaneInfo.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                            retrieveNextAllLinksPage()
                            return@collectLatest
                        }
                        emptyCollectableChildFolders()
                        retrieveNextLinksPage()
                    }
            }
        }
    }

    private fun emptyCollectableChildFolders() {
        viewModelScope.launch(Dispatchers.Main) {
            _childFoldersFlat.emit(PaginationState.retrieving())
        }
    }

    fun insertANewFolder(
        folder: Folder, ignoreFolderAlreadyExistsThrowable: Boolean, onCompletion: () -> Unit
    ) {
        viewModelScope.launch {
            localFoldersRepo.insertANewFolder(folder, ignoreFolderAlreadyExistsThrowable)
                .collectLatest {
                    it.onSuccess {
                        pushUIEvent(
                            UIEvent.Type.ShowSnackbar(
                                message = Localization.Key.FolderHasBeenCreatedSuccessful.getLocalizedString()
                                    .replaceFirstPlaceHolderWith(folder.name) + it.getRemoteOnlyFailureMsg()
                            )
                        )
                    }.onFailure {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(message = it))
                    }
                }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun deleteAFolder(folder: Folder, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localFoldersRepo.deleteAFolder(folderID = folder.localId).collectLatest {
                it.onSuccess {
                    onCompletion()
                    pushUIEvent(
                        UIEvent.Type.ShowSnackbar(
                            Localization.getLocalizedString(
                                Localization.Key.DeletedTheFolder
                            )
                                .replaceFirstPlaceHolderWith(folder.name) + it.getRemoteOnlyFailureMsg()
                        )
                    )
                }.onFailure {
                    onCompletion()
                }.pushSnackbarOnFailure()
            }
        }
    }

    fun deleteALink(link: Link, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localLinksRepo.deleteALink(link.localId).collectLatest {
                it.onSuccess {
                    Localization.Key.DeletedTheLink.pushLocalizedSnackbar(append = it.getRemoteOnlyFailureMsg())
                }.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun deleteTheNote(folder: Folder, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localFoldersRepo.deleteAFolderNote(folder.localId).collectLatest {
                it.onSuccess {
                    pushUIEvent(
                        UIEvent.Type.ShowSnackbar(
                            Localization.getLocalizedString(
                                Localization.Key.DeletedTheNoteOfAFolder
                            )
                                .replaceFirstPlaceHolderWith(folder.name) + it.getRemoteOnlyFailureMsg()
                        )
                    )
                }.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun deleteTheNote(link: Link, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localLinksRepo.deleteALinkNote(link.localId).collectLatest {
                it.onSuccess {
                    Localization.Key.DeletedTheNoteOfALink.pushLocalizedSnackbar(append = it.getRemoteOnlyFailureMsg())
                }.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun archiveAFolder(folder: Folder, onCompletion: () -> Unit) {
        viewModelScope.launch {
            if (folder.isArchived) {
                localFoldersRepo.markFolderAsRegularFolder(folder.localId).collectLatest {
                    it.onSuccess {
                        pushUIEvent(
                            UIEvent.Type.ShowSnackbar(
                                Localization.getLocalizedString(
                                    Localization.Key.UnArchivedTheFolder
                                )
                                    .replaceFirstPlaceHolderWith(folder.name) + it.getRemoteOnlyFailureMsg()
                            )
                        )
                    }.pushSnackbarOnFailure()
                }
            } else {
                localFoldersRepo.markFolderAsArchive(folder.localId).collectLatest {
                    it.onSuccess {
                        pushUIEvent(
                            UIEvent.Type.ShowSnackbar(
                                Localization.getLocalizedString(
                                    Localization.Key.ArchivedTheFolder
                                )
                                    .replaceFirstPlaceHolderWith(folder.name) + it.getRemoteOnlyFailureMsg()
                            )
                        )
                    }.pushSnackbarOnFailure()
                }
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun markALinkAsImp(link: Link, tagIds: List<Long>?, onCompletion: () -> Unit) {
        viewModelScope.launch {
            if (link.linkType == LinkType.IMPORTANT_LINK) {
                deleteALink(link, onCompletion = {})
                return@launch
            }
            localLinksRepo.addANewLink(
                link = link.copy(
                    idOfLinkedFolder = Constants.IMPORTANT_LINKS_ID,
                    localId = 0, linkType = LinkType.IMPORTANT_LINK,
                ),
                linkSaveConfig = LinkSaveConfig.forceSaveWithoutRetrieving(),
                selectedTagIds = tagIds
            ).collectLatest {
                it.onSuccess {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.AddedCopyToImpLinks.getLocalizedString()))
                }
                it.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun refreshLinkMetadata(
        refreshLinkType: RefreshLinkType,
        link: Link,
        onCompletion: () -> Unit
    ) {
        viewModelScope.launch {
            localLinksRepo.refreshLinkMetadata(link, refreshLinkType).collectLatest {
                it.onSuccess {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(message = Localization.Key.LinkRefreshedSuccessfully.getLocalizedString() + it.getRemoteOnlyFailureMsg()))
                }.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun archiveALink(link: Link, onCompletion: () -> Unit) {
        viewModelScope.launch {
            if (link.linkType == LinkType.ARCHIVE_LINK) {
                // we can also revert to the same folder from where it was originally archived, but this should be fine
                localLinksRepo.updateALink(
                    link = link.copy(
                        linkType = LinkType.SAVED_LINK, idOfLinkedFolder = null
                    ), updatedLinkTagsPair = null
                ).collectLatest {
                    it.onSuccess {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(message = Localization.Key.UnArchived.getLocalizedString() + it.getRemoteOnlyFailureMsg()))
                    }
                    it.pushSnackbarOnFailure()
                }
            } else {
                localLinksRepo.archiveALink(link.localId).collectLatest {
                    it.onSuccess {
                        Localization.Key.ArchivedTheLink.pushLocalizedSnackbar(append = it.getRemoteOnlyFailureMsg())
                    }.pushSnackbarOnFailure()
                }
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun updateLink(updatedLinkTagsPair: LinkTagsPair, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localLinksRepo.updateALink(
                link = updatedLinkTagsPair.link, updatedLinkTagsPair = updatedLinkTagsPair
            ).collectLatest {
                it.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun updateFolder(newFolderData: Folder, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localFoldersRepo.updateFolder(newFolderData).collectLatest {
                it.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun clearSelectedTags() {
        _selectedTags.clear()
    }

    fun deleteATag(tagId: Long, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localTagsRepo.deleteATag(tagId).collect()
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun renameATag(localId: Long, newName: String, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localTagsRepo.renameATag(
                localTagId = localId, newName = newName
            ).collect()
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun addANewLink(
        link: Link,
        selectedTags: List<Tag>?,
        linkSaveConfig: LinkSaveConfig,
        onCompletion: () -> Unit,
        pushSnackbarOnSuccess: Boolean = true
    ) {
        viewModelScope.launch {
            localLinksRepo.addANewLink(
                link = link, selectedTagIds = selectedTags?.map {
                    it.localId
                }, linkSaveConfig = linkSaveConfig
            ).collectLatest {
                it.onSuccess {
                    onCompletion()
                    if (pushSnackbarOnSuccess) {
                        Localization.Key.SavedTheLink.pushLocalizedSnackbar(append = it.getRemoteOnlyFailureMsg())
                    }
                    clearSelectedTags()
                }.onFailure {
                    onCompletion()
                    UIEvent.pushUIEvent(UIEvent.Type.ShowSnackbar(it))
                    clearSelectedTags()
                }
            }
        }
    }

    /*init {
        viewModelScope.launch {
            localTagsRepo.createATag(Tag(name = "SAVED_LINK")).collect {
                it.onSuccess { (tagId) ->
                    localLinksRepo.addMultipleLinks((0..500).map {
                        Link(
                            linkType = LinkType.SAVED_LINK,
                            title = "SAVED_LINK_$it",
                            url = "",
                            imgURL = "",
                            note = "",
                            idOfLinkedFolder = Constants.SAVED_LINKS_ID
                        )
                    }).run {
                        localTagsRepo.createLinkTags(this.map {
                            LinkTag(linkId = it, tagId = tagId)
                        })
                    }
                    linkoraLog("TESTING__SAVED_LINK")
                }
            }
            localTagsRepo.createATag(Tag(name = "IMPORTANT_LINK")).collect {
                it.onSuccess { (tagId) ->
                    localLinksRepo.addMultipleLinks((0..500).map {
                        Link(
                            linkType = LinkType.IMPORTANT_LINK,
                            title = "IMPORTANT_LINK_$it",
                            url = "",
                            imgURL = "",
                            note = "",
                            idOfLinkedFolder = Constants.IMPORTANT_LINKS_ID
                        )
                    }).run {
                        localTagsRepo.createLinkTags(this.map {
                            LinkTag(linkId = it, tagId = tagId)
                        })
                    }
                    linkoraLog("TESTING__IMPORTANT_LINK")
                }
            }
            localTagsRepo.createATag(Tag(name = "ARCHIVE_LINK")).collect {
                it.onSuccess { (tagId) ->
                    localLinksRepo.addMultipleLinks((0..500).map {
                        Link(
                            linkType = LinkType.ARCHIVE_LINK,
                            title = "ARCHIVE_LINK_$it",
                            url = "",
                            imgURL = "",
                            note = "",
                            idOfLinkedFolder = Constants.ARCHIVE_ID
                        )
                    }).run {
                        localTagsRepo.createLinkTags(this.map {
                            LinkTag(linkId = it, tagId = tagId)
                        })
                    }
                    linkoraLog("TESTING__ARCHIVE_LINK")
                }
            }
            localTagsRepo.createATag(Tag(name = "HISTORY_LINK")).collect {
                it.onSuccess { (tagId) ->
                    localLinksRepo.addMultipleLinks((0..500).map {
                        Link(
                            linkType = LinkType.HISTORY_LINK,
                            title = "HISTORY_LINK_$it",
                            url = "",
                            imgURL = "",
                            note = "",
                            idOfLinkedFolder = Constants.HISTORY_ID
                        )
                    }).run {
                        localTagsRepo.createLinkTags(this.map {
                            LinkTag(linkId = it, tagId = tagId)
                        })
                    }
                    linkoraLog("TESTING__HISTORY_LINK")
                }
            }


            (0..500).forEach { index ->
                localFoldersRepo.insertANewFolder(
                    folder = Folder(
                        name = "REGULAR_ROOT_FOLDER-$index",
                        note = "",
                        parentFolderId = null
                    ),
                    ignoreFolderAlreadyExistsException = true
                ).collect {
                    if (index == 0) {
                        it.onSuccess { (folderId) ->
                            localTagsRepo.createATag(Tag(name = "FOLDER_LINK")).collect {
                                it.onSuccess { (tagId) ->
                                    localLinksRepo.addMultipleLinks((0..500).map {
                                        Link(
                                            linkType = LinkType.FOLDER_LINK,
                                            title = "FOLDER_LINK_$it",
                                            url = "",
                                            imgURL = "",
                                            note = "",
                                            idOfLinkedFolder = folderId
                                        )
                                    }).run {
                                        localTagsRepo.createLinkTags(this.map {
                                            LinkTag(linkId = it, tagId = tagId)
                                        })
                                    }
                                }
                            }

                            (0..250).forEach { index ->
                                localFoldersRepo.insertANewFolder(
                                    folder = Folder(
                                        name = "CHILD_FOLDER-$index",
                                        note = "",
                                        parentFolderId = folderId
                                    ),
                                    ignoreFolderAlreadyExistsException = true
                                ).collect()
                            }
                        }
                    }
                }
                localFoldersRepo.insertANewFolder(
                    folder = Folder(
                        name = "ARCHIVED_ROOT_FOLDER-$index",
                        note = "",
                        parentFolderId = null,
                        isArchived = true
                    ),
                    ignoreFolderAlreadyExistsException = true
                ).collect()
            }
        }.invokeOnCompletion {
            linkoraLog("_DONE__DONE__DONE__DONE__DONE__DONE_")
        }
    }*/
}