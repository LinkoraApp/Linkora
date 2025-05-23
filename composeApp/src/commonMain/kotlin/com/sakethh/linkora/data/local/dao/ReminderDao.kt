package com.sakethh.linkora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sakethh.linkora.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert
    suspend fun createAReminder(reminder: Reminder): Long

    @Update
    suspend fun updateAReminder(reminder: Reminder)

    @Query("DELETE FROM reminder WHERE id = :reminderId")
    suspend fun deleteAReminder(reminderId: Long)

    @Query("SELECT * FROM reminder WHERE id = :reminderId")
    suspend fun getAReminder(reminderId: Long): Reminder

    @Query("SELECT * FROM reminder")
    fun getAllReminders(): Flow<List<Reminder>>
}