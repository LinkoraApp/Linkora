package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.utils.linkoraLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSettingsScreen(linkId: Long?) {
    LaunchedEffect(Unit) {
        linkoraLog("reminderId: $linkId")
    }
    val navController = LocalNavController.current
    SettingsSectionScaffold(
        topAppBarText = "Reminders",
        navController = navController,
        actions = {},
    ) { paddingValues, topAppBarScrollBehaviour ->

    }
}