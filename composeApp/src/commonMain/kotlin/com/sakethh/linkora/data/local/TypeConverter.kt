package com.sakethh.linkora.data.local

import androidx.room3.TypeConverter
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.model.tag.LinkTag
import kotlinx.serialization.json.Json

class TypeConverter {
    @TypeConverter
    fun stringToLinkType(string: String): LinkType = LinkType.valueOf(string)

    @TypeConverter
    fun linkTypeToString(linkType: LinkType): String = linkType.name

    @TypeConverter
    fun stringToMediaType(string: String): MediaType = MediaType.valueOf(string)

    @TypeConverter
    fun mediaTypeToString(mediaType: MediaType): String = mediaType.name

    @TypeConverter
    fun fromTagList(value: List<LinkTag>): String = Json.encodeToString(value)

    @TypeConverter
    fun toTagList(value: String): List<LinkTag> = Json.decodeFromString(value)
}