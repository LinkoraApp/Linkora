package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.FlatChildFolderData
import com.sakethh.linkora.domain.model.FlatSearchResult
import kotlinx.coroutines.flow.Flow

interface LocalDatabaseUtilsRepo {
    suspend fun resetDatabase()

    suspend fun getFoldersRowCount(): Long

    fun getChildFolderData(
        parentFolderId: Long,
        linkType: LinkType,
        sortOption: String,
        pageSize: Int,
        startIndex: Long
    ): Flow<Result<List<FlatChildFolderData>>>

    fun search(
        query: String,
        sortOption: String,
        pageSize: Int,
        startIndex: Long,
        shouldShowTags: Boolean,
        shouldShowFolders: Boolean,
        includeArchivedFolders: Boolean,
        includeRegularFolders: Boolean,
        shouldShowLinks: Boolean,
        isLinkTypeFilterActive: Boolean,
        activeLinkTypeFilters: List<String>,
        assignPath: Boolean
    ): Flow<Result<List<FlatSearchResult>>>
}