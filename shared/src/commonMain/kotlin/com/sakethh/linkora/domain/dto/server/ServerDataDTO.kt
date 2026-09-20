package com.sakethh.linkora.domain.dto.server

import com.sakethh.linkora.domain.dto.server.folder.FolderDTO
import com.sakethh.linkora.domain.dto.server.link.LinkDTO
import com.sakethh.linkora.domain.dto.server.panel.PanelDTO
import com.sakethh.linkora.domain.dto.server.panel.PanelFolderDTO
import com.sakethh.linkora.domain.dto.server.tag.TagDTO
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.LinkTagDTO
import com.sakethh.linkora.domain.model.tag.Tag
import kotlinx.serialization.Serializable

@Serializable
data class ServerDataDTO(
    val links: List<LinkDTO>,
    val folders: List<FolderDTO>,
    val panels: List<PanelDTO>,
    val panelFolders: List<PanelFolderDTO>,
    val tags: List<TagDTO>,
    val linkTags: List<LinkTagDTO>,
)

@Serializable
data class AllTablesDTO(
    val links: List<Link>,
    val folders: List<Folder>,
    val panels: List<Panel>,
    val panelFolders: List<PanelFolder>,
    val tags: List<Tag>,
    val linkTagsPairs: List<LinkTag>,
)
