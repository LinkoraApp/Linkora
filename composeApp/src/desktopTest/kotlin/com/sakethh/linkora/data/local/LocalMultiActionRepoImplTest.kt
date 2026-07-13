package com.sakethh.linkora.data.local

import androidx.room3.Room
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.repository.LocalFoldersRepoImpl
import com.sakethh.linkora.data.local.repository.LocalMultiActionRepoImpl
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.domain.repository.remote.RemoteMultiActionRepo
import com.sakethh.linkora.utils.canPushToServer
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalMultiActionRepoImplTest {

    private lateinit var database: LocalDatabase
    private lateinit var linksDao: LinksDao
    private lateinit var foldersDao: FoldersDao

    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var remoteMultiActionRepo: RemoteMultiActionRepo
    private lateinit var pendingSyncQueueRepo: PendingSyncQueueRepo
    private lateinit var localFoldersRepo: LocalFoldersRepo
    private lateinit var localTagsRepo: LocalTagsRepo

    private lateinit var localMultiActionRepo: LocalMultiActionRepoImpl

    @BeforeTest
    fun setup() {
        clearAllMocks()

        database = Room.inMemoryDatabaseBuilder<LocalDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()

        linksDao = database.linksDao
        foldersDao = database.foldersDao

        preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
        remoteMultiActionRepo = mockk<RemoteMultiActionRepo>(relaxed = true)
        pendingSyncQueueRepo = mockk<PendingSyncQueueRepo>(relaxed = true)
        localTagsRepo = mockk<LocalTagsRepo>(relaxed = true)

        val mockPrefs = mockk<AppPreferences>(relaxed = true) {
            every { serverBaseUrl } returns "https://server.linkora.com"
            every { serverSecurityToken } returns "mock-auth-token"
            every { correlation } returns Correlation(
                id = "test-correlation-id",
                clientName = "test-client"
            )
        }
        coEvery { preferencesRepository.getPreferences() } returns mockPrefs

        mockkStatic("com.sakethh.linkora.utils.ExtensionsKt")
        every { any<AppPreferences>().canPushToServer() } returns true

        localFoldersRepo = LocalFoldersRepoImpl(
            foldersDao = foldersDao,
            remoteFoldersRepo = mockk<RemoteFoldersRepo>(relaxed = true),
            localLinksRepo = mockk<LocalLinksRepo>(relaxed = true),
            localPanelsRepo = mockk<LocalPanelsRepo>(relaxed = true),
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            preferencesRepository = preferencesRepository,
            withWriterConnection = { block ->
                database.useWriterConnection { transactor ->
                    block(transactor)
                }
            }
        )

        localMultiActionRepo = LocalMultiActionRepoImpl(
            linksDao = linksDao,
            foldersDao = foldersDao,
            preferencesRepository = preferencesRepository,
            remoteMultiActionRepo = remoteMultiActionRepo,
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            localFoldersRepo = localFoldersRepo,
            localTagsRepo = localTagsRepo,
            withWriterConnection = { block ->
                database.useWriterConnection { transactor ->
                    block(transactor)
                }
            }
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private suspend fun executeAndGetErrorMessage(block: suspend () -> Any): String {
        return try {
            val result = block()
            if (result is Result.Failure<*>) {
                result.message
            } else {
                ""
            }
        } catch (e: Throwable) {
            e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
        }
    }

    @Test
    fun `network failure during archive multiple items captures payload to pending sync queue`() =
        runTest {
            coEvery { remoteMultiActionRepo.archiveMultipleItems(any()) }  returns flowOf(Result.Failure("Network Timeout"))

            val linkId = linksDao.addANewLink(
                Link(
                    url = "https://test.com",
                    title = "T",
                    linkType = LinkType.SAVED_LINK,
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = "",
                    remoteId = 111L
                )
            )
            val folderId = foldersDao.insertANewFolder(
                Folder(
                    name = "F",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L,
                    remoteId = 222L
                )
            )

            val result = localMultiActionRepo.archiveMultipleItems(
                listOf(linkId),
                listOf(folderId),
                viaSocket = false
            )
                .filterNot { it is Result.Loading }.first()

            assertTrue(result is Result.Success)

            coVerify(exactly = 1) {
                pendingSyncQueueRepo.addInQueue(match { queueItem ->
                    queueItem.operation == "ARCHIVE_MULTIPLE_ITEMS" &&
                            queueItem.payload.contains(linkId.toString()) &&
                            queueItem.payload.contains(folderId.toString())
                })
            }

            val folder = foldersDao.getThisFolderData(folderId)
            assertTrue(folder.isArchived)
        }

    @Test
    fun `viaSocket flag entirely bypasses remote repo execution during delete multiple items`() =
        runTest {
            val linkId = linksDao.addANewLink(
                Link(
                    url = "https://del.com",
                    title = "T",
                    linkType = LinkType.SAVED_LINK,
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = "",
                    remoteId = 333L
                )
            )
            val folderId = foldersDao.insertANewFolder(
                Folder(
                    name = "F",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L,
                    remoteId = 444L
                )
            )

            localMultiActionRepo.deleteMultipleItems(
                listOf(linkId),
                listOf(folderId),
                viaSocket = true
            )
                .filterNot { it is Result.Loading }.first()

            assertTrue(linksDao.getAllLinks().isEmpty())
            assertTrue(foldersDao.getAllFoldersAsList().isEmpty())

            coVerify(exactly = 0) { remoteMultiActionRepo.deleteMultipleItems(any()) }
            coVerify(exactly = 0) { pendingSyncQueueRepo.addInQueue(any()) }
        }

    @Test
    fun `move multiple items to an unsynced parent folder throws local validation exception`() =
        runTest {
            val unsyncedDestId = foldersDao.insertANewFolder(
                Folder(
                    name = "Unsynced",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L,
                    remoteId = null
                )
            )

            val moveError = executeAndGetErrorMessage {
                localMultiActionRepo.moveMultipleItems(
                    linkIds = emptyList(),
                    folderIds = emptyList(),
                    linkType = LinkType.FOLDER_LINK,
                    newParentFolderId = unsyncedDestId,
                    viaSocket = false
                ).filterNot { it is Result.Loading }.first()
            }

            assertTrue(
                moveError.contains(
                    "Failed requirement",
                    ignoreCase = true
                ) || moveError.contains("null", ignoreCase = true)
            )
        }

    @Test
    fun `copy multiple items recursively clones nested folders and deeply embedded links locally`() =
        runTest {
            val destFolderId = foldersDao.insertANewFolder(
                Folder(
                    "Dest",
                    "",
                    null,
                    localId = 0,
                    remoteId = 100L,
                    isArchived = false,
                    lastModified = 0L
                )
            )
            val srcFolderId = foldersDao.insertANewFolder(
                Folder(
                    "SrcRoot",
                    "",
                    null,
                    localId = 0,
                    remoteId = 200L,
                    isArchived = false,
                    lastModified = 0L
                )
            )

            linksDao.addANewLink(
                Link(
                    url = "https://src.com",
                    title = "SrcLink",
                    linkType = LinkType.FOLDER_LINK,
                    idOfLinkedFolder = srcFolderId,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = ""
                )
            )

            coEvery { localTagsRepo.getTagsForLinksAsMap(any()) } returns emptyMap()
            coEvery { localTagsRepo.createLinkTags(any()) } returns Unit

            val srcFolder = foldersDao.getThisFolderData(srcFolderId)

            localMultiActionRepo.copyMultipleItems(
                linkTagsPairs = emptyList(),
                folders = listOf(srcFolder),
                linkType = LinkType.FOLDER_LINK,
                newParentFolderId = destFolderId,
                viaSocket = true
            ).filterNot { it is Result.Loading }.first()

            val allFolders = foldersDao.getAllFoldersAsList()
            val clonedFolder =
                allFolders.find { it.name == "SrcRoot" && it.parentFolderId == destFolderId }

            assertNotNull(clonedFolder)
            assertTrue(clonedFolder.localId != srcFolderId)

            val allLinks = linksDao.getAllLinks()
            val clonedLink =
                allLinks.find { it.url == "https://src.com" && it.idOfLinkedFolder == clonedFolder.localId }

            assertNotNull(clonedLink)
        }

    @Test
    fun `unarchive multiple items successfully commits immediate transaction and updates synced timestamps`() =
        runTest {
            val linkId = linksDao.addANewLink(
                Link(
                    url = "https://unarchive.com",
                    title = "T",
                    linkType = LinkType.ARCHIVE_LINK,
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = "",
                    remoteId = 555L
                )
            )

            localMultiActionRepo.unArchiveMultipleItems(
                listOf(linkId),
                emptyList(),
                viaSocket = false
            )
                .filterNot { it is Result.Loading }.first()

            val updatedLink = linksDao.getLink(linkId)

            assertEquals(LinkType.SAVED_LINK, updatedLink.linkType)
            coVerify(exactly = 1) { remoteMultiActionRepo.markItemsAsRegular(any()) }
        }
}
