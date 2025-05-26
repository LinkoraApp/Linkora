package com.sakethh.linkora.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.sakethh.linkora.common.utils.roundedCornerShape
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.components.link.LinkListItemComposable
import com.sakethh.linkora.ui.domain.model.LinkUIComponentParam
import com.sakethh.linkora.ui.screens.settings.section.data.components.ToggleButton
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.pulsateEffect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageReminderBtmSheet(
    isVisible: MutableState<Boolean>,
    btmSheetState: SheetState,
    link: Link,
    onSaveClick: (
        title: String, description: String, selectedReminderType: Reminder.Type, selectedReminderMode: Reminder.Mode, datePickerState: DatePickerState, timePickerState: TimePickerState, graphicsLayer: GraphicsLayer
    ) -> Unit,
    scheduledReminder: Reminder?,
    onEditClick: (reminder: Reminder) -> Unit = {},
    onDeleteClick: (reminder: Reminder) -> Unit = {},
    sheetTitle: String? = null,
    reminderTitle: MutableState<String> = rememberSaveable {
        mutableStateOf("")
    },
    reminderDesc: MutableState<String> = rememberSaveable {
        mutableStateOf("")
    },
    datePickerState: DatePickerState = rememberDatePickerState(),
    timePickerState: TimePickerState = rememberTimePickerState(is24Hour = false),
    selectedTime: MutableState<String> = rememberSaveable {
        mutableStateOf("Click to select time")
    },
    selectedDate: MutableState<String> = rememberSaveable {
        mutableStateOf("Click to select date")
    },
    selectedReminderType: MutableState<String> = rememberSaveable {
        mutableStateOf(Reminder.Type.ONCE.toString())
    },
    selectedReminderMode: MutableState<String> = rememberSaveable {
        mutableStateOf(Reminder.Mode.CRUCIAL.name)
    }
) {
    val coroutineScope = rememberCoroutineScope()
    val graphicsLayer = rememberGraphicsLayer()
    if (isVisible.value.not()) return
    val forceScheduleReminder = rememberSaveable {
        mutableStateOf(false)
    }
    val showDatePicker = rememberSaveable {
        mutableStateOf(false)
    }
    val showTimePicker = rememberSaveable {
        mutableStateOf(false)
    }
    val showProgressbar = rememberSaveable {
        mutableStateOf(false)
    }
    val remindersType = remember {
        listOf(Reminder.Type.ONCE, Reminder.Type.PERIODIC, Reminder.Type.STICKY)
    }
    ModalBottomSheet(
        properties = remember { ModalBottomSheetProperties(shouldDismissOnBackPress = false) },
        sheetState = btmSheetState,
        onDismissRequest = {
            isVisible.value = false
        }) {
        LazyColumn(modifier = Modifier.wrapContentSize().animateContentSize()) {
            item {
                Spacer(modifier = Modifier.height(7.5.dp))
                Text(
                    text = sheetTitle
                        ?: if (forceScheduleReminder.value.not() && scheduledReminder != null) "A reminder is already scheduled for this link" else "Add a new Reminder",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(
                        start = 15.dp, end = 15.dp
                    ),
                )
            }
            item {
                LinkListItemComposable(
                    linkUIComponentParam = LinkUIComponentParam(
                        link = link,
                        isSelectionModeEnabled = mutableStateOf(false),
                        onMoreIconClick = {},
                        onLinkClick = {},
                        onForceOpenInExternalBrowserClicked = {},
                        isItemSelected = mutableStateOf(false),
                        onLongClick = {},
                        showQuickOptions = false
                    ), forTitleOnlyView = false, modifier = Modifier.drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    })
            }
            if (scheduledReminder != null && forceScheduleReminder.value.not()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${scheduledReminder.date.dayOfMonth}-${scheduledReminder.date.month}-${scheduledReminder.date.year} ${scheduledReminder.time.hour}:${scheduledReminder.time.minute}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        IconButton(onClick = {
                            onDeleteClick(scheduledReminder)
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever, contentDescription = null
                            )
                        }
                    }
                    Button(
                        modifier = Modifier.pulsateEffect().fillMaxWidth()
                            .padding(start = 15.dp, end = 15.dp), onClick = {
                            forceScheduleReminder.value = true
                        }) {
                        Text(
                            text = "Schedule another reminder",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                return@LazyColumn
            }
            item {
                TextField(
                    modifier = Modifier.fillMaxWidth().padding(
                    start = 15.dp, end = 15.dp, bottom = 15.dp, top = 15.dp
                ), value = reminderTitle.value, onValueChange = {
                    reminderTitle.value = it
                }, textStyle = MaterialTheme.typography.titleSmall, placeholder = {
                    Text(
                        text = "Reminder title", style = MaterialTheme.typography.titleSmall
                    )
                })
                TextField(
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, bottom = 15.dp),
                    value = reminderDesc.value,
                    onValueChange = {
                        reminderDesc.value = it
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    placeholder = {
                        Text(
                            text = "Reminder description",
                            style = MaterialTheme.typography.titleSmall
                        )
                    })
            }
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Spacer(Modifier)
                    remindersType.forEachIndexed { index, reminder ->
                        val selected = selectedReminderType.value == reminder.toString()
                        key(index) {
                            ToggleButton(
                                shape = remindersType.roundedCornerShape(index),
                                checked = selected,
                                onCheckedChange = {
                                    selectedReminderType.value = reminder.toString()
                                }) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = reminder.imgVector,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                                    )
                                    Spacer(
                                        modifier = Modifier.width(5.dp)
                                    )
                                    Text(
                                        style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                                        text = reminder.toString(),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                when (selectedReminderType.value) {
                    Reminder.Type.ONCE.toString() -> {
                        ReminderSetting(
                            string = selectedDate.value,
                            icon = Icons.Default.CalendarMonth,
                            shape = RoundedCornerShape(
                                topStart = 15.dp,
                                topEnd = 15.dp,
                                bottomStart = 5.dp,
                                bottomEnd = 5.dp
                            ),
                            paddingValues = PaddingValues(
                                start = 15.dp, end = 15.dp, top = 15.dp, bottom = 7.5.dp
                            ),
                            onClick = {
                                showDatePicker.value = true
                            })
                        ReminderSetting(
                            string = selectedTime.value,
                            icon = Icons.Default.AccessTime,
                            shape = RoundedCornerShape(
                                bottomStart = 15.dp,
                                bottomEnd = 15.dp,
                                topStart = 5.dp,
                                topEnd = 5.dp
                            ),
                            paddingValues = PaddingValues(
                                start = 15.dp, end = 15.dp, bottom = 15.dp
                            ),
                            onClick = {
                                showTimePicker.value = true
                            })
                    }

                    Reminder.Type.PERIODIC.toString() -> {

                    }
                }
            }
            if (selectedReminderType.value != Reminder.Type.STICKY.toString()) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Spacer(Modifier)
                        Reminder.Mode.entries.forEachIndexed { index, mode ->
                            val selected = selectedReminderMode.value == mode.name
                            key(index) {
                                ToggleButton(
                                    shape = remindersType.roundedCornerShape(index),
                                    checked = selected,
                                    onCheckedChange = {
                                        selectedReminderMode.value = mode.name
                                    }) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = mode.imgVector,
                                            contentDescription = null,
                                            tint = if (selected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                                        )
                                        Spacer(
                                            modifier = Modifier.width(5.dp)
                                        )
                                        Text(
                                            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                                            text = mode.name,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                AnimatedContent(targetState = showProgressbar.value) {
                    if (it) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(15.dp))
                    } else {
                        Button(modifier = Modifier.fillMaxWidth().padding(15.dp), onClick = {
                            if (reminderTitle.value.isBlank() or reminderDesc.value.isBlank() or (selectedReminderType.value != Reminder.Type.STICKY.toString() && datePickerState.selectedDateMillis == null)) {
                                coroutineScope.launch {
                                    pushUIEvent(UIEvent.Type.ShowSnackbar(message = "All fields, including date and time, are required."))
                                }
                                return@Button
                            }
                            showProgressbar.value = true
                            onSaveClick(
                                reminderTitle.value,
                                reminderDesc.value,
                                remindersType.find {
                                    it.toString() == selectedReminderType.value
                                }!!,
                                Reminder.Mode.valueOf(selectedReminderMode.value),
                                datePickerState,
                                timePickerState,
                                graphicsLayer
                            )
                        }) {
                            Text(text = "Save", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = {
            showDatePicker.value = false
        }, confirmButton = {
            Button(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp, bottom = 10.dp), onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate.value = SimpleDateFormat("MMMM dd, yyyy").format(Date(it))
                    }
                    showDatePicker.value = false
                }) {
                Text(text = "Confirm", style = MaterialTheme.typography.titleMedium)
            }
        }, modifier = Modifier.scale(0.95f)
        ) {
            DatePicker(state = datePickerState)
        }
    }
    if (showTimePicker.value) {
        BasicAlertDialog(
            onDismissRequest = {
                showTimePicker.value = false
            },
            modifier = Modifier.clip(RoundedCornerShape(10.dp)).zIndex(100f)
                .background(AlertDialogDefaults.containerColor).clip(
                    AlertDialogDefaults.shape
                )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.height(15.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = "Pick the time",
                        fontSize = 24.sp,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 15.dp)
                    )
                }
                Spacer(modifier = Modifier.height(15.dp))
                TimePicker(state = timePickerState)
                Button(
                    onClick = {
                        selectedTime.value = buildString {
                            append((if (timePickerState.isAfternoon) timePickerState.hour - 12 else timePickerState.hour).run {
                                if (this.toString().length == 1) "0$this" else this
                            }.run {
                                if (this.toString() == "00" && timePickerState.isAfternoon) "12" else this
                            })
                            append(":")
                            append(timePickerState.minute.run {
                                if (this.toString().length == 1) "0$this" else this
                            })
                            append(if (timePickerState.isAfternoon) " PM" else " AM")
                        }
                        showTimePicker.value = false
                    },
                    modifier = Modifier.fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp, bottom = 15.dp)
                ) {
                    Text(text = "Confirm", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ReminderSetting(
    string: String,
    icon: ImageVector,
    paddingValues: PaddingValues,
    shape: Shape,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(paddingValues), shape = shape, onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(5.dp)
        ) {
            Text(
                text = string,
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 5.dp)
            )
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = icon, contentDescription = null
                )
            }
        }
    }
}