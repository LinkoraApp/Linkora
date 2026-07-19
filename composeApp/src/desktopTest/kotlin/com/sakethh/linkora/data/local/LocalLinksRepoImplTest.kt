package com.sakethh.linkora.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.data.local.repository.LocalLinksRepoImpl
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteLinksRepo
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.utils.canPushToServer
import com.sakethh.linkora.utils.isAValidLink
import io.ktor.client.HttpClient
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

class LocalLinksRepoImplTest {
    private lateinit var database: LocalDatabase
    private lateinit var linksDao: LinksDao
    private lateinit var foldersDao: FoldersDao
    private lateinit var tagsDao: TagsDao

    private lateinit var remoteLinksRepo: RemoteLinksRepo
    private lateinit var pendingSyncQueueRepo: PendingSyncQueueRepo
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var standardClient: HttpClient

    private lateinit var localLinksRepo: LocalLinksRepo

    @BeforeTest
    fun setup() {
        clearAllMocks()

        database =
            Room.inMemoryDatabaseBuilder<LocalDatabase>()
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.Unconfined)
                .build()

        linksDao = database.linksDao
        foldersDao = database.foldersDao
        tagsDao = database.tagsDao

        remoteLinksRepo = mockk<RemoteLinksRepo>(relaxed = true)
        pendingSyncQueueRepo = mockk<PendingSyncQueueRepo>(relaxed = true)
        preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
        standardClient = mockk<HttpClient>(relaxed = true)

        val mockPrefs =
            mockk<AppPreferences>(relaxed = true) {
                every { serverBaseUrl } returns "https://server.linkora.com"
                every { serverSecurityToken } returns "mock-auth-token"
                every { correlation } returns
                    Correlation(
                        id = "test-correlation-id",
                        clientName = "test-client",
                    )
            }
        coEvery { preferencesRepository.getPreferences() } returns mockPrefs

        mockkStatic("com.sakethh.linkora.utils.ExtensionsKt")
        every { any<AppPreferences>().canPushToServer() } returns true

        // Safely mock URL validation to ensure it behaves predictably in desktopTest
        every { any<String>().isAValidLink() } returns true
        every { "htp://broken-url".isAValidLink() } returns false

        localLinksRepo =
            LocalLinksRepoImpl(
                linksDao = linksDao,
                primaryUserAgent = { "Mozilla/5.0 Test Agent" },
                proxyUrl = { "https://proxy.linkora.com" },
                standardClient = standardClient,
                remoteLinksRepo = remoteLinksRepo,
                foldersDao = foldersDao,
                pendingSyncQueueRepo = pendingSyncQueueRepo,
                preferencesRepository = preferencesRepository,
                tagsDao = tagsDao,
                webCapture = mockk(relaxed = true),
            )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private suspend fun executeAndGetErrorMessage(block: suspend () -> List<Any>): String = try {
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

    @Test
    fun `adding link with improperly formatted url throws local validation exception`() = runTest {
        val invalidLink =
            Link(
                url = "htp://broken-url",
                title = "Test",
                linkType = LinkType.SAVED_LINK,
                idOfLinkedFolder = null,
                imgURL = "",
                note = "",
                lastModified = 0L,
                userAgent = "",
            )
        val config =
            mockk<LinkSaveConfig>(relaxed = true) {
                every { forceSaveWithoutRetrievingData } returns true
            }

        val errorMessage = executeAndGetErrorMessage {
            localLinksRepo.addANewLink(invalidLink, null, config).toList()
        }

        assertTrue(
            errorMessage.contains("invalid", ignoreCase = true),
            "Expected 'Invalid' exception for malformed URL, but got: $errorMessage",
        )
    }

    @Test
    fun `adding exact duplicate link when skipSavingIfExists is enabled outputs existence failure`() = runTest {
        val link =
            Link(
                url = "https://duplicate.com",
                title = "Test",
                linkType = LinkType.SAVED_LINK,
                idOfLinkedFolder = null,
                imgURL = "",
                note = "",
                lastModified = 0L,
                userAgent = "",
            )
        val config =
            mockk<LinkSaveConfig>(relaxed = true) {
                every { forceSaveWithoutRetrievingData } returns true
                every { skipSavingIfExists } returns true
            }

        localLinksRepo.addANewLink(link, null, config).toList()

        val errorMessage = executeAndGetErrorMessage {
            localLinksRepo.addANewLink(link, null, config).toList()
        }

        assertTrue(
            errorMessage.contains(
                "saved",
                ignoreCase = true,
            ) ||
                errorMessage.contains("collection", ignoreCase = true),
            "Expected 'saved' or 'collection' duplicate localized string, but got: $errorMessage",
        )
    }

    @Test
    fun `network failure during remote link creation explicitly captures dto to pending sync queue`() = runTest {
        coEvery { remoteLinksRepo.addANewLink(any()) } returns
            flowOf(Result.Failure("Network Timeout"))

        val link =
            Link(
                url = "https://offline.com",
                title = "Test",
                linkType = LinkType.SAVED_LINK,
                idOfLinkedFolder = null,
                imgURL = "",
                note = "",
                lastModified = 0L,
                userAgent = "",
            )
        val config =
            mockk<LinkSaveConfig>(relaxed = true) {
                every { forceSaveWithoutRetrievingData } returns true
            }

        localLinksRepo.addANewLink(link, null, config).toList()

        coVerify(exactly = 1) {
            pendingSyncQueueRepo.addInQueue(
                match { queueItem ->
                    queueItem.operation == "CREATE_A_NEW_LINK" &&
                        queueItem.payload.contains("https://offline.com")
                },
            )
        }
    }

    @Test
    fun `viaSocket flag entirely bypasses remote repo execution during link modifications`() = runTest {
        val linkId =
            linksDao.addANewLink(
                Link(
                    url = "https://socket.com",
                    title = "Old",
                    linkType = LinkType.SAVED_LINK,
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = "",
                ),
            )

        localLinksRepo.updateLinkTitle(linkId, "New Title", viaSocket = true).toList()

        val dbLink = linksDao.getLink(linkId)
        assertEquals("New Title", dbLink.title)

        coVerify(exactly = 0) { remoteLinksRepo.updateLinkTitle(any()) }
        coVerify(exactly = 0) { pendingSyncQueueRepo.addInQueue(any()) }
    }

    @Test
    fun `updating a link strictly prunes removed tags and attaches newly selected ones via cross tables`() = runTest {
        val linkId =
            linksDao.addANewLink(
                Link(
                    url = "https://tags.com",
                    title = "Tags",
                    linkType = LinkType.SAVED_LINK,
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = "",
                ),
            )
        val tag1 = Tag(localId = 0, name = "Keep", lastModified = 0L)
        val tag2 = Tag(localId = 0, name = "Remove", lastModified = 0L)
        val tag3 = Tag(localId = 0, name = "Add", lastModified = 0L)

        val t1Id = tagsDao.createATag(tag1)
        val t2Id = tagsDao.createATag(tag2)
        val t3Id = tagsDao.createATag(tag3)

        tagsDao.createLinkTags(
            listOf(
                LinkTag(linkId = linkId, tagId = t1Id),
                LinkTag(linkId = linkId, tagId = t2Id),
            ),
        )

        val updatedPair =
            LinkTagsPair(
                link = linksDao.getLink(linkId),
                tags = listOf(tagsDao.getATag(t1Id), tagsDao.getATag(t3Id)),
            )

        localLinksRepo.updateALink(linksDao.getLink(linkId), updatedPair, viaSocket = true).toList()

        val currentTags = tagsDao.getTags(linkId)

        assertEquals(2, currentTags.size)
        assertTrue(currentTags.any { it.name == "Keep" })
        assertTrue(currentTags.any { it.name == "Add" })
        assertTrue(
            currentTags.none { it.name == "Remove" },
            "Unselected tag was not cleanly deleted from cross-reference table",
        )
    }

    @Test
    fun `delete duplicate links scans deeply across categories and aggressively purges clones keeping one intact`() = runTest {
        linksDao.addANewLink(
            Link(
                url = "https://dup.com",
                title = "D1",
                linkType = LinkType.SAVED_LINK,
                idOfLinkedFolder = null,
                imgURL = "",
                note = "",
                lastModified = 0L,
                userAgent = "",
            ),
        )
        linksDao.addANewLink(
            Link(
                url = "https://dup.com",
                title = "D2",
                linkType = LinkType.SAVED_LINK,
                idOfLinkedFolder = null,
                imgURL = "",
                note = "",
                lastModified = 0L,
                userAgent = "",
            ),
        )
        linksDao.addANewLink(
            Link(
                url = "https://unique.com",
                title = "U",
                linkType = LinkType.SAVED_LINK,
                idOfLinkedFolder = null,
                imgURL = "",
                note = "",
                lastModified = 0L,
                userAgent = "",
            ),
        )

        linksDao.addANewLink(
            Link(
                url = "https://hist.com",
                title = "H1",
                linkType = LinkType.HISTORY_LINK,
                idOfLinkedFolder = null,
                imgURL = "",
                note = "",
                lastModified = 0L,
                userAgent = "",
            ),
        )
        linksDao.addANewLink(
            Link(
                url = "https://hist.com",
                title = "H2",
                linkType = LinkType.HISTORY_LINK,
                idOfLinkedFolder = null,
                imgURL = "",
                note = "",
                lastModified = 0L,
                userAgent = "",
            ),
        )

        localLinksRepo.deleteDuplicateLinks(viaSocket = true).toList()

        val allLinks = linksDao.getAllLinks()
        val savedLinks = allLinks.filter { it.linkType == LinkType.SAVED_LINK }
        val historyLinks = allLinks.filter { it.linkType == LinkType.HISTORY_LINK }

        assertEquals(
            2,
            savedLinks.size,
            "Expected exactly 1 duplicate purged and 1 unique retained in SAVED",
        )
        assertEquals(1, savedLinks.filter { it.url == "https://dup.com" }.size)

        assertEquals(1, historyLinks.size, "Expected exactly 1 duplicate purged in HISTORY")
        assertEquals(1, historyLinks.filter { it.url == "https://hist.com" }.size)
    }
}
