package com.sakethh.linkora.data.local

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.data.local.repository.LocalTagsRepoImpl
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteTagsRepo
import com.sakethh.linkora.utils.canPushToServer
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocalTagsRepoImplTest {

    private lateinit var database: LocalDatabase
    private lateinit var tagsDao: TagsDao

    private lateinit var remoteTagsRepo: RemoteTagsRepo
    private lateinit var preferencesRepository: PreferencesRepository
    private lateinit var pendingSyncQueueRepo: PendingSyncQueueRepo

    private lateinit var localTagsRepo: LocalTagsRepo
    private lateinit var linksDao: LinksDao

    @BeforeTest
    fun setup() {
        clearAllMocks()

        database = Room.inMemoryDatabaseBuilder<LocalDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()

        tagsDao = database.tagsDao
        linksDao = database.linksDao

        remoteTagsRepo = mockk<RemoteTagsRepo>(relaxed = true)
        preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
        pendingSyncQueueRepo = mockk<PendingSyncQueueRepo>(relaxed = true)

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

        localTagsRepo = LocalTagsRepoImpl(
            tagsDao = tagsDao,
            remoteTagsRepo = remoteTagsRepo,
            preferencesRepository = preferencesRepository,
            pendingSyncQueueRepo = pendingSyncQueueRepo
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
    fun `creating a tag locally successfully executes remote call and updates local DB`() =
        runTest {
            val newTag = Tag(localId = 0, name = "Android", lastModified = 0L)

            localTagsRepo.createATag(newTag, viaSocket = false).toList()

            val dbTags = tagsDao.getAllTagsAsList()
            assertEquals(1, dbTags.size)
            assertEquals("Android", dbTags.first().name)

            coVerify(exactly = 1) { remoteTagsRepo.createATag(match { it.name == "Android" }) }
        }

    @Test
    fun `viaSocket flag entirely bypasses remote repo execution during tag creation`() = runTest {
        val newTag = Tag(localId = 0, name = "Kotlin", lastModified = 0L)

        localTagsRepo.createATag(newTag, viaSocket = true).toList()

        val dbTags = tagsDao.getAllTagsAsList()
        assertEquals(1, dbTags.size)
        assertEquals("Kotlin", dbTags.first().name)

        coVerify(exactly = 0) { remoteTagsRepo.createATag(any()) }
        coVerify(exactly = 0) { pendingSyncQueueRepo.addInQueue(any()) }
    }

    @Test
    fun `network failure during remote tag creation explicitly captures dto to pending sync queue`() =
        runTest {
            coEvery { remoteTagsRepo.createATag(any()) } throws RuntimeException("Network Timeout")

            val newTag = Tag(localId = 0, name = "OfflineTag", lastModified = 0L)

            localTagsRepo.createATag(newTag, viaSocket = false).toList()

            coVerify(exactly = 1) {
                pendingSyncQueueRepo.addInQueue(match { queueItem ->
                    queueItem.operation == "CREATE_TAG" && queueItem.payload.contains("OfflineTag")
                })
            }
        }

    @Test
    fun `attempting to rename or delete an unsynced tag with null remoteId throws local validation exception`() =
        runTest {
            // Tag inserted with NO remoteId
            val tagId = tagsDao.createATag(
                Tag(
                    localId = 0,
                    name = "Unsynced",
                    lastModified = 0L,
                    remoteId = null
                )
            )

            val renameError = executeAndGetErrorMessage {
                localTagsRepo.renameATag(tagId, "NewName", viaSocket = false).toList()
            }

            val deleteError = executeAndGetErrorMessage {
                localTagsRepo.deleteATag(tagId, viaSocket = false).toList()
            }

            assertTrue(
                renameError.contains(
                    "Failed requirement",
                    ignoreCase = true
                ) || renameError.contains("null", ignoreCase = true),
                "Expected require() to fail for rename with null remoteId, got: $renameError"
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
    fun `renaming a synced tag correctly updates local DB and pushes changes`() = runTest {
        val tagId = tagsDao.createATag(Tag(localId = 0, name = "OldName", lastModified = 0L))
        tagsDao.updateRemoteId(tagId, 999L) // Manually sync it

        localTagsRepo.renameATag(tagId, "NewName", viaSocket = false).toList()

        val updatedTag = tagsDao.getATag(tagId)
        assertEquals("NewName", updatedTag.name)

        coVerify(exactly = 1) {
            remoteTagsRepo.renameATag(match { it.id == 999L && it.newName == "NewName" })
        }
    }

    @Test
    fun `deleting a synced tag correctly drops it from local DB and pushes changes`() = runTest {
        val tagId = tagsDao.createATag(Tag(localId = 0, name = "ToDelete", lastModified = 0L))
        tagsDao.updateRemoteId(tagId, 888L) // Manually sync it

        localTagsRepo.deleteATag(tagId, viaSocket = false).toList()

        val allTags = tagsDao.getAllTagsAsList()
        assertTrue(allTags.isEmpty(), "Expected tag to be completely deleted from local DB")

        coVerify(exactly = 1) {
            remoteTagsRepo.deleteATag(match { it.id == 888L })
        }
    }

    @Test
    fun `getTagsForLinksAsMap aggregates deeply nested cross table results into correct mapped collections`() =
        runTest {
            val link1Id = linksDao.addANewLink(
                Link(
                    url = "https://l1.com",
                    title = "L1",
                    linkType = LinkType.SAVED_LINK,
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = ""
                )
            )
            val link2Id = linksDao.addANewLink(
                Link(
                    url = "https://l2.com",
                    title = "L2",
                    linkType = LinkType.SAVED_LINK,
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0L,
                    userAgent = ""
                )
            )

            val tag1Id = tagsDao.createATag(Tag(localId = 0, name = "Tag1", lastModified = 0L))
            val tag2Id = tagsDao.createATag(Tag(localId = 0, name = "Tag2", lastModified = 0L))
            val tag3Id = tagsDao.createATag(Tag(localId = 0, name = "Tag3", lastModified = 0L))

            // Link 1 has Tag 1 and Tag 2
            tagsDao.createLinkTags(
                listOf(
                    LinkTag(linkId = link1Id, tagId = tag1Id),
                    LinkTag(linkId = link1Id, tagId = tag2Id)
                )
            )

            // Link 2 has Tag 3 only
            tagsDao.createLinkTags(
                listOf(
                    LinkTag(linkId = link2Id, tagId = tag3Id)
                )
            )

            val linksMap = localTagsRepo.getTagsForLinksAsMap(listOf(link1Id, link2Id))

            assertEquals(2, linksMap[link1Id]?.size, "Link 1 should have exactly 2 tags")
            assertEquals(linksMap[link1Id]?.any { it.name == "Tag1" }, true)
            assertEquals(linksMap[link1Id]?.any { it.name == "Tag2" }, true)

            assertEquals(1, linksMap[link2Id]?.size, "Link 2 should have exactly 1 tag")
            assertEquals(linksMap[link2Id]?.any { it.name == "Tag3" }, true)
        }
}