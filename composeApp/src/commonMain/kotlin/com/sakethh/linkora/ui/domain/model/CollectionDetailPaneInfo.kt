package com.sakethh.linkora.ui.domain.model

import androidx.compose.runtime.Stable
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.tag.Tag
import kotlinx.serialization.Serializable

@Serializable
@Stable
data class CollectionDetailPaneInfo(
    val currentFolder: Folder?,
    val currentTag: Tag?,
    val collectionType: CollectionType?,
)

enum class CollectionType {
    FOLDER,
    TAG
}
