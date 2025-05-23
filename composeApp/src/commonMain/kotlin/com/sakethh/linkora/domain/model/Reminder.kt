package com.sakethh.linkora.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.sakethh.linkora.ui.domain.ReminderMode
import com.sakethh.linkora.ui.screens.settings.section.general.reminders.ReminderType
import kotlinx.serialization.Serializable


@Entity(tableName = "reminder")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val linkId: Long,
    val title: String,
    val description: String,
    val reminderType: ReminderType,
    val reminderMode: ReminderMode,
    val date: Date,
    val time: Time,
    val linkView: String
) {
    @Serializable
    data class Date(
        val year: Int, val month: Int, val dayOfMonth: Int
    )

    @Serializable
    data class Time(val hour: Int, val minute: Int, val second: Int = 0)
}