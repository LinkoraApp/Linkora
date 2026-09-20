package com.sakethh.linkora.ui.screens.collections

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Stable
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.LastSeenId
import com.sakethh.linkora.ui.LastSeenString
import com.sakethh.linkora.ui.domain.PaginationState
import kotlinx.coroutines.flow.StateFlow

@Stable
data class CollectionScreenParams(
    val collectionPagerState: PagerState,
    val rootRegularFolders: StateFlow<PaginationState<Map<Pair<LastSeenId, LastSeenString>, List<Folder>>>>,
    val allTags: StateFlow<PaginationState<Map<Pair<LastSeenId, LastSeenString>, List<Tag>>>>,
    var currentCollectionSource: String,
    val performAction: (CollectionsScreenAction) -> Unit,
    val onRetrieveNextRegularRootFolderPage: () -> Unit,
    val onRetrieveNextTagsPage: () -> Unit,
    val onRegularRootFolderFirstVisibleItemIndexChange: (Long) -> Unit,
    val onTagsFirstVisibleItemIndexChange: (Long) -> Unit,
)
