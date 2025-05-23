package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.ReminderRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersSettingsScreenVM(
    private val reminderRepo: ReminderRepo, private val linksRepo: LocalLinksRepo
) : ViewModel() {
    val reminders = reminderRepo.getAllReminders().map {
        it.map {
            ReminderData(
                link = linksRepo.getALink(it.linkId), reminder = it.copy(
                    date = Reminder.Date(
                        month = if (it.date.month.toString().length == 1) "0${it.date.month}" else it.date.month,
                        dayOfMonth = if (it.date.dayOfMonth.toString().length == 1) "0${it.date.dayOfMonth}" else it.date.dayOfMonth,
                        year = it.date.year
                    ), time = Reminder.Time(
                        hour = if (it.time.hour.toString().length == 1) "0${it.time.hour}" else it.time.hour,
                        minute = if (it.time.minute.toString().length == 1) "0${it.time.minute}" else it.time.minute
                    )
                )
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    fun deleteAReminder(reminderId: Long) {
        viewModelScope.launch {
            com.sakethh.cancelAReminder(reminderId.toInt())
            reminderRepo.deleteAReminder(reminderId)
        }
    }
}