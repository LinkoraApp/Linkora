package com.sakethh.linkora.data.sync

import com.sakethh.linkora.data.remote.repository.sync.LocalDataUpdateService
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.folder.FolderDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateTitleOfTheLinkDTO
import com.sakethh.linkora.domain.dto.server.panel.PanelFolderDTO
import com.sakethh.linkora.domain.dto.server.tag.TagDTO
import com.sakethh.linkora.domain.model.WebSocketEvent
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalMultiActionRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class LocalDataUpdateServiceTest {

    private lateinit var localFoldersRepo: LocalFoldersRepo
    private lateinit var localLinksRepo: LocalLinksRepo
    private lateinit var localPanelsRepo: LocalPanelsRepo
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var localMultiActionRepo: LocalMultiActionRepo
    private lateinit var localTagsRepo: LocalTagsRepo
    private lateinit var remoteSyncRepo: RemoteSyncRepo

    private lateinit var localDataUpdateService: LocalDataUpdateService

    private val myClientCorrelationId = "client-123"

    @BeforeTest
    fun setup() {
        clearAllMocks()

        localFoldersRepo = mockk(relaxed = true)
        localLinksRepo = mockk(relaxed = true)
        localPanelsRepo = mockk(relaxed = true)
        preferencesRepository = mockk(relaxed = true)
        localMultiActionRepo = mockk(relaxed = true)
        localTagsRepo = mockk(relaxed = true)
        remoteSyncRepo = mockk(relaxed = true)

        val mockPrefs = mockk<AppPreferences>(relaxed = true) {
            every { correlation } returns Correlation(
                id = myClientCorrelationId,
                clientName = "TestClient"
            )
        }
        coEvery { preferencesRepository.getPreferences() } returns mockPrefs
        coEvery { preferencesRepository.readPreferenceValue<Long>(any()) } returns 0L
        coEvery { preferencesRepository.changePreferenceValue<Long>(any(), any()) } returns Unit

        localDataUpdateService = LocalDataUpdateService(
            localFoldersRepo = localFoldersRepo,
            localLinksRepo = localLinksRepo,
            localPanelsRepo = localPanelsRepo,
            preferencesRepository = preferencesRepository,
            localMultiActionRepo = localMultiActionRepo,
            localTagsRepo = localTagsRepo,
            remoteSyncRepo = remoteSyncRepo
        )
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `events with matching correlation id update timestamp but abort local database operations`() =
        runTest {
            val tagDto = TagDTO(
                id = 100L,
                name = "Kotlin",
                eventTimestamp = 5000L,
                correlation = Correlation(id = myClientCorrelationId, clientName = "TestClient")
            )
            val event = WebSocketEvent(
                operation = SyncServerRoute.CREATE_TAG.name,
                payload = Json.encodeToJsonElement(tagDto)
            )

            localDataUpdateService.updateLocalDBAccordingToEvent(event)

            coVerify(exactly = 0) { localTagsRepo.createATag(any(), any()) }
            coVerify(exactly = 1) {
                preferencesRepository.changePreferenceValue(
                    match { it.key == AppPreferences.LAST_TIME_SYNCED_WITH_SERVER.key },
                    5000L
                )
            }
        }

    @Test
    fun `create folder sync dynamically resolves remote parent folder id to local id before insertion`() =
        runTest {
            val folderDto = FolderDTO(
                id = 200L,
                name = "NewRemoteFolder",
                note = "",
                parentFolderId = 50L,
                isArchived = false,
                eventTimestamp = 6000L,
                correlation = Correlation(id = "other-client", clientName = "Other")
            )
            val event = WebSocketEvent(
                operation = SyncServerRoute.CREATE_FOLDER.name,
                payload = Json.encodeToJsonElement(folderDto)
            )

            coEvery { localFoldersRepo.getLocalIdOfAFolder(50L) } returns 5L
            coEvery {
                localFoldersRepo.insertANewFolder(
                    any(),
                    any(),
                    any()
                )
            } returns flowOf(Result.Success(1))

            localDataUpdateService.updateLocalDBAccordingToEvent(event)

            coVerify(exactly = 1) {
                localFoldersRepo.insertANewFolder(
                    match { it.name == "NewRemoteFolder" && it.parentFolderId == 5L && it.remoteId == 200L },
                    ignoreFolderAlreadyExistsException = true,
                    viaSocket = true
                )
            }
            coVerify(exactly = 1) {
                preferencesRepository.changePreferenceValue(
                    match { it.key == AppPreferences.LAST_TIME_SYNCED_WITH_SERVER.key },
                    6000L
                )
            }
        }

    @Test
    fun `delete link safely aborts if remote id cannot be mapped to a local link id`() = runTest {
        val idBasedDTO = IDBasedDTO(
            id = 300L,
            eventTimestamp = 7000L,
            correlation = Correlation(id = "other-client", clientName = "Other")
        )
        val event = WebSocketEvent(
            operation = SyncServerRoute.DELETE_A_LINK.name,
            payload = Json.encodeToJsonElement(idBasedDTO)
        )

        coEvery { localLinksRepo.getLocalLinkId(300L) } returns null

        localDataUpdateService.updateLocalDBAccordingToEvent(event)

        coVerify(exactly = 0) { localLinksRepo.deleteALink(any(), any()) }
    }

    @Test
    fun `update link title successfully maps local id and executes update operation`() = runTest {
        val updateDto = UpdateTitleOfTheLinkDTO(
            linkId = 400L,
            newTitleOfTheLink = "Updated Title",
            eventTimestamp = 8000L,
            correlation = Correlation(id = "other-client", clientName = "Other")
        )
        val event = WebSocketEvent(
            operation = SyncServerRoute.UPDATE_LINK_TITLE.name,
            payload = Json.encodeToJsonElement(updateDto)
        )

        coEvery { localLinksRepo.getLocalLinkId(400L) } returns 42L
        coEvery {
            localLinksRepo.updateLinkTitle(
                any(),
                any(),
                any()
            )
        } returns flowOf(Result.Success(Unit))

        localDataUpdateService.updateLocalDBAccordingToEvent(event)

        coVerify(exactly = 1) { localLinksRepo.updateLinkTitle(42L, "Updated Title", true) }
        coVerify(exactly = 1) {
            preferencesRepository.changePreferenceValue(
                match { it.key == AppPreferences.LAST_TIME_SYNCED_WITH_SERVER.key },
                8000L
            )
        }
    }

    @Test
    fun `add panel folder requires both local folder id and local panel id to execute`() = runTest {
        val panelFolderDto = PanelFolderDTO(
            id = 500L,
            folderId = 600L,
            panelPosition = 1,
            folderName = "PF",
            connectedPanelId = 700L,
            eventTimestamp = 9000L,
            correlation = Correlation(id = "other-client", clientName = "Other")
        )
        val event = WebSocketEvent(
            operation = SyncServerRoute.ADD_A_NEW_FOLDER_IN_A_PANEL.name,
            payload = Json.encodeToJsonElement(panelFolderDto)
        )

        coEvery { localFoldersRepo.getLocalIdOfAFolder(600L) } returns 6L
        coEvery { localPanelsRepo.getLocalPanelId(700L) } returns 7L
        coEvery {
            localPanelsRepo.addANewFolderInAPanel(
                any(),
                any()
            )
        } returns flowOf(Result.Success(Unit))

        localDataUpdateService.updateLocalDBAccordingToEvent(event)

        coVerify(exactly = 1) {
            localPanelsRepo.addANewFolderInAPanel(
                match { it.folderId == 6L && it.connectedPanelId == 7L && it.remoteId == 500L },
                viaSocket = true
            )
        }
        coVerify(exactly = 1) {
            preferencesRepository.changePreferenceValue(
                match { it.key == AppPreferences.LAST_TIME_SYNCED_WITH_SERVER.key },
                9000L
            )
        }
    }
}