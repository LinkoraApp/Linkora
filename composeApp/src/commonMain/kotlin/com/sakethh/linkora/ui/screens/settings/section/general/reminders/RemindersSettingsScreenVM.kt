package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.ReminderRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

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

    @OptIn(ExperimentalMaterial3Api::class)
    fun updateAReminder(
        reminderId: Long,
        title: String,
        description: String,
        timePickerState: TimePickerState,
        datePickerState: DatePickerState,
        reminderType: Reminder.Type,
        reminderMode: Reminder.Mode,
        graphicsLayer: GraphicsLayer,
        onCompletion: () -> Unit
    ) {
        viewModelScope.launch {
            com.sakethh.cancelAReminder(reminderId.toInt())
            val selectedDate =
                SimpleDateFormat("yyyy\nMM\ndd").format(Date(datePickerState.selectedDateMillis!!))
                    .split("\n").map { it.toInt() }

            val localDate = Reminder.Date(
                selectedDate[0], selectedDate[1], selectedDate[2]
            )
            val localTime = Reminder.Time(
                timePickerState.hour, timePickerState.minute
            )
            val updatedReminder = reminderRepo.getAReminder(reminderId).copy(
                title = title,
                description = description,
                reminderType = reminderType,
                reminderMode = reminderMode,
                date = localDate,
                time = localTime
            )
            com.sakethh.scheduleAReminder(
                reminder = updatedReminder,
                graphicsLayer = graphicsLayer,
                onCompletion = { base64String ->
                    // yet another update needs to be done as the image and title may have changed if a refresh of the link took place after saving the reminder
                    reminderRepo.updateAReminder(updatedReminder.copy(linkView = base64String))
                })
        }.invokeOnCompletion {
            onCompletion()
        }
    }
}