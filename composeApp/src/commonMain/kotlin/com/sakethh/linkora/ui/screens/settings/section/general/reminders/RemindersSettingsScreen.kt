package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.ManageReminderBtmSheet
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.utils.genericViewModelFactory
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSettingsScreen() {
    val navController = LocalNavController.current
    val localUriHandler = LocalUriHandler.current
    val manageReminderBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isManageReminderBtmSheetVisible = rememberSaveable { mutableStateOf(false) }
    val selectedReminderData = rememberDeserializableMutableObject {
        mutableStateOf<ReminderData>(
            ReminderData(
                link = Link(
                    linkType = LinkType.SAVED_LINK,
                    localId = 0,
                    remoteId = 0,
                    title = "TODO()",
                    url = "TODO()",
                    baseURL = "TODO()",
                    imgURL = "TODO()",
                    note = "TODO()",
                    idOfLinkedFolder = 0,
                    userAgent = "TODO()",
                    markedAsImportant = false,
                    mediaType = MediaType.GIF,
                    lastModified = 0
                ), reminder = Reminder(
                    linkId = 0,
                    title = "TODO()",
                    description = "TODO()",
                    reminderType = Reminder.Type.ONCE,
                    reminderMode = Reminder.Mode.CRUCIAL,
                    date = Reminder.Date("", "", ""),
                    time = Reminder.Time("", ""),
                    linkView = "TODO()"
                )
            )
        )
    }
    val remindersSettingsScreenVM: RemindersSettingsScreenVM =
        viewModel(factory = genericViewModelFactory {
            RemindersSettingsScreenVM(
                reminderRepo = DependencyContainer.remindersRepo.value,
                linksRepo = DependencyContainer.localLinksRepo.value
            )
        })
    val coroutineScope = rememberCoroutineScope()

    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState(is24Hour = false)

    val reminders = remindersSettingsScreenVM.reminders.collectAsStateWithLifecycle()
    val localFocusManager = LocalFocusManager.current
    SettingsSectionScaffold(
        topAppBarText = "Manage Reminders", navController = navController, bottomBar = {
            OutlinedTextField(
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    localFocusManager.clearFocus(force = true)
                }),
                placeholder = {
                    Text(text = "Search Reminders", style = MaterialTheme.typography.titleSmall)
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    AnimatedVisibility(
                        enter = fadeIn(),
                        exit = fadeOut(),
                        visible = remindersSettingsScreenVM.searchQuery.isNotBlank()
                    ) {
                        IconButton(modifier = Modifier.padding(end = 5.dp), onClick = {
                            localFocusManager.clearFocus(force = true)
                            remindersSettingsScreenVM.updateSearchQuery("")
                        }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                        }
                    }
                },
                value = remindersSettingsScreenVM.searchQuery,
                onValueChange = {
                    remindersSettingsScreenVM.updateSearchQuery(it)
                },
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.fillMaxWidth().padding(15.dp),
            )
        }) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection).fillMaxSize()
        ) {
            item {
                Text(
                    text = "Upcoming",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(
                        top = 15.dp, start = 15.dp, end = 15.dp, bottom = 7.5.dp
                    ),
                )
            }
            items(reminders.value) {
                ReminderComponent(link = it.link, reminder = it.reminder, onUrlClick = { url ->
                    localUriHandler.openUri(url)
                }, onDeleteClick = {
                    remindersSettingsScreenVM.deleteAReminder(it.reminder.id)
                }, onEditClick = {
                    selectedReminderData.value = it

                    timePickerState.minute = it.reminder.time.minute.toInt()
                    timePickerState.hour = it.reminder.time.hour.toInt()
                    timePickerState.isAfternoon = it.reminder.time.hour.toInt() > 12

                    datePickerState.selectedDateMillis = Calendar.getInstance().apply {
                        set(
                            it.reminder.date.year.toInt(),
                            it.reminder.date.month.toInt() - 1,
                            it.reminder.date.dayOfMonth.toInt(),
                            it.reminder.time.hour.toInt(),
                            it.reminder.time.minute.toInt(),
                            it.reminder.time.second.toInt()
                        )
                    }.timeInMillis

                    isManageReminderBtmSheetVisible.value = true
                    coroutineScope.launch {
                        manageReminderBtmSheetState.show()
                    }
                })
            }
        }
    }
    ManageReminderBtmSheet(
        isVisible = isManageReminderBtmSheetVisible,
        btmSheetState = manageReminderBtmSheetState,
        link = selectedReminderData.value.link,
        onSaveClick = { title, description, selectedReminderType, selectedReminderMode, datePickerState, timePickerState, graphicsLayer ->
            remindersSettingsScreenVM.updateAReminder(
                reminderId = selectedReminderData.value.reminder.id,
                title = title,
                description = description,
                timePickerState = timePickerState,
                datePickerState = datePickerState,
                reminderType = selectedReminderType,
                reminderMode = selectedReminderMode,
                graphicsLayer = graphicsLayer
            ) {
                coroutineScope.launch {
                    manageReminderBtmSheetState.hide()
                }.invokeOnCompletion {
                    isManageReminderBtmSheetVisible.value = false
                }
            }
        },
        scheduledReminder = null,
        sheetTitle = "Update Reminder",
        reminderTitle = rememberSaveable(selectedReminderData.value) {
            mutableStateOf(selectedReminderData.value.reminder.title)
        },
        reminderDesc = rememberSaveable(selectedReminderData.value) {
            mutableStateOf(selectedReminderData.value.reminder.description)
        },
        datePickerState = datePickerState,
        timePickerState = timePickerState,
        selectedTime = rememberSaveable(selectedReminderData.value) {
            mutableStateOf("${selectedReminderData.value.reminder.time.hour}:${selectedReminderData.value.reminder.time.minute}")
        },
        selectedDate = rememberSaveable(selectedReminderData.value) {
            mutableStateOf("${selectedReminderData.value.reminder.date.dayOfMonth}-${selectedReminderData.value.reminder.date.month}-${selectedReminderData.value.reminder.date.year}")
        },
        selectedReminderType = rememberSaveable(selectedReminderData.value) {
            mutableStateOf(selectedReminderData.value.reminder.reminderType.name)
        },
        selectedReminderMode = rememberSaveable(selectedReminderData.value) {
            mutableStateOf(selectedReminderData.value.reminder.reminderMode.name)
        },
    )
}