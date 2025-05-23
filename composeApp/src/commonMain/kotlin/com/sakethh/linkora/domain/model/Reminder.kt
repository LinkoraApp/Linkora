package com.sakethh.linkora.domain.model

import com.sakethh.linkora.ui.domain.ReminderMode
import com.sakethh.linkora.ui.screens.settings.section.general.reminders.ReminderType
import kotlinx.serialization.Serializable


data class Reminder(
    val id: Long = 0,
    val linkId: Long,
    val title: String,
    val description: String,
    val reminderType: ReminderType,
    val reminderMode: ReminderMode,
    val date: Date,
    val time: Time
) {
    @Serializable
    data class Date(
        val year: Int, val month: Int, val dayOfMonth: Int
    )

    @Serializable
    data class Time(val hour: Int, val minute: Int, val second: Int = 0)
}