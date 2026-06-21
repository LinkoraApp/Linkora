package com.sakethh.linkora.data.sync

import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.data.remote.repository.sync.PendingQueueService
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.NewItemResponseDTO
import com.sakethh.linkora.domain.dto.server.TimeStampBasedResponse
import com.sakethh.linkora.domain.dto.server.link.AddLinkDTO
import com.sakethh.linkora.domain.dto.server.panel.AddANewPanelFolderDTO
import com.sakethh.linkora.domain.dto.server.tag.CreateTagDTO
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteFoldersRepo
import com.sakethh.linkora.domain.repository.remote.RemoteLinksRepo
import com.sakethh.linkora.domain.repository.remote.RemoteMultiActionRepo
import com.sakethh.linkora.domain.repository.remote.RemotePanelsRepo
import com.sakethh.linkora.domain.repository.remote.RemoteTagsRepo
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PendingQueueServiceTest {

    private lateinit var localFoldersRepo: LocalFoldersRepo
    private lateinit var localLinksRepo: LocalLinksRepo
    private lateinit var localPanelsRepo: LocalPanelsRepo
    private lateinit var pendingSyncQueueRepo: PendingSyncQueueRepo
    private lateinit var remoteFoldersRepo: RemoteFoldersRepo
    private lateinit var remoteLinksRepo: RemoteLinksRepo
    private lateinit var remotePanelsRepo: RemotePanelsRepo
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var remoteMultiActionRepo: RemoteMultiActionRepo
    private lateinit var remoteTagsRepo: RemoteTagsRepo
    private lateinit var linksDao: LinksDao
    private lateinit var foldersDao: FoldersDao
    private lateinit var tagsDao: TagsDao

    private lateinit var pendingQueueService: PendingQueueService

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
        remoteMultiActionRepo = mockk(relaxed = true)
        remoteTagsRepo = mockk(relaxed = true)
        linksDao = mockk(relaxed = true)
        foldersDao = mockk(relaxed = true)
        tagsDao = mockk(relaxed = true)

        coEvery { preferencesRepository.readPreferenceValue<Long>(any()) } returns 0L
        coEvery { preferencesRepository.changePreferenceValue<Long>(any(), any()) } returns Unit

        pendingQueueService = PendingQueueService(
            localFoldersRepo = localFoldersRepo,
            localLinksRepo = localLinksRepo,
            localPanelsRepo = localPanelsRepo,
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            remoteFoldersRepo = remoteFoldersRepo,
            remoteLinksRepo = remoteLinksRepo,
            remotePanelsRepo = remotePanelsRepo,
            preferencesRepository = preferencesRepository,
            remoteMultiActionRepo = remoteMultiActionRepo,
            remoteTagsRepo = remoteTagsRepo,
            linksDao = linksDao,
            foldersDao = foldersDao,
            tagsDao = tagsDao
        )
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `queue processing with empty queue returns immediately and makes no remote calls`() =
        runTest {
            coEvery { pendingSyncQueueRepo.getAllItemsFromQueue() } returns emptyList()

            val channel = Channel<Result<Unit>>(Channel.UNLIMITED)

            with(pendingQueueService) {
                val result =
                    channel.pushPendingSyncQueueToServer().filterNot { it is Result.Loading }
                        .first()
                assertTrue(result is Result.Success)
            }

            coVerify(exactly = 0) { remoteTagsRepo.createATag(any()) }
            coVerify(exactly = 0) { remoteLinksRepo.addANewLink(any()) }
            coVerify(exactly = 0) {
                preferencesRepository.changePreferenceValue<Long>(
                    any(),
                    any()
                )
            }
        }

    @Test
    fun `create tag sync operation updates remote id and clears queue`() = runTest {
        val createTagDTO = CreateTagDTO(
            name = "Kotlin",
            eventTimestamp = 1000L,
            offlineSyncItemId = 42L,
            correlation = null
        )
        val queueItem = PendingSyncQueue(
            id = 1L,
            operation = SyncServerRoute.CREATE_TAG.name,
            payload = Json.encodeToString(createTagDTO)
        )

        val timeStampResponse = TimeStampBasedResponse(2000L, "Success")
        val newItemResponse = NewItemResponseDTO(timeStampResponse, 99L, null)

        coEvery { pendingSyncQueueRepo.getAllItemsFromQueue() } returns listOf(queueItem)
        coEvery { remoteTagsRepo.createATag(any()) } returns flowOf(Result.Success(newItemResponse))

        val channel = Channel<Result<Unit>>(Channel.UNLIMITED)

        with(pendingQueueService) {
            channel.pushPendingSyncQueueToServer().filterNot { it is Result.Loading }.first()
        }

        coVerify(exactly = 1) { tagsDao.updateRemoteId(localId = 42L, newRemoteId = 99L) }
        coVerify(exactly = 1) { pendingSyncQueueRepo.removeFromQueue(1L) }
        coVerify(exactly = 1) { preferencesRepository.changePreferenceValue(any(), 2000L) }
    }

    @Test
    fun `delete tag sync operation completes timestamp sync and clears queue`() = runTest {
        val idBasedDTO = IDBasedDTO(id = 99L, eventTimestamp = 1000L, correlation = null)
        val queueItem = PendingSyncQueue(
            id = 2L,
            operation = SyncServerRoute.DELETE_TAG.name,
            payload = Json.encodeToString(idBasedDTO)
        )

        val timeStampResponse = TimeStampBasedResponse(2000L, "Deleted")

        coEvery { pendingSyncQueueRepo.getAllItemsFromQueue() } returns listOf(queueItem)
        coEvery { remoteTagsRepo.deleteATag(any()) } returns flowOf(Result.Success(timeStampResponse))

        val channel = Channel<Result<Unit>>(Channel.UNLIMITED)

        with(pendingQueueService) {
            channel.pushPendingSyncQueueToServer().filterNot { it is Result.Loading }.first()
        }

        coVerify(exactly = 1) { remoteTagsRepo.deleteATag(any()) }
        coVerify(exactly = 1) { pendingSyncQueueRepo.removeFromQueue(2L) }
        coVerify(exactly = 1) { preferencesRepository.changePreferenceValue(any(), 2000L) }
    }

    @Test
    fun `create new link sync operation updates deeply nested link local properties`() = runTest {
        val addLinkDTO = AddLinkDTO(
            url = "https://example.com", title = "Test", imgURL = "", note = "",
            idOfLinkedFolder = null, tags = emptyList(),
            mediaType = MediaType.IMAGE, eventTimestamp = 1000L, offlineSyncItemId = 15L,
            linkType = LinkType.IMPORTANT_LINK,
            baseURL = "example.com",
            userAgent = "TestAgent",
            markedAsImportant = true,
            correlation = null,
        )
        val queueItem = PendingSyncQueue(
            id = 3L,
            operation = SyncServerRoute.CREATE_A_NEW_LINK.name,
            payload = Json.encodeToString(addLinkDTO)
        )

        val timeStampResponse = TimeStampBasedResponse(2000L, "Success")
        val newItemResponse = NewItemResponseDTO(timeStampResponse, 88L, null)
        val linkMock = mockk<Link>(relaxed = true) {
            every { localId } returns 15L
            every { copy(remoteId = 88L) } returns this
        }

        coEvery { pendingSyncQueueRepo.getAllItemsFromQueue() } returns listOf(queueItem)
        coEvery { tagsDao.getTags(any()) } returns emptyList()
        coEvery { remoteLinksRepo.addANewLink(any()) } returns flowOf(Result.Success(newItemResponse))
        coEvery { localLinksRepo.getALink(15L) } returns linkMock
        coEvery { localLinksRepo.updateALink(any(), any(), any()) } returns flowOf(
            Result.Success(
                Unit
            )
        )

        val channel = Channel<Result<Unit>>(Channel.UNLIMITED)

        with(pendingQueueService) {
            channel.pushPendingSyncQueueToServer().filterNot { it is Result.Loading }.first()
        }

        coVerify(exactly = 1) { localLinksRepo.updateALink(any(), null, true) }
        coVerify(exactly = 1) { pendingSyncQueueRepo.removeFromQueue(3L) }
        coVerify(exactly = 1) { preferencesRepository.changePreferenceValue(any(), 2000L) }
    }

    @Test
    fun `add new panel folder sync operation dynamically resolves remote parent ids`() = runTest {
        val addPanelFolderDTO = AddANewPanelFolderDTO(
            folderId = 5L, panelPosition = 1, folderName = "PanelFolder", connectedPanelId = 10L,
            offlineSyncItemId = 25L, eventTimestamp = 1000L, correlation = null
        )
        val queueItem = PendingSyncQueue(
            id = 4L,
            operation = SyncServerRoute.ADD_A_NEW_FOLDER_IN_A_PANEL.name,
            payload = Json.encodeToString(addPanelFolderDTO)
        )

        val timeStampResponse = TimeStampBasedResponse(2000L, "Success")
        val newItemResponse = NewItemResponseDTO(timeStampResponse, 77L, null)
        val panelFolderMock = mockk<PanelFolder>(relaxed = true) {
            every { localId } returns 25L
            every { copy(remoteId = 77L) } returns this
        }

        coEvery { pendingSyncQueueRepo.getAllItemsFromQueue() } returns listOf(queueItem)
        coEvery { localFoldersRepo.getRemoteIdOfAFolder(5L) } returns 500L
        coEvery { localPanelsRepo.getRemotePanelId(10L) } returns 1000L
        coEvery { remotePanelsRepo.addANewFolderInAPanel(any()) } returns flowOf(
            Result.Success(
                newItemResponse
            )
        )
        coEvery { localPanelsRepo.getPanelFolder(25L) } returns panelFolderMock

        val channel = Channel<Result<Unit>>(Channel.UNLIMITED)

        with(pendingQueueService) {
            channel.pushPendingSyncQueueToServer().filterNot { it is Result.Loading }.first()
        }

        coVerify(exactly = 1) {
            remotePanelsRepo.addANewFolderInAPanel(match { it.folderId == 500L && it.connectedPanelId == 1000L })
        }
        coVerify(exactly = 1) { localPanelsRepo.updateAPanelFolder(any()) }
        coVerify(exactly = 1) { pendingSyncQueueRepo.removeFromQueue(4L) }
        coVerify(exactly = 1) { preferencesRepository.changePreferenceValue(any(), 2000L) }
    }
}