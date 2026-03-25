package com.sakethh.linkora.domain.model

import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.Tag
import kotlinx.serialization.Serializable

@Serializable
data class JSONExportSchema(
    /*
 * `schemaVersion` should always be the first property.
 * DO NOT CHANGE THIS FIRST PROPERTY.
 * Since we have to know if the import file is based on the
 * legacy or the latest schema,
 * instead of parsing the content to an object two times,
 * we will walk through the string manually to check if this property exists.
 * Since we absolutely guarantee that we always have this property first,
 * we won't have to check if we are walking through the correct chars or not.
 */
    val schemaVersion: Int,
    val links: List<Link>,
    val folders: List<Folder>,
    val panels: PanelForJSONExportSchema,
    val tags: List<Tag> = emptyList(),
    val linkTags: List<LinkTag> = emptyList()
) {
    companion object {
        const val VERSION = 13
    }
}

@Serializable
data class PanelForJSONExportSchema(
    val panels: List<Panel>, val panelFolders: List<PanelFolder>
)
