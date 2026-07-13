package com.sakethh.linkora.data.sync

import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.data.remote.repository.sync.RemoteSyncRepoImpl
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.repository.local.LocalDatabaseUtilsRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalMultiActionRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.domain.repository.remote.RemoteLinksRepo
import com.sakethh.linkora.domain.repository.remote.RemoteMultiActionRepo
import com.sakethh.linkora.domain.repository.remote.RemotePanelsRepo
import com.sakethh.linkora.domain.repository.remote.RemoteTagsRepo
import com.sakethh.linkora.platform.Network
import com.sakethh.linkora.utils.canPushToServer
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class RemoteSyncRepoImplTest {
    private lateinit var localFoldersRepo: LocalFoldersRepo
    private lateinit var localLinksRepo: LocalLinksRepo
    private lateinit var localPanelsRepo: LocalPanelsRepo
    private lateinit var pendingSyncQueueRepo: PendingSyncQueueRepo
    private lateinit var remoteFoldersRepo: RemoteFoldersRepo
    private lateinit var remoteLinksRepo: RemoteLinksRepo
    private lateinit var remotePanelsRepo: RemotePanelsRepo
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var localMultiActionRepo: LocalMultiActionRepo
    private lateinit var remoteMultiActionRepo: RemoteMultiActionRepo
    private lateinit var linksDao: LinksDao
    private lateinit var foldersDao: FoldersDao
    private lateinit var localTagsRepo: LocalTagsRepo
    private lateinit var remoteTagsRepo: RemoteTagsRepo
    private lateinit var tagsDao: TagsDao
    private lateinit var localDatabaseUtilsRepo: LocalDatabaseUtilsRepo
    private lateinit var network: Network

    private lateinit var remoteSyncRepoImpl: RemoteSyncRepoImpl

    @BeforeTest
    fun setup() {
        clearAllMocks()

        localFoldersRepo = mockk(relaxed = true)
        localLinksRepo = mockk(relaxed = true)
        localPanelsRepo = mockk(relaxed = true)
        pendingSyncQueueRepo = mockk(relaxed = true)
        remoteFoldersRepo = mockk(relaxed = true)
        remoteLinksRepo = mockk(relaxed = true)
        remotePanelsRepo = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        localMultiActionRepo = mockk(relaxed = true)
        remoteMultiActionRepo = mockk(relaxed = true)
        linksDao = mockk(relaxed = true)
        foldersDao = mockk(relaxed = true)
        localTagsRepo = mockk(relaxed = true)
        remoteTagsRepo = mockk(relaxed = true)
        tagsDao = mockk(relaxed = true)
        localDatabaseUtilsRepo = mockk(relaxed = true)
        network = mockk(relaxed = true)

        val mockPrefs =
            mockk<AppPreferences>(relaxed = true) {
                every { correlation } returns
                    Correlation(
                        id = "test-correlation",
                        clientName = "test-client",
                    )
            }
        coEvery { preferencesRepository.getPreferences() } returns mockPrefs
        coEvery { preferencesRepository.readPreferenceValue<Long>(any()) } returns 0L
        coEvery { preferencesRepository.changePreferenceValue<Long>(any(), any()) } returns Unit

        mockkStatic("com.sakethh.linkora.utils.ExtensionsKt")
        every { any<AppPreferences>().canPushToServer() } returns true

        val mockEngine = MockEngine { _ ->
            respond(
                content = """{"eventTimestamp": 2000, "message": "Success"}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val httpClient =
            HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        coEvery { network.getSyncServerClient() } returns httpClient

        remoteSyncRepoImpl =
            RemoteSyncRepoImpl(
                localFoldersRepo = localFoldersRepo,
                localLinksRepo = localLinksRepo,
                localPanelsRepo = localPanelsRepo,
                authToken = { "mock-token" },
                baseUrl = { "https://server.linkora.com/api/" },
                websocketScheme = { "wss://" },
                pendingSyncQueueRepo = pendingSyncQueueRepo,
                remoteFoldersRepo = remoteFoldersRepo,
                remoteLinksRepo = remoteLinksRepo,
                remotePanelsRepo = remotePanelsRepo,
                preferencesRepository = preferencesRepository,
                localMultiActionRepo = localMultiActionRepo,
                remoteMultiActionRepo = remoteMultiActionRepo,
                linksDao = linksDao,
                foldersDao = foldersDao,
                localTagsRepo = localTagsRepo,
                remoteTagsRepo = remoteTagsRepo,
                tagsDao = tagsDao,
                localDatabaseUtilsRepo = localDatabaseUtilsRepo,
                network = network,
            )
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `pushNonSyncedDataToServer sweeps all local tables and constructs exact DTO payloads for the queue`() = runTest {
        val folder =
            Folder(
                name = "UnsyncedFolder",
                note = "",
                parentFolderId = null,
                localId = 1L,
                remoteId = null,
                isArchived = false,
                lastModified = 1000L,
            )
        val tag = Tag(localId = 2L, name = "UnsyncedTag", lastModified = 1000L)
        val link =
            Link(
                url = "https://x.com",
                title = "L",
                linkType = LinkType.SAVED_LINK,
                idOfLinkedFolder = null,
                imgURL = "",
                note = "",
                localId = 3L,
                mediaType = MediaType.IMAGE,
                lastModified = 1000L,
                userAgent = "",
            )
        val panel = Panel(localId = 4L, panelName = "UnsyncedPanel", lastModified = 1000L)
        val panelFolder =
            PanelFolder(
                localId = 5L,
                folderId = 1L,
                connectedPanelId = 4L,
                panelPosition = 0,
                folderName = "PF",
                lastModified = 1000L,
            )

        coEvery { localFoldersRepo.getUnSyncedFolders() } returns listOf(folder)
        coEvery { tagsDao.getUnsyncedTags() } returns listOf(tag)
        coEvery { localLinksRepo.getUnSyncedLinks() } returns listOf(link)
        coEvery { localPanelsRepo.getUnSyncedPanels() } returns listOf(panel)
        coEvery { localPanelsRepo.getUnSyncedPanelFolders() } returns listOf(panelFolder)
        coEvery { tagsDao.getTags(any()) } returns emptyList() // No tags connected to the link
        coEvery { pendingSyncQueueRepo.getAllItemsFromQueue() } returns
            emptyList() // Prevent loop in pushPendingSyncQueueToServer

        val channel = Channel<Result<Unit>>(Channel.UNLIMITED)

        with(remoteSyncRepoImpl) {
            channel.pushNonSyncedDataToServer()
        }

        coVerify(exactly = 1) {
            pendingSyncQueueRepo.addInQueue(
                match {
                    it.operation == SyncServerRoute.CREATE_FOLDER.name &&
                        it.payload.contains(
                            "UnsyncedFolder",
                        )
                },
            )
        }
        coVerify(exactly = 1) {
            pendingSyncQueueRepo.addInQueue(
                match {
                    it.operation == SyncServerRoute.CREATE_TAG.name &&
                        it.payload.contains(
                            "UnsyncedTag",
                        )
                },
            )
        }
        coVerify(exactly = 1) {
            pendingSyncQueueRepo.addInQueue(
                match {
                    it.operation == SyncServerRoute.CREATE_A_NEW_LINK.name &&
                        it.payload.contains(
                            "https://x.com",
                        )
                },
            )
        }
        coVerify(exactly = 1) {
            pendingSyncQueueRepo.addInQueue(
                match {
                    it.operation == SyncServerRoute.ADD_A_NEW_PANEL.name &&
                        it.payload.contains(
                            "UnsyncedPanel",
                        )
                },
            )
        }
        coVerify(exactly = 1) {
            pendingSyncQueueRepo.addInQueue(
                match {
                    it.operation == SyncServerRoute.ADD_A_NEW_FOLDER_IN_A_PANEL.name &&
                        it.payload.contains(
                            "connectedPanelId\":4",
                        )
                },
            )
        }
    }

    @Test
    fun `deleteEverything completely wipes the local SQLite database via utils repo`() = runTest {
        val result =
            remoteSyncRepoImpl
                .deleteEverything(deleteOnRemote = true)
                .filterNot { it is Result.Loading }
                .first()

        assertTrue(
            result is Result.Success,
            "Expected successful flow emission from postFlow / wrappedResultFlow",
        )

        coVerify(exactly = 1) { localDatabaseUtilsRepo.resetDatabase() }

        coVerify(exactly = 1) {
            preferencesRepository.changePreferenceValue(
                match { it.key == AppPreferences.LAST_SELECTED_PANEL_ID.key },
                any<Long>(),
            )
        }

        coVerify(exactly = 1) {
            preferencesRepository.changePreferenceValue(
                match { it.key == AppPreferences.LAST_TIME_SYNCED_WITH_SERVER.key },
                2000L,
            )
        }
    }
}
