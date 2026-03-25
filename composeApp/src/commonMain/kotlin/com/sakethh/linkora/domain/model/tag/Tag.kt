package com.sakethh.linkora.domain.model.tag

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.utils.getSystemEpochSeconds
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "tags", indices = [
        Index(value = ["name"], name = "idx_tags_name")
    ]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val remoteId: Long? = null,
    val lastModified: Long = getSystemEpochSeconds(),
    val name: String
)

@Entity(
    tableName = "link_tags", primaryKeys = ["linkId", "tagId"], foreignKeys = [ForeignKey(
        entity = Link::class,
        parentColumns = ["localId"],
        childColumns = ["linkId"],
        onDelete = ForeignKey.CASCADE
    ), ForeignKey(
        entity = Tag::class,
        parentColumns = ["localId"],
        childColumns = ["tagId"],
        onDelete = ForeignKey.CASCADE
    )]
)
@Serializable
data class LinkTag(
    val remoteId: Long? = null,
    val linkId: Long,
    val tagId: Long,
    val lastModified: Long = getSystemEpochSeconds()
)