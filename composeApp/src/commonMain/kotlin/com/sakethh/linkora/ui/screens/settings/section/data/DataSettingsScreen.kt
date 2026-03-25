package com.sakethh.linkora.ui.screens.settings.section.data

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BackupTable
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SwitchLeft
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.ImportFileType
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.platform.PlatformSpecificBackHandler
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.DeleteDialogBoxType
import com.sakethh.linkora.ui.components.DeleteFolderOrLinkDialog
import com.sakethh.linkora.ui.components.DeleteFolderOrLinkDialogParam
import com.sakethh.linkora.ui.components.InfoCard
import com.sakethh.linkora.ui.domain.ImportFileSelectionMethod
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.SettingSectionComponent
import com.sakethh.linkora.ui.screens.settings.SettingSectionComponentParam
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.screens.settings.section.data.sync.ServerManagementBottomSheet
import com.sakethh.linkora.ui.screens.settings.section.data.sync.ServerManagementViewModel
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.asLocalizedString
import com.sakethh.linkora.utils.currentSavedServerConfig
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.intPreferencesKey
import com.sakethh.linkora.utils.rememberLocalizedString
import com.sakethh.linkora.utils.stringPreferencesKey
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataSettingsScreen() {
    val navController = LocalNavController.current
    val serverManagementViewModel: ServerManagementViewModel = linkoraViewModel()
    val dataSettingsScreenVM: DataSettingsScreenVM = linkoraViewModel()
    var isImportExportProgressUIVisible by rememberSaveable {
        mutableStateOf(false)
    }
    var isForcePushAndPullProgressUIVisible by rememberSaveable {
        mutableStateOf(false)
    }
    val shouldDeleteEntireDialogBoxAppear = rememberSaveable { mutableStateOf(false) }

    val dataOperationTitle = rememberSaveable {
        mutableStateOf("")
    }
    val shouldServerInfoBtmSheetBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val serverInfoBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val platform = platform
    val coilPlatformContext = LocalPlatformContext.current
    val importFileSelectionMethod = rememberSaveable {
        mutableStateOf(ImportFileSelectionMethod.FilePicker.name)
    }
    val showFileLocationPickerDialog = rememberSaveable {
        mutableStateOf(false)
    }
    val selectedImportFormat = rememberSaveable {
        mutableStateOf(ImportFileType.JSON.name)
    }
    val showAlertDialogWithProgress = rememberSaveable {
        mutableStateOf(false)
    }
    var labelForAlertDialogWithProgress by rememberSaveable {
        mutableStateOf("")
    }
    val exportLocation = rememberSaveable(AppPreferences.currentExportLocation.value) {
        mutableStateOf(AppPreferences.currentExportLocation.value)
    }

    val localFocusManager = LocalFocusManager.current

    var maxConcurrentRefreshCount by rememberSaveable(AppPreferences.maxConcurrentRefreshCount) {
        mutableIntStateOf(AppPreferences.maxConcurrentRefreshCount)
    }

    val refreshesInProgress = rememberSaveable(
        !DataSettingsScreenVM.refreshLinksState.value.isInRefreshingState,
        !(platform is Platform.Android && dataSettingsScreenVM.isAnyRefreshingScheduledOnAndroid.value)
    ) {
        !DataSettingsScreenVM.refreshLinksState.value.isInRefreshingState && !(platform is Platform.Android && dataSettingsScreenVM.isAnyRefreshingScheduledOnAndroid.value)
    }

    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.DataSettingsScreen.toString(),
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.animateContentSize().fillMaxSize()
                .addEdgeToEdgeScaffoldPadding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            item {
                Spacer(modifier = Modifier)
            }
            if (platform != Platform.Web){
                item {
                    Text(
                        text = Localization.rememberLocalizedString(Localization.Key.ImportLabel),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                    )
                    if (platform is Platform.Desktop) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 5.dp)
                        ) {
                            Text(
                                text = Localization.Key.ImportMethodLabel.rememberLocalizedString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (importFileSelectionMethod.value == ImportFileSelectionMethod.FileLocationString.name) Localization.Key.FileLocationLabel.rememberLocalizedString() else Localization.Key.FilePickerLabel.rememberLocalizedString(),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            FilledTonalIconButton(
                                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                    .size(24.dp), onClick = {
                                    if (importFileSelectionMethod.value == ImportFileSelectionMethod.FileLocationString.name) importFileSelectionMethod.value =
                                        ImportFileSelectionMethod.FilePicker.name else importFileSelectionMethod.value =
                                        ImportFileSelectionMethod.FileLocationString.name
                                }) {
                                Icon(
                                    imageVector = Icons.Default.SwitchLeft,
                                    contentDescription = null,
                                    modifier = Modifier.rotate(if (importFileSelectionMethod.value == ImportFileSelectionMethod.FileLocationString.name) 0f else 180f)
                                )
                            }
                        }
                    }
                    if (AppPreferences.isServerConfigured()) {
                        Text(
                            text = Localization.rememberLocalizedString(Localization.Key.ImportLabelDesc),
                            style = MaterialTheme.typography.titleSmall,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 5.dp),
                        )
                    }
                }

                item {
                    SettingComponent(
                        SettingComponentParam(
                            isIconNeeded = rememberSaveable { mutableStateOf(true) },
                            title = Localization.rememberLocalizedString(Localization.Key.ImportUsingJsonFile),
                            doesDescriptionExists = true,
                            description = Localization.rememberLocalizedString(Localization.Key.ImportUsingJsonFileDesc),
                            isSwitchNeeded = false,
                            isSwitchEnabled = rememberSaveable { mutableStateOf(false) },
                            onSwitchStateChange = {
                                if (importFileSelectionMethod.value == ImportFileSelectionMethod.FileLocationString.name) {
                                    selectedImportFormat.value = ImportFileType.JSON.name
                                    showFileLocationPickerDialog.value = true
                                    return@SettingComponentParam
                                }
                                dataOperationTitle.value =
                                    Localization.getLocalizedString(Localization.Key.ImportUsingJsonFile)
                                dataSettingsScreenVM.importDataFromAFile(
                                    importFileType = ImportFileType.JSON,
                                    onStart = {
                                        isImportExportProgressUIVisible = true
                                    },
                                    onCompletion = {
                                        isImportExportProgressUIVisible = false
                                    },
                                    importFileSelectionMethod = ImportFileSelectionMethod.FilePicker to ""
                                )
                            },
                            icon = Icons.Default.DataObject,
                            shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                    )
                }

                item {
                    SettingComponent(
                        SettingComponentParam(
                            isIconNeeded = rememberSaveable { mutableStateOf(true) },
                            title = Localization.rememberLocalizedString(Localization.Key.ImportDataFromHtmlFile),
                            doesDescriptionExists = true,
                            description = Localization.rememberLocalizedString(Localization.Key.ImportDataFromHtmlFileDesc),
                            isSwitchNeeded = false,
                            isSwitchEnabled = AppPreferences.useAmoledTheme,
                            onSwitchStateChange = {
                                if (importFileSelectionMethod.value == ImportFileSelectionMethod.FileLocationString.name) {
                                    selectedImportFormat.value = ImportFileType.HTML.name
                                    showFileLocationPickerDialog.value = true
                                    return@SettingComponentParam
                                }
                                dataOperationTitle.value =
                                    Localization.getLocalizedString(Localization.Key.ImportDataFromHtmlFile)
                                dataSettingsScreenVM.importDataFromAFile(
                                    importFileType = ImportFileType.HTML,
                                    onStart = {
                                        isImportExportProgressUIVisible = true
                                    },
                                    onCompletion = {
                                        isImportExportProgressUIVisible = false
                                    },
                                    importFileSelectionMethod = ImportFileSelectionMethod.FilePicker to ""
                                )
                            },
                            icon = Icons.Default.Html,
                            shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                    )
                }

                item {
                    Text(
                        text = Localization.rememberLocalizedString(Localization.Key.ExportLabel),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                    )
                    if (AppPreferences.isServerConfigured()) {
                        Text(
                            text = Localization.rememberLocalizedString(Localization.Key.ExportLabelDesc),
                            style = MaterialTheme.typography.titleSmall,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 5.dp),
                        )
                    }
                }

                item {
                    TextField(supportingText = {
                        if (platform is Platform.Android) {
                            Text(
                                text = Localization.Key.CurrentExportLocationSupportingText.rememberLocalizedString(),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }, textStyle = MaterialTheme.typography.titleSmall, trailingIcon = {
                        FilledTonalIconButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .pressScaleEffect().padding(end = 5.dp), onClick = {
                                dataSettingsScreenVM.changeExportLocation(
                                    exportLocation = exportLocation.value,
                                    platform = platform,
                                    exportLocationType = ExportLocationType.EXPORT
                                )
                            }) {
                            Icon(
                                imageVector = if (platform is Platform.Android) Icons.Default.FolderOpen else Icons.Default.Save,
                                contentDescription = null
                            )
                        }
                    }, readOnly = platform is Platform.Android, label = {
                        Text(
                            text = Localization.Key.CurrentExportLocation.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Start,
                        )
                    }, value = exportLocation.value, onValueChange = {
                        exportLocation.value = it
                    }, modifier = Modifier.padding(start = 15.dp, end = 15.dp).fillMaxWidth())
                }

                item {
                    SettingComponent(
                        SettingComponentParam(
                            isIconNeeded = rememberSaveable { mutableStateOf(true) },
                            title = Localization.rememberLocalizedString(Localization.Key.ExportDataAsJson),
                            doesDescriptionExists = true,
                            description = Localization.rememberLocalizedString(Localization.Key.ExportDataAsJsonDesc),
                            isSwitchNeeded = false,
                            isSwitchEnabled = AppPreferences.useAmoledTheme,
                            onSwitchStateChange = {
                                dataOperationTitle.value =
                                    Localization.Key.ExportingDataToJSON.getLocalizedString()
                                dataSettingsScreenVM.exportDataToAFile(
                                    platform = platform,
                                    exportFileType = ExportFileType.JSON,
                                    onStart = {
                                        isImportExportProgressUIVisible = true
                                    },
                                    onCompletion = {
                                        isImportExportProgressUIVisible = false
                                    })
                            },
                            icon = Icons.Default.DataObject,
                            shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                    )
                }

                item {
                    SettingComponent(
                        SettingComponentParam(
                            isIconNeeded = rememberSaveable { mutableStateOf(true) },
                            title = Localization.rememberLocalizedString(Localization.Key.ExportDataAsHtml),
                            doesDescriptionExists = true,
                            description = Localization.rememberLocalizedString(Localization.Key.ExportDataAsHtmlDesc),
                            isSwitchNeeded = false,
                            isSwitchEnabled = AppPreferences.useAmoledTheme,
                            onSwitchStateChange = {
                                dataOperationTitle.value =
                                    Localization.Key.ExportingDataToHTML.getLocalizedString()
                                dataSettingsScreenVM.exportDataToAFile(
                                    platform = platform,
                                    exportFileType = ExportFileType.HTML,
                                    onStart = {
                                        isImportExportProgressUIVisible = true
                                    },
                                    onCompletion = {
                                        isImportExportProgressUIVisible = false
                                    })
                            },
                            icon = Icons.Default.Html,
                            shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    SettingSectionComponent(
                        SettingSectionComponentParam(
                            onClick = {
                                navController.navigate(Navigation.Settings.Data.SnapshotsScreen)
                            },
                            sectionTitle = Localization.Key.Snapshots.rememberLocalizedString(),
                            sectionIcon = Icons.Default.BackupTable,
                            shouldArrowIconAppear = true,
                            fontSize = 16.sp,
                            bottomSpacing = 0.dp
                        )
                    )
                }
            }

            item {
                Text(
                    text = Localization.rememberLocalizedString(Localization.Key.Sync),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                )
            }

            item {
                if (!AppPreferences.isServerConfigured()) {
                    SettingComponent(
                        SettingComponentParam(
                            isIconNeeded = rememberSaveable { mutableStateOf(true) },
                            title = Localization.rememberLocalizedString(Localization.Key.ConnectToALinkoraServer),
                            doesDescriptionExists = true,
                            description = Localization.rememberLocalizedString(Localization.Key.ConnectToALinkoraServerDesc),
                            isSwitchNeeded = false,
                            isSwitchEnabled = AppPreferences.useAmoledTheme,
                            onSwitchStateChange = {
                                navController.navigate(Navigation.Settings.Data.ServerSetupScreen)
                            },
                            icon = Icons.Default.CloudSync,
                            shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                    )
                } else {
                    SettingComponent(
                        SettingComponentParam(
                            isIconNeeded = rememberSaveable { mutableStateOf(true) },
                            title = Localization.rememberLocalizedString(Localization.Key.ManageConnectedServer),
                            doesDescriptionExists = true,
                            description = Localization.rememberLocalizedString(Localization.Key.ManageConnectedServerDesc),
                            isSwitchNeeded = false,
                            isSwitchEnabled = AppPreferences.useAmoledTheme,
                            onSwitchStateChange = {
                                shouldServerInfoBtmSheetBeVisible.value = true
                                coroutineScope.launch {
                                    serverInfoBtmSheetState.expand()
                                }
                            },
                            icon = Icons.Default.WbCloudy,
                            shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                    )

                }
            }

            if (AppPreferences.isServerConfigured()) {
                item {
                    SettingComponent(
                        SettingComponentParam(
                            isIconNeeded = rememberSaveable { mutableStateOf(true) },
                            title = Localization.Key.InitiateManualSync.rememberLocalizedString(),
                            doesDescriptionExists = true,
                            description = Localization.Key.InitiateManualSyncDesc.rememberLocalizedString(),
                            isSwitchNeeded = false,
                            isSwitchEnabled = AppPreferences.useAmoledTheme,
                            onSwitchStateChange = {
                                serverManagementViewModel.saveServerConnectionAndSync(
                                    serverConnection = currentSavedServerConfig(),
                                    timeStampAfter = {
                                        AppPreferences.lastSyncedLocally(serverManagementViewModel.preferencesRepository)
                                    },
                                    onSyncStart = {
                                        isForcePushAndPullProgressUIVisible = true
                                    },
                                    onCompletion = {
                                        isForcePushAndPullProgressUIVisible = false
                                    })
                            },
                            icon = Icons.Default.Sync,
                            shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                    )
                }
            }

            item {
                HorizontalDivider(
                    Modifier.padding(
                        start = 15.dp,
                        end = 15.dp,
                        bottom = 30.dp,
                    ), color = DividerDefaults.color.copy(0.5f)
                )
                SettingComponent(
                    SettingComponentParam(
                        isIconNeeded = rememberSaveable { mutableStateOf(true) },
                        title = Localization.Key.EnforceStrictDefaultIDsLabel.rememberLocalizedString(),
                        doesDescriptionExists = true,
                        description = Localization.Key.EnforceStrictDefaultIDsDesc.rememberLocalizedString(),
                        isSwitchNeeded = false,
                        isSwitchEnabled = AppPreferences.useAmoledTheme,
                        onSwitchStateChange = {
                            dataSettingsScreenVM.forceSetDefaultFolderToInternalIds(onStart = {
                                labelForAlertDialogWithProgress =
                                    Localization.Key.UpdatingInternalIDsLabel.getLocalizedString()
                                showAlertDialogWithProgress.value = true
                            }, onCompletion = {
                                showAlertDialogWithProgress.value = false
                            })
                        },
                        icon = Icons.Default.AutoFixHigh,
                        shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                )
            }

            item {
                HorizontalDivider(
                    Modifier.padding(
                        start = 15.dp,
                        end = 15.dp,
                        bottom = 30.dp,
                    ), color = DividerDefaults.color.copy(0.5f)
                )
                SettingComponent(
                    SettingComponentParam(
                        isIconNeeded = rememberSaveable { mutableStateOf(true) },
                        title = Localization.rememberLocalizedString(Localization.Key.DeleteDuplicateLinksFromAllCollections),
                        doesDescriptionExists = true,
                        description = Localization.rememberLocalizedString(Localization.Key.DeleteDuplicateLinksFromAllCollectionsDesc),
                        isSwitchNeeded = false,
                        isSwitchEnabled = AppPreferences.useAmoledTheme,
                        onSwitchStateChange = {
                            dataSettingsScreenVM.deleteDuplicates(onStart = {
                                labelForAlertDialogWithProgress =
                                    Localization.Key.DeletingDuplicatesLabel.getLocalizedString()
                                showAlertDialogWithProgress.value = true
                            }, onCompletion = {
                                showAlertDialogWithProgress.value = false
                            })
                        },
                        icon = Icons.Default.Delete,
                        shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                )
            }
            item {
                HorizontalDivider(
                    Modifier.padding(
                        start = 15.dp,
                        end = 15.dp,
                        bottom = 30.dp,
                    ), color = DividerDefaults.color.copy(0.5f)
                )
                SettingComponent(
                    SettingComponentParam(
                        isIconNeeded = rememberSaveable { mutableStateOf(true) },
                        title = Localization.rememberLocalizedString(Localization.Key.DeleteEntireDataPermanently),
                        doesDescriptionExists = true,
                        description = Localization.rememberLocalizedString(Localization.Key.DeleteEntireDataPermanentlyDesc),
                        isSwitchNeeded = false,
                        isSwitchEnabled = AppPreferences.useAmoledTheme,
                        onSwitchStateChange = {
                            shouldDeleteEntireDialogBoxAppear.value = true
                        },
                        icon = Icons.Default.DeleteForever,
                        shouldFilledIconBeUsed = rememberSaveable { mutableStateOf(true) })
                )
            }
            item {
                HorizontalDivider(
                    Modifier.padding(
                        start = 15.dp,
                        end = 15.dp,
                        bottom = 30.dp,
                    ), color = DividerDefaults.color.copy(0.5f)
                )
                SettingComponent(
                    SettingComponentParam(
                        title = Localization.rememberLocalizedString(Localization.Key.ClearImageCache),
                        doesDescriptionExists = true,
                        description = Localization.rememberLocalizedString(Localization.Key.ClearImageCacheDesc),
                        isSwitchNeeded = false,
                        isIconNeeded = rememberSaveable {
                            mutableStateOf(true)
                        },
                        icon = Icons.Default.BrokenImage,
                        isSwitchEnabled = rememberSaveable {
                            mutableStateOf(false)
                        },
                        onSwitchStateChange = {
                            ImageLoader(coilPlatformContext).let {
                                it.diskCache?.clear()
                                it.memoryCache?.clear()
                            }
                        },
                        shouldFilledIconBeUsed = rememberSaveable {
                            mutableStateOf(true)
                        })
                )
            }
            item {
                HorizontalDivider(
                    Modifier.padding(
                        start = 15.dp,
                        end = 15.dp,
                    ), color = DividerDefaults.color.copy(0.5f)
                )
                Text(
                    text = Localization.Key.Refresh.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(
                        top = 15.dp,
                        start = 15.dp, end = 15.dp,
                        bottom = if (DataSettingsScreenVM.refreshLinksState.value.isInRefreshingState || dataSettingsScreenVM.isAnyRefreshingScheduledOnAndroid.value) 0.dp else 15.dp
                    ),
                )
                AnimatedContent(
                    targetState = refreshesInProgress
                ) { refreshesInProgress ->
                    if (refreshesInProgress) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 15.dp,
                                    end = 15.dp,
                                    bottom = 20.dp
                                )
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(0.5f),
                                        shape = RoundedCornerShape(15.dp)
                                    )
                            ) {
                                Text(
                                    text = Localization.Key.RefreshType.rememberLocalizedString(),
                                    style = MaterialTheme.typography.titleSmall,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Start,
                                    modifier = Modifier.padding(start = 15.dp, top = 15.dp),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                for (refreshLinkType in RefreshLinkType.entries) {
                                    val isSelected =
                                        refreshLinkType.name == AppPreferences.selectedLinkRefreshType.name
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable(onClick = {
                                            AppPreferences.selectedLinkRefreshType = refreshLinkType
                                            dataSettingsScreenVM.changeSettingPreferenceValue(
                                                preferenceKey = stringPreferencesKey(
                                                    AppPreferenceType.REFRESH_LINK_TYPE.name
                                                ),
                                                newValue = AppPreferences.selectedLinkRefreshType.name
                                            )
                                        }, indication = null, interactionSource = null)
                                            .pressScaleEffect(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                AppPreferences.selectedLinkRefreshType =
                                                    refreshLinkType
                                                dataSettingsScreenVM.changeSettingPreferenceValue(
                                                    preferenceKey = stringPreferencesKey(
                                                        AppPreferenceType.REFRESH_LINK_TYPE.name
                                                    ),
                                                    newValue = AppPreferences.selectedLinkRefreshType.name
                                                )
                                            })
                                        Text(
                                            text = refreshLinkType.asLocalizedString(),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                            style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall
                                        )
                                    }
                                }

                                TextField(
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = MaterialTheme.typography.titleSmall,
                                    trailingIcon = {
                                        FilledTonalIconButton(
                                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                                .pressScaleEffect().padding(end = 5.dp), onClick = {
                                                AppPreferences.maxConcurrentRefreshCount =
                                                    maxConcurrentRefreshCount
                                                dataSettingsScreenVM.changeSettingPreferenceValue(
                                                    preferenceKey = intPreferencesKey(
                                                        AppPreferenceType.MAX_CONCURRENT_REFRESH_COUNT.name
                                                    ),
                                                    newValue = AppPreferences.maxConcurrentRefreshCount
                                                )
                                                localFocusManager.clearFocus(force = true)
                                            }) {
                                            Icon(
                                                imageVector = Icons.Default.Save,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = Localization.Key.RefreshLinkTextFieldLabel.rememberLocalizedString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            textAlign = TextAlign.Start,
                                        )
                                    },
                                    value = maxConcurrentRefreshCount.toString(),
                                    onValueChange = {
                                        maxConcurrentRefreshCount = try {
                                            it.toInt()
                                        } catch (_: Exception) {
                                            0
                                        } catch (_: Error) {
                                            0
                                        }.coerceAtLeast(minimumValue = 1)
                                    },
                                    modifier = Modifier.padding(
                                        start = 15.dp,
                                        end = 15.dp,
                                        bottom = 15.dp
                                    )
                                        .fillMaxWidth()
                                )
                            }

                            SettingComponent(
                                SettingComponentParam(
                                    title = Localization.Key.RefreshLinksComponentLabel.rememberLocalizedString(),
                                    doesDescriptionExists = true,
                                    description = Localization.Key.RefreshLinksComponentDesc.rememberLocalizedString(),
                                    isSwitchNeeded = false,
                                    isIconNeeded = rememberSaveable {
                                        mutableStateOf(true)
                                    },
                                    icon = Icons.Default.Refresh,
                                    isSwitchEnabled = rememberSaveable {
                                        mutableStateOf(false)
                                    },
                                    onSwitchStateChange = {
                                        dataSettingsScreenVM.refreshAllLinks()
                                    },
                                    shouldFilledIconBeUsed = rememberSaveable {
                                        mutableStateOf(true)
                                    })
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                .animateContentSize()
                        ) {
                            if (dataSettingsScreenVM.isAnyRefreshingScheduledOnAndroid.value) {
                                InfoCard(
                                    info = Localization.Key.WorkManagerDesc.rememberLocalizedString(),
                                    paddingValues = PaddingValues(start = 20.dp, end = 20.dp)
                                )
                            }
                            if (DataSettingsScreenVM.refreshLinksState.value.isInRefreshingState) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                ) {
                                    Text(
                                        text = Localization.rememberLocalizedString(Localization.Key.RefreshingLinks),
                                        style = MaterialTheme.typography.titleMedium,
                                        modifier = Modifier.padding(
                                            start = 15.dp, end = 15.dp
                                        )
                                    )
                                    if (DataSettingsScreenVM.refreshLinksState.value.currentIteration != 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(start = 15.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            LinearProgressIndicator(
                                                modifier = Modifier.fillMaxWidth(0.85f),
                                                progress = {
                                                    DataSettingsScreenVM.refreshLinksState.value.currentIteration.toFloat() / DataSettingsScreenVM.totalLinksForRefresh.value
                                                })
                                            IconButton(
                                                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                                                onClick = {
                                                    dataSettingsScreenVM.cancelRefreshingAllLinks()
                                                }) {
                                                Icon(
                                                    imageVector = Icons.Default.Cancel,
                                                    contentDescription = ""
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = Localization.Key.NoOfLinksRefreshed.rememberLocalizedString()
                                            .replace(
                                                LinkoraPlaceHolder.First.value,
                                                DataSettingsScreenVM.refreshLinksState.value.currentIteration.toString()
                                            ).replace(
                                                LinkoraPlaceHolder.Second.value,
                                                DataSettingsScreenVM.totalLinksForRefresh.value.toString()
                                            ),
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(
                                            start = 15.dp, end = 15.dp
                                        ),
                                        lineHeight = 18.sp
                                    )
                                    Card(
                                        border = BorderStroke(
                                            1.dp, contentColorFor(MaterialTheme.colorScheme.surface)
                                        ),
                                        colors = CardDefaults.cardColors(containerColor = AlertDialogDefaults.containerColor),
                                        modifier = Modifier.fillMaxWidth().padding(
                                            start = 15.dp, end = 15.dp, top = 20.dp
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                                .padding(
                                                    top = 10.dp, bottom = 10.dp
                                                ), verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Info,
                                                contentDescription = null,
                                                modifier = Modifier.padding(
                                                    start = 10.dp, end = 10.dp
                                                )
                                            )
                                            Text(
                                                text = if (platform is Platform.Android) Localization.Key.RefreshingLinksAndroidDesc.rememberLocalizedString() else Localization.Key.RefreshingLinksDesktopDesc.rememberLocalizedString(),
                                                style = MaterialTheme.typography.titleSmall,
                                                lineHeight = 18.sp,
                                                modifier = Modifier.padding(end = 15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    if (showFileLocationPickerDialog.value) {
        val fileLocation = rememberSaveable {
            mutableStateOf("")
        }
        AlertDialog(onDismissRequest = {
            showFileLocationPickerDialog.value = false
        }, confirmButton = {
            Button(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth(),
                onClick = {
                    dataSettingsScreenVM.importDataFromAFile(
                        importFileType = ImportFileType.valueOf(
                            selectedImportFormat.value
                        ),
                        onStart = {
                            showFileLocationPickerDialog.value = false
                            isImportExportProgressUIVisible = true
                        },
                        onCompletion = {
                            isImportExportProgressUIVisible = false
                            showFileLocationPickerDialog.value = false
                        },
                        importFileSelectionMethod = ImportFileSelectionMethod.FileLocationString to fileLocation.value
                    )
                }) {
                Text(
                    text = Localization.Key.ImportLabel.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }, dismissButton = {
            OutlinedButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth(),
                onClick = {
                    showFileLocationPickerDialog.value = false
                }) {
                Text(
                    text = Localization.Key.Cancel.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }, text = {
            OutlinedTextField(
                label = {
                    Text(
                        text = Localization.Key.FileLocationLabel.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1
                    )
                },
                value = fileLocation.value,
                onValueChange = {
                    fileLocation.value = it
                },
                textStyle = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth()
            )
        }, title = {
            Text(
                text = Localization.Key.ProvideAValidFileLocation.rememberLocalizedString(),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp
            )
        })
    }
    ServerManagementBottomSheet(
        sheetState = serverInfoBtmSheetState,
        isVisible = shouldServerInfoBtmSheetBeVisible,
        removeTheConnection = { onDeletion ->
            serverManagementViewModel.deleteTheConnection(onDeletion)
        })
    LogsScreen(
        isVisible = isImportExportProgressUIVisible,
        onCancel = {
            dataSettingsScreenVM.cancelImportExportJob()
        },
        logs = dataSettingsScreenVM.importExportProgressLogs,
        operationTitle = dataOperationTitle.value,
        operationDesc = Localization.Key.ImportExportScreenTopAppBarDesc.rememberLocalizedString()
    )
    LogsScreen(
        isVisible = isForcePushAndPullProgressUIVisible,
        onCancel = {
            serverManagementViewModel.cancelServerConnectionAndSync(removeConnection = false)
            isForcePushAndPullProgressUIVisible = false
        },
        logs = serverManagementViewModel.dataSyncLogs,
        operationTitle = Localization.Key.SyncingDataLabel.rememberLocalizedString(),
        operationDesc = Localization.Key.InitiateManualSyncDescAlt.rememberLocalizedString()
    )
    if (shouldDeleteEntireDialogBoxAppear.value) {
        DeleteFolderOrLinkDialog(
            deleteFolderOrLinkDialogParam = DeleteFolderOrLinkDialogParam(
                onDismiss = {
                    shouldDeleteEntireDialogBoxAppear.value = false
                },
                deleteDialogBoxType = DeleteDialogBoxType.REMOVE_ENTIRE_DATA,
                onDeleteClick = { onCompletion, deleteEverythingFromRemote ->
                    dataSettingsScreenVM.deleteEntireDatabase(
                        deleteEverythingFromRemote, onCompletion
                    )
                })
        )
    }
    if (showAlertDialogWithProgress.value) {
        AlertDialog(onDismissRequest = {}, content = {
            Column(
                modifier = Modifier.clip(AlertDialogDefaults.shape)
                    .background(AlertDialogDefaults.containerColor).padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Text(
                    text = labelForAlertDialogWithProgress,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp
                )
                LinearProgressIndicator()
            }
        })
    }
    PlatformSpecificBackHandler {
        if (isImportExportProgressUIVisible || showAlertDialogWithProgress.value || shouldDeleteEntireDialogBoxAppear.value) {
            return@PlatformSpecificBackHandler
        } else {
            navController.navigateUp()
        }
    }
}