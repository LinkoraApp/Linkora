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

    @TypeConverter
    fun reminderTypeToSerializedString(type: Reminder.Type) = type.toString()

    @TypeConverter
    fun serializedReminderTypeToReminderTypeObj(string: String): Reminder.Type = when (string) {
        Reminder.Type.ONCE.toString() -> Reminder.Type.ONCE
        Reminder.Type.PERIODIC.WEEKLY.toString() -> Reminder.Type.PERIODIC.WEEKLY
        Reminder.Type.PERIODIC.MONTHLY.toString() -> Reminder.Type.PERIODIC.MONTHLY
        else -> Reminder.Type.STICKY
    }

    @TypeConverter
    fun listOfStringToString(list: List<String>): String {
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun listOfIntToString(list: List<Int>): String {
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun stringToListOfString(string: String): List<String> {
        return Json.decodeFromString(string)
    }

    @TypeConverter
    fun stringToListOfInt(string: String): List<Int> {
        return Json.decodeFromString(string)
    }
}