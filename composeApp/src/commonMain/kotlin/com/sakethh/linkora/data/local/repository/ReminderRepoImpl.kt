package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.data.local.dao.ReminderDao
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.repository.local.ReminderRepo
import kotlinx.coroutines.flow.Flow

class ReminderRepoImpl(private val reminderDao: ReminderDao) : ReminderRepo {
    override suspend fun createAReminder(reminder: Reminder): Long {
        return reminderDao.createAReminder(reminder)
    }

    override suspend fun deleteAReminder(reminderId: Long) {
        reminderDao.deleteAReminder(reminderId)
    }

    override suspend fun getAReminder(reminderId: Long): Reminder {
        return reminderDao.getAReminder(reminderId)
    }

    override fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders()
    }

    override suspend fun updateAReminder(reminder: Reminder) {
        return reminderDao.updateAReminder(reminder)
    }

    override suspend fun existingReminder(linkId: Long): Reminder? {
        return reminderDao.existingReminder(linkId)
    }
}