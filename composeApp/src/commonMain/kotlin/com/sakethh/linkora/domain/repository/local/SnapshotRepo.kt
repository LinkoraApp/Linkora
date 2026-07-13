package com.sakethh.linkora.domain.repository.local

import androidx.compose.runtime.State
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.Snapshot
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.Tag
import kotlinx.coroutines.CoroutineScope

interface SnapshotRepo {
    val isAnySnapshotOngoing: State<Boolean>

    suspend fun getASnapshot(id: Long): Snapshot

    suspend fun addASnapshot(snapshot: Snapshot): Long

    suspend fun deleteASnapshot(id: Long)

    context(coroutineScope: CoroutineScope)
    fun collectLatestAndExport()

    suspend fun createAManualSnapshot(
        allLinks: List<Link>,
        allFolders: List<Folder>,
        allPanels: List<Panel>,
        allPanelFolders: List<PanelFolder>,
        allTags: List<Tag>,
        allLinkTagsPairs: List<LinkTag>,
        onCompletion: () -> Unit,
    )
}
