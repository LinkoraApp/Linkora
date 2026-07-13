package com.sakethh.linkora.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.PanelsDao
import com.sakethh.linkora.data.local.repository.LocalPanelsRepoImpl
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemotePanelsRepo
import com.sakethh.linkora.utils.canPushToServer
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalPanelsRepoImplTest {

    private lateinit var database: LocalDatabase
    private lateinit var panelsDao: PanelsDao
    private lateinit var foldersDao: FoldersDao

    private lateinit var remotePanelsRepo: RemotePanelsRepo
    private lateinit var pendingSyncQueueRepo: PendingSyncQueueRepo
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var localPanelsRepo: LocalPanelsRepo

    @BeforeTest
    fun setup() {
        clearAllMocks()

        database = Room.inMemoryDatabaseBuilder<LocalDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()

        panelsDao = database.panelsDao
        foldersDao = database.foldersDao

        remotePanelsRepo = mockk<RemotePanelsRepo>(relaxed = true)
        pendingSyncQueueRepo = mockk<PendingSyncQueueRepo>(relaxed = true)
        preferencesRepository = mockk<PreferencesRepository>(relaxed = true)

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

        localPanelsRepo = LocalPanelsRepoImpl(
            panelsDao = panelsDao,
            remotePanelsRepo = remotePanelsRepo,
            foldersDao = foldersDao,
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            preferencesRepository = preferencesRepository
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private suspend fun executeAndGetErrorMessage(block: suspend () -> List<Any>): String {
        return try {
            val results = block()
            val lastResult = results.lastOrNull()
            if (lastResult is Result.Failure<*>) {
                lastResult.message
            } else {
                ""
            }
        } catch (e: Throwable) {
            e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
        }
    }

    @Test
    fun `adding a new panel locally successfully executes remote call and updates local DB`() =
        runTest {
            val panel = Panel(localId = 0, panelName = "Research", lastModified = 0L)

            localPanelsRepo.addANewPanel(panel, viaSocket = false).toList()

            val dbPanels = panelsDao.getAllThePanelsAsAList()
            assertEquals(1, dbPanels.size)
            assertEquals("Research", dbPanels.first().panelName)

            coVerify(exactly = 1) { remotePanelsRepo.addANewPanel(match { it.panelName == "Research" }) }
        }

    @Test
    fun `viaSocket flag entirely bypasses remote repo execution during panel creation`() = runTest {
        val panel = Panel(localId = 0, panelName = "OfflinePanel", lastModified = 0L)

        localPanelsRepo.addANewPanel(panel, viaSocket = true).toList()

        val dbPanels = panelsDao.getAllThePanelsAsAList()
        assertEquals(1, dbPanels.size)
        assertEquals("OfflinePanel", dbPanels.first().panelName)

        coVerify(exactly = 0) { remotePanelsRepo.addANewPanel(any()) }
        coVerify(exactly = 0) { pendingSyncQueueRepo.addInQueue(any()) }
    }

    @Test
    fun `network failure during remote panel creation explicitly captures dto to pending sync queue`() =
        runTest {
            coEvery { remotePanelsRepo.addANewPanel(any()) }  returns flowOf(Result.Failure("Network Timeout"))

            val panel = Panel(localId = 0, panelName = "QueuedPanel", lastModified = 0L)

            localPanelsRepo.addANewPanel(panel, viaSocket = false).toList()

            coVerify(exactly = 1) {
                pendingSyncQueueRepo.addInQueue(match { queueItem ->
                    queueItem.operation == "ADD_A_NEW_PANEL" && queueItem.payload.contains("QueuedPanel")
                })
            }
        }

    @Test
    fun `attempting to update or delete an unsynced panel throws local validation exception`() =
        runTest {
            val panelId = panelsDao.addANewPanel(
                Panel(
                    localId = 0,
                    panelName = "Unsynced",
                    lastModified = 0L,
                    remoteId = null
                )
            )

            val updateError = executeAndGetErrorMessage {
                localPanelsRepo.updateAPanelName("NewName", panelId, viaSocket = false).toList()
            }

            val deleteError = executeAndGetErrorMessage {
                localPanelsRepo.deleteAPanel(panelId, viaSocket = false).toList()
            }

            assertTrue(
                updateError.contains(
                    "Failed requirement",
                    ignoreCase = true
                ) || updateError.contains("null", ignoreCase = true),
                "Expected require() to fail for update with null remoteId, got: $updateError"
            )

            assertTrue(
                deleteError.contains(
                    "Failed requirement",
                    ignoreCase = true
                ) || deleteError.contains("null", ignoreCase = true),
                "Expected require() to fail for delete with null remoteId, got: $deleteError"
            )
        }

    @Test
    fun `attempting to add an unsynced folder to an unsynced panel throws local validation exception`() =
        runTest {
            val folderId = foldersDao.insertANewFolder(
                Folder(
                    name = "UnsyncedFolder",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L,
                    remoteId = null
                )
            )
            val panelId = panelsDao.addANewPanel(
                Panel(
                    localId = 0,
                    panelName = "UnsyncedPanel",
                    lastModified = 0L,
                    remoteId = null
                )
            )

            val panelFolder = PanelFolder(
                localId = 0,
                folderId = folderId,
                connectedPanelId = panelId,
                panelPosition = 1,
                folderName = "UnsyncedFolder",
                lastModified = 0L
            )

            val addFolderError = executeAndGetErrorMessage {
                localPanelsRepo.addANewFolderInAPanel(panelFolder, viaSocket = false).toList()
            }

            assertTrue(
                addFolderError.contains(
                    "Failed requirement",
                    ignoreCase = true
                ) || addFolderError.contains("null", ignoreCase = true),
                "Expected require() to fail for adding a panel folder without remote ids, got: $addFolderError"
            )
        }

    @Test
    fun `deleting a synced panel correctly drops it and its connected folders locally and pushes changes`() =
        runTest {
            val folderId = foldersDao.insertANewFolder(
                Folder(
                    name = "TargetFolder",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L,
                    remoteId = 111L
                )
            )
            val panelId = panelsDao.addANewPanel(
                Panel(
                    localId = 0,
                    panelName = "TargetPanel",
                    lastModified = 0L,
                    remoteId = 222L
                )
            )

            panelsDao.addANewFolderInAPanel(
                PanelFolder(
                    localId = 0,
                    folderId = folderId,
                    connectedPanelId = panelId,
                    panelPosition = 1,
                    folderName = "TargetFolder",
                    lastModified = 0L
                )
            )

            localPanelsRepo.deleteAPanel(panelId, viaSocket = false).toList()

            val allPanels = panelsDao.getAllThePanelsAsAList()
            val allPanelFolders = panelsDao.getAllThePanelFoldersAsAList()

            assertTrue(allPanels.isEmpty(), "Expected panel to be completely deleted from local DB")
            assertTrue(
                allPanelFolders.isEmpty(),
                "Expected cascaded deletion of connected panel folders"
            )

            coVerify(exactly = 1) {
                remotePanelsRepo.deleteAPanel(match { it.id == 222L })
            }
        }
}