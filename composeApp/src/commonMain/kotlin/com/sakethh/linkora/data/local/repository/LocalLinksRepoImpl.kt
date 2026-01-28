package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.asAddLinkDTO
import com.sakethh.linkora.domain.asLinkDTO
import com.sakethh.linkora.domain.dto.server.IDBasedDTO
import com.sakethh.linkora.domain.dto.server.link.DeleteDuplicateLinksDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateNoteOfALinkDTO
import com.sakethh.linkora.domain.dto.server.link.UpdateTitleOfTheLinkDTO
import com.sakethh.linkora.domain.dto.twitter.TwitterMetaDataDTO
import com.sakethh.linkora.domain.mapToResultFlow
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.model.ScrapedLinkInfo
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.LinkTagDTO
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteLinksRepo
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.defaultFolderIds
import com.sakethh.linkora.utils.getSystemEpochSeconds
import com.sakethh.linkora.utils.host
import com.sakethh.linkora.utils.ifNot
import com.sakethh.linkora.utils.isATwitterUrl
import com.sakethh.linkora.utils.isAValidLink
import com.sakethh.linkora.utils.isNotNullOrNotBlank
import com.sakethh.linkora.utils.performLocalOperationWithRemoteSyncFlow
import com.sakethh.linkora.utils.updateLastSyncedWithServerTimeStamp
import com.sakethh.linkora.utils.wrappedResultFlow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup

