package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.model.link.Link

data class ReminderData(
    val link: Link, val reminder: Reminder
)

