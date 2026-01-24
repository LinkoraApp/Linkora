package com.sakethh.linkora.data.local.repository

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import com.sakethh.linkora.data.local.dao.SnapshotDao
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.FileType
import com.sakethh.linkora.domain.SnapshotFormat
import com.sakethh.linkora.domain.dto.server.AllTablesDTO
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.PanelForJSONExportSchema
import com.sakethh.linkora.domain.model.Snapshot
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.repository.ExportDataRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.SnapshotRepo
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.AppVM.Companion.pauseSnapshots
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.pushSnackbar
import com.sakethh.linkora.utils.septetCombine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SnapshotRepoImpl(
    private val snapshotDao: SnapshotDao,
    private val isSnapshotsEnabled: State<Boolean>,
    private val autoDeleteSnapshots: () -> Boolean,
    private val snapshotExportFormatId: () -> Int,
    private val linksRepo: LocalLinksRepo,
    private val foldersRepo: LocalFoldersRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val exportDataRepo: ExportDataRepo,
    private val localTagsRepo: LocalTagsRepo,
    private val fileManager: FileManager,
) : SnapshotRepo {

    private var snapshotsJob: Job? = null

    private val _isAnySnapshotOngoing = mutableStateOf(false)

    override val isAnySnapshotOngoing: State<Boolean> = _isAnySnapshotOngoing

    companion object {
        private val forceSnapshot = mutableStateOf(false)

        fun forceTriggerASnapshot() {
            forceSnapshot.value = !forceSnapshot.value
        }
    }

    override suspend fun getASnapshot(id: Long): Snapshot {
        return snapshotDao.getASnapshot(id)
    }

    override suspend fun addASnapshot(snapshot: Snapshot): Long {
        return snapshotDao.addASnapshot(snapshot)
    }

    override suspend fun deleteASnapshot(id: Long) {
        snapshotDao.deleteASnapshot(id)
    }

    // REFACTOR: NESTED collectLatest
    override context(coroutineScope: CoroutineScope) fun collectLatestAndExport() {
        snapshotsJob?.cancel()
        coroutineScope.launch {
            snapshotFlow {
                isSnapshotsEnabled.value
            }.debounce(1000).collectLatest {
                if (it) {
                    snapshotsJob = this.launch(Dispatchers.Default) {
                        linkoraLog("data checks for snapshots are now enabled")
                        septetCombine(
                            linksRepo.getAllLinksAsFlow(),
                            foldersRepo.getAllFoldersAsFlow(),
                            localPanelsRepo.getAllThePanels(),
                            localPanelsRepo.getAllThePanelFoldersAsAFlow(),
                            snapshotFlow {
                                forceSnapshot.value
                            },
                            localTagsRepo.getAllTags(AppPreferences.selectedSortingType.value),
                            localTagsRepo.getAllLinkTags()
                        ) { links, folders, panels, panelFolders, _, tags, linkTags ->
                            AllTablesDTO(
                                links = links,
                                folders = folders,
                                panels = panels,
                                panelFolders = panelFolders,
                                tags = tags,
                                linkTagsPairs = linkTags,
                            )
                        }.cancellable().drop(1).debounce(1000).flowOn(Dispatchers.Default)
                            .collectLatest {
                                if (pauseSnapshots || (it.links + it.folders + it.panelFolders + it.panels + it.tags + it.linkTagsPairs).isEmpty()) return@collectLatest
                                try {
                                    _isAnySnapshotOngoing.value = true
                                    createAManualSnapshot(
                                        allLinks = it.links,
                                        allFolders = it.folders,
                                        allPanels = it.panels,
                                        allPanelFolders = it.panelFolders,
                                        allTags = it.tags,
                                        allLinkTagsPairs = it.linkTagsPairs,
                                        onCompletion = {})
                                } catch (e: Exception) {
                                    e.pushSnackbar()
                                } finally {
                                    _isAnySnapshotOngoing.value = false
                                }
                            }
                    }
                } else {
                    linkoraLog("data checks for snapshots are disabled")
                    snapshotsJob?.cancel()
                }
            }
        }
    }

    override suspend fun createAManualSnapshot(
        allLinks: List<Link>,
        allFolders: List<Folder>,
        allPanels: List<Panel>,
        allPanelFolders: List<PanelFolder>,
        allTags: List<Tag>,
        allLinkTagsPairs: List<LinkTag>,
        onCompletion: () -> Unit
    ) {
        if (autoDeleteSnapshots()) {
            fileManager.deleteAutoBackups(
                backupLocation = AppPreferences.currentBackupLocation.value,
                threshold = AppPreferences.backupAutoDeleteThreshold.intValue,
                onCompletion = {
                    linkoraLog(
                        "Deleted $it snapshot files as the threshold was ${AppPreferences.backupAutoDeleteThreshold.intValue}"
                    )
                })
        }

        if (snapshotExportFormatId() == SnapshotFormat.JSON.id || snapshotExportFormatId() == SnapshotFormat.BOTH.id) {

            val serializedJsonExportString =
                JSONExportSchema(schemaVersion = JSONExportSchema.VERSION, links = allLinks.map {
                    it.copy(
                        remoteId = null, lastModified = 0
                    )
                }, folders = allFolders.map {
                    it.copy(
                        remoteId = null, lastModified = 0
                    )
                }, panels = PanelForJSONExportSchema(panels = allPanels.map {
                    it.copy(
                        remoteId = null, lastModified = 0
                    )
                }, panelFolders = allPanelFolders.map {
                    it.copy(
                        remoteId = null, lastModified = 0
                    )
                }), tags = allTags.map {
                    it.copy(remoteId = null, lastModified = 0)
                }, linkTags = allLinkTagsPairs.map {
                    it.copy(remoteId = null, lastModified = 0)
                }).run {
                    Json.encodeToString(this)
                }

            fileManager.exportSnapshotData(
                exportLocation = AppPreferences.currentBackupLocation.value,
                rawExportString = serializedJsonExportString,
                fileType = FileType.JSON
            )
        }

        if (snapshotExportFormatId() == SnapshotFormat.HTML.id || snapshotExportFormatId() == SnapshotFormat.BOTH.id) {
            fileManager.exportSnapshotData(
                rawExportString = exportDataRepo.rawExportDataAsHTML(
                    links = allLinks, folders = allFolders
                ),
                fileType = ExportFileType.HTML,
                exportLocation = AppPreferences.currentBackupLocation.value,
            )
        }
        onCompletion()
    }
}