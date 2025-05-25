package com.sakethh.linkora.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNotifications
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SettingsInputSvideo
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.navigation.Navigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val navController = LocalNavController.current
    val topAppBarScrollState = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(topBar = {
        Column {
            LargeTopAppBar(
                colors = TopAppBarDefaults.largeTopAppBarColors(scrolledContainerColor = MaterialTheme.colorScheme.surface),
                scrollBehavior = topAppBarScrollState,
                title = {
                Text(
                    text = Localization.rememberLocalizedString(Localization.Key.Settings),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp
                )
            })
        }
    }) { it ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .nestedScroll(topAppBarScrollState.nestedScrollConnection)
        ) {
            items(settingsScreenOptions(navController)) {
                SettingSectionComponent(
                    SettingSectionComponentParam(
                        onClick = it.onClick,
                        sectionTitle = it.sectionTitle,
                        sectionIcon = it.sectionIcon,
                        shouldArrowIconAppear = it.shouldArrowIconAppear
                    )
                )
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

private fun settingsScreenOptions(navController: NavController): List<SettingSectionComponentParam> {
    return listOf(
        SettingSectionComponentParam(
            onClick = {
                navController.navigate(Navigation.Settings.ThemeSettingsScreen)
            },
            sectionTitle = Localization.getLocalizedString(Localization.Key.Theme),
            sectionIcon = Icons.Default.ColorLens
        ),
        SettingSectionComponentParam(
            onClick = {
                navController.navigate(Navigation.Settings.GeneralSettingsScreen)
            },
            sectionTitle = Localization.getLocalizedString(Localization.Key.General),
            sectionIcon = Icons.Default.SettingsInputSvideo
        ),
        SettingSectionComponentParam(
            onClick = {
                navController.navigate(Navigation.Settings.AdvancedSettingsScreen)
            },
            sectionTitle = Localization.getLocalizedString(Localization.Key.Advanced),
            sectionIcon = Icons.Default.Build
        ),
        SettingSectionComponentParam(
            onClick = {
                navController.navigate(Navigation.Settings.LayoutSettingsScreen)
            },
            sectionTitle = Localization.getLocalizedString(Localization.Key.Layout),
            sectionIcon = Icons.Default.Dashboard
        ),
        SettingSectionComponentParam(
            onClick = {
                navController.navigate(Navigation.Settings.RemindersSettingsScreen)
            },
            sectionTitle = "Reminders",
            sectionIcon = Icons.Default.EditNotifications
        ),
        SettingSectionComponentParam(
            onClick = {
                navController.navigate(Navigation.Settings.LanguageSettingsScreen)
            },
            sectionTitle = Localization.getLocalizedString(Localization.Key.Language),
            sectionIcon = Icons.Default.Language
        ),
        SettingSectionComponentParam(
            onClick = {
                navController.navigate(Navigation.Settings.DataSettingsScreen)
            },
            sectionTitle = Localization.getLocalizedString(Localization.Key.Data),
            sectionIcon = Icons.Default.Storage
        ),
        /*SettingSectionComponentParam(
            onClick = {

            },
            sectionTitle = Localization.getLocalizedString(Localization.Key.Privacy),
            sectionIcon = Icons.Default.PrivacyTip
        ),*/
        SettingSectionComponentParam(
            onClick = {
                navController.navigate(Navigation.Settings.AboutSettingsScreen)
            },
            sectionTitle = Localization.getLocalizedString(Localization.Key.About),
            sectionIcon = Icons.Default.Info
        ),
        SettingSectionComponentParam(
            onClick = {
                navController.navigate(Navigation.Settings.AcknowledgementSettingsScreen)
            },
            sectionTitle = Localization.getLocalizedString(Localization.Key.Acknowledgments),
            sectionIcon = Icons.Default.Group
        ),
    )
}