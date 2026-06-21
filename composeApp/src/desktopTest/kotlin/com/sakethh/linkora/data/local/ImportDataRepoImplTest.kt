package com.sakethh.linkora.data.local

import androidx.room3.Room
import androidx.room3.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.ImportDataRepoImpl
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.PanelsDao
import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.PanelForJSONExportSchema
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.utils.Constants
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportDataRepoImplTest {

    private lateinit var database: LocalDatabase
    private lateinit var foldersDao: FoldersDao
    private lateinit var linksDao: LinksDao
    private lateinit var panelsDao: PanelsDao
    private lateinit var tagsDao: TagsDao

    private lateinit var localLinksRepo: LocalLinksRepo
    private lateinit var localFoldersRepo: LocalFoldersRepo
    private lateinit var localPanelsRepo: LocalPanelsRepo
    private lateinit var localTagsRepo: LocalTagsRepo
    private lateinit var remoteSyncRepo: RemoteSyncRepo
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var importDataRepo: ImportDataRepoImpl
    private var pushToServerAllowed = false

    @BeforeTest
    fun setup() {
        clearAllMocks()
        pushToServerAllowed = false

        database = Room.inMemoryDatabaseBuilder<LocalDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()

        foldersDao = database.foldersDao
        linksDao = database.linksDao
        panelsDao = database.panelsDao
        tagsDao = database.tagsDao

        localLinksRepo = mockk<LocalLinksRepo>(relaxed = true) {
            coEvery { addMultipleLinks(any()) } coAnswers {
                linksDao.addMultipleLinks(firstArg())
            }
        }

        localFoldersRepo = mockk<LocalFoldersRepo>(relaxed = true) {
            coEvery { getLatestFoldersTableID() } coAnswers {
                try {
                    foldersDao.getLatestFoldersTableID()
                } catch (e: Exception) {
                    0L
                }
            }
            coEvery { insertANewFolderLocally(any()) } coAnswers {
                foldersDao.insertANewFolder(firstArg())
            }
        }

        localPanelsRepo = mockk<LocalPanelsRepo>(relaxed = true) {
            coEvery { getLatestPanelID() } coAnswers {
                try {
                    panelsDao.getLatestPanelID()
                } catch (e: Exception) {
                    0L
                }
            }
            coEvery { addANewPanelLocally(any()) } coAnswers {
                panelsDao.addANewPanel(firstArg())
            }
            coEvery { addMultiplePanelFolders(any()) } coAnswers {
                panelsDao.addMultiplePanelFolders(firstArg())
            }
        }

        localTagsRepo = mockk<LocalTagsRepo>(relaxed = true) {
            coEvery { createATagLocally(any()) } coAnswers {
                tagsDao.createATag(firstArg())
            }
            coEvery { createLinkTags(any()) } coAnswers {
                tagsDao.createLinkTags(firstArg())
            }
        }

        remoteSyncRepo = mockk<RemoteSyncRepo>(relaxed = true)
        preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
        every { preferencesRepository.getPreferences().primaryJsoupUserAgent } returns "TestAgent"

        importDataRepo = ImportDataRepoImpl(
            localLinksRepo = localLinksRepo,
            localFoldersRepo = localFoldersRepo,
            localPanelsRepo = localPanelsRepo,
            localTagsRepo = localTagsRepo,
            remoteSyncRepo = remoteSyncRepo,
            preferencesRepository = preferencesRepository,
            withWriterConnection = { block -> database.useWriterConnection { block(it) } },
            canPushToServer = { pushToServerAllowed }
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun `html import ignores empty elements and database remains empty`() = runTest {
        val emptyHtml = ""

        val results = importDataRepo.importDataFromHTML(emptyHtml).toList()

        assertTrue(results.last() is Result.Success)
        val links = linksDao.getAllLinks()
        assertTrue(links.isEmpty())
    }

    private suspend fun prePopulateSystemFolders() {
        val defaultIds = listOf(
            Constants.SAVED_LINKS_ID,
            Constants.IMPORTANT_LINKS_ID,
            Constants.HISTORY_ID,
            Constants.ARCHIVE_ID
        )
        defaultIds.forEach { id ->
            foldersDao.insertANewFolder(
                Folder(
                    localId = id,
                    name = "System Folder",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0L
                )
            )
        }
    }

    @Test
    fun `html import maps predefined folder names to specific link types without creating folders`() =
        runTest {
            prePopulateSystemFolders()

            val html = """
            <dl>
                <dt><h3>Saved Links</h3><dl><dt><a href="https://saved.com">Saved</a></dt></dl></dt>
                <dt><h3>Important Links</h3><dl><dt><a href="https://important.com">Important</a></dt></dl></dt>
            </dl>
        """.trimIndent()

            importDataRepo.importDataFromHTML(html).toList()

            val links = linksDao.getAllLinks()
            val folders = foldersDao.getAllFoldersAsList()

            assertEquals(2, links.size)
            assertEquals(6, folders.size)
        }

    @Test
    fun `html import correctly writes nested folders to database and assigns correct parent ids`() =
        runTest {
            prePopulateSystemFolders()

            val html = """
            <dl><dt><h3>Depth 1 Folder</h3><dl>
                <dt><a href="https://link1.com">Link 1</a></dt>
                <dt><h3>Depth 2 Folder</h3><dl>
                    <dt><a href="https://link2.com">Link 2</a></dt>
                </dl></dt>
            </dl></dt></dl>
        """.trimIndent()

            importDataRepo.importDataFromHTML(html).toList()

            val folders = foldersDao.getAllFoldersAsList()
            val links = linksDao.getAllLinks()

            // 4 system folders + 2 new nested folders
            assertEquals(6, folders.size)
            assertEquals(2, links.size)

            val depth1 = folders.find { it.name == "Depth 1 Folder" }
            val depth2 = folders.find { it.name == "Depth 2 Folder" }

            assertTrue(depth1 != null && depth1.parentFolderId == null)
            assertTrue(depth2 != null && depth2.parentFolderId == depth1.localId)

            assertTrue(links.any { it.url == "https://link1.com" && it.idOfLinkedFolder == depth1.localId })
            assertTrue(links.any { it.url == "https://link2.com" && it.idOfLinkedFolder == depth2.localId })
        }

    @Test
    fun `json import separates non folder links and writes them to database`() = runTest {
        val schema = JSONExportSchema(
            schemaVersion = JSONExportSchema.VERSION,
            links = listOf(
                Link(
                    localId = 10,
                    linkType = LinkType.SAVED_LINK,
                    url = "https://a.com",
                    title = "A",
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0,
                    userAgent = ""
                ),
                Link(
                    localId = 20,
                    linkType = LinkType.IMPORTANT_LINK,
                    url = "https://b.com",
                    title = "B",
                    idOfLinkedFolder = null,
                    imgURL = "",
                    note = "",
                    lastModified = 0,
                    userAgent = ""
                )
            ),
            folders = emptyList(),
            panels = PanelForJSONExportSchema(emptyList(), emptyList())
        )

        importDataRepo.importDataFromObj(schema).toList()

        val links = linksDao.getAllLinks()
        assertEquals(2, links.size)
        assertTrue(links.any { it.url == "https://a.com" })
        assertTrue(links.any { it.url == "https://b.com" })
    }

    @Test
    fun `json import recreates nested folder hierarchy in database and reassigns link ids`() =
        runTest {
            val schema = JSONExportSchema(
                schemaVersion = JSONExportSchema.VERSION,
                links = listOf(
                    Link(
                        localId = 1,
                        linkType = LinkType.FOLDER_LINK,
                        url = "https://1.com",
                        title = "1",
                        idOfLinkedFolder = 10,
                        imgURL = "",
                        note = "",
                        lastModified = 0,
                        userAgent = ""
                    )
                ),
                folders = listOf(
                    Folder(
                        localId = 10,
                        name = "D1",
                        parentFolderId = null,
                        note = "",
                        isArchived = false,
                        lastModified = 0
                    ),
                    Folder(
                        localId = 11,
                        name = "D2",
                        parentFolderId = 10,
                        note = "",
                        isArchived = false,
                        lastModified = 0
                    )
                ),
                panels = PanelForJSONExportSchema(emptyList(), emptyList())
            )

            importDataRepo.importDataFromObj(schema).toList()

            val folders = foldersDao.getAllFoldersAsList()
            val links = linksDao.getAllLinks()

            assertEquals(2, folders.size)
            assertEquals(1, links.size)

            val d1 = folders.find { it.name == "D1" }
            val d2 = folders.find { it.name == "D2" }

            assertTrue(d1 != null && d1.parentFolderId == null)
            assertTrue(d2 != null && d2.parentFolderId == d1.localId)
            assertEquals(links.first().idOfLinkedFolder, d1.localId)
        }

    @Test
    fun `json import maps tags to database and associates them with freshly inserted links`() =
        runTest {
            val schema = JSONExportSchema(
                schemaVersion = JSONExportSchema.VERSION,
                links = listOf(
                    Link(
                        localId = 50,
                        linkType = LinkType.SAVED_LINK,
                        url = "https://tagged.com",
                        title = "Tagged",
                        idOfLinkedFolder = null,
                        imgURL = "",
                        note = "",
                        lastModified = 0,
                        userAgent = ""
                    )
                ),
                folders = emptyList(),
                panels = PanelForJSONExportSchema(emptyList(), emptyList()),
                tags = listOf(Tag(localId = 5, name = "Test Tag")),
                linkTags = listOf(LinkTag(linkId = 50, tagId = 5))
            )

            importDataRepo.importDataFromObj(schema).toList()

            val dbTags = tagsDao.getAllTagsAsList()
            val dbLinkTags = tagsDao.getAllTagLinksAsList()
            val dbLinks = linksDao.getAllLinks()

            assertEquals(1, dbTags.size)
            assertEquals(1, dbLinkTags.size)
            assertEquals(dbTags.first().name, "Test Tag")
            assertEquals(dbLinkTags.first().tagId, dbTags.first().localId)
            assertEquals(dbLinkTags.first().linkId, dbLinks.first().localId)
        }

    @Test
    fun `json import maps panels and connects them to nested database folders correctly`() =
        runTest {
            val schema = JSONExportSchema(
                schemaVersion = JSONExportSchema.VERSION,
                links = emptyList(),
                folders = listOf(
                    Folder(
                        localId = 10,
                        name = "Panel Base",
                        parentFolderId = null,
                        note = "",
                        isArchived = false,
                        lastModified = 0
                    )
                ),
                panels = PanelForJSONExportSchema(
                    panels = listOf(Panel(localId = 5, panelName = "Test Panel")),
                    panelFolders = listOf(
                        PanelFolder(
                            localId = 1,
                            folderId = 10,
                            connectedPanelId = 5,
                            panelPosition = 0,
                            folderName = "Panel Base"
                        )
                    )
                )
            )

            importDataRepo.importDataFromObj(schema).toList()

            val dbPanels = panelsDao.getAllThePanelsAsAList()
            val dbPanelFolders = panelsDao.getAllThePanelFoldersAsAList()
            val dbFolders = foldersDao.getAllFoldersAsList()

            assertEquals(1, dbPanels.size)
            assertEquals(1, dbPanelFolders.size)
            assertEquals(dbPanels.first().panelName, "Test Panel")
            assertEquals(dbPanelFolders.first().folderId, dbFolders.first().localId)
            assertEquals(dbPanelFolders.first().connectedPanelId, dbPanels.first().localId)
        }

    @Test
    fun `remote push is executed only when server configuration allows it`() = runTest {
        pushToServerAllowed = true
        val html = "<dl></dl>"

        with(remoteSyncRepo) {
            coEvery { any<SendChannel<Result<Unit>>>().pushNonSyncedDataToServer() } returns Unit
        }

        importDataRepo.importDataFromHTML(html).toList()

        with(remoteSyncRepo) {
            coVerify(exactly = 1) { any<SendChannel<Result<Unit>>>().pushNonSyncedDataToServer() }
        }

        pushToServerAllowed = false
        importDataRepo.importDataFromHTML(html).toList()

        with(remoteSyncRepo) {
            coVerify(exactly = 1) { any<SendChannel<Result<Unit>>>().pushNonSyncedDataToServer() }
        }
    }
}