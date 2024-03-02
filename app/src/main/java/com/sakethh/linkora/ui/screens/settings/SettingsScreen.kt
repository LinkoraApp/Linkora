package com.sakethh.linkora.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.sakethh.linkora.R
import com.sakethh.linkora.ui.commonComposables.DataDialogBoxType
import com.sakethh.linkora.ui.commonComposables.DeleteDialogBox
import com.sakethh.linkora.ui.commonComposables.DeleteDialogBoxParam
import com.sakethh.linkora.ui.commonComposables.pulsateEffect
import com.sakethh.linkora.ui.screens.openInWeb
import com.sakethh.linkora.ui.viewmodels.localDB.DeleteVM
import com.sakethh.linkora.data.localDB.dto.RecentlyVisited
import com.sakethh.linkora.utils.isNetworkAvailable
import com.sakethh.linkora.ui.navigation.NavigationRoutes
import com.sakethh.linkora.ui.viewmodels.SettingsScreenVM.Settings.dataStore
import com.sakethh.linkora.ui.screens.settings.composables.ImportConflictBtmSheet
import com.sakethh.linkora.ui.screens.settings.composables.ImportExceptionDialogBox
import com.sakethh.linkora.ui.screens.settings.composables.PermissionDialog
import com.sakethh.linkora.ui.screens.settings.composables.RegularSettingComponent
import com.sakethh.linkora.ui.screens.settings.composables.SettingsAppInfoComponent
import com.sakethh.linkora.ui.screens.settings.composables.SettingsDataComposable
import com.sakethh.linkora.ui.screens.settings.composables.SettingsNewVersionCheckerDialogBox
import com.sakethh.linkora.ui.screens.settings.composables.SettingsNewVersionUpdateBtmContent
import com.sakethh.linkora.ui.theme.LinkoraTheme
import com.sakethh.linkora.ui.viewmodels.SettingsScreenVM
import com.sakethh.linkora.ui.viewmodels.SettingsUIElement
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun SettingsScreen(navController: NavController) {
    val importModalBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val settingsScreenVM: SettingsScreenVM = viewModel()
    val generalSectionData = settingsScreenVM.generalSection(context)
    val isPermissionDialogBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val isImportExceptionBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val isImportConflictBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            val file = createTempFile()
            uri?.let { context.contentResolver.openInputStream(it) }.use { input ->
                file.outputStream().use { output ->
                    input?.copyTo(output)
                }
            }
            settingsScreenVM.importData(
                settingsScreenVM.exceptionType,
                file.readText(),
                context,
                isImportExceptionBoxVisible
            )
            file.delete()
        }
    val runtimePermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            isPermissionDialogBoxVisible.value = !it
        })
    val dataSectionData = settingsScreenVM.dataSection(
        runtimePermission,
        context,
        isDialogBoxVisible = isPermissionDialogBoxVisible,
        activityResultLauncher = activityResultLauncher,
        importModalBtmSheetState = isImportConflictBoxVisible
    )
    val coroutineScope = rememberCoroutineScope()
    val shouldVersionCheckerDialogAppear = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldBtmModalSheetBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val btmModalSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val privacySectionData = settingsScreenVM.privacySection(context)
    LinkoraTheme {
        Scaffold(topBar = {
            Column {
                TopAppBar(title = {
                    Text(
                        text = "Settings",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 24.sp
                    )
                })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.25f))
            }
        }) { it ->
            LazyColumn(modifier = Modifier.padding(it)) {
                item(key = "settingsCard") {
                    Card(
                        border = BorderStroke(
                            1.dp, contentColorFor(MaterialTheme.colorScheme.surface)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = contentColorFor(MaterialTheme.colorScheme.surface)
                        ),
                        modifier = Modifier
                            .padding(15.dp)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .animateContentSize()
                    ) {
                        Row {
                            Text(
                                text = "Linkora",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .padding(start = 15.dp, top = 30.dp)
                                    .alignByBaseline()
                            )
                            Text(
                                text = SettingsScreenVM.appVersionName,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 12.sp,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                        if (!SettingsScreenVM.Settings.isAutoCheckUpdatesEnabled.value && !SettingsScreenVM.Settings.isOnLatestUpdate.value && isNetworkAvailable(
                                context
                            )
                        ) {
                            SettingsAppInfoComponent(hasDescription = false,
                                description = "",
                                icon = Icons.Outlined.Update,
                                title = "Check for latest version",
                                onClick = {
                                    shouldVersionCheckerDialogAppear.value = true
                                    if (isNetworkAvailable(context)) {
                                        coroutineScope.launch {
                                            SettingsScreenVM.Settings.latestAppVersionRetriever(
                                                context
                                            )
                                        }.invokeOnCompletion {
                                            shouldVersionCheckerDialogAppear.value = false
                                            if (SettingsScreenVM.appVersionCode < SettingsScreenVM.latestAppInfoFromServer.stableVersionCode.value || SettingsScreenVM.appVersionCode < SettingsScreenVM.latestAppInfoFromServer.nonStableVersionCode.value) {
                                                shouldBtmModalSheetBeVisible.value = true
                                                SettingsScreenVM.Settings.isOnLatestUpdate.value =
                                                    false
                                            } else {
                                                SettingsScreenVM.Settings.isOnLatestUpdate.value =
                                                    true
                                            }
                                        }
                                    } else {
                                        shouldVersionCheckerDialogAppear.value = false
                                        Toast.makeText(
                                            context,
                                            "network error, check your network connection and try again",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        } else if (SettingsScreenVM.Settings.isAutoCheckUpdatesEnabled.value && !SettingsScreenVM.Settings.isOnLatestUpdate.value && isNetworkAvailable(
                                context
                            )
                        ) {
                            if (SettingsScreenVM.latestAppInfoFromServer.stableVersionCode.value == 0) LaunchedEffect(
                                key1 = Unit
                            ) {
                                SettingsScreenVM.Settings.latestAppVersionRetriever(context)
                            }
                            SettingsAppInfoComponent(hasDescription = false,
                                description = "",
                                icon = Icons.Outlined.GetApp,
                                title = if (SettingsScreenVM.appVersionCode < SettingsScreenVM.latestAppInfoFromServer.nonStableVersionCode.value) {
                                    "Linkora ${SettingsScreenVM.latestAppInfoFromServer.nonStableVersionValue.value} is now available. Check it out and download it."
                                } else {
                                    "Linkora ${SettingsScreenVM.latestAppInfoFromServer.stableVersionValue.value} is now available. Check it out and download it."
                                },
                                onClick = {
                                    shouldVersionCheckerDialogAppear.value = true
                                    if (isNetworkAvailable(context)) {
                                        coroutineScope.launch {
                                            if (SettingsScreenVM.latestAppInfoFromServer.stableVersionCode.value == 0) {
                                                SettingsScreenVM.Settings.latestAppVersionRetriever(
                                                    context
                                                )
                                            }
                                        }.invokeOnCompletion {
                                            shouldVersionCheckerDialogAppear.value = false
                                            if (SettingsScreenVM.appVersionCode < SettingsScreenVM.latestAppInfoFromServer.stableVersionCode.value || SettingsScreenVM.appVersionCode < SettingsScreenVM.latestAppInfoFromServer.nonStableVersionCode.value) {
                                                shouldBtmModalSheetBeVisible.value = true
                                                SettingsScreenVM.Settings.isOnLatestUpdate.value =
                                                    false
                                            } else {
                                                SettingsScreenVM.Settings.isOnLatestUpdate.value =
                                                    true
                                            }
                                        }
                                    } else {
                                        shouldVersionCheckerDialogAppear.value = false
                                        Toast.makeText(
                                            context,
                                            "network error, check your network connection and try again",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                })
                        } else {
                            if (isNetworkAvailable(context)) {
                                Card(
                                    border = BorderStroke(
                                        1.dp, contentColorFor(MaterialTheme.colorScheme.surface)
                                    ),
                                    colors = CardDefaults.cardColors(containerColor = AlertDialogDefaults.containerColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 15.dp, end = 15.dp, top = 15.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                            .padding(
                                                top = 10.dp, bottom = 10.dp
                                            ), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            androidx.compose.material3.Icon(
                                                imageVector = Icons.Outlined.CheckCircle,
                                                contentDescription = null,
                                                modifier = Modifier.padding(
                                                    start = 10.dp, end = 10.dp
                                                )
                                            )
                                        }
                                        Text(
                                            text = "You are using latest version of Linkora.",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontSize = 14.sp,
                                            lineHeight = 18.sp,
                                            textAlign = TextAlign.Start,
                                            modifier = Modifier.padding(end = 15.dp)
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        SettingsAppInfoComponent(description = "The source code for Linkora is public and open-source; feel free to check out what Linkora does under the hood.",
                            icon = null,
                            usingLocalIcon = true,
                            title = "Github",
                            localIcon = R.drawable.github_logo,
                            onClick = {
                                coroutineScope.launch {
                                    openInWeb(
                                        recentlyVisitedData = RecentlyVisited(
                                            title = "Linkora on Github",
                                            webURL = "https://www.github.com/sakethpathike/Linkora",
                                            baseURL = "github.com",
                                            imgURL = "it.imgURL",
                                            infoForSaving = "Linkora on Github"
                                        ),
                                        context = context,
                                        uriHandler = uriHandler,
                                        forceOpenInExternalBrowser = false
                                    )
                                }
                            })

                        HorizontalDivider(
                            modifier = Modifier.padding(20.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        SettingsAppInfoComponent(description = "Follow @LinkoraApp on the bird app to get the latest information about releases and everything in between about Linkora.",
                            icon = null,
                            usingLocalIcon = true,
                            localIcon = R.drawable.twitter_logo,
                            title = "Twitter",
                            onClick = {
                                coroutineScope.launch {
                                    openInWeb(
                                        recentlyVisitedData = RecentlyVisited(
                                            title = "Linkora on Twitter",
                                            webURL = "https://www.twitter.com/LinkoraApp",
                                            baseURL = "twitter.com",
                                            imgURL = "it.imgURL",
                                            infoForSaving = "Linkora on Twitter"
                                        ),
                                        context = context,
                                        uriHandler = uriHandler,
                                        forceOpenInExternalBrowser = false
                                    )
                                }
                            })
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
                item(key = "themeTitle") {
                    Text(
                        text = "Theme",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 15.dp, top = 20.dp)
                    )
                    Spacer(
                        modifier = Modifier.padding(
                            bottom = 10.dp
                        )
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !SettingsScreenVM.Settings.shouldDarkThemeBeEnabled.value) {
                    item(key = "Follow System Theme") {
                        RegularSettingComponent(
                            settingsUIElement = SettingsUIElement(title = "Follow System Theme",
                                doesDescriptionExists = false,
                                isSwitchNeeded = true,
                                description = null,
                                isSwitchEnabled = SettingsScreenVM.Settings.shouldFollowSystemTheme,
                                onSwitchStateChange = {
                                    coroutineScope.launch {
                                        SettingsScreenVM.Settings.changeSettingPreferenceValue(
                                            preferenceKey = booleanPreferencesKey(
                                                SettingsScreenVM.SettingsPreferences.FOLLOW_SYSTEM_THEME.name
                                            ),
                                            dataStore = context.dataStore,
                                            newValue = !SettingsScreenVM.Settings.shouldFollowSystemTheme.value
                                        )
                                        SettingsScreenVM.Settings.shouldFollowSystemTheme.value =
                                            SettingsScreenVM.Settings.readSettingPreferenceValue(
                                                preferenceKey = booleanPreferencesKey(
                                                    SettingsScreenVM.SettingsPreferences.FOLLOW_SYSTEM_THEME.name
                                                ), dataStore = context.dataStore
                                            ) == true
                                    }
                                })
                        )
                    }
                }
                if (!SettingsScreenVM.Settings.shouldFollowSystemTheme.value) {
                    item(key = "Use Dark Mode") {
                        RegularSettingComponent(
                            settingsUIElement = SettingsUIElement(title = "Use Dark Mode",
                                doesDescriptionExists = false,
                                description = null,
                                isSwitchNeeded = true,
                                isSwitchEnabled = SettingsScreenVM.Settings.shouldDarkThemeBeEnabled,
                                onSwitchStateChange = {
                                    coroutineScope.launch {
                                        SettingsScreenVM.Settings.changeSettingPreferenceValue(
                                            preferenceKey = booleanPreferencesKey(
                                                SettingsScreenVM.SettingsPreferences.DARK_THEME.name
                                            ),
                                            dataStore = context.dataStore,
                                            newValue = !SettingsScreenVM.Settings.shouldDarkThemeBeEnabled.value
                                        )
                                        SettingsScreenVM.Settings.shouldDarkThemeBeEnabled.value =
                                            SettingsScreenVM.Settings.readSettingPreferenceValue(
                                                preferenceKey = booleanPreferencesKey(
                                                    SettingsScreenVM.SettingsPreferences.DARK_THEME.name
                                                ), dataStore = context.dataStore
                                            ) == true
                                    }
                                })
                        )
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item(key = "Use dynamic theming") {
                        RegularSettingComponent(
                            settingsUIElement = SettingsUIElement(title = "Use dynamic theming",
                                doesDescriptionExists = true,
                                description = "Change colour themes within the app based on your wallpaper.",
                                isSwitchNeeded = true,
                                isSwitchEnabled = SettingsScreenVM.Settings.shouldFollowDynamicTheming,
                                onSwitchStateChange = {
                                    coroutineScope.launch {
                                        SettingsScreenVM.Settings.changeSettingPreferenceValue(
                                            preferenceKey = booleanPreferencesKey(
                                                SettingsScreenVM.SettingsPreferences.DYNAMIC_THEMING.name
                                            ),
                                            dataStore = context.dataStore,
                                            newValue = !SettingsScreenVM.Settings.shouldFollowDynamicTheming.value
                                        )
                                        SettingsScreenVM.Settings.shouldFollowDynamicTheming.value =
                                            SettingsScreenVM.Settings.readSettingPreferenceValue(
                                                preferenceKey = booleanPreferencesKey(
                                                    SettingsScreenVM.SettingsPreferences.DYNAMIC_THEMING.name
                                                ), dataStore = context.dataStore
                                            ) == true
                                    }
                                })
                        )
                    }
                }
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 25.dp, end = 25.dp),
                        thickness = 0.25.dp,
                        color = MaterialTheme.colorScheme.outline.copy(0.50f)
                    )
                }
                item(key = "General") {
                    Text(
                        text = "General",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 15.dp, top = 20.dp)
                    )
                    Spacer(
                        modifier = Modifier.padding(
                            bottom = 10.dp
                        )
                    )
                }
                items(items = generalSectionData, key = { settingsUIElement ->
                    settingsUIElement.title
                }) { settingsUIElement ->
                    RegularSettingComponent(
                        settingsUIElement = settingsUIElement
                    )
                }
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 25.dp, end = 25.dp),
                        thickness = 0.25.dp,
                        color = MaterialTheme.colorScheme.outline.copy(0.50f)
                    )
                }
                item(key = "Data") {
                    Spacer(modifier = Modifier.padding(top = 20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Data",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(
                                start = 15.dp
                            )
                        )
                        Text(
                            text = "Beta",
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .padding(
                                    start = 5.dp
                                )
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .padding(5.dp)
                        )
                    }
                    Text(
                        text = "The Import feature is polished, not perfect. While it's much better, surprises might still pop up during import. However, exporting should be no problem.",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(start = 15.dp, top = 15.dp, end = 15.dp)
                    )
                }
                items(items = dataSectionData, key = { settingsUIElement ->
                    settingsUIElement.title
                }) { settingsUIElement ->
                    settingsUIElement.description?.let { it1 ->
                        SettingsDataComposable(
                            onClick = {
                                settingsUIElement.onSwitchStateChange()
                            },
                            title = settingsUIElement.title,
                            description = it1,
                            icon = settingsUIElement.icon!!
                        )
                    }
                }
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 25.dp, end = 25.dp),
                        thickness = 0.25.dp,
                        color = MaterialTheme.colorScheme.outline.copy(0.50f)
                    )
                }
                item(key = "Privacy") {
                    Text(
                        text = "Privacy",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(start = 15.dp, top = 20.dp, bottom = 10.dp)
                    )
                }
                item(key = "Privacy Element") {
                    Box(
                        modifier = Modifier
                            .combinedClickable(interactionSource = remember {
                                MutableInteractionSource()
                            }, indication = null,
                                onClick = {
                                    privacySectionData.onSwitchStateChange()
                                },
                                onLongClick = {

                                })
                            .pulsateEffect()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .padding(
                                        start = 15.dp
                                    )
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = privacySectionData.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(
                                        top = 15.dp
                                    )
                                )
                                Switch(
                                    checked = privacySectionData.isSwitchEnabled.value,
                                    onCheckedChange = {
                                        privacySectionData.onSwitchStateChange()
                                    },
                                    modifier = Modifier.padding(
                                        top = 15.dp, end = 15.dp
                                    )
                                )
                            }
                            Text(
                                text = if (!SettingsScreenVM.Settings.isSendCrashReportsEnabled.value) "Every single bit of data is stored locally on your device." else "Linkora collects data related to app crashes and errors, device information, and app version.",
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(
                                    top = 10.dp, start = 15.dp, bottom = 20.dp, end = 15.dp
                                ),
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Start,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
        PermissionDialog(isVisible = isPermissionDialogBoxVisible,
            permissionDenied = when (ContextCompat.checkSelfPermission(
                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
                PackageManager.PERMISSION_GRANTED -> false
                else -> true
            },
            onClick = {
                context as Activity
                context.openApplicationSettings()
            })
        SettingsNewVersionCheckerDialogBox(shouldDialogBoxAppear = shouldVersionCheckerDialogAppear)
        if (shouldBtmModalSheetBeVisible.value) {
            ModalBottomSheet(sheetState = btmModalSheetState, onDismissRequest = {
                coroutineScope.launch {
                    if (btmModalSheetState.isVisible) {
                        btmModalSheetState.hide()
                    }
                }.invokeOnCompletion {
                    shouldBtmModalSheetBeVisible.value = false
                }
            }) {
                SettingsNewVersionUpdateBtmContent(
                    shouldBtmModalSheetBeVisible = shouldBtmModalSheetBeVisible,
                    modalBtmSheetState = btmModalSheetState
                )
            }
        }
        ImportExceptionDialogBox(
            isVisible = isImportExceptionBoxVisible,
            onClick = { activityResultLauncher.launch("text/*") },
            exceptionType = settingsScreenVM.exceptionType
        )
        val deleteVM: DeleteVM = viewModel()
        ImportConflictBtmSheet(isUIVisible = isImportConflictBoxVisible,
            modalBottomSheetState = importModalBottomSheetState,
            onMergeClick = {
                activityResultLauncher.launch("text/*")
            },
            onDeleteExistingDataClick = {
                deleteVM.deleteEntireLinksAndFoldersData(onTaskCompleted = {
                    activityResultLauncher.launch("text/*")
                })
            },
            onDataExportClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    settingsScreenVM.exportDataToAFile(
                        context = context,
                        isDialogBoxVisible = isPermissionDialogBoxVisible,
                        runtimePermission = runtimePermission
                    )
                } else {
                    when (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )) {
                        PackageManager.PERMISSION_GRANTED -> {
                            settingsScreenVM.exportDataToAFile(
                                context = context,
                                isDialogBoxVisible = isPermissionDialogBoxVisible,
                                runtimePermission = runtimePermission
                            )
                            Toast.makeText(
                                context, "Successfully Exported", Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            runtimePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            Toast.makeText(
                                context, "Permission required to write the data", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            },
            onExportAndThenImportClick = {
                fun exportDataToAFile() {
                    settingsScreenVM.exportDataToAFile(
                        context = context,
                        isDialogBoxVisible = isPermissionDialogBoxVisible,
                        runtimePermission = runtimePermission
                    )
                    Toast.makeText(
                        context, "Successfully Exported", Toast.LENGTH_SHORT
                    ).show()
                    deleteVM.deleteEntireLinksAndFoldersData(onTaskCompleted = {
                        activityResultLauncher.launch("text/*")
                    })
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    exportDataToAFile()
                } else {
                    when (ContextCompat.checkSelfPermission(
                        context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )) {
                        PackageManager.PERMISSION_GRANTED -> {
                            exportDataToAFile()
                        }

                        else -> {
                            runtimePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            Toast.makeText(
                                context, "Permission required to write the data", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            })
        DeleteDialogBox(
            DeleteDialogBoxParam(shouldDialogBoxAppear = settingsScreenVM.shouldDeleteDialogBoxAppear,
                deleteDialogBoxType = DataDialogBoxType.REMOVE_ENTIRE_DATA,
                onDeleteClick = {
                    deleteVM.deleteEntireLinksAndFoldersData()
                    Toast.makeText(
                        context, "Deleted entire data from the local database", Toast.LENGTH_SHORT
                    ).show()
                })
        )
    }
    BackHandler {
        if (btmModalSheetState.isVisible) {
            coroutineScope.launch {
                btmModalSheetState.hide()
            }
        } else if (SettingsScreenVM.Settings.isHomeScreenEnabled.value) {
            navController.navigate(NavigationRoutes.HOME_SCREEN.name) {
                popUpTo(0)
            }
        } else {
            navController.navigate(NavigationRoutes.COLLECTIONS_SCREEN.name) {
                popUpTo(0)
            }
        }
    }
}

fun Activity.openApplicationSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)
    ).also {
        startActivity(it)
    }
}