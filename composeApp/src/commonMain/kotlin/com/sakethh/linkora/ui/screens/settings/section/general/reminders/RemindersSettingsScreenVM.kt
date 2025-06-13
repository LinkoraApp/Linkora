package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.ReminderRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class RemindersSettingsScreenVM(
    private val reminderRepo: ReminderRepo, private val linksRepo: LocalLinksRepo
) : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    companion object {
        val selectedWeeklyDays = mutableStateListOf<String>()
        val selectedMonthlyDates = mutableStateListOf<Int>()
    }

    val reminders = reminderRepo.getAllReminders().map {
        it.map {
            ReminderData(
                link = linksRepo.getALink(it.linkId), reminder = it.copy(
                    date = if (it.date != null) Reminder.Date(
                        month = if (it.date.month.toString().length == 1) "0${it.date.month}" else it.date.month,
                        dayOfMonth = if (it.date.dayOfMonth.toString().length == 1) "0${it.date.dayOfMonth}" else it.date.dayOfMonth,
                        year = it.date.year
                    ) else null, time = if (it.time != null) Reminder.Time(
                        hour = if (it.time.hour.toString().length == 1) "0${it.time.hour}" else it.time.hour,
                        minute = if (it.time.minute.toString().length == 1) "0${it.time.minute}" else it.time.minute
                    ) else null
                )
            )
        }
    }.combine(snapshotFlow { searchQuery }) { allReminders, searchQuery ->
        allReminders to searchQuery
    }.transform { (it, searchQuery) ->
        if (searchQuery.isBlank()) {
            emit(it)
        } else {
            emit(it.filter {
                if(it.reminder.time == null) false else it.reminder.time.hour.contains(searchQuery) || it.reminder.time.minute.contains(
                    searchQuery
                ) || if(it.reminder.date == null) false else it.reminder.date.month.contains(searchQuery) || it.reminder.date.year.contains(
                    searchQuery
                ) || it.reminder.date.dayOfMonth.contains(searchQuery) || it.reminder.title.contains(
                    searchQuery
                ) || it.reminder.description.contains(
                    searchQuery
                ) || it.link.title.contains(searchQuery) || it.link.note.contains(searchQuery) || it.link.url.contains(
                    searchQuery
                )
            })
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
                onSuccess = { base64String ->
                    // yet another update needs to be done as the image and title may have changed if a refresh of the link took place after saving the reminder
                    reminderRepo.updateAReminder(updatedReminder.copy(linkView = base64String))
                }, onFailure = {})
        }.invokeOnCompletion {
            onCompletion()
        }
    }
}