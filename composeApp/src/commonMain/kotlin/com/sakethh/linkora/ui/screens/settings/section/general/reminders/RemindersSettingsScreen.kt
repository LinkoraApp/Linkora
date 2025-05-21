package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSettingsScreen() {
    val navController = LocalNavController.current

    SettingsSectionScaffold(
        topAppBarText = "Reminders",
        navController = navController,
        actions = {},
    ) { paddingValues, topAppBarScrollBehaviour ->
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
            items(5) {

            }
        }
    }
}