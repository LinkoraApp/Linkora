package com.sakethh.linkora.domain.model

import com.sakethh.linkora.ui.domain.ReminderMode
import com.sakethh.linkora.ui.screens.settings.section.general.reminders.ReminderType
import java.time.LocalDateTime


data class Reminder(
    val id: Long = 0,
    val linkId: Long,
    val title: String,
    val description: String,
    val reminderType: ReminderType,
    val reminderMode: ReminderMode,
    val time: LocalDateTime
)