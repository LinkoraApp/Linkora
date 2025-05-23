package com.sakethh.linkora.data.local

import androidx.room.TypeConverter
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.model.Reminder
import kotlinx.serialization.encodeToString
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
    fun dateToSerializedString(date: Reminder.Date) = Json.encodeToString(date)

    @TypeConverter
    fun serializedStringToDate(string: String) = Json.decodeFromString<Reminder.Date>(string)

    @TypeConverter
    fun timeToSerializedString(time: Reminder.Time) = Json.encodeToString(time)

    @TypeConverter
    fun serializedStringToTime(string: String) = Json.decodeFromString<Reminder.Time>(string)
}