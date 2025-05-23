package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.model.Reminder

interface ReminderRepo {
    suspend fun createAReminder(reminder: Reminder): Long

    suspend fun deleteAReminder(reminderId: Long)
}