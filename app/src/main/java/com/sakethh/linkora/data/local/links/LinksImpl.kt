package com.sakethh.linkora.data.local.links

import android.content.Context
import android.widget.Toast
import com.sakethh.linkora.data.local.ArchivedLinks
import com.sakethh.linkora.data.local.ImportantLinks
import com.sakethh.linkora.data.local.LinksTable
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.data.local.RecentlyVisited
import com.sakethh.linkora.data.local.folders.FoldersRepo
import com.sakethh.linkora.data.remote.scrape.LinkMetaDataScrapperResult
import com.sakethh.linkora.data.remote.scrape.LinkMetaDataScrapperService
import com.sakethh.linkora.ui.CommonUiEvents
import com.sakethh.linkora.ui.screens.settings.SettingsScreenVM
import com.sakethh.linkora.ui.viewmodels.commonBtmSheets.OptionsBtmSheetVM
import com.sakethh.linkora.utils.isAValidURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class LinksImpl @Inject constructor(
    private val localDatabase: LocalDatabase,
    private val linksRepo: LinksRepo, private val foldersRepo: FoldersRepo,
    private val linkMetaDataScrapperService: LinkMetaDataScrapperService
) : LinksRepo {
    override suspend fun addANewLinkToSavedLinks(
        linksTable: LinksTable,
        onTaskCompleted: () -> Unit,
        autoDetectTitle: Boolean
    ): CommonUiEvents {
        if (!isAValidURL(linksTable.webURL)) {
            onTaskCompleted()
            return CommonUiEvents.ShowToast("invalid url")
        } else if (linksTable.keyOfLinkedFolderV10?.let {
                doesThisLinkExistsInAFolderV10(
                    linksTable.webURL,
                    it
                )
            } == true
        ) {
            onTaskCompleted()
            return CommonUiEvents.ShowToast("given link already exists")
        } else {
            when (val linkDataExtractor =
                linkMetaDataScrapperService.scrapeLinkData(linksTable.webURL)) {
                is LinkMetaDataScrapperResult.Failure -> {
                    val linkData = LinksTable(
                        title = linksTable.title,
                        webURL = "http" + linksTable.webURL.substringAfter("http")
                            .substringBefore(" ").trim(),
                        baseURL = linksTable.baseURL,
                        imgURL = linksTable.imgURL,
                        infoForSaving = linksTable.infoForSaving,
                        isLinkedWithSavedLinks = true,
                        isLinkedWithFolders = false,
                        keyOfLinkedFolderV10 = null,
                        keyOfLinkedFolder = null,
                        isLinkedWithImpFolder = false,
                        isLinkedWithArchivedFolder = false,
                        keyOfArchiveLinkedFolderV10 = 0,
                        keyOfImpLinkedFolder = ""
                    )
                    localDatabase.linksDao().addANewLinkToSavedLinksOrInFolders(linkData)
                    onTaskCompleted()
                    return CommonUiEvents.ShowToast("couldn't retrieve metadata now, but linkora saved the link")
                }

                is LinkMetaDataScrapperResult.Success -> {
                    val linkData = LinksTable(
                        title = if (SettingsScreenVM.Settings.isAutoDetectTitleForLinksEnabled.value || autoDetectTitle) linkDataExtractor.data.title else linksTable.title,
                        webURL = "http" + linksTable.webURL.substringAfter("http")
                            .substringBefore(" ").trim(),
                        baseURL = linkDataExtractor.data.baseURL,
                        imgURL = linkDataExtractor.data.imgURL,
                        infoForSaving = linksTable.infoForSaving,
                        isLinkedWithSavedLinks = true,
                        isLinkedWithFolders = false,
                        keyOfLinkedFolderV10 = null,
                        keyOfLinkedFolder = null,
                        isLinkedWithImpFolder = false,
                        isLinkedWithArchivedFolder = false,
                        keyOfArchiveLinkedFolderV10 = 0,
                        keyOfImpLinkedFolder = ""
                    )
                    localDatabase.linksDao().addANewLinkToSavedLinksOrInFolders(linkData)
                    onTaskCompleted()
                    return CommonUiEvents.ShowToast("added the url")
                }
            }
        }
    }

    override suspend fun addANewLinkInAFolder(
        linksTable: LinksTable,
        onTaskCompleted: () -> Unit,
        autoDetectTitle: Boolean
    ): CommonUiEvents {
        if (!isAValidURL(linksTable.webURL)) {
            onTaskCompleted()
            return CommonUiEvents.ShowToast("invalid url")
        } else if (linksTable.keyOfLinkedFolderV10?.let {
                doesThisLinkExistsInAFolderV10(
                    linksTable.webURL,
                    it
                )
            } == true
        ) {
            onTaskCompleted()
            return CommonUiEvents.ShowToast("given link already exists")
        } else {
            when (val linkDataExtractor =
                linkMetaDataScrapperService.scrapeLinkData(linksTable.webURL)) {
                is LinkMetaDataScrapperResult.Failure -> {
                    val linkData = LinksTable(
                        title = linksTable.title,
                        webURL = "http" + linksTable.webURL.substringAfter("http")
                            .substringBefore(" ").trim(),
                        baseURL = linksTable.baseURL,
                        imgURL = linksTable.imgURL,
                        infoForSaving = linksTable.infoForSaving,
                        isLinkedWithSavedLinks = false,
                        isLinkedWithFolders = true,
                        keyOfLinkedFolderV10 = linksTable.keyOfLinkedFolderV10,
                        keyOfLinkedFolder = linksTable.keyOfLinkedFolder,
                        isLinkedWithImpFolder = false,
                        isLinkedWithArchivedFolder = false,
                        keyOfArchiveLinkedFolderV10 = 0,
                        keyOfImpLinkedFolder = ""
                    )
                    localDatabase.linksDao().addANewLinkToSavedLinksOrInFolders(linkData)
                    onTaskCompleted()
                    return CommonUiEvents.ShowToast("couldn't retrieve metadata now, but linkora saved the link")
                }

                is LinkMetaDataScrapperResult.Success -> {
                    val linkData = LinksTable(
                        title = if (SettingsScreenVM.Settings.isAutoDetectTitleForLinksEnabled.value || autoDetectTitle) linkDataExtractor.data.title else linksTable.title,
                        webURL = "http" + linksTable.webURL.substringAfter("http")
                            .substringBefore(" ").trim(),
                        baseURL = linkDataExtractor.data.baseURL,
                        imgURL = linkDataExtractor.data.imgURL,
                        infoForSaving = linksTable.infoForSaving,
                        isLinkedWithSavedLinks = false,
                        isLinkedWithFolders = true,
                        keyOfLinkedFolderV10 = linksTable.keyOfLinkedFolderV10,
                        keyOfLinkedFolder = linksTable.keyOfLinkedFolder,
                        isLinkedWithImpFolder = false,
                        isLinkedWithArchivedFolder = false,
                        keyOfArchiveLinkedFolderV10 = 0,
                        keyOfImpLinkedFolder = ""
                    )
                    localDatabase.linksDao().addANewLinkToSavedLinksOrInFolders(linkData)
                    onTaskCompleted()
                    return CommonUiEvents.ShowToast("added the url")
                }
            }
        }
    }

    override suspend fun addListOfDataInLinksTable(list: List<LinksTable>) {
        return localDatabase.linksDao().addListOfDataInLinksTable(list)
    }

    override suspend fun deleteANoteFromArchiveLinks(webURL: String) {
        return localDatabase.linksDao().deleteANoteFromArchiveLinks(webURL)
    }

    override suspend fun deleteALinkNoteFromArchiveBasedFolderLinksV10(
        webURL: String,
        folderID: Long
    ) {
        return localDatabase.linksDao().deleteALinkNoteFromArchiveBasedFolderLinksV10(
            webURL,
            folderID
        )
    }

    override suspend fun deleteALinkNoteFromArchiveBasedFolderLinksV9(
        webURL: String,
        folderName: String
    ) {
        return localDatabase.linksDao().deleteALinkNoteFromArchiveBasedFolderLinksV9(
            webURL,
            folderName
        )
    }

    override suspend fun deleteANoteFromImportantLinks(webURL: String) {
        return localDatabase.linksDao().deleteANoteFromImportantLinks(webURL)
    }

    override suspend fun deleteANoteFromRecentlyVisited(webURL: String) {
        return localDatabase.linksDao().deleteANoteFromRecentlyVisited(webURL)
    }

    override suspend fun deleteALinkInfoFromSavedLinks(webURL: String) {
        return localDatabase.linksDao().deleteALinkInfoFromSavedLinks(webURL)
    }

    override suspend fun deleteALinkInfoOfFolders(linkID: Long) {
        return localDatabase.linksDao().deleteALinkInfoOfFolders(linkID)
    }

    override suspend fun deleteALinkFromSavedLinksBasedOnURL(webURL: String) {
        return localDatabase.linksDao().deleteALinkFromSavedLinksBasedOnURL(webURL)
    }

    override suspend fun deleteALinkFromImpLinks(linkID: Long) {
        return localDatabase.linksDao().deleteALinkFromImpLinks(linkID)
    }

    override suspend fun deleteALinkFromImpLinksBasedOnURL(webURL: String) {
        return localDatabase.linksDao().deleteALinkFromImpLinksBasedOnURL(webURL)
    }

    override suspend fun deleteALinkFromArchiveLinksV9(webURL: String) {
        return localDatabase.linksDao().deleteALinkFromArchiveLinksV9(webURL)
    }

    override suspend fun deleteALinkFromArchiveLinks(id: Long) {
        return localDatabase.linksDao().deleteALinkFromArchiveLinks(id)
    }

    override suspend fun archiveLinkTableUpdater(
        archivedLinks: ArchivedLinks,
        context: Context,
        onTaskCompleted: () -> Unit
    ) {
        if (doesThisExistsInArchiveLinks(webURL = archivedLinks.webURL)) {
            deleteALinkFromArchiveLinksV9(webURL = archivedLinks.webURL)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "removed the link from archive(s)",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            onTaskCompleted()
        } else {
            addANewLinkToArchiveLink(archivedLinks = archivedLinks)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "moved the link to archive(s)", Toast.LENGTH_SHORT)
                    .show()
            }
            onTaskCompleted()
        }
        OptionsBtmSheetVM(
            linksRepo,
            foldersRepo
        ).updateArchiveLinkCardData(url = archivedLinks.webURL)
    }

    override suspend fun deleteALinkFromArchiveFolderBasedLinksV10(
        webURL: String,
        archiveFolderID: Long
    ) {
        return localDatabase.linksDao().deleteALinkFromArchiveFolderBasedLinksV10(
            webURL,
            archiveFolderID
        )
    }

    override suspend fun deleteALinkFromArchiveFolderBasedLinksV9(
        webURL: String,
        folderName: String
    ) {
        return localDatabase.linksDao().deleteALinkFromArchiveFolderBasedLinksV9(
            webURL,
            folderName
        )
    }

    override suspend fun deleteALinkFromLinksTable(linkID: Long) {
        return localDatabase.linksDao().deleteALinkFromLinksTable(linkID)
    }

    override suspend fun deleteALinkFromSpecificFolderV10(webURL: String, folderID: Long) {
        return localDatabase.linksDao().deleteALinkFromSpecificFolderV10(webURL, folderID)
    }

    override suspend fun deleteALinkFromSpecificFolderV9(webURL: String, folderName: String) {
        return localDatabase.linksDao().deleteALinkFromSpecificFolderV9(webURL, folderName)
    }

    override suspend fun deleteARecentlyVisitedLink(webURL: String) {
        return localDatabase.linksDao().deleteARecentlyVisitedLink(webURL)
    }

    override suspend fun deleteARecentlyVisitedLink(linkID: Long) {
        return localDatabase.linksDao().deleteARecentlyVisitedLink(linkID)
    }

    override suspend fun deleteThisFolderLinksV10(folderID: Long) {
        return localDatabase.linksDao().deleteThisFolderLinksV10(folderID)
    }

    override suspend fun deleteMultipleLinksFromLinksTable(links: Array<Long>) {
        return localDatabase.linksDao().deleteMultipleLinksFromLinksTable(links)
    }

    override suspend fun deleteThisFolderLinksV9(folderName: String) {
        return localDatabase.linksDao().deleteThisFolderLinksV9(folderName)
    }

    override suspend fun deleteThisArchiveFolderDataV9(folderID: String) {
        return localDatabase.linksDao().deleteThisArchiveFolderDataV9(folderID)
    }

    override suspend fun addANewLinkToImpLinks(
        importantLink: ImportantLinks,
        onTaskCompleted: () -> Unit,
        autoDetectTitle: Boolean
    ): CommonUiEvents {
        if (!isAValidURL(importantLink.webURL)) {
            onTaskCompleted()
            return CommonUiEvents.ShowToast("invalid url")
        } else if (doesThisExistsInImpLinks(importantLink.webURL)
        ) {
            onTaskCompleted()
            return CommonUiEvents.ShowToast("given link already exists")
        } else {
            when (val linkDataExtractor =
                linkMetaDataScrapperService.scrapeLinkData(importantLink.webURL)) {
                is LinkMetaDataScrapperResult.Failure -> {
                    localDatabase.linksDao()
                        .addANewLinkToImpLinks(importantLink)
                    onTaskCompleted()
                    return CommonUiEvents.ShowToast("couldn't retrieve metadata now, but linkora saved the link")
                }

                is LinkMetaDataScrapperResult.Success -> {
                    val importantLinkScrappedData = ImportantLinks(
                        title = linkDataExtractor.data.title,
                        webURL = importantLink.webURL,
                        baseURL = linkDataExtractor.data.baseURL,
                        imgURL = linkDataExtractor.data.imgURL,
                        infoForSaving = importantLink.infoForSaving
                    )
                    localDatabase.linksDao()
                        .addANewLinkToImpLinks(importantLinkScrappedData)
                    onTaskCompleted()
                    return CommonUiEvents.ShowToast("added the url")
                }
            }
        }
    }

    override suspend fun addANewLinkToArchiveLink(archivedLinks: ArchivedLinks) {
        return localDatabase.linksDao().addANewLinkToArchiveLink(archivedLinks)
    }

    override fun getAllSavedLinks(): Flow<List<LinksTable>> {
        return localDatabase.linksDao().getAllSavedLinks()
    }

    override suspend fun getAllFromLinksTable(): List<LinksTable> {
        return localDatabase.linksDao().getAllFromLinksTable()
    }

    override suspend fun getAllRecentlyVisitedLinks(): List<RecentlyVisited> {
        return localDatabase.linksDao().getAllRecentlyVisitedLinks()
    }

    override suspend fun getAllImpLinks(): List<ImportantLinks> {
        return localDatabase.linksDao().getAllImpLinks()
    }

    override suspend fun getAllArchiveLinks(): List<ArchivedLinks> {
        return localDatabase.linksDao().getAllArchiveLinks()
    }

    override fun getAllArchiveFoldersLinks(): Flow<List<LinksTable>> {
        return localDatabase.linksDao().getAllArchiveFoldersLinks()
    }

    override fun getLinksOfThisFolderV10(folderID: Long): Flow<List<LinksTable>> {
        return localDatabase.linksDao().getLinksOfThisFolderV10(folderID)
    }

    override fun getLinksOfThisFolderV9(folderName: String): Flow<List<LinksTable>> {
        return localDatabase.linksDao().getLinksOfThisFolderV9(folderName)
    }

    override fun getThisArchiveFolderLinksV10(folderID: Long): Flow<List<LinksTable>> {
        return localDatabase.linksDao().getThisArchiveFolderLinksV10(folderID)
    }

    override fun getThisArchiveFolderLinksV9(folderName: String): Flow<List<LinksTable>> {
        return localDatabase.linksDao().getThisArchiveFolderLinksV9(folderName)
    }

    override suspend fun doesThisExistsInImpLinks(webURL: String): Boolean {
        return localDatabase.linksDao().doesThisExistsInImpLinks(webURL)
    }

    override suspend fun doesThisExistsInSavedLinks(webURL: String): Boolean {
        return localDatabase.linksDao().doesThisExistsInSavedLinks(webURL)
    }

    override suspend fun doesThisLinkExistsInAFolderV10(webURL: String, folderID: Long): Boolean {
        return localDatabase.linksDao().doesThisLinkExistsInAFolderV10(webURL, folderID)
    }

    override suspend fun getLastIDOfHistoryTable(): Long {
        return localDatabase.linksDao().getLastIDOfHistoryTable()
    }

    override suspend fun getLastIDOfLinksTable(): Long {
        return localDatabase.linksDao().getLastIDOfLinksTable()
    }

    override suspend fun getLastIDOfImpLinksTable(): Long {
        return localDatabase.linksDao().getLastIDOfImpLinksTable()
    }

    override suspend fun getLastIDOfArchivedLinksTable(): Long {
        return localDatabase.linksDao().getLastIDOfArchivedLinksTable()
    }

    override suspend fun doesThisLinkExistsInAFolderV9(
        webURL: String,
        folderName: String
    ): Boolean {
        return localDatabase.linksDao().doesThisLinkExistsInAFolderV9(webURL, folderName)
    }

    override suspend fun doesThisExistsInArchiveLinks(webURL: String): Boolean {
        return localDatabase.linksDao().doesThisExistsInArchiveLinks(webURL)
    }

    override suspend fun doesThisExistsInRecentlyVisitedLinks(webURL: String): Boolean {
        return localDatabase.linksDao().doesThisExistsInRecentlyVisitedLinks(webURL)
    }

    override suspend fun isLinksTableEmpty(): Boolean {
        return localDatabase.linksDao().isLinksTableEmpty()
    }

    override suspend fun isArchivedLinksTableEmpty(): Boolean {
        return localDatabase.linksDao().isArchivedLinksTableEmpty()
    }

    override suspend fun isArchivedFoldersTableEmpty(): Boolean {
        return localDatabase.linksDao().isArchivedFoldersTableEmpty()
    }

    override suspend fun isFoldersTableEmpty(): Boolean {
        return localDatabase.foldersDao().isFoldersTableEmpty()
    }

    override suspend fun isImpLinksTableEmpty(): Boolean {
        return localDatabase.linksDao().isImpLinksTableEmpty()
    }

    override suspend fun isHistoryLinksTableEmpty(): Boolean {
        return localDatabase.linksDao().isHistoryLinksTableEmpty()
    }

    override suspend fun copyLinkFromLinksTableToArchiveLinks(id: Long) {
        localDatabase.linksDao().copyLinkFromLinksTableToArchiveLinks(id)
    }

    override suspend fun copyLinkFromImpLinksTableToArchiveLinks(id: Long) {
        localDatabase.linksDao().copyLinkFromImpLinksTableToArchiveLinks(id)
    }

    override suspend fun copyLinkFromImpTableToArchiveLinks(link: String) {
        localDatabase.linksDao().copyLinkFromImpTableToArchiveLinks(link)
    }

    override suspend fun updateALinkDataFromLinksTable(linksTable: LinksTable) {
        localDatabase.linksDao().updateALinkDataFromLinksTable(linksTable)
    }

    override suspend fun updateALinkDataFromImpLinksTable(importantLinks: ImportantLinks) {
        localDatabase.linksDao().updateALinkDataFromImpLinksTable(importantLinks)
    }

    override suspend fun updateALinkDataFromRecentlyVisitedLinksTable(recentlyVisited: RecentlyVisited) {
        localDatabase.linksDao().updateALinkDataFromRecentlyVisitedLinksTable(recentlyVisited)
    }

    override suspend fun updateALinkDataFromArchivedLinksTable(archivedLinks: ArchivedLinks) {
        localDatabase.linksDao().updateALinkDataFromArchivedLinksTable(archivedLinks)
    }

    override suspend fun renameALinkTitleFromRecentlyVisited(webURL: String, newTitle: String) {
        localDatabase.linksDao().renameALinkTitleFromRecentlyVisited(webURL, newTitle)
    }

    override suspend fun renameALinkInfoFromRecentlyVisitedLinks(webURL: String, newInfo: String) {
        localDatabase.linksDao().renameALinkInfoFromRecentlyVisitedLinks(webURL, newInfo)
    }

    override suspend fun updateImpLinkTitle(id: Long, newTitle: String) {
        localDatabase.linksDao().renameALinkTitleFromImpLinks(id, newTitle)
    }

    override suspend fun updateImpLinkNote(id: Long, newInfo: String) {
        localDatabase.linksDao().renameALinkInfoFromImpLinks(id, newInfo)
    }

    override suspend fun updateLinkInfoFromLinksTable(linkID: Long, newInfo: String) {
        localDatabase.linksDao().renameALinkInfoFromLinksTable(linkID, newInfo)
    }

    override suspend fun renameALinkTitleFromArchiveLinks(webURL: String, newTitle: String) {
        localDatabase.linksDao().renameALinkTitleFromArchiveLinks(webURL, newTitle)
    }

    override suspend fun renameALinkInfoFromSavedLinks(webURL: String, newInfo: String) {
        localDatabase.linksDao().renameALinkInfoFromSavedLinks(webURL, newInfo)
    }

    override suspend fun renameALinkInfoFromArchiveLinks(webURL: String, newInfo: String) {
        localDatabase.linksDao().renameALinkInfoFromArchiveLinks(webURL, newInfo)
    }

    override suspend fun renameALinkInfoFromArchiveBasedFolderLinksV10(
        webURL: String,
        newInfo: String,
        folderID: Long
    ) {
        localDatabase.linksDao()
            .renameALinkInfoFromArchiveBasedFolderLinksV10(webURL, newInfo, folderID)
    }

    override suspend fun renameALinkInfoFromArchiveBasedFolderLinksV9(
        webURL: String,
        newInfo: String,
        folderName: String
    ) {
        localDatabase.linksDao()
            .renameALinkInfoFromArchiveBasedFolderLinksV9(webURL, newInfo, folderName)
    }

    override suspend fun renameALinkTitleFromArchiveBasedFolderLinksV10(
        webURL: String,
        newTitle: String,
        folderID: Long
    ) {
        localDatabase.linksDao()
            .renameALinkTitleFromArchiveBasedFolderLinksV10(webURL, newTitle, folderID)
    }

    override suspend fun renameALinkTitleFromArchiveBasedFolderLinksV9(
        webURL: String,
        newTitle: String,
        folderName: String
    ) {
        localDatabase.linksDao()
            .renameALinkTitleFromArchiveBasedFolderLinksV9(webURL, newTitle, folderName)
    }

    override suspend fun renameFolderNameForExistingArchivedFolderDataV9(
        currentFolderName: String,
        newFolderName: String
    ) {
        localDatabase.linksDao()
            .renameFolderNameForExistingArchivedFolderDataV9(currentFolderName, newFolderName)
    }

    override suspend fun renameFolderNameForExistingFolderDataV9(
        currentFolderName: String,
        newFolderName: String
    ) {
        localDatabase.linksDao()
            .renameFolderNameForExistingFolderDataV9(currentFolderName, newFolderName)
    }

    override suspend fun moveFolderLinksDataToArchiveV9(folderName: String) {
        localDatabase.linksDao().moveFolderLinksDataToArchiveV9(folderName)
    }

    override suspend fun moveArchiveFolderBackToRootFolderV10(folderID: Long) {
        localDatabase.linksDao().moveArchiveFolderBackToRootFolderV10(folderID)
    }

    override suspend fun moveArchiveFolderBackToRootFolderV9(folderName: String) {
        localDatabase.linksDao().moveArchiveFolderBackToRootFolderV9(folderName)
    }

    override suspend fun renameALinkTitleFromSavedLinks(webURL: String, newTitle: String) {
        localDatabase.linksDao().renameALinkTitleFromSavedLinks(webURL, newTitle)
    }

    override suspend fun updateLinkTitleFromLinksTable(linkID: Long, newTitle: String) {
        localDatabase.linksDao().updateLinkInfoFromLinksTable(linkID, newTitle)
    }

    override suspend fun renameALinkTitleFromFoldersV9(
        webURL: String,
        newTitle: String,
        folderName: String
    ) {
        localDatabase.linksDao().renameALinkTitleFromFoldersV9(webURL, newTitle, folderName)
    }

    override suspend fun addANewLinkInRecentlyVisited(recentlyVisited: RecentlyVisited) {
        localDatabase.linksDao().addANewLinkInRecentlyVisited(recentlyVisited)
    }

}