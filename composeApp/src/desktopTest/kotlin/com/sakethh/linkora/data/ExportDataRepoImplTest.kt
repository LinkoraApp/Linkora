package com.sakethh.linkora.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.LinkoraExports
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportDataRepoImplTest {

    private lateinit var database: LocalDatabase
    private lateinit var foldersDao: FoldersDao
    private lateinit var linksDao: LinksDao

    private lateinit var localLinksRepo: LocalLinksRepo
    private lateinit var localFoldersRepo: LocalFoldersRepo
    private lateinit var localPanelsRepo: LocalPanelsRepo
    private lateinit var localTagsRepo: LocalTagsRepo

    private lateinit var exportDataRepo: ExportDataRepoImpl

    @BeforeTest
    fun setup() {
        clearAllMocks()
        database = Room.inMemoryDatabaseBuilder<LocalDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()

        foldersDao = database.foldersDao
        linksDao = database.linksDao

        localLinksRepo = mockk<LocalLinksRepo>(relaxed = true)
        localFoldersRepo = mockk<LocalFoldersRepo>(relaxed = true)
        localPanelsRepo = mockk<LocalPanelsRepo>(relaxed = true)
        localTagsRepo = mockk<LocalTagsRepo>(relaxed = true)

        coEvery { localLinksRepo.getAllLinks() } coAnswers { linksDao.getAllLinks() }
        coEvery { localFoldersRepo.getAllFoldersAsList() } coAnswers { foldersDao.getAllFoldersAsList() }
        coEvery { localLinksRepo.getLinksOfThisFolderAsList(any()) } coAnswers {
            linksDao.getLinksOfThisFolderAsList(
                firstArg()
            )
        }
        coEvery { localFoldersRepo.getChildFoldersOfThisParentIDAsList(any()) } coAnswers {
            foldersDao.getChildFoldersAsList(
                firstArg()
            )
        }
        coEvery { localFoldersRepo.getAllRootFoldersAsList() } coAnswers { foldersDao.getAllRootFoldersAsList() }

        exportDataRepo = ExportDataRepoImpl(
            localLinksRepo,
            localFoldersRepo,
            localPanelsRepo,
            localTagsRepo
        )
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    private fun extractFolderBlock(html: String, folderName: String): String {
        val startHeader = "<DT><H3>$folderName</H3>"
        val startIndex = html.indexOf(startHeader)
        if (startIndex == -1) return ""

        val blockStart = html.indexOf("<DL><p>", startIndex)
        if (blockStart == -1) return ""

        var dlCount = 0
        var i = blockStart
        while (i < html.length) {
            if (html.startsWith("<DL><p>", i)) {
                dlCount++
                i += 7
            } else if (html.startsWith("</DL><p>", i)) {
                dlCount--
                i += 8
                if (dlCount == 0) {
                    return html.substring(startIndex, i)
                }
            } else {
                i++
            }
        }
        return ""
    }

    @Test
    fun `html export nests child folders inside parent folders up to depth 4`() = runTest {
        val f1Id = foldersDao.insertANewFolder(
            Folder(
                name = "L1",
                note = "",
                parentFolderId = null,
                isArchived = false,
                lastModified = 0
            )
        )
        val f2Id = foldersDao.insertANewFolder(
            Folder(
                name = "L2",
                note = "",
                parentFolderId = f1Id,
                isArchived = false,
                lastModified = 0
            )
        )
        val f3Id = foldersDao.insertANewFolder(
            Folder(
                name = "L3",
                note = "",
                parentFolderId = f2Id,
                isArchived = false,
                lastModified = 0
            )
        )
        val f4Id = foldersDao.insertANewFolder(
            Folder(
                name = "L4",
                note = "",
                parentFolderId = f3Id,
                isArchived = false,
                lastModified = 0
            )
        )

        linksDao.addANewLink(
            Link(
                linkType = LinkType.FOLDER_LINK,
                title = "Link1",
                url = "http://1.com",
                idOfLinkedFolder = f1Id,
                lastModified = 0,
                userAgent = "",
                note = "",
                imgURL = ""
            )
        )
        linksDao.addANewLink(
            Link(
                linkType = LinkType.FOLDER_LINK,
                title = "Link2",
                url = "http://2.com",
                idOfLinkedFolder = f2Id,
                lastModified = 0,
                userAgent = "",
                note = "",
                imgURL = ""
            )
        )
        linksDao.addANewLink(
            Link(
                linkType = LinkType.FOLDER_LINK,
                title = "Link4",
                url = "http://4.com",
                idOfLinkedFolder = f4Id,
                lastModified = 0,
                userAgent = "",
                note = "",
                imgURL = ""
            )
        )

        val results = exportDataRepo.rawExportDataAsHTML().toList()
        val htmlOutput = (results.last() as Result.Success).data

        val l1Block = extractFolderBlock(htmlOutput, "L1")
        assertTrue(l1Block.isNotEmpty())
        assertTrue(l1Block.contains("http://1.com"))
        assertTrue(l1Block.contains("<DT><H3>L2</H3>"))

        val l2Block = extractFolderBlock(l1Block, "L2")
        assertTrue(l2Block.isNotEmpty())
        assertTrue(l2Block.contains("http://2.com"))
        assertFalse(l2Block.contains("http://1.com"))
        assertTrue(l2Block.contains("<DT><H3>L3</H3>"))

        val l3Block = extractFolderBlock(l2Block, "L3")
        assertTrue(l3Block.isNotEmpty())
        assertFalse(l3Block.contains("http://1.com"))
        assertFalse(l3Block.contains("http://2.com"))
        assertTrue(l3Block.contains("<DT><H3>L4</H3>"))

        val l4Block = extractFolderBlock(l3Block, "L4")
        assertTrue(l4Block.isNotEmpty())
        assertTrue(l4Block.contains("http://4.com"))
    }

    @Test
    fun `html export isolates root links to prevent leakage into unrelated folders`() = runTest {
        val rootAId = foldersDao.insertANewFolder(
            Folder(
                name = "RootA",
                note = "",
                parentFolderId = null,
                isArchived = false,
                lastModified = 0
            )
        )
        val rootBId = foldersDao.insertANewFolder(
            Folder(
                name = "RootB",
                note = "",
                parentFolderId = null,
                isArchived = false,
                lastModified = 0
            )
        )

        linksDao.addANewLink(
            Link(
                linkType = LinkType.FOLDER_LINK,
                title = "LinkA",
                url = "http://a.com",
                idOfLinkedFolder = rootAId,
                lastModified = 0,
                userAgent = "",
                note = "",
                imgURL = ""
            )
        )
        linksDao.addANewLink(
            Link(
                linkType = LinkType.FOLDER_LINK,
                title = "LinkB",
                url = "http://b.com",
                idOfLinkedFolder = rootBId,
                lastModified = 0,
                userAgent = "",
                note = "",
                imgURL = ""
            )
        )

        val results = exportDataRepo.rawExportDataAsHTML().toList()
        val htmlOutput = (results.last() as Result.Success).data

        val rootABlock = extractFolderBlock(htmlOutput, "RootA")
        val rootBBlock = extractFolderBlock(htmlOutput, "RootB")

        assertTrue(rootABlock.contains("http://a.com"))
        assertFalse(rootABlock.contains("http://b.com"))

        assertTrue(rootBBlock.contains("http://b.com"))
        assertFalse(rootBBlock.contains("http://a.com"))
    }

    @Test
    fun `html export drops orphaned folders with invalid parent ids`() = runTest {
        foldersDao.insertANewFolder(
            Folder(
                name = "ValidRoot",
                note = "",
                parentFolderId = null,
                isArchived = false,
                lastModified = 0
            )
        )
        // 9999L parent does not exist, so it is not a root folder, nor a valid child.
        foldersDao.insertANewFolder(
            Folder(
                name = "OrphanFolder",
                note = "",
                parentFolderId = 9999L,
                isArchived = false,
                lastModified = 0
            )
        )

        val results = exportDataRepo.rawExportDataAsHTML().toList()
        val htmlOutput = (results.last() as Result.Success).data

        assertTrue(htmlOutput.contains("ValidRoot"))
        assertFalse(htmlOutput.contains("OrphanFolder"))
    }

    @Test
    fun `html export drops links connected to non existent folders`() = runTest {
        linksDao.addANewLink(
            Link(
                linkType = LinkType.SAVED_LINK,
                title = "ValidSaved",
                url = "http://saved.com",
                idOfLinkedFolder = Constants.SAVED_LINKS_ID,
                lastModified = 0,
                userAgent = "",
                note = "",
                imgURL = ""
            )
        )
        // 9999L linked folder does not exist.
        linksDao.addANewLink(
            Link(
                linkType = LinkType.FOLDER_LINK,
                title = "OrphanLink",
                url = "http://orphan.com",
                idOfLinkedFolder = 9999L,
                lastModified = 0,
                userAgent = "",
                note = "",
                imgURL = ""
            )
        )

        val results = exportDataRepo.rawExportDataAsHTML().toList()
        val htmlOutput = (results.last() as Result.Success).data

        assertTrue(htmlOutput.contains("http://saved.com"))
        assertFalse(htmlOutput.contains("http://orphan.com"))
    }

    @Test
    fun `html export safely processes empty database without crashing`() = runTest {
        val results = exportDataRepo.rawExportDataAsHTML().toList()
        val htmlOutput = (results.last() as Result.Success).data

        assertTrue(htmlOutput.contains(LinkoraExports.SAVED_LINKS__LINKORA_EXPORT.name))
        assertTrue(htmlOutput.contains(LinkoraExports.IMPORTANT_LINKS__LINKORA_EXPORT.name))
        assertTrue(htmlOutput.contains(LinkoraExports.REGULAR_FOLDERS__LINKORA_EXPORT.name))
    }

    @Test
    fun `html export correctly separates archived folders from regular folders into distinct overarching blocks`() =
        runTest {
            foldersDao.insertANewFolder(
                Folder(
                    name = "RegularRoot",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0
                )
            )
            foldersDao.insertANewFolder(
                Folder(
                    name = "ArchivedRoot",
                    note = "",
                    parentFolderId = null,
                    isArchived = true,
                    lastModified = 0
                )
            )

            val results = exportDataRepo.rawExportDataAsHTML().toList()
            val htmlOutput = (results.last() as Result.Success).data

            val regularSectionBlock =
                extractFolderBlock(htmlOutput, LinkoraExports.REGULAR_FOLDERS__LINKORA_EXPORT.name)
            val archivedSectionBlock =
                extractFolderBlock(htmlOutput, LinkoraExports.ARCHIVED_FOLDERS__LINKORA_EXPORT.name)

            assertTrue(regularSectionBlock.contains("RegularRoot"))
            assertFalse(regularSectionBlock.contains("ArchivedRoot"))

            assertTrue(archivedSectionBlock.contains("ArchivedRoot"))
            assertFalse(archivedSectionBlock.contains("RegularRoot"))
        }

    @Test
    fun `html export categorizes predefined default link types seamlessly into exact top level sections`() =
        runTest {
            linksDao.addANewLink(
                Link(
                    linkType = LinkType.SAVED_LINK,
                    title = "Saved",
                    url = "http://saved.com",
                    idOfLinkedFolder = Constants.SAVED_LINKS_ID,
                    lastModified = 0,
                    userAgent = "",
                    note = "",
                    imgURL = ""
                )
            )
            linksDao.addANewLink(
                Link(
                    linkType = LinkType.IMPORTANT_LINK,
                    title = "Important",
                    url = "http://important.com",
                    idOfLinkedFolder = Constants.IMPORTANT_LINKS_ID,
                    lastModified = 0,
                    userAgent = "",
                    note = "",
                    imgURL = ""
                )
            )
            linksDao.addANewLink(
                Link(
                    linkType = LinkType.HISTORY_LINK,
                    title = "History",
                    url = "http://history.com",
                    idOfLinkedFolder = Constants.HISTORY_ID,
                    lastModified = 0,
                    userAgent = "",
                    note = "",
                    imgURL = ""
                )
            )
            linksDao.addANewLink(
                Link(
                    linkType = LinkType.ARCHIVE_LINK,
                    title = "Archive",
                    url = "http://archive.com",
                    idOfLinkedFolder = Constants.ARCHIVE_ID,
                    lastModified = 0,
                    userAgent = "",
                    note = "",
                    imgURL = ""
                )
            )

            val results = exportDataRepo.rawExportDataAsHTML().toList()
            val htmlOutput = (results.last() as Result.Success).data

            assertTrue(
                extractFolderBlock(
                    htmlOutput,
                    LinkoraExports.SAVED_LINKS__LINKORA_EXPORT.name
                ).contains("http://saved.com")
            )
            assertTrue(
                extractFolderBlock(
                    htmlOutput,
                    LinkoraExports.IMPORTANT_LINKS__LINKORA_EXPORT.name
                ).contains("http://important.com")
            )
            assertTrue(
                extractFolderBlock(
                    htmlOutput,
                    LinkoraExports.HISTORY_LINKS__LINKORA_EXPORT.name
                ).contains("http://history.com")
            )
            assertTrue(
                extractFolderBlock(
                    htmlOutput,
                    LinkoraExports.ARCHIVED_LINKS__LINKORA_EXPORT.name
                ).contains("http://archive.com")
            )
        }

    @Test
    fun `json export accurately maintains parent ids and linked folder ids mapping across deep hierarchies`() =
        runTest {
            val f1Id = foldersDao.insertANewFolder(
                Folder(
                    name = "Root",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0
                )
            )
            val f2Id = foldersDao.insertANewFolder(
                Folder(
                    name = "Child",
                    note = "",
                    parentFolderId = f1Id,
                    isArchived = false,
                    lastModified = 0
                )
            )

            linksDao.addANewLink(
                Link(
                    linkType = LinkType.FOLDER_LINK,
                    title = "Link2",
                    url = "http://2.com",
                    idOfLinkedFolder = f2Id,
                    lastModified = 0,
                    userAgent = "",
                    note = "",
                    imgURL = ""
                )
            )

            val results = exportDataRepo.rawExportDataAsJSON().toList()
            val rawJsonString = (results.last() as Result.Success).data
            val parsedJson = Json.decodeFromString<JSONExportSchema>(rawJsonString)

            assertEquals(JSONExportSchema.VERSION, parsedJson.schemaVersion)

            val exportedRoot = parsedJson.folders.find { it.name == "Root" }
            val exportedChild = parsedJson.folders.find { it.name == "Child" }
            val exportedLink = parsedJson.links.find { it.url == "http://2.com" }

            assertTrue(exportedRoot != null)
            assertTrue(exportedChild != null)
            assertTrue(exportedLink != null)

            assertEquals(null, exportedRoot.parentFolderId)
            assertEquals(exportedRoot.localId, exportedChild.parentFolderId)
            assertEquals(exportedChild.localId, exportedLink.idOfLinkedFolder)
        }

    @Test
    fun `json export strips out remote ids preventing cross contamination across instances`() =
        runTest {
            val f1Id = foldersDao.insertANewFolder(
                Folder(
                    name = "Root",
                    note = "",
                    parentFolderId = null,
                    isArchived = false,
                    lastModified = 0,
                    remoteId = 999L
                )
            )
            linksDao.addANewLink(
                Link(
                    linkType = LinkType.FOLDER_LINK,
                    title = "Link",
                    url = "http://a.com",
                    idOfLinkedFolder = f1Id,
                    lastModified = 0,
                    userAgent = "",
                    note = "",
                    imgURL = "",
                    remoteId = 888L
                )
            )

            val results = exportDataRepo.rawExportDataAsJSON().toList()
            val rawJsonString = (results.last() as Result.Success).data
            val parsedJson = Json.decodeFromString<JSONExportSchema>(rawJsonString)

            assertEquals(null, parsedJson.folders.first().remoteId)
            assertEquals(null, parsedJson.links.first().remoteId)
        }

    @Test
    fun `json export successfully serializes empty database`() = runTest {
        val results = exportDataRepo.rawExportDataAsJSON().toList()
        val rawJsonString = (results.last() as Result.Success).data
        val parsedJson = Json.decodeFromString<JSONExportSchema>(rawJsonString)

        assertEquals(JSONExportSchema.VERSION, parsedJson.schemaVersion)
        assertTrue(parsedJson.links.isEmpty())
        assertTrue(parsedJson.folders.isEmpty())
        assertTrue(parsedJson.panels.panels.isEmpty())
        assertTrue(parsedJson.tags.isEmpty())
    }
}