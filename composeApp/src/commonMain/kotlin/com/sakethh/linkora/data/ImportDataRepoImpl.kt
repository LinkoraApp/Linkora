package com.sakethh.linkora.data

import androidx.room3.Transactor
import androidx.room3.immediateTransaction
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.repository.ImportDataRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.LinkoraExports
import com.sakethh.linkora.utils.catchAsThrowableAndEmitFailure
import com.sakethh.linkora.utils.getSystemEpochSeconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.serialization.json.Json

typealias NewFolderIdOfParent = Long?

class ImportDataRepoImpl(
    private val localLinksRepo: LocalLinksRepo,
    private val localFoldersRepo: LocalFoldersRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val localTagsRepo: LocalTagsRepo,
    private val remoteSyncRepo: RemoteSyncRepo,
    private val withWriterConnection: suspend (suspend (Transactor) -> Unit) -> Unit,
    private val canPushToServer: () -> Boolean,
) : ImportDataRepo {
    override suspend fun importDataFromObj(jsonExportSchema: JSONExportSchema): Flow<Result<Unit>> {
        return channelFlow<Result<Unit>> {
            withWriterConnection { transactor ->
                transactor.immediateTransaction {
                    send(Result.Loading(message = "Starting data import from JSON file"))

                    send(Result.Loading(message = "Reading and deserializing JSON file"))

                    send(Result.Loading(message = "Deserialization completed: Links=${jsonExportSchema.links.size}, Folders=${jsonExportSchema.folders.size}, Panels=${jsonExportSchema.panels.panels.size}, PanelFolders=${jsonExportSchema.panels.panelFolders.size}"))

                    val linksGroupedByParentIds = jsonExportSchema.links.groupBy {
                        it.idOfLinkedFolder
                    }

                    val foldersGroupedByParentIds = jsonExportSchema.folders.groupBy {
                        it.parentFolderId
                    }

                    val panelFoldersGroupedByFolderIds =
                        jsonExportSchema.panels.panelFolders.groupBy {
                            it.folderId
                        }

                    val linksTagsGroupedByLinkId = jsonExportSchema.linkTags.groupBy {
                        it.linkId
                    }

                    send(Result.Loading(message = "Filtering non-folder linked links."))

                    val linksGroupedByLinkType = jsonExportSchema.links.groupBy {
                        it.linkType
                    }

                    val srcNonFolderLinks = linksGroupedByLinkType.filterKeys {
                        it != LinkType.FOLDER_LINK
                    }

                    val updatedLinkTags = mutableListOf<LinkTag>()

                    fun mapLinkTagsToNewLinkIds(
                        previousLinkIds: List<Long>, newLinkIds: List<Long>
                    ): List<LinkTag> {
                        require(previousLinkIds.size == newLinkIds.size)

                        return previousLinkIds.flatMapIndexed { index, previousLinkId ->
                            val previousLinkTags =
                                linksTagsGroupedByLinkId[previousLinkId] ?: emptyList()
                            previousLinkTags.map {
                                it.copy(linkId = newLinkIds[index])
                            }
                        }
                    }

                    send(Result.Loading(message = "Adding non-folder linked links to local repository."))
                    srcNonFolderLinks.values.flatten()
                        .chunked(Constants.MAX_INSERTION_IN_DB_SINGLE_SHOT)
                        .forEach { chunkedLinks ->
                            localLinksRepo.addMultipleLinks(chunkedLinks.map { it.copy(localId = 0) })
                                .let { newLinkIds ->
                                    updatedLinkTags.addAll(
                                        mapLinkTagsToNewLinkIds(
                                            previousLinkIds = chunkedLinks.map { it.localId },
                                            newLinkIds = newLinkIds
                                        )
                                    )
                                }
                        }
                    var latestFolderId = localFoldersRepo.getLatestFoldersTableID()
                    send(Result.Loading(message = "Retrieved latest folder ID: $latestFolderId"))
                    val updatedPanelFolders = mutableListOf<PanelFolder>()

                    // no more recursion (OOM, SO 🤓☝️) boi
                    val foldersDeque = ArrayDeque<Pair<Folder, NewFolderIdOfParent>>()
                    foldersGroupedByParentIds[null]?.forEach {
                        foldersDeque.addLast(it to null)
                    }

                    while (foldersDeque.isNotEmpty()) {
                        val (currentFolder, parentFolderId) = foldersDeque.removeLast()

                        val newParentFolderId = ++latestFolderId

                        localFoldersRepo.insertANewFolderLocally(
                            currentFolder.copy(
                                localId = newParentFolderId, parentFolderId = parentFolderId
                            )
                        )

                        val srcFolderLinks =
                            linksGroupedByParentIds[currentFolder.localId] ?: emptyList()

                        srcFolderLinks.chunked(Constants.MAX_INSERTION_IN_DB_SINGLE_SHOT)
                            .forEach { chunkedFolderLinks ->
                                localLinksRepo.addMultipleLinks(chunkedFolderLinks.map {
                                    it.copy(localId = 0, idOfLinkedFolder = newParentFolderId)
                                }).let { newLinkIds ->
                                    updatedLinkTags.addAll(
                                        mapLinkTagsToNewLinkIds(
                                            previousLinkIds = chunkedFolderLinks.map {
                                                it.localId
                                            }, newLinkIds = newLinkIds
                                        )
                                    )
                                }
                            }

                        updatedPanelFolders.addAll(panelFoldersGroupedByFolderIds[currentFolder.localId]?.map {
                            it.copy(
                                folderId = newParentFolderId
                            )
                        } ?: emptyList())

                        foldersGroupedByParentIds[currentFolder.localId]?.forEach {
                            foldersDeque.addLast(it to newParentFolderId)
                        }
                    }

                    var latestPanelId = localPanelsRepo.getLatestPanelID()
                    send(Result.Loading(message = "Retrieved latest panel ID: $latestPanelId"))

                    val finalPanelFoldersGroupedByPanelId = updatedPanelFolders.groupBy {
                        it.connectedPanelId
                    }

                    jsonExportSchema.panels.panels.forEach { currentPanel ->
                        ++latestPanelId
                        send(Result.Loading(message = "Assigned new ID=$latestPanelId to panel: ${currentPanel.panelName}"))

                        send(Result.Loading(message = "Inserting panel: ${currentPanel.panelName} with new ID=$latestPanelId"))
                        val updatedPanelData = currentPanel.copy(localId = latestPanelId)
                        localPanelsRepo.addANewPanelLocally(updatedPanelData)
                        send(Result.Loading(message = "Inserted panel: ${currentPanel.panelName} with new ID=$latestPanelId"))

                        send(Result.Loading(message = "Adding panel folder associations for panel: ${currentPanel.panelName}"))

                        localPanelsRepo.addMultiplePanelFolders(finalPanelFoldersGroupedByPanelId[currentPanel.localId]?.map {
                            it.copy(connectedPanelId = latestPanelId, localId = 0)
                        } ?: emptyList())
                    }

                    val tagsGroupedFilterByTagId = updatedLinkTags.groupBy {
                        it.tagId
                    }

                    jsonExportSchema.tags.forEach { currentTag ->
                        localTagsRepo.createATagLocally(currentTag.copy(localId = 0))
                            .let { currTagNewId ->
                                localTagsRepo.createLinkTags(tagsGroupedFilterByTagId[currentTag.localId]?.map {
                                    it.copy(tagId = currTagNewId)
                                } ?: emptyList())
                            }
                    }
                }
            }
            if (canPushToServer()) {
                send(Result.Loading(message = "Server is configured. Initiating push."))
                with(remoteSyncRepo) {
                    this@channelFlow.pushNonSyncedDataToServer()
                }
            } else {
                send(Result.Loading(message = "Server is not configured. Skipping push."))
            }
            send(Result.Success(Unit))
        }.catchAsThrowableAndEmitFailure()
    }

    override suspend fun importDataFromHTML(html: String): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        return channelFlow<Result<Unit>> {

            withWriterConnection { transactor ->
                transactor.immediateTransaction {
                    val rootElement = Ksoup.parse(html).body().select("dl").first()

                    if (rootElement == null) {
                        send(Result.Loading(message = "No HTML element to process"))
                        return@immediateTransaction
                    }

                    // while this is significantly better than the recursion implementation. i liked recursion impl more.
                    val htmlElementsDeque = ArrayDeque<Triple<Element, Long?, String?>>()
                    htmlElementsDeque.addLast(Triple(rootElement, null, null))

                    val linksToInsert = mutableListOf<Link>()

                    while (htmlElementsDeque.isNotEmpty()) {
                        val (currentElement, currentParentId, currentParentName) = htmlElementsDeque.removeLast()

                        currentElement.children().filter { child -> child.`is`("dt") }
                            .asReversed()
                            .forEach { filteredDtElement ->
                                filteredDtElement.children().forEach { filteredDtChildElement ->
                                    when {
                                        filteredDtChildElement.`is`("a") -> {
                                            val linkAddress =
                                                filteredDtChildElement.attribute("href")?.value ?: return@forEach

                                            val linkTitle = filteredDtChildElement.text()

                                            send(Result.Loading(message = "Found link: Title = $linkTitle, Address = $linkAddress, Parent Folder ID = $currentParentId"))

                                            val linkType = when {
                                                currentParentName == LinkoraExports.IMPORTANT_LINKS__LINKORA_EXPORT.name -> LinkType.IMPORTANT_LINK
                                                currentParentName == LinkoraExports.HISTORY_LINKS__LINKORA_EXPORT.name -> LinkType.HISTORY_LINK
                                                currentParentName == LinkoraExports.ARCHIVED_LINKS__LINKORA_EXPORT.name -> LinkType.ARCHIVE_LINK
                                                currentParentId != null && currentParentId > 0 -> LinkType.FOLDER_LINK
                                                else -> LinkType.SAVED_LINK
                                            }

                                            linksToInsert.add(
                                                Link(
                                                    linkType = linkType,
                                                    title = linkTitle,
                                                    url = linkAddress,
                                                    imgURL = "",
                                                    note = "",
                                                    idOfLinkedFolder = when (linkType) {
                                                        LinkType.SAVED_LINK -> Constants.SAVED_LINKS_ID
                                                        LinkType.IMPORTANT_LINK -> Constants.IMPORTANT_LINKS_ID
                                                        LinkType.ARCHIVE_LINK -> Constants.ARCHIVE_ID
                                                        LinkType.HISTORY_LINK -> Constants.HISTORY_ID
                                                        else -> currentParentId
                                                    },
                                                    lastModified = eventTimestamp
                                                )
                                            )
                                        }

                                        filteredDtChildElement.`is`("dl") -> {
                                            val folderName =
                                                filteredDtChildElement.siblingElements().first()
                                                    ?.text()

                                            send(Result.Loading(message = "Found folder: Name = $folderName, Parent Folder ID = $currentParentId"))

                                            var newFolderId: Long? = null

                                            if (!LinkoraExports.entries.map { it.name }
                                                    .contains(folderName)) {
                                                send(Result.Loading(message = "Folder does not exist, inserting new folder: $folderName"))
                                                try {
                                                    newFolderId =
                                                        localFoldersRepo.insertANewFolderLocally(
                                                            folder = Folder(
                                                                name = folderName.toString(),
                                                                note = "",
                                                                parentFolderId = currentParentId,
                                                                isArchived = currentParentName == LinkoraExports.ARCHIVED_FOLDERS__LINKORA_EXPORT.name,
                                                                lastModified = eventTimestamp
                                                            )
                                                        )
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }

                                            htmlElementsDeque.addLast(
                                                Triple(
                                                    filteredDtChildElement,
                                                    newFolderId ?: currentParentId,
                                                    folderName
                                                )
                                            )
                                        }

                                        else -> Unit
                                    }
                                }
                            }
                    }
                    linksToInsert.chunked(Constants.MAX_INSERTION_IN_DB_SINGLE_SHOT).forEach {
                        localLinksRepo.addMultipleLinks(it)
                    }
                }
            }

            if (canPushToServer()) {
                send(Result.Loading(message = "Server is configured. Initiating push."))
                with(remoteSyncRepo) {
                    this@channelFlow.pushNonSyncedDataToServer()
                }
            } else {
                send(Result.Loading(message = "Server is not configured. Skipping push."))
            }
            send(Result.Success(Unit))
        }.catchAsThrowableAndEmitFailure()
    }
}