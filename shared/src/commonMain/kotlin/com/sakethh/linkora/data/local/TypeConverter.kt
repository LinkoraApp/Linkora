package com.sakethh.linkora.data.local

import androidx.room3.ColumnTypeConverter
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.model.tag.LinkTag
import kotlinx.serialization.json.Json

class TypeConverter {
    @ColumnTypeConverter
    fun stringToLinkType(string: String): LinkType = LinkType.valueOf(string)

    @ColumnTypeConverter
    fun linkTypeToString(linkType: LinkType): String = linkType.name

    @ColumnTypeConverter
    fun stringToMediaType(string: String): MediaType = MediaType.valueOf(string)

    @ColumnTypeConverter
    fun mediaTypeToString(mediaType: MediaType): String = mediaType.name

    @ColumnTypeConverter
    fun fromTagList(value: List<LinkTag>): String = Json.encodeToString(value)

    @ColumnTypeConverter
    fun toTagList(value: String): List<LinkTag> = Json.decodeFromString(value)
}