class LocalLinksRepoImpl(
    private val linksDao: LinksDao,
    private val primaryUserAgent: () -> String,
    private val standardClient: HttpClient,
    private val remoteLinksRepo: RemoteLinksRepo,
    private val foldersDao: FoldersDao,
    private val pendingSyncQueueRepo: PendingSyncQueueRepo,
    private val preferencesRepository: PreferencesRepository,
    private val tagsDao: TagsDao
) : LocalLinksRepo {
    override suspend fun changeIdOfALink(existingId: Long, newId: Long) {
        linksDao.changeIdOfALink(existingId, newId)
    }

    override suspend fun addANewLink(
        link: Link, selectedTagIds: List<Long>?, linkSaveConfig: LinkSaveConfig, viaSocket: Boolean
    ): Flow<Result<Unit>> {
        if (link.linkType == LinkType.HISTORY_LINK) {
            linksDao.deleteLinksFromHistory(link.url)
        }
        var newLinkId: Long? = null
        val eventTimestamp = getSystemEpochSeconds()
        val remoteTagIds = if (selectedTagIds != null) {
            tagsDao.getRemoteTagIds(selectedTagIds)
        } else {
            emptyList()
        }
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = !viaSocket,
            onRemoteOperationFailure = {
                if (newLinkId == null) return@performLocalOperationWithRemoteSyncFlow

                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.CREATE_A_NEW_LINK.name,
                        payload = if (link.idOfLinkedFolder != null && link.idOfLinkedFolder !in defaultFolderIds()) {
                            Json.encodeToString(
                                linksDao.getLink(newLinkId!!).copy(
                                    idOfLinkedFolder = link.idOfLinkedFolder,
                                    lastModified = eventTimestamp
                                ).asAddLinkDTO(remoteTagIds).copy(offlineSyncItemId = newLinkId!!)
                            )
                        } else {
                            Json.encodeToString(
                                linksDao.getLink(newLinkId!!).copy(lastModified = eventTimestamp)
                                    .asAddLinkDTO(remoteTagIds)
                                    .copy(offlineSyncItemId = newLinkId!!)
                            )
                        }
                    )
                )
            },
            remoteOperation = {
                if (newLinkId == null) return@performLocalOperationWithRemoteSyncFlow emptyFlow()

                if (link.idOfLinkedFolder != null && link.idOfLinkedFolder !in defaultFolderIds()) {
                    val remoteIdOfLinkedFolder =
                        foldersDao.getRemoteFolderId(link.idOfLinkedFolder)
                    remoteLinksRepo.addANewLink(
                        linksDao.getLink(newLinkId!!).copy(
                            idOfLinkedFolder = remoteIdOfLinkedFolder, lastModified = eventTimestamp
                        ).asAddLinkDTO(remoteTagIds)
                    )
                } else {
                    remoteLinksRepo.addANewLink(
                        linksDao.getLink(newLinkId!!).copy(lastModified = eventTimestamp)
                            .asAddLinkDTO(remoteTagIds)
                    )
                }
            },
            remoteOperationOnSuccess = {
                if (newLinkId == null) return@performLocalOperationWithRemoteSyncFlow

                linksDao.updateALink(
                    linksDao.getLink(newLinkId).copy(
                        remoteId = it.id, lastModified = it.timeStampBasedResponse.eventTimestamp
                    )
                )
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.timeStampBasedResponse.eventTimestamp)
            }) {

            // throwing manually (failing early) skips remote execution — see @performLocalOperationWithRemoteSyncFlow

            if (linkSaveConfig.skipSavingIfExists) {
                when (val linkType = link.linkType) {
                    LinkType.FOLDER_LINK -> {
                        require(
                            link.idOfLinkedFolder != null && !linksDao.doesLinkExist(
                                folderId = link.idOfLinkedFolder, url = link.url
                            )
                        ) {
                            "You've already added this link to the selected folder."
                        }
                    }

                    else -> {
                        require(!linksDao.doesLinkExist(linkType = linkType, url = link.url)) {
                            when (linkType) {
                                LinkType.SAVED_LINK -> "You've already saved this link in the \"Saved\" collection."
                                LinkType.HISTORY_LINK -> "This link is already in your history."
                                LinkType.IMPORTANT_LINK -> "You've already marked this link as important."
                                LinkType.ARCHIVE_LINK -> "This link is already archived."
                                else -> "You've already saved this link."
                            }
                        }
                    }
                }
            }

            newLinkId =
                if (linkSaveConfig.forceSaveWithoutRetrievingData || (!linkSaveConfig.forceAutoDetectTitle && link.imgURL.isNotBlank())) {
                    link.url.isAValidLink().ifNot {
                        throw Link.Invalid()
                    }
                    linkoraLog("not retrieving")
                    linksDao.addANewLink(link.copy(lastModified = eventTimestamp, localId = 0))
                } else {
                    try {
                        linkoraLog("retrieving")
                        if (link.url.isATwitterUrl()) {
                            retrieveFromVxTwitterApi(link.url)
                        } else {
                            scrapeLinkData(
                                link.url, link.userAgent ?: primaryUserAgent()
                            )
                        }
                    } catch (e: Exception) {
                        if (linkSaveConfig.forceSaveIfRetrievalFails) {
                            ScrapedLinkInfo(title = "", imgUrl = "")
                        } else {
                            linkoraLog("retrieve failed")
                            throw e
                        }
                    }.let { scrapedLinkInfo ->
                        linksDao.addANewLink(
                            link.copy(
                                title = if (linkSaveConfig.forceAutoDetectTitle) scrapedLinkInfo.title else link.title,
                                imgURL = link.imgURL.ifBlank { scrapedLinkInfo.imgUrl },
                                localId = 0,
                                mediaType = scrapedLinkInfo.mediaType,
                                lastModified = eventTimestamp
                            )
                        )
                    }
                }

            selectedTagIds?.let { selectedTagIds ->
                tagsDao.createLinkTags(
                    linksTags = selectedTagIds.map { tagId ->
                        LinkTag(
                            linkId = newLinkId, tagId = tagId
                        )
                    })
            }
        }
    }

    override suspend fun addMultipleLinks(links: List<Link>): List<Long> {
        return linksDao.addMultipleLinks(links)
    }

    override suspend fun getLinks(
        linkType: LinkType,
        parentFolderId: Long,
        sortOption: String,
        pageSize: Int,
        startIndex: Long
    ): Flow<Result<List<Link>>> {
        return linksDao.getSortedLinks(linkType, parentFolderId, sortOption, pageSize, startIndex)
            .mapToResultFlow()
    }

    override suspend fun getLinks(
        tagId: Long, sortOption: String,
        pageSize: Int, startIndex: Long
    ): Flow<Result<List<Link>>> {
        return linksDao.getSortedLinks(
            tagId = tagId,
            sortOption = sortOption,
            pageSize = pageSize,
            startIndex = startIndex
        ).mapToResultFlow()
    }

    override fun getLinksAsNonResultFlow(
        linkType: LinkType, parentFolderId: Long, sortOption: String
    ): Flow<List<Link>> {
        return linksDao.getSortedLinks(linkType, parentFolderId, sortOption)
    }

    override suspend fun getLinks(
        linkType: LinkType,
        sortOption: String,
        pageSize: Int, startIndex: Long
    ): Flow<Result<List<Link>>> {
        return linksDao.getSortedLinks(
            linkType = linkType,
            sortOption = sortOption,
            pageSize = pageSize,
            startIndex = startIndex
        ).mapToResultFlow()
    }

    override fun getLinksAsNonResultFlow(
        linkType: LinkType, sortOption: String
    ): Flow<List<Link>> {
        return linksDao.getSortedLinks(linkType, sortOption)
    }

    override suspend fun getAllLinks(sortOption: String): Flow<Result<List<Link>>> {
        return linksDao.getAllLinks(sortOption).mapToResultFlow()
    }


    private suspend fun retrieveFromVxTwitterApi(tweetURL: String): ScrapedLinkInfo {
        val vxTwitterResponseBody =
            standardClient.get("https://api.vxtwitter.com/${tweetURL.substringAfter(".com/")}")
                .body<TwitterMetaDataDTO>()
        return ScrapedLinkInfo(
            title = vxTwitterResponseBody.text,
            imgUrl = vxTwitterResponseBody.media.takeIf { vxTwitterResponseBody.hasMedia && it.isNotEmpty() }
                ?.find { it.type in listOf("image", "video", "gif") }
                ?.let { if (it.type == "image") it.url else it.thumbnailUrl }
                ?: vxTwitterResponseBody.userPfp,
            mediaType = if (vxTwitterResponseBody.media.isNotEmpty() && vxTwitterResponseBody.media.first().type == "video") MediaType.VIDEO else MediaType.IMAGE)
    }

    private suspend fun scrapeLinkData(
        linkUrl: String, userAgent: String
    ): ScrapedLinkInfo {
        val baseUrl: String
        try {
            baseUrl = linkUrl.host()
        } catch (e: Exception) {
            throw Link.Invalid()
        }
        val rawHTML = withContext(Dispatchers.IO) {
            Jsoup.connect(
                "http" + linkUrl.substringAfter("http").substringBefore(" ").trim()
            ).userAgent(userAgent).followRedirects(true).header("Accept", "text/html")
                .header("Accept-Encoding", "gzip,deflate").header("Accept-Language", "en;q=1.0")
                .header("Connection", "keep-alive").ignoreContentType(true).maxBodySize(0)
                .ignoreHttpErrors(true).get()
        }.toString()

        val document = Jsoup.parse(rawHTML)
        val ogImage = document.select("meta[property=og:image]").attr("content")
        val twitterImage = document.select("meta[name=twitter:image]").attr("content")
        val favicon = document.select("link[rel=icon]").attr("href")
        val ogTitle = document.select("meta[property=og:title]").attr("content")
        val pageTitle = document.title()

        val imgURL = when {
            ogImage.isNotNullOrNotBlank() -> {
                if (ogImage.startsWith("/")) {
                    "https://$baseUrl$ogImage"
                } else {
                    ogImage
                }
            }

            ogImage.isNullOrBlank() && twitterImage.isNotNullOrNotBlank() -> if (twitterImage.startsWith(
                    "/"
                )
            ) {
                "https://$baseUrl$twitterImage"
            } else {
                twitterImage
            }

            ogImage.isNullOrBlank() && twitterImage.isNullOrBlank() && favicon.isNotNullOrNotBlank() -> {
                if (favicon.startsWith("/")) {
                    "https://$baseUrl$favicon"
                } else {
                    favicon
                }
            }

            else -> ""
        }

        val title = when {
            ogTitle.isNotNullOrNotBlank() -> ogTitle
            else -> pageTitle
        }
        return ScrapedLinkInfo(title, imgURL)
    }


    override suspend fun deleteLinksOfFolder(folderId: Long): Flow<Result<Unit>> {
        return wrappedResultFlow {
            linksDao.deleteLinksOfFolder(folderId)
        }
    }

    override suspend fun deleteALinkNote(linkId: Long): Flow<Result<Unit>> {
        val remoteId = getRemoteIdOfLink(linkId)
        val eventTimestamp = getSystemEpochSeconds()
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = true,
            remoteOperation = {
                require(remoteId != null)
                remoteLinksRepo.updateALinkNote(
                    UpdateNoteOfALinkDTO(
                        linkId, "", eventTimestamp
                    )
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
                linksDao.updateLinkTimestamp(timestamp = it.eventTimestamp, linkId)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.UPDATE_LINK_NOTE.name,
                        payload = Json.encodeToString(
                            UpdateNoteOfALinkDTO(
                                linkId, newNote = "", eventTimestamp = eventTimestamp
                            )
                        )
                    )
                )
            }) {
            linksDao.deleteALinkNote(linkId)
            linksDao.updateLinkTimestamp(timestamp = eventTimestamp, linkId)
        }
    }

    override suspend fun deleteALink(linkId: Long, viaSocket: Boolean): Flow<Result<Unit>> {
        val remoteId = getRemoteIdOfLink(linkId)
        val eventTimestamp = getSystemEpochSeconds()
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = viaSocket.not(),
            remoteOperation = {
                require(remoteId != null)
                remoteLinksRepo.deleteALink(IDBasedDTO(remoteId, eventTimestamp))
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
            },
            onRemoteOperationFailure = {
                if (remoteId != null) {
                    pendingSyncQueueRepo.addInQueue(
                        PendingSyncQueue(
                            operation = SyncServerRoute.DELETE_A_LINK.name,
                            payload = Json.encodeToString(
                                IDBasedDTO(
                                    remoteId, getSystemEpochSeconds()
                                )
                            )
                        )
                    )
                }
            }) {
            linksDao.deleteALink(linkId)
        }
    }

    override suspend fun deleteMultipleLinks(
        linkIds: List<Long>, viaSocket: Boolean
    ): Flow<Result<Unit>> {
        return wrappedResultFlow {
            linksDao.deleteMultipleLinks(linkIds)
        }
    }

    override suspend fun archiveALink(linkId: Long, viaSocket: Boolean): Flow<Result<Unit>> {
        val remoteId = getRemoteIdOfLink(linkId)
        val eventTimestamp = getSystemEpochSeconds()
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = viaSocket.not(),
            remoteOperation = {
                require(remoteId != null)
                remoteLinksRepo.archiveALink(IDBasedDTO(remoteId, eventTimestamp))
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
                linksDao.updateLinkTimestamp(it.eventTimestamp, linkId)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.ARCHIVE_LINK.name,
                        payload = Json.encodeToString(
                            IDBasedDTO(
                                linkId, eventTimestamp
                            )
                        )
                    )
                )
            }) {
            linksDao.archiveALink(linkId)
            linksDao.updateLinkTimestamp(eventTimestamp, linkId)
        }
    }

    override suspend fun updateLinkNote(
        linkId: Long, newNote: String, viaSocket: Boolean
    ): Flow<Result<Unit>> {
        val remoteId = getRemoteIdOfLink(linkId)
        val eventTimestamp = getSystemEpochSeconds()
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = viaSocket.not(),
            remoteOperation = {
                require(remoteId != null)
                remoteLinksRepo.updateALinkNote(
                    UpdateNoteOfALinkDTO(
                        remoteId, newNote, eventTimestamp
                    )
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
                linksDao.updateLinkTimestamp(eventTimestamp, linkId)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.UPDATE_LINK_NOTE.name,
                        payload = Json.encodeToString(
                            UpdateNoteOfALinkDTO(
                                linkId = linkId, newNote = newNote, eventTimestamp = eventTimestamp
                            )
                        )
                    )
                )
            }) {
            linksDao.updateLinkNote(linkId, newNote)
            linksDao.updateLinkTimestamp(eventTimestamp, linkId)
        }
    }

    override suspend fun updateLinkTitle(
        linkId: Long, newTitle: String, viaSocket: Boolean
    ): Flow<Result<Unit>> {
        val remoteId = getRemoteIdOfLink(linkId)
        val eventTimestamp = getSystemEpochSeconds()
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = viaSocket.not(),
            remoteOperation = {
                require(remoteId != null)
                remoteLinksRepo.updateLinkTitle(
                    UpdateTitleOfTheLinkDTO(
                        remoteId, newTitle, eventTimestamp
                    )
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
                linksDao.updateLinkTimestamp(it.eventTimestamp, linkId)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.UPDATE_LINK_TITLE.name,
                        payload = Json.encodeToString(
                            UpdateTitleOfTheLinkDTO(
                                linkId, newTitle, eventTimestamp
                            )
                        )
                    )
                )
            }) {
            linksDao.updateLinkTitle(linkId, newTitle)
            linksDao.updateLinkTimestamp(eventTimestamp, linkId)
        }
    }

    override suspend fun isInArchive(url: String): Flow<Result<Boolean>> {
        return wrappedResultFlow {
            linksDao.isInArchive(url)
        }
    }

    override fun search(query: String, sortOption: String): Flow<Result<List<Link>>> {
        return linksDao.search(query, sortOption).mapToResultFlow()
    }

    override suspend fun getLinksOfThisFolderAsList(folderID: Long): List<Link> {
        return linksDao.getLinksOfThisFolderAsList(folderID)
    }

    override suspend fun getAllLinks(): List<Link> {
        return linksDao.getAllLinks()
    }

    override fun getAllLinksAsFlow(): Flow<List<Link>> {
        return linksDao.getAllLinksAsFlow()
    }

    override suspend fun updateALink(
        link: Link, updatedLinkTagsPair: LinkTagsPair?, viaSocket: Boolean
    ): Flow<Result<Unit>> {
        val remoteLinkTagDTOs = if (link.remoteId != null) {
            updatedLinkTagsPair?.tags?.map {
                LinkTagDTO(linkId = link.remoteId, tagId = it.remoteId ?: -54545)
            }
        } else {
            null
        }
        val remoteId = getRemoteIdOfLink(link.localId)
        val eventTimestamp = getSystemEpochSeconds()
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = viaSocket.not(),
            remoteOperation = {
                require(remoteId != null)
                remoteLinksRepo.updateLink(
                    link.asLinkDTO(
                        id = remoteId, remoteLinkTags = remoteLinkTagDTOs ?: emptyList()
                    )
                )
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
                linksDao.updateLinkTimestamp(it.eventTimestamp, link.localId)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.UPDATE_LINK.name,
                        payload = Json.encodeToString(
                            link.asLinkDTO(
                                id = link.localId, remoteLinkTags = remoteLinkTagDTOs ?: emptyList()
                            ).copy(eventTimestamp = eventTimestamp)
                        )
                    )
                )
            }) {

            linksDao.updateALink(link)
            linksDao.updateLinkTimestamp(eventTimestamp, link.localId)

            if (updatedLinkTagsPair == null) return@performLocalOperationWithRemoteSyncFlow


            val tagsAttachedToTheLink = tagsDao.getTags(linkId = updatedLinkTagsPair.link.localId)

            val newlySelectedTags = updatedLinkTagsPair.tags.filter { curFilterTag ->
                curFilterTag !in tagsAttachedToTheLink
            }

            val unselectedTags = tagsAttachedToTheLink.filter { curFilterTag ->
                curFilterTag !in updatedLinkTagsPair.tags
            }
            tagsDao.deleteLinkTagsBasedOnTags(unselectedTags.map { it.localId })
            tagsDao.createLinkTags(newlySelectedTags.map {
                LinkTag(
                    linkId = updatedLinkTagsPair.link.localId, tagId = it.localId
                )
            })

        }
    }

    override suspend fun refreshLinkMetadata(link: Link): Flow<Result<Unit>> {
        val remoteId = getRemoteIdOfLink(link.localId)
        val eventTimestamp = getSystemEpochSeconds()
        val remoteLinkTagDTOs = if (link.remoteId != null) {
            tagsDao.getTags(link.localId).map {
                LinkTagDTO(linkId = link.remoteId, tagId = it.remoteId ?: -54545)
            }
        } else {
            null
        }
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = true,
            remoteOperation = {
                require(remoteId != null)
                remoteLinksRepo.updateLink(
                    linksDao.getLink(link.localId)
                        .asLinkDTO(id = remoteId, remoteLinkTags = remoteLinkTagDTOs ?: emptyList())
                        .run {
                            copy(
                                idOfLinkedFolder = foldersDao.getRemoteFolderId(
                                    idOfLinkedFolder ?: -45454
                                )
                            )
                        })
            },
            remoteOperationOnSuccess = {
                preferencesRepository.updateLastSyncedWithServerTimeStamp(it.eventTimestamp)
                linksDao.updateLinkTimestamp(it.eventTimestamp, link.localId)
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.UPDATE_LINK.name,
                        payload = Json.encodeToString(
                            linksDao.getLink(link.localId).asLinkDTO(
                                id = link.localId,
                                remoteLinkTags = remoteLinkTagDTOs ?: emptyList()
                            ).copy(eventTimestamp = eventTimestamp)
                        )
                    )
                )
            }) {
            if (link.url.isATwitterUrl()) {
                retrieveFromVxTwitterApi(link.url)
            } else {
                scrapeLinkData(
                    linkUrl = link.url, userAgent = link.userAgent ?: primaryUserAgent()
                )
            }.let { scrapedLinkInfo ->
                linksDao.updateALink(
                    link.copy(
                        title = scrapedLinkInfo.title,
                        imgURL = scrapedLinkInfo.imgUrl,
                        mediaType = scrapedLinkInfo.mediaType,
                        lastModified = getSystemEpochSeconds()
                    )
                )
            }
        }
    }

    private suspend fun getRemoteIdOfLink(localLinkId: Long): Long? {
        return linksDao.getRemoteId(localLinkId)
    }

    override suspend fun getLocalLinkId(remoteID: Long): Long? {
        return linksDao.getLocalIdOfALink(remoteID)
    }

    override suspend fun getRemoteLinkId(localId: Long): Long? {
        return linksDao.getRemoteId(localId)
    }

    override suspend fun getALink(localLinkId: Long): Link {
        return linksDao.getLink(localLinkId)
    }

    override suspend fun getUnSyncedLinks(): List<Link> {
        return linksDao.getUnSyncedLinks()
    }

    override suspend fun getLatestId(): Long {
        return linksDao.getLatestId()
    }

    override suspend fun doesLinkExist(linkType: LinkType, url: String): Boolean {
        return linksDao.doesLinkExist(linkType, url)
    }

    override suspend fun deleteDuplicateLinks(viaSocket: Boolean): Flow<Result<Unit>> {
        val eventTimestamp = getSystemEpochSeconds()
        val linksToBeDeleted = mutableListOf<Link>()
        return performLocalOperationWithRemoteSyncFlow(
            performRemoteOperation = viaSocket.not(),
            remoteOperation = {
                remoteLinksRepo.deleteDuplicateLinks(DeleteDuplicateLinksDTO(linkIds = linksToBeDeleted.filter {
                    it.remoteId != null
                }.map { it.remoteId!! }, eventTimestamp = eventTimestamp))
            },
            onRemoteOperationFailure = {
                pendingSyncQueueRepo.addInQueue(
                    PendingSyncQueue(
                        operation = SyncServerRoute.DELETE_DUPLICATE_LINKS.name,
                        payload = Json.encodeToString(
                            DeleteDuplicateLinksDTO(linkIds = linksToBeDeleted.filterNot {
                                it.remoteId == null
                            }.map {
                                it.remoteId!!
                            }, eventTimestamp = eventTimestamp)
                        )
                    )
                )
            },
            localOperation = {
                coroutineScope {
                    val allLinks = linksDao.getAllLinks()
                    val allFolders = allLinks.map {
                        it.idOfLinkedFolder
                    }.filter {
                        it != null && it !in defaultFolderIds()
                    }.distinct()

                    val historyLinks =
                        async { allLinks.filter { it.linkType == LinkType.HISTORY_LINK } }
                    val importantLinks =
                        async { allLinks.filter { it.linkType == LinkType.IMPORTANT_LINK } }
                    val savedLinks = async {
                        allLinks.filter { it.linkType == LinkType.SAVED_LINK }
                    }
                    val archiveLinks = async {
                        allLinks.filter { it.linkType == LinkType.ARCHIVE_LINK }
                    }
                    val folderLinks = async {
                        allLinks.filter { it.linkType == LinkType.FOLDER_LINK }
                    }

                    val filteredHistoryLinks = historyLinks.await().distinctBy {
                        it.url
                    }
                    val filteredImportantLinks = importantLinks.await().distinctBy {
                        it.url
                    }
                    val filteredSavedLinks = savedLinks.await().distinctBy {
                        it.url
                    }
                    val filteredArchiveLinks = archiveLinks.await().distinctBy {
                        it.url
                    }
                    val filteredFolderLinks = mutableListOf<Link>()

                    allFolders.forEach { currentFolderId ->
                        filteredFolderLinks.addAll(folderLinks.await().filter {
                            it.idOfLinkedFolder == currentFolderId
                        }.distinctBy {
                            it.url
                        })
                    }


                    awaitAll(async {
                        linksToBeDeleted.addAll(historyLinks.await().filterNot {
                            filteredHistoryLinks.contains(it)
                        })
                    }, async {
                        linksToBeDeleted.addAll(importantLinks.await().filterNot {
                            filteredImportantLinks.contains(it)
                        })
                    }, async {
                        linksToBeDeleted.addAll(savedLinks.await().filterNot {
                            filteredSavedLinks.contains(it)
                        })
                    }, async {
                        linksToBeDeleted.addAll(archiveLinks.await().filterNot {
                            filteredArchiveLinks.contains(it)
                        })
                    }, async {
                        linksToBeDeleted.addAll(folderLinks.await().filterNot {
                            filteredFolderLinks.contains(it)
                        })
                    })
                    linksDao.deleteLinks(linksToBeDeleted.map { it.localId })
                }
            })
    }

    override suspend fun deleteLinksLocally(linksIds: List<Long>): Flow<Result<Unit>> {
        return wrappedResultFlow {
            linksDao.deleteLinks(linksIds)
        }
    }

    override fun getAllLinks(
        applyLinkFilters: Boolean,
        activeLinkFilters: List<String>,
        sortOption: String,
        pageSize: Int,
        startIndex: Long
    ): Flow<Result<List<Link>>> {
        return linksDao.getAllLinks(
            applyLinkFilters,
            activeLinkFilters,
            sortOption,
            pageSize,
            startIndex
        ).mapToResultFlow()
    }
}