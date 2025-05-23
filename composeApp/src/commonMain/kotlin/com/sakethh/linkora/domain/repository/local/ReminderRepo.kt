package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepo {
    suspend fun createAReminder(reminder: Reminder): Long

    suspend fun updateAReminder(reminder: Reminder)

    suspend fun deleteAReminder(reminderId: Long)

    suspend fun getAReminder(reminderId: Long): Reminder

    fun getAllReminders(): Flow<List<Reminder>>
}