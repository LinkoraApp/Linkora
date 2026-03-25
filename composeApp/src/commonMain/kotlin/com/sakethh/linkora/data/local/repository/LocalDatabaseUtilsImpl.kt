package com.sakethh.linkora.data.local.repository

import androidx.room3.executeSQL
import androidx.room3.immediateTransaction
import androidx.room3.useWriterConnection
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.mapToResultFlow
import com.sakethh.linkora.domain.model.FlatChildFolderData
import com.sakethh.linkora.domain.model.FlatSearchResult
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.repository.local.LocalDatabaseUtilsRepo
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.Sorting
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class LocalDatabaseUtilsImpl(private val localDatabase: LocalDatabase) : LocalDatabaseUtilsRepo {

    override suspend fun resetDatabase() {
        localDatabase.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                executeSQL("DELETE FROM `link_tags`")
                executeSQL("DELETE FROM `panel_folder`")
                executeSQL("DELETE FROM `links`")
                executeSQL("DELETE FROM `tags`")
                executeSQL("DELETE FROM `panel`")
                executeSQL("DELETE FROM `folders`")
                executeSQL("DELETE FROM `localized_strings`")
                executeSQL("DELETE FROM `localized_languages`")
                executeSQL("DELETE FROM `pending_sync_queue`")
                executeSQL("DELETE FROM `snapshot`")

                executeSQL("DELETE FROM sqlite_sequence WHERE name IN ('links', 'folders', 'localized_strings', 'panel', 'panel_folder', 'pending_sync_queue', 'snapshot', 'tags')")
            }

            transactor.executeSQL("PRAGMA wal_checkpoint(TRUNCATE)")
        }
    }

    override suspend fun getFoldersRowCount(): Long {
        return localDatabase.localDatabaseUtilsDao.getFoldersRowCount()
    }

    override fun getChildFolderData(
        parentFolderId: Long,
        linkType: LinkType,
        sortOption: String,
        pageSize: Int,
        lastTypeOrder: Int?,
        lastSortStr: String?,
        lastId: Long?
    ): Flow<Result<List<FlatChildFolderData>>> {

        val safeId = if (lastId == Constants.EMPTY_LAST_SEEN_ID) null else lastId

        return when (sortOption) {
            Sorting.A_TO_Z, Sorting.Z_TO_A -> localDatabase.localDatabaseUtilsDao.getChildDataSortedByName(
                pageSize = pageSize,
                isAscending = sortOption == Sorting.A_TO_Z,
                parentFolderId = parentFolderId,
                linkType = linkType,
                lastTypeOrder = lastTypeOrder,
                lastSortStr = lastSortStr,
                lastUniqueId = safeId
            )

            else -> localDatabase.localDatabaseUtilsDao.getChildDataSortedById(
                pageSize = pageSize,
                isAscending = sortOption == Sorting.OLD_TO_NEW,
                parentFolderId = parentFolderId,
                linkType = linkType,
                lastTypeOrder = lastTypeOrder,
                lastSortId = safeId,
            )
        }.mapToResultFlow()
    }


    override fun search(
        query: String,
        sortOption: String,
        pageSize: Int,
        shouldShowTags: Boolean,
        shouldShowFolders: Boolean,
        includeArchivedFolders: Boolean,
        includeRegularFolders: Boolean,
        shouldShowLinks: Boolean,
        isLinkTypeFilterActive: Boolean,
        activeLinkTypeFilters: List<String>,
        assignPath: Boolean,
        lastTypeOrder: Int,
        lastSortStr: String,
        lastSortNum: Long,
        lastId: Long
    ): Flow<Result<List<FlatSearchResult>>> {
        return localDatabase.localDatabaseUtilsDao.search(
            query,
            sortOption,
            pageSize,
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
        ).transform { searchResult ->
            if (!assignPath) {
                emit(searchResult)
                return@transform
            }
            coroutineScope {
                searchResult.map { searchResultItem ->
                    async {
                        searchResultItem.itemType.takeIf { searchResultItemType ->
                            searchResultItemType != Constants.TAG
                        }?.let { searchResultItemType ->
                            searchResultItem.apply {
                                this.path = mutableListOf<Folder>().apply {
                                    if (searchResultItemType == Constants.LINK && searchResultItem.linkType != LinkType.FOLDER_LINK) return@apply

                                    val parentFolderId =
                                        if (searchResultItemType == Constants.LINK) searchResultItem.linkIdOfLinkedFolder
                                        else searchResultItem.folderParentId
                                    if (parentFolderId != null && parentFolderId > 0) {
                                        var ancestorFolder =
                                            localDatabase.foldersDao.getThisFolderData(
                                                parentFolderId
                                            )
                                        add(ancestorFolder)

                                        while (ancestorFolder.parentFolderId != null) {
                                            ancestorFolder =
                                                localDatabase.foldersDao.getThisFolderData(
                                                    ancestorFolder.parentFolderId
                                                )
                                            add(ancestorFolder)
                                        }
                                    }
                                }.asReversed()
                            }
                        } ?: searchResultItem
                    }
                }.awaitAll().run {
                    emit(this)
                }
            }
        }.mapToResultFlow()
    }

}