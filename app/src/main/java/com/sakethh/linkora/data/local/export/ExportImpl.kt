package com.sakethh.linkora.data.local.export

import android.os.Build
import android.os.Environment
import com.sakethh.linkora.data.local.folders.FoldersRepo
import com.sakethh.linkora.data.local.links.LinksRepo
import com.sakethh.linkora.data.local.panels.PanelsRepo
import com.sakethh.linkora.data.models.ExportSchema
import com.sakethh.linkora.utils.LinkoraExports
import com.sakethh.linkora.utils.linkoraLog
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

class ExportImpl @Inject constructor(
    private val linksRepo: LinksRepo,
    private val foldersRepo: FoldersRepo,
    private val panelsRepo: PanelsRepo
) : ExportRepo {
    override suspend fun exportToAFile(exportInHTMLFormat: Boolean) = coroutineScope {
        ExportRequestInfo.updateState(ExportRequestState.GATHERING_DATA)
        val linksTableData = async {
            linksRepo.getAllFromLinksTable()
        }
        val impLinksTableData = async {
            linksRepo.getAllImpLinks()
        }
        val foldersData = async {
            foldersRepo.getAllFolders()
        }
        val archiveLinksData = async {
            linksRepo.getAllArchiveLinks()
        }
        val historyLinksData = async {
            linksRepo.getAllRecentlyVisitedLinks()
        }

        val panelsData = async {
            panelsRepo.getAllThePanelsAsAList()
        }

        val panelFoldersData = async {
            panelsRepo.getAllThePanelFoldersAsAList()
        }

        val defaultFolder = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            File(Environment.getExternalStorageDirectory(), "Linkora/Exports")
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Linkora/Exports"
            )
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !defaultFolder.exists()) {
            File(Environment.getExternalStorageDirectory(), "Linkora/Exports").mkdirs()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !defaultFolder.exists()) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "Linkora/Exports"
            ).mkdirs()
        }
        val file = File(
            defaultFolder, "LinkoraExport-${
                DateFormat.getDateTimeInstance().format(Date()).replace(":", "").replace(" ", "")
            }.${if (exportInHTMLFormat) "html" else "json"}"
        )
        val linksTable = linksTableData.await()
        val importantLinksTable = impLinksTableData.await()
        val foldersTable = foldersData.await()
        val archivedLinksTable = archiveLinksData.await()
        val historyLinksTable = historyLinksData.await()
        val exportPanels = panelsData.await()
        val exportPanelFolders = panelFoldersData.await()

        ExportRequestInfo.updateState(ExportRequestState.WRITING_TO_THE_FILE)

        if (!exportInHTMLFormat) {
            file.writeText(
                Json.encodeToString(
                    ExportSchema(
                        schemaVersion = 11,
                        linksTable = linksTable,
                        importantLinksTable = importantLinksTable,
                        foldersTable = foldersTable,
                        archivedLinksTable = archivedLinksTable,
                        archivedFoldersTable = emptyList(),
                        historyLinksTable = historyLinksTable,
                        panels = exportPanels,
                        panelFolders = exportPanelFolders,
                    )
                )
            )
        } else {
            var htmlFileRawText = ""

            // Saved Links :
            var savedLinksSection = dtH3(LinkoraExports.SAVED_LINKS__LINKORA_EXPORTS.name)

            var savedLinks = ""
            linksTable.filter { it.isLinkedWithSavedLinks }.forEach { savedLink ->
                savedLinks += dtA(linkTitle = savedLink.title, link = savedLink.webURL)
            }

            savedLinksSection += dlP(savedLinks)
            htmlFileRawText += savedLinksSection


            // Important Links :
            var impLinksSection = dtH3(LinkoraExports.IMPORTANT_LINKS__LINKORA_EXPORTS.name)

            var impLinks = ""
            importantLinksTable.forEach { impLink ->
                impLinks += dtA(linkTitle = impLink.title, link = impLink.webURL)
            }

            impLinksSection += dlP(impLinks)
            htmlFileRawText += impLinksSection


            // Regular Folders :
            htmlFileRawText += dtH3(LinkoraExports.REGULAR_FOLDERS__LINKORA_EXPORTS.name) + dlP(
                foldersSectionInHtml(
                    parentFolderId = null,
                    forArchiveFolders = false
                )
            )

            // Archived Folders :
            htmlFileRawText += dtH3(LinkoraExports.ARCHIVED_FOLDERS__LINKORA_EXPORTS.name) + dlP(
                foldersSectionInHtml(
                    parentFolderId = null,
                    forArchiveFolders = true
                )
            )


            // History Links :
            var historyLinksSection = dtH3(LinkoraExports.HISTORY_LINKS__LINKORA_EXPORTS.name)

            var historyLinks = ""
            historyLinksTable.forEach { historyLink ->
                historyLinks += dtA(linkTitle = historyLink.title, link = historyLink.webURL)
            }

            historyLinksSection += dlP(historyLinks)
            htmlFileRawText += historyLinksSection


            // Archived Links :
            var archivedLinksSection = dtH3(LinkoraExports.ARCHIVED_LINKS__LINKORA_EXPORTS.name)

            var archivedLinks = ""
            archivedLinksTable.forEach { archivedLink ->
                archivedLinks += dtA(linkTitle = archivedLink.title, link = archivedLink.webURL)
            }

            archivedLinksSection += dlP(archivedLinks)
            htmlFileRawText += archivedLinksSection

            // Result :
            linkoraLog(dlP(htmlFileRawText))

            file.writeText(dlP(htmlFileRawText))
        }

        ExportRequestInfo.updateState(ExportRequestState.IDLE)
    }

    private fun dlP(children: String): String {
        return "<DL><p>\n$children</DL><p>\n"
    }

    private fun dtH3(folderName: String): String {
        return "<DT><H3>$folderName</H3>\n"
    }

    private fun dtA(linkTitle: String, link: String): String {
        return "<DT><A HREF=\"$link\">$linkTitle</A>\n"
    }


    private suspend fun foldersSectionInHtml(
        parentFolderId: Long?, forArchiveFolders: Boolean
    ): String {
        var foldersSection = ""
        if (parentFolderId == null) {
            if (forArchiveFolders) {
                foldersRepo.getAllArchiveFoldersV10AsList()
            } else {
                foldersRepo.getAllRootFoldersList()
            }
        } else {
            foldersRepo.getChildFoldersOfThisParentIDAsList(parentFolderId)
        }.forEach { childFolder ->
            val currentFolderDTH3 = dtH3(childFolder.folderName)
            var folderLinksDTA = ""
            linksRepo.getLinksOfThisFolderAsList(childFolder.id).forEach { filteredLink ->
                folderLinksDTA += dtA(linkTitle = filteredLink.title, link = filteredLink.webURL)
            }
            val nestedFolderHTML = foldersSectionInHtml(childFolder.id, forArchiveFolders)
            foldersSection += currentFolderDTH3 + dlP(folderLinksDTA + nestedFolderHTML)
        }
        return foldersSection
    }
}