package com.sakethh.linkora.domain.model.legacy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Export")
data class LegacyExportSchema(
    /*
* `appVersion` should always be the first property.
* DO NOT CHANGE THIS FIRST PROPERTY.
* Since we have to know if the import file is based on the
* legacy or the latest schema,
* instead of parsing the content to an object two times,
* we will walk through the string manually to check if this property exists.
* Since we absolutely guarantee that we always have this property first,
* we won't have to check if we are walking through the correct chars or not.
*/
    @SerialName("appVersion") val schemaVersion: Int = 11,
    @SerialName("savedLinks") val linksTable: List<LinksTable>,
    @SerialName("importantLinks") val importantLinksTable: List<ImportantLinks>,
    @SerialName("folders") val foldersTable: List<FoldersTable>,
    @SerialName("archivedLinks") val archivedLinksTable: List<ArchivedLinks>,
    @SerialName("archivedFolders") val archivedFoldersTable: List<ArchivedFolders>,
    @SerialName("historyLinks") val historyLinksTable: List<RecentlyVisited>,
    val panels: List<Panel> = emptyList(),
    val panelFolders: List<PanelFolder> = emptyList(),
)