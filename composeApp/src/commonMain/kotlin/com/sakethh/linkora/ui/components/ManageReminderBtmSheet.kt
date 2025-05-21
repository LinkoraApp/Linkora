package com.sakethh.linkora.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.common.utils.roundedCornerShape
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.components.link.LinkListItemComposable
import com.sakethh.linkora.ui.domain.ReminderMode
import com.sakethh.linkora.ui.domain.model.LinkUIComponentParam
import com.sakethh.linkora.ui.screens.settings.section.data.components.ToggleButton
import com.sakethh.linkora.ui.screens.settings.section.general.reminders.ReminderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageReminderBtmSheet(
    isVisible: MutableState<Boolean>, btmSheetState: SheetState, link: Link
) {
    if (isVisible.value.not()) return

    val selectedReminderType = rememberSaveable {
        mutableStateOf(ReminderType.ONCE.name)
    }
    val selectedReminderMode = rememberSaveable {
        mutableStateOf(ReminderMode.URGENT.name)
    }
    val selectedDate = rememberSaveable {
        mutableStateOf("Click to select date")
    }
    val selectedTime = rememberSaveable {
        mutableStateOf("Click to select time")
    }
    val reminderTitle = rememberSaveable {
        mutableStateOf("")
    }
    val reminderDesc = rememberSaveable {
        mutableStateOf("")
    }
    ModalBottomSheet(properties = remember { ModalBottomSheetProperties(shouldDismissOnBackPress = false) },sheetState = btmSheetState, onDismissRequest = {
        isVisible.value = false
    }) {
        LazyColumn(modifier = Modifier.animateContentSize()) {
            item {
                Spacer(modifier = Modifier.height(7.5.dp))
                Text(
                    text = "Add a new Reminder",
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
                    ), forTitleOnlyView = false
                )
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
                    ReminderType.entries.forEachIndexed { index, reminder ->
                        val selected = selectedReminderType.value == reminder.name
                        key(index) {
                            ToggleButton(
                                shape = ReminderType.entries.roundedCornerShape(index),
                                checked = selected,
                                onCheckedChange = {
                                    selectedReminderType.value = reminder.name
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
                                        text = reminder.name,
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
                    ReminderType.ONCE.name -> {
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
                            )
                        )
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
                            )
                        )
                    }
                }
            }
            if (selectedReminderType.value != ReminderType.STICKY.name) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Spacer(Modifier)
                        ReminderMode.entries.forEachIndexed { index, mode ->
                            val selected = selectedReminderMode.value == mode.name
                            key(index) {
                                ToggleButton(
                                    shape = ReminderType.entries.roundedCornerShape(index),
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
                Button(modifier = Modifier.fillMaxWidth().padding(15.dp), onClick = {

                }) {
                    Text(text = "Save", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun ReminderSetting(
    string: String, icon: ImageVector, paddingValues: PaddingValues, shape: Shape
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(paddingValues), shape = shape
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
            IconButton(onClick = {

            }) {
                Icon(
                    imageVector = icon, contentDescription = null
                )
            }
        }
    }
}