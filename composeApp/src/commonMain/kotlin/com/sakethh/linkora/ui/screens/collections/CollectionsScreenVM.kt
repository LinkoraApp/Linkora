package com.sakethh.linkora.ui.screens.collections

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.ui.LastSeenId
import com.sakethh.linkora.ui.LastSeenString
import com.sakethh.linkora.ui.Paginator
import com.sakethh.linkora.ui.domain.AddANewLinkDialogBoxAction
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushLocalizedSnackbar
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.asStateInWhileSubscribed
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.intPreferencesKey
import com.sakethh.linkora.utils.onError
import com.sakethh.linkora.utils.onPagesFinished
import com.sakethh.linkora.utils.onRetrieved
import com.sakethh.linkora.utils.onRetrieving
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import com.sakethh.linkora.utils.replaceFirstPlaceHolderWith
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CollectionsScreenVM(
    private val localFoldersRepo: LocalFoldersRepo,
    private val localLinksRepo: LocalLinksRepo,
    private val localTagsRepo: LocalTagsRepo,
    private val preferencesRepo: PreferencesRepository
) : ViewModel() {
    val sortingType get() = preferencesRepo.getPreferences().selectedSortingType
    val shuffleLinks get() = preferencesRepo.getPreferences().forceShuffleLinks
    val preferencesAsFlow = preferencesRepo.preferencesAsFlow
    var currentCollectionSource by mutableStateOf(if (preferencesRepo.getPreferences().selectedCollectionSourceId == 0) Localization.Key.Folders.getLocalizedString() else Localization.Key.Tags.getLocalizedString())
    val collectionPagerState = PagerState(
        currentPage = preferencesRepo.getPreferences().selectedCollectionSourceId,
        pageCount = { 2 })

    init {
        viewModelScope.launch {
            snapshotFlow {
                collectionPagerState.currentPage
            }.distinctUntilChanged().debounce(150L).collectLatest { currentPage ->
                currentCollectionSource =
                    if (currentPage == 0) Localization.Key.Folders.getLocalizedString() else Localization.Key.Tags.getLocalizedString()

                preferencesRepo.changePreferenceValue(
                    preferenceKey = intPreferencesKey(AppPreferences.COLLECTION_SOURCE_ID.key),
                    newValue = currentPage
                )
            }
        }
    }

    companion object {
        val selectedLinkTagPairsViaLongClick = mutableStateListOf<LinkTagsPair>()
        val selectedFoldersViaLongClick = mutableStateListOf<Folder>()
        val isSelectionEnabled = mutableStateOf(false)

        fun clearAllSelections() {
            isSelectionEnabled.value = false
            selectedLinkTagPairsViaLongClick.clear()
            selectedFoldersViaLongClick.clear()
        }
        var inCollectionsListPane by mutableStateOf(false)
    }


    var foldersSearchQuery by mutableStateOf("")
    private val _foldersSearchQueryResult = MutableStateFlow(emptyList<Folder>())
    val foldersSearchQueryResult = _foldersSearchQueryResult.asStateFlow()

    init {
        viewModelScope.launch {
            combine(snapshotFlow {
                foldersSearchQuery
            }, preferencesAsFlow.map {
                it.selectedSortingType
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

    fun performAction(collectionsScreenAction: CollectionsScreenAction) =
        when (collectionsScreenAction) {
            is CollectionsScreenAction.AddANewLink -> addANewLink(
                link = collectionsScreenAction.link,
                selectedTags = collectionsScreenAction.selectedTags,
                linkSaveConfig = collectionsScreenAction.linkSaveConfig,
                onCompletion = collectionsScreenAction.onCompletion,
                pushSnackbarOnSuccess = collectionsScreenAction.pushSnackbarOnSuccess
            )
        }

    private val _rootRegularFolders = MutableStateFlow(
        PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = emptyMap<Pair<LastSeenId, LastSeenString>, List<Folder>>()
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


    private val _allTags = MutableStateFlow(
        value = PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = emptyMap<Pair<LastSeenId, LastSeenString>, List<Tag>>()
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
        onRetrieve = { lastSeenId, lastSeenString ->
            localFoldersRepo.getRootFolders(
                sortingType,
                isArchived = false,
                pageSize = Constants.PAGE_SIZE,
                lastSeenId = lastSeenId,
                lastSeenName = lastSeenString
            )
        },
        onRetrieved = { currentKey, retrievedData ->
            _rootRegularFolders.onRetrieved(
                currentKey = currentKey,
                data = retrievedData,
                shouldShuffle = shuffleLinks,
                idSelector = { it.localId },
                stringSelector = { it.name })
        },
        onError = _rootRegularFolders::onError,
        onRetrieving = _rootRegularFolders::onRetrieving,
        onPagesFinished = _rootRegularFolders::onPagesFinished
    )


    private val tagsPaginator = Paginator(
        coroutineScope = viewModelScope,
        onRetrieve = { lastSeenId, lastSeenString ->
            localTagsRepo.getTags(
                sortOption = sortingType,
                pageSize = Constants.PAGE_SIZE,
                lastSeenId = lastSeenId,
                lastSeenName = lastSeenString
            )
        },
        onRetrieved = { currentKey, retrievedData ->
            _allTags.onRetrieved(
                currentKey = currentKey,
                data = retrievedData,
                shouldShuffle = shuffleLinks,
                idSelector = { it.localId },
                stringSelector = { it.name })
        },
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

    private val appPreferencesCombined = preferencesAsFlow.map {
        Pair(it.forceShuffleLinks, it.selectedSortingType)
    }

    init {

        viewModelScope.launch {
            tagsPaginator.retrieveNextBatch()
        }

        suspend fun loadRootFolders() {
            _rootRegularFolders.emit(PaginationState.retrievingOnEmpty())
            regularRootFoldersPaginator.retrieveNextBatch()
        }

        viewModelScope.launch {
            loadRootFolders()
        }


        // ==== RESET THE STATE OF PAGINATORS (+HANDLE DESKTOP COLLECTION-DETAIL-PANE) =====

        viewModelScope.launch {
            var lastSortingType = sortingType
            appPreferencesCombined.collectLatest { (shuffleLinks, sortingType) ->
                val isSortingTypeChanged = if (sortingType == lastSortingType) {
                    false
                } else {
                    lastSortingType = sortingType
                    true
                }

                if (isSortingTypeChanged) {
                    tagsPaginator.cancelAndReset()
                    regularRootFoldersPaginator.cancelAndReset()

                    loadRootFolders()
                    tagsPaginator.retrieveNextBatch()
                }
            }
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
        refreshLinkType: RefreshLinkType, link: Link, onCompletion: () -> Unit
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
}