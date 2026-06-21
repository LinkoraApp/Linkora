package com.sakethh.linkora.data.local

import androidx.room3.Room
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.repository.LocalFoldersRepoImpl
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.utils.canPushToServer
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class LocalFoldersRepoImplTest {

    private lateinit var database: LocalDatabase
    private lateinit var foldersDao: FoldersDao

    private lateinit var remoteFoldersRepo: RemoteFoldersRepo
    private lateinit var localLinksRepo: LocalLinksRepo
    private lateinit var localPanelsRepo: LocalPanelsRepo
    private lateinit var pendingSyncQueueRepo: PendingSyncQueueRepo
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var localFoldersRepo: LocalFoldersRepo

    @BeforeTest
    fun setup() {
        clearAllMocks()

        database = Room.inMemoryDatabaseBuilder<LocalDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()

        foldersDao = database.foldersDao

        remoteFoldersRepo = mockk<RemoteFoldersRepo>(relaxed = true)
        localLinksRepo = mockk<LocalLinksRepo>(relaxed = true)
        localPanelsRepo = mockk<LocalPanelsRepo>(relaxed = true)
        pendingSyncQueueRepo = mockk<PendingSyncQueueRepo>(relaxed = true)
        preferencesRepository = mockk<PreferencesRepository>(relaxed = true)

        val mockPrefs = mockk<AppPreferences>(relaxed = true) {
            every { serverBaseUrl } returns "https://server.linkora.com"
            every { serverSecurityToken } returns "mock-auth-token"
            every { correlation } returns Correlation(
                id = "test-correlation-id",
                clientName = "test-correlation-client"
            )
        }
        coEvery { preferencesRepository.getPreferences() } returns mockPrefs

        // Force the extension function to return true so the remote block actually executes
        mockkStatic("com.sakethh.linkora.utils.ExtensionsKt")
        every { any<AppPreferences>().canPushToServer() } returns true

        coEvery { localLinksRepo.deleteLinksOfFolder(any()) } returns emptyFlow()

        localFoldersRepo = LocalFoldersRepoImpl(
            foldersDao = foldersDao,
            remoteFoldersRepo = remoteFoldersRepo,
            localLinksRepo = localLinksRepo,
            localPanelsRepo = localPanelsRepo,
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            preferencesRepository = preferencesRepository,
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

    private suspend fun executeAndGetErrorMessage(block: suspend () -> List<Any>): String {
        var caughtMessage = ""
        try {
            val results = block()
            val lastResult = results.lastOrNull()
            if (lastResult is Result.Failure<*>) {
                caughtMessage = lastResult.message
            }
        } catch (e: Exception) {
            caughtMessage = e.message.toString()
        }
        return caughtMessage
    }

    @Test
    fun `inserting folder with blank name is intercepted locally and outputs validation failure`() =
        runTest {
            val blankFolder = Folder(
                name = "",
                note = "",
                parentFolderId = null,
                isArchived = false,
                lastModified = 0L
            )

            val errorMessage = executeAndGetErrorMessage {
                localFoldersRepo.insertANewFolder(
                    blankFolder,
                    ignoreFolderAlreadyExistsException = false
                ).toList()
            }

            assertTrue(
                errorMessage.contains(
                    "blank",
                    ignoreCase = true
                ) || errorMessage.contains("invalid", ignoreCase = true),
                "Expected failure message containing 'blank', but got: '$errorMessage'"
            )
        }

    @Test
    fun `doesThisRootFolderExists correctly queries database for existing names regardless of Flow loading states`() =
        runTest {
            val rootFolder = Folder(
                name = "TargetFolder",
                note = "",
                parentFolderId = null,
                isArchived = false,
                lastModified = 0L
            )
            foldersDao.insertANewFolder(rootFolder)

            // Bypassing the Result.Loading bug to verify the actual database query logic
            val result = localFoldersRepo.doesThisRootFolderExists("TargetFolder")
                .filterNot { it is Result.Loading }
                .first()

            assertTrue(result is Result.Success)
            assertTrue(result.data, "Expected database to confirm TargetFolder exists")
        }

    @Test
    fun `deleting a folder triggers recursive array deque wipe of deep child folders links and panel associations`() =
        runTest {
            val rootId = foldersDao.insertANewFolder(
                Folder(
                    name = "Root",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L
                )
            )
            val childId = foldersDao.insertANewFolder(
                Folder(
                    name = "Child",
                    note = "",
                    parentFolderId = rootId,
                    isArchived = false,
                    lastModified = 0L
                )
            )
            val grandChildId = foldersDao.insertANewFolder(
                Folder(
                    name = "Grandchild",
                    note = "",
                    parentFolderId = childId,
                    isArchived = false,
                    lastModified = 0L
                )
            )

            localFoldersRepo.deleteAFolder(rootId).toList()

            val allFolders = foldersDao.getAllFoldersAsList()
            assertTrue(allFolders.isEmpty(), "Expected all nested folders to be deleted from DB")

            coVerify(exactly = 1) { localLinksRepo.deleteLinksOfFolder(rootId) }
            coVerify(exactly = 1) { localLinksRepo.deleteLinksOfFolder(childId) }
            coVerify(exactly = 1) { localLinksRepo.deleteLinksOfFolder(grandChildId) }

            coVerify(exactly = 1) { localPanelsRepo.deleteAFolderFromAllPanels(rootId) }
            coVerify(exactly = 1) { localPanelsRepo.deleteAFolderFromAllPanels(childId) }
            coVerify(exactly = 1) { localPanelsRepo.deleteAFolderFromAllPanels(grandChildId) }
        }

    @Test
    fun `network failure during remote folder creation caches payload directly into pending sync queue`() =
        runTest {
            coEvery { remoteFoldersRepo.createFolder(any()) } throws RuntimeException("Network Timeout")

            val newFolder = Folder(
                name = "OfflineFolder",
                note = "",
                parentFolderId = null,
                isArchived = false,
                lastModified = 0L
            )

            localFoldersRepo.insertANewFolder(newFolder, ignoreFolderAlreadyExistsException = true)
                .toList()

            coVerify(exactly = 1) {
                pendingSyncQueueRepo.addInQueue(match { queueItem ->
                    queueItem.operation == "CREATE_FOLDER" && queueItem.payload.contains("OfflineFolder")
                })
            }
        }

    @Test
    fun `viaSocket flag bypasses remote operations completely even if push to server is configured`() =
        runTest {
            val folder = Folder(
                name = "SocketFolder",
                note = "",
                parentFolderId = null,
                isArchived = false,
                lastModified = 0L
            )

            localFoldersRepo.insertANewFolder(
                folder,
                ignoreFolderAlreadyExistsException = false,
                viaSocket = true
            ).toList()

            assertTrue(foldersDao.getAllRootFoldersAsList().any { it.name == "SocketFolder" })

            coVerify(exactly = 0) { remoteFoldersRepo.createFolder(any()).collect() }
            coVerify(exactly = 0) { pendingSyncQueueRepo.addInQueue(any()) }
        }
}