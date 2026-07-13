package com.sakethh.linkora.data

import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.RawExportString
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.PanelForJSONExportSchema
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.repository.ExportDataRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.utils.LinkoraExports
import com.sakethh.linkora.utils.catchAsExceptionAndEmitFailure
import com.sakethh.linkora.utils.getLocalizedString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json

class ExportDataRepoImpl(
    private val localLinksRepo: LocalLinksRepo,
    private val localFoldersRepo: LocalFoldersRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val localTagsRepo: LocalTagsRepo,
) : ExportDataRepo {
    override suspend fun rawExportDataAsJSON(): Flow<Result<RawExportString>> = flow {
        supervisorScope {
            emit(
                Result.Loading(
                    message = Localization.Key.PreparingToExportYourData.getLocalizedString(),
                ),
            )
            val deferredLinks = async {
                localLinksRepo.getAllLinks()
            }
            val deferredFolders = async {
                localFoldersRepo.getAllFoldersAsList()
            }
            val deferredPanels = async {
                localPanelsRepo.getAllThePanelsAsAList()
            }
            val deferredPanelFolders = async {
                localPanelsRepo.getAllThePanelFoldersAsAList()
            }
            val deferredTags = async {
                localTagsRepo.getAllTagsAsList()
            }
            val deferredLinkTags = async {
                localTagsRepo.getAllLinkTagsAsList()
            }
            emit(
                Result.Loading(
                    message = Localization.Key.CollectingLinksForExport.getLocalizedString(),
                ),
            )
            val links = deferredLinks.await()

            emit(
                Result.Loading(
                    message = Localization.Key.CollectingFoldersForExport.getLocalizedString(),
                ),
            )
            val folders = deferredFolders.await()

            emit(
                Result.Loading(
                    message = Localization.Key.CollectingPanelsForExport.getLocalizedString(),
                ),
            )
            val panels = deferredPanels.await()

            emit(
                Result.Loading(
                    message =
                    Localization.Key.CollectingPanelFoldersForExport.getLocalizedString(),
                ),
            )
            val panelFolders = deferredPanelFolders.await()

            emit(
                Result.Loading(
                    message =
                    Localization.Key.SerializingCollectedDataForExport.getLocalizedString(),
                ),
            )

            val tags = deferredTags.await()

            val linkTags = deferredLinkTags.await()

            val exportObject =
                JSONExportSchema(
                    schemaVersion = JSONExportSchema.VERSION,
                    links = links.map { it.copy(remoteId = null, lastModified = 0) },
                    folders = folders.map { it.copy(remoteId = null, lastModified = 0) },
                    panels =
                    PanelForJSONExportSchema(
                        panels = panels.map { it.copy(remoteId = null, lastModified = 0) },
                        panelFolders =
                        panelFolders.map {
                            it.copy(
                                remoteId = null,
                                lastModified = 0,
                            )
                        },
                    ),
                    tags = tags.map { it.copy(remoteId = null, lastModified = 0) },
                    linkTags = linkTags.map { it.copy(remoteId = null, lastModified = 0) },
                )

            val requiredRawExportString = Json.encodeToString(exportObject)
            emit(Result.Success(data = requiredRawExportString))
        }
    }
        .catchAsExceptionAndEmitFailure()

    private operator fun StringBuilder.plusAssign(string: String) {
        append(string)
    }

    private operator fun StringBuilder.plusAssign(stringBuilder: StringBuilder) {
        append(stringBuilder)
    }

    override suspend fun rawExportDataAsHTML(): Flow<Result<RawExportString>> = channelFlow<Result<RawExportString>> {
        supervisorScope {
            val htmlFileRawText = StringBuilder()

            send(Result.Loading(message = "Starting export data as HTML"))

            val allLinks =
                localLinksRepo.getAllLinks().groupBy {
                    it.linkType
                }

            send(Result.Loading(message = "Fetched all links, total: ${allLinks.size}"))

            val savedLinksSection =
                StringBuilder(dtH3(LinkoraExports.SAVED_LINKS__LINKORA_EXPORT.name))
            send(Result.Loading(message = "Processing saved links"))

            val savedLinks = StringBuilder()
            val deferredSavedLinks = async {
                allLinks[LinkType.SAVED_LINK]?.forEach { savedLink ->
                    savedLinks += dtA(linkTitle = savedLink.title, link = savedLink.url)
                    send(Result.Loading(message = "Processed saved link: ${savedLink.title}"))
                }
            }

            val impLinksSection =
                StringBuilder(dtH3(LinkoraExports.IMPORTANT_LINKS__LINKORA_EXPORT.name))
            send(Result.Loading(message = "Processing important links"))

            val impLinks = StringBuilder()
            val deferredImpLinks = async {
                allLinks[LinkType.IMPORTANT_LINK]?.forEach { impLink ->
                    impLinks += dtA(linkTitle = impLink.title, link = impLink.url)
                    send(Result.Loading(message = "Processed important link: ${impLink.title}"))
                }
            }

            send(Result.Loading(message = "Processing regular folders and respective links"))
            val deferredRegularFoldersAndRespectiveLinks = async {
                dtH3(LinkoraExports.REGULAR_FOLDERS__LINKORA_EXPORT.name) +
                    dlP(
                        foldersSectionInHtml(parentFolderId = null, forArchiveFolders = false),
                    )
            }

            send(Result.Loading(message = "Processing archived folders and respective links"))
            val deferredArchivedFoldersAndRespectiveLinks = async {
                dtH3(LinkoraExports.ARCHIVED_FOLDERS__LINKORA_EXPORT.name) +
                    dlP(
                        foldersSectionInHtml(parentFolderId = null, forArchiveFolders = true),
                    )
            }

            val historyLinksSection =
                StringBuilder(dtH3(LinkoraExports.HISTORY_LINKS__LINKORA_EXPORT.name))
            send(Result.Loading(message = "Processing history links"))

            val historyLinks = StringBuilder()
            val deferredHistoryLinks = async {
                allLinks[LinkType.HISTORY_LINK]?.forEach { historyLink ->
                    historyLinks +=
                        dtA(
                            linkTitle = historyLink.title,
                            link = historyLink.url,
                        )
                    send(Result.Loading(message = "Processed history link: ${historyLink.title}"))
                }
            }

            val archivedLinksSection =
                StringBuilder(dtH3(LinkoraExports.ARCHIVED_LINKS__LINKORA_EXPORT.name))
            send(Result.Loading(message = "Processing archived links"))

            val archivedLinks = StringBuilder()
            val deferredArchivedLinks = async {
                allLinks[LinkType.ARCHIVE_LINK]?.forEach { archivedLink ->
                    archivedLinks +=
                        dtA(
                            linkTitle = archivedLink.title,
                            link = archivedLink.url,
                        )
                    send(Result.Loading(message = "Processed archived link: ${archivedLink.title}"))
                }
            }

            deferredSavedLinks.await()
            savedLinksSection += dlP(savedLinks.toString())
            htmlFileRawText += savedLinksSection

            deferredImpLinks.await()
            impLinksSection += dlP(impLinks.toString())
            htmlFileRawText += impLinksSection

            htmlFileRawText += deferredRegularFoldersAndRespectiveLinks.await()

            htmlFileRawText += deferredArchivedFoldersAndRespectiveLinks.await()

            deferredHistoryLinks.await()
            historyLinksSection += dlP(historyLinks.toString())
            htmlFileRawText += historyLinksSection

            deferredArchivedLinks.await()
            archivedLinksSection += dlP(archivedLinks.toString())
            htmlFileRawText += archivedLinksSection

            send(Result.Loading(message = "Completed export process, finalizing HTML"))

            send(Result.Success(dlP(htmlFileRawText.toString())))
        }
    }
        .catchAsExceptionAndEmitFailure()

    override suspend fun rawExportDataAsHTML(
        links: List<Link>,
        folders: List<Folder>,
    ): RawExportString {
        val completableDeferred = CompletableDeferred<String>()
        val linksGroupedByType = links.groupBy {
            it.linkType
        }
        supervisorScope {
            val htmlFileRawText = StringBuilder()

            val savedLinksSection = StringBuilder(dtH3(LinkoraExports.SAVED_LINKS__LINKORA_EXPORT.name))

            val savedLinks = StringBuilder()
            val deferredSavedLinks = async {
                linksGroupedByType[LinkType.SAVED_LINK]?.forEach { savedLink ->
                    savedLinks += dtA(linkTitle = savedLink.title, link = savedLink.url)
                }
            }

            val impLinksSection = StringBuilder(dtH3(LinkoraExports.IMPORTANT_LINKS__LINKORA_EXPORT.name))

            val impLinks = StringBuilder()
            val deferredImpLinks = async {
                linksGroupedByType[LinkType.IMPORTANT_LINK]?.forEach { impLink ->
                    impLinks += dtA(linkTitle = impLink.title, link = impLink.url)
                }
            }

            val deferredRegularFoldersAndRespectiveLinks = async {
                dtH3(LinkoraExports.REGULAR_FOLDERS__LINKORA_EXPORT.name) +
                    dlP(
                        foldersSectionInHtml(
                            allFolders = folders,
                            allLinks = links,
                            forArchiveFolders = false,
                        ),
                    )
            }

            val deferredArchivedFoldersAndRespectiveLinks = async {
                dtH3(LinkoraExports.ARCHIVED_FOLDERS__LINKORA_EXPORT.name) +
                    dlP(
                        foldersSectionInHtml(
                            allFolders = folders,
                            allLinks = links,
                            forArchiveFolders = true,
                        ),
                    )
            }

            val historyLinksSection =
                StringBuilder(dtH3(LinkoraExports.HISTORY_LINKS__LINKORA_EXPORT.name))

            val historyLinks = StringBuilder()
            val deferredHistoryLinks = async {
                linksGroupedByType[LinkType.HISTORY_LINK]?.forEach { historyLink ->
                    historyLinks +=
                        dtA(
                            linkTitle = historyLink.title,
                            link = historyLink.url,
                        )
                }
            }

            val archivedLinksSection =
                StringBuilder(dtH3(LinkoraExports.ARCHIVED_LINKS__LINKORA_EXPORT.name))

            val archivedLinks = StringBuilder()
            val deferredArchivedLinks = async {
                linksGroupedByType[LinkType.ARCHIVE_LINK]?.forEach { archivedLink ->
                    archivedLinks +=
                        dtA(
                            linkTitle = archivedLink.title,
                            link = archivedLink.url,
                        )
                }
            }

            deferredSavedLinks.await()
            savedLinksSection += dlP(savedLinks.toString())
            htmlFileRawText += savedLinksSection
            deferredImpLinks.await()
            impLinksSection += dlP(impLinks.toString())
            htmlFileRawText += impLinksSection

            htmlFileRawText += deferredRegularFoldersAndRespectiveLinks.await()

            htmlFileRawText += deferredArchivedFoldersAndRespectiveLinks.await()

            deferredHistoryLinks.await()
            historyLinksSection += dlP(historyLinks.toString())
            htmlFileRawText += historyLinksSection

            deferredArchivedLinks.await()
            archivedLinksSection += dlP(archivedLinks.toString())
            htmlFileRawText += archivedLinksSection

            completableDeferred.complete(dlP(htmlFileRawText.toString()))
        }
        return completableDeferred.await()
    }

    private suspend fun <T> SendChannel<Result<T>>.foldersSectionInHtml(
        parentFolderId: Long?,
        forArchiveFolders: Boolean,
    ): String {
        val foldersSection = StringBuilder()

        val foldersList =
            if (parentFolderId == null) {
                send(Result.Loading(message = "Fetching all top-level folders"))
                localFoldersRepo.getAllRootFoldersAsList().filter {
                    it.isArchived == forArchiveFolders
                }
            } else {
                send(Result.Loading(message = "Fetching child folders for parent ID: $parentFolderId"))
                localFoldersRepo.getChildFoldersOfThisParentIDAsList(parentFolderId)
            }

        send(Result.Loading(message = "Found ${foldersList.size} folders to process"))

        if (foldersList.isEmpty()) return ""

        val folderHtmlMap = mutableMapOf<Long, String>()

        val folderFrameDeque = ArrayDeque<FolderFrame>()

        foldersList.asReversed().forEach {
            folderFrameDeque.addLast(FolderFrame(it))
        }

        while (folderFrameDeque.isNotEmpty()) {
            val frame = folderFrameDeque.last()

            if (!frame.isChildrenProcessed) {
                val childFolder = frame.folder
                send(
                    Result.Loading(
                        message = "Processing folder: ${childFolder.name} (ID: ${childFolder.localId})",
                    ),
                )

                val folderLinksDTA = StringBuilder()
                val linksList = localLinksRepo.getLinksOfThisFolderAsList(childFolder.localId)

                send(
                    Result.Loading(
                        message = "Found ${linksList.size} links in folder: ${childFolder.name}",
                    ),
                )

                linksList.forEach { filteredLink ->
                    send(
                        Result.Loading(
                            message =
                            "Processing link: ${filteredLink.title} (URL: ${filteredLink.url}) in folder: ${childFolder.name}",
                        ),
                    )
                    folderLinksDTA.append(dtA(linkTitle = filteredLink.title, link = filteredLink.url))
                }
                frame.linksHtml = folderLinksDTA.toString()

                send(Result.Loading(message = "Completed links for folder: ${childFolder.name}"))

                val children = localFoldersRepo.getChildFoldersOfThisParentIDAsList(childFolder.localId)
                frame.childIds = children.map { it.localId }

                if (children.isNotEmpty()) {
                    children.asReversed().forEach {
                        folderFrameDeque.addLast(FolderFrame(it))
                    }
                }

                frame.isChildrenProcessed = true
            } else {
                folderFrameDeque.removeLast()

                val childFolder = frame.folder

                val nestedFolderHTML = StringBuilder()
                frame.childIds.forEach { childId ->
                    val childHtml = folderHtmlMap[childId] ?: ""
                    nestedFolderHTML.append(childHtml)
                    folderHtmlMap.remove(childId)
                }

                val currentFolderDTH3 = dtH3(childFolder.name)

                val fullHtml =
                    currentFolderDTH3 +
                        dlP(
                            frame.linksHtml + nestedFolderHTML.toString(),
                        )

                folderHtmlMap[childFolder.localId] = fullHtml

                if (childFolder.parentFolderId == null) {
                    if (forArchiveFolders) {
                        send(
                            Result.Loading(message = "Top-level archived folder ${childFolder.name} processed"),
                        )
                    } else {
                        send(
                            Result.Loading(
                                message = "Top-level non-archived folder ${childFolder.name} processed",
                            ),
                        )
                    }
                }
                send(
                    Result.Loading(
                        message =
                        "Finished processing folder: ${childFolder.name} (ID: ${childFolder.localId})",
                    ),
                )
            }
        }

        foldersList.forEach { folder ->
            foldersSection.append(folderHtmlMap[folder.localId] ?: "")
        }

        return foldersSection.toString()
    }

    private fun foldersSectionInHtml(
        allFolders: List<Folder>,
        allLinks: List<Link>,
        forArchiveFolders: Boolean,
    ): String {
        val foldersByParent = allFolders.groupBy { it.parentFolderId }
        val linksByParent = allLinks.groupBy { it.idOfLinkedFolder }

        val rootFolders = allFolders.filter {
            it.parentFolderId == null && it.isArchived == forArchiveFolders
        }

        if (rootFolders.isEmpty()) return ""

        val folderHtmlMap = mutableMapOf<Long, String>()

        val folderFrameDeque = ArrayDeque<FolderFrame>()

        rootFolders.asReversed().forEach {
            folderFrameDeque.addLast(FolderFrame(it))
        }

        while (folderFrameDeque.isNotEmpty()) {
            val frame = folderFrameDeque.last()

            if (!frame.isChildrenProcessed) {
                val folderLinksBuilder = StringBuilder()
                val links = linksByParent[frame.folder.localId] ?: emptyList()

                links.forEach { filteredLink ->
                    folderLinksBuilder.append(
                        dtA(linkTitle = filteredLink.title, link = filteredLink.url),
                    )
                }
                frame.linksHtml = folderLinksBuilder.toString()

                val children = foldersByParent[frame.folder.localId] ?: emptyList()

                if (children.isNotEmpty()) {
                    children.asReversed().forEach {
                        folderFrameDeque.addLast(FolderFrame(it))
                    }
                }

                frame.isChildrenProcessed = true
            } else {
                folderFrameDeque.removeLast()

                val folder = frame.folder
                val children = foldersByParent[folder.localId] ?: emptyList()

                val nestedHtmlBuilder = StringBuilder()
                children.forEach { child ->
                    nestedHtmlBuilder.append(folderHtmlMap[child.localId] ?: "")
                    folderHtmlMap.remove(child.localId)
                }

                val currentFolderDTH3 = dtH3(folder.name)
                val fullHtml =
                    currentFolderDTH3 +
                        dlP(
                            frame.linksHtml + nestedHtmlBuilder.toString(),
                        )

                folderHtmlMap[folder.localId] = fullHtml
            }
        }

        val finalSection = StringBuilder()
        rootFolders.forEach { folder ->
            finalSection.append(folderHtmlMap[folder.localId] ?: "")
        }

        return finalSection.toString()
    }

    private fun dlP(children: String) = "<DL><p>\n$children</DL><p>\n"

    private fun dtH3(folderName: String) = "<DT><H3>$folderName</H3>\n"

    private fun dtA(
        linkTitle: String,
        link: String,
    ) = "<DT><A HREF=\"$link\">$linkTitle</A>\n"
}

private data class FolderFrame(
    val folder: Folder,
    var isChildrenProcessed: Boolean = false,
    var childIds: List<Long> = emptyList(),
    var linksHtml: String = "",
)
