package com.sakethh.linkora.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.repository.LocalDatabaseUtilsImpl
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.Sorting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalDatabaseUtilsImplTest {

    private lateinit var database: LocalDatabase
    private lateinit var localDatabaseUtilsRepo: LocalDatabaseUtilsImpl

    @BeforeTest
    fun setup() {
        database = Room.inMemoryDatabaseBuilder<LocalDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()

        localDatabaseUtilsRepo = LocalDatabaseUtilsImpl(database)
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `resetDatabase executes raw SQL to thoroughly truncate all active tables and sequences`() =
        runTest {
            database.foldersDao.insertANewFolder(
                Folder(
                    name = "TargetFolder",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L
                )
            )
            database.linksDao.addANewLink(
                Link(
                    url = "https://delete.me",
                    title = "Del",
                    linkType = LinkType.SAVED_LINK,
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = ""
                )
            )

            assertTrue(database.foldersDao.getAllFoldersAsList().isNotEmpty())
            assertTrue(database.linksDao.getAllLinks().isNotEmpty())

            localDatabaseUtilsRepo.resetDatabase()

            assertTrue(database.foldersDao.getAllFoldersAsList().isEmpty())
            assertTrue(database.linksDao.getAllLinks().isEmpty())
        }

    @Test
    fun `getFoldersRowCount natively counts exactly the number of folders in the database`() =
        runTest {
            assertEquals(0, localDatabaseUtilsRepo.getFoldersRowCount())

            database.foldersDao.insertANewFolder(
                Folder(
                    name = "F1",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L
                )
            )
            database.foldersDao.insertANewFolder(
                Folder(
                    name = "F2",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L
                )
            )
            database.foldersDao.insertANewFolder(
                Folder(
                    name = "F3",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L
                )
            )

            assertEquals(3, localDatabaseUtilsRepo.getFoldersRowCount())
        }

    @Test
    fun `getChildFolderData correctly translates empty last seen id constant to null to avoid SQL constraint violations`() =
        runTest {
            val result = localDatabaseUtilsRepo.getChildFolderData(
                parentFolderId = 1L,
                linkType = LinkType.FOLDER_LINK,
                sortOption = Sorting.A_TO_Z,
                pageSize = 10,
                lastTypeOrder = 0,
                lastSortStr = "A",
                lastId = Constants.EMPTY_LAST_SEEN_ID
            ).filterNot { it is Result.Loading }.first()

            assertTrue(result is Result.Success)
            assertTrue(result.data.isEmpty())
        }

    @Test
    fun `search with assignPath dynamically builds hierarchical ancestor tree and strictly reverses it for deep structures`() =
        runTest {
            val rootId = database.foldersDao.insertANewFolder(
                Folder(
                    name = "Root",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L
                )
            )
            val childId = database.foldersDao.insertANewFolder(
                Folder(
                    name = "Child",
                    note = "",
                    parentFolderId = rootId,
                    isArchived = false,
                    lastModified = 0L
                )
            )
            val grandChildId = database.foldersDao.insertANewFolder(
                Folder(
                    name = "GrandChild",
                    note = "TargetNote",
                    parentFolderId = childId,
                    isArchived = false,
                    lastModified = 0L
                )
            )

            val result = localDatabaseUtilsRepo.search(
                query = "TargetNote",
                sortOption = Sorting.NEW_TO_OLD,
                pageSize = 10,
                shouldShowTags = true,
                shouldShowFolders = true,
                includeArchivedFolders = false,
                includeRegularFolders = true,
                shouldShowLinks = true,
                isLinkTypeFilterActive = false,
                activeLinkTypeFilters = emptyList(),
                assignPath = true,
                lastTypeOrder = 0,
                lastSortStr = "",
                lastSortNum = 0L,
                lastId = 0L
            ).filterNot { it is Result.Loading }.first()

            val searchData = (result as Result.Success).data
            val targetItem = searchData.find {
                it.folderParentId == childId || (it.itemType == Constants.FOLDER && it.folderLocalId == grandChildId)
            }

            assertNotNull(
                targetItem,
                "Search result missing. Ensure FTS tables map synchronously in in-memory SQLite."
            )

            val path = targetItem.path
            assertNotNull(path)
            assertEquals(2, path.size, "Expected exactly 2 ancestors in the path")

            assertEquals(
                "Root",
                path[0].name,
                "Path index 0 must be the top-most root folder due to .asReversed()"
            )
            assertEquals("Child", path[1].name, "Path index 1 must be the immediate parent")
        }

    @Test
    fun `search without assignPath immediately emits raw results bypassing recursive folder path resolution`() =
        runTest {
            val rootId = database.foldersDao.insertANewFolder(
                Folder(
                    name = "Root",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L
                )
            )
            val targetId = database.foldersDao.insertANewFolder(
                Folder(
                    name = "FastSearchTarget",
                    note = "",
                    parentFolderId = rootId,
                    isArchived = false,
                    lastModified = 0L
                )
            )

            val result = localDatabaseUtilsRepo.search(
                query = "FastSearchTarget",
                sortOption = Sorting.NEW_TO_OLD,
                pageSize = 10,
                shouldShowTags = true,
                shouldShowFolders = true,
                includeArchivedFolders = false,
                includeRegularFolders = true,
                shouldShowLinks = true,
                isLinkTypeFilterActive = false,
                activeLinkTypeFilters = emptyList(),
                assignPath = false,
                lastTypeOrder = 0,
                lastSortStr = "",
                lastSortNum = 0L,
                lastId = 0L
            ).filterNot { it is Result.Loading }.first()

            val searchData = (result as Result.Success).data
            val targetItem = searchData.find {
                it.folderParentId == rootId || (it.itemType == Constants.FOLDER && it.folderLocalId == targetId)
            }

            assertNotNull(targetItem)
            assertTrue(
                targetItem.path.isNullOrEmpty(),
                "Path should remain empty because assignPath was disabled"
            )
        }

    @Test
    fun `search with assignPath specifically aborts path resolution early for non folder links`() =
        runTest {
            database.linksDao.addANewLink(
                Link(
                    url = "https://xyz.com",
                    title = "SavedLinkTarget",
                    linkType = LinkType.SAVED_LINK,
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = ""
                )
            )

            val result = localDatabaseUtilsRepo.search(
                query = "SavedLinkTarget",
                sortOption = Sorting.NEW_TO_OLD,
                pageSize = 10,
                shouldShowTags = true,
                shouldShowFolders = true,
                includeArchivedFolders = false,
                includeRegularFolders = true,
                shouldShowLinks = true,
                isLinkTypeFilterActive = false,
                activeLinkTypeFilters = emptyList(),
                assignPath = true,
                lastTypeOrder = 0,
                lastSortStr = "",
                lastSortNum = 0L,
                lastId = 0L
            ).filterNot { it is Result.Loading }.first()

            val searchData = (result as Result.Success).data
            val targetItem = searchData.find {
                it.itemType == Constants.LINK && it.linkType != LinkType.FOLDER_LINK
            }

            assertNotNull(targetItem)
            assertTrue(
                targetItem.path.isNullOrEmpty(),
                "Path should be empty because SAVED_LINKs execute the 'return@apply' early exit condition"
            )
        }
}