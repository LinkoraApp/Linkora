package com.sakethh.linkora.ui.screens.settings.section.data.capture

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.booleanPreferencesKey
import com.sakethh.linkora.utils.intPreferencesKey
import com.sakethh.linkora.utils.stringPreferencesKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPageCaptureScreen() {
    val dataSettingsScreenVM = linkoraViewModel<DataSettingsScreenVM>()
    val preferences by dataSettingsScreenVM.preferencesAsFlow.collectAsStateWithLifecycle()

    val localFocusManager = LocalFocusManager.current
    var whitelistDomains by
        rememberSaveable(preferences.webCaptureWhitelistDomains) {
            mutableStateOf(preferences.webCaptureWhitelistDomains)
        }
    var blacklistDomains by
        rememberSaveable(preferences.webCaptureBlacklistDomains) {
            mutableStateOf(preferences.webCaptureBlacklistDomains)
        }
    var webCaptureLocation by
        rememberSaveable(preferences.webCapturesLocation) {
            mutableStateOf(preferences.webCapturesLocation)
        }
    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.Data.WebPageCapturesScreen.toString(),
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier =
            Modifier.animateContentSize()
                .fillMaxSize()
                .addEdgeToEdgeScaffoldPadding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item {
                Spacer(modifier = Modifier)
            }
            item {
                SettingComponent(
                    SettingComponentParam(
                        isIconNeeded = false,
                        title = "Use Web-captures",
                        doesDescriptionExists = true,
                        description =
                        "Automatically downloads pages as HTML for offline view whenever a new link is saved or refreshed. Works well for text and media, though heavy JS sites may not fully load. Processes entirely on-device, which can be resource-heavy.",
                        isSwitchNeeded = true,
                        isSwitchEnabled = preferences.useWebCaptures,
                        onSwitchStateChange = {
                            dataSettingsScreenVM.changeSettingPreferenceValue(
                                preferenceKey = booleanPreferencesKey(AppPreferences.USE_WEB_CAPTURES.key),
                                newValue = !preferences.useWebCaptures,
                            )
                        },
                        icon = Icons.Default.Web,
                        shouldFilledIconBeUsed = false,
                    ),
                )
            }

            if (preferences.useWebCaptures) {
                item {
                    TextField(
                        supportingText = {
                            Text(
                                text =
                                "If the selected directory is moved or deleted, web-captures will silently fail. Make sure the selected directory always exists.",
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                        textStyle = MaterialTheme.typography.titleSmall,
                        trailingIcon = {
                            FilledTonalIconButton(
                                modifier =
                                Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                    .pressScaleEffect()
                                    .padding(end = 5.dp),
                                onClick = {
                                    dataSettingsScreenVM.changeExportLocation(
                                        exportLocation = webCaptureLocation,
                                        platform = platform,
                                        exportLocationType = ExportLocationType.WEB_CAPTURE,
                                    )
                                },
                            ) {
                                Icon(
                                    imageVector =
                                    if (platform is Platform.Android) {
                                        Icons.Default.FolderOpen
                                    } else {
                                        Icons.Default.Save
                                    },
                                    contentDescription = null,
                                )
                            }
                        },
                        readOnly = platform is Platform.Android,
                        label = {
                            Text(
                                text = "Current web-captures location",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Start,
                            )
                        },
                        value = webCaptureLocation,
                        onValueChange = {
                            dataSettingsScreenVM.changeSettingPreferenceValue(
                                preferenceKey = stringPreferencesKey(AppPreferences.WEB_CAPTURES_LOCATION.key),
                                newValue = it,
                            )
                        },
                        modifier = Modifier.padding(horizontal = 15.dp).fillMaxWidth(),
                    )
                }
                item {
                    SettingComponent(
                        SettingComponentParam(
                            isIconNeeded = false,
                            title = "Delete capture with link",
                            doesDescriptionExists = true,
                            description =
                            "Automatically delete the capture folder and its contents from your local storage when the corresponding link is deleted from the app.",
                            isSwitchNeeded = true,
                            isSwitchEnabled = preferences.webCaptureDeleteOnLinkDelete,
                            onSwitchStateChange = {
                                dataSettingsScreenVM.changeSettingPreferenceValue(
                                    preferenceKey =
                                    booleanPreferencesKey(
                                        AppPreferences.WEB_CAPTURE_DELETE_ON_LINK_DELETE.key,
                                    ),
                                    newValue = !preferences.webCaptureDeleteOnLinkDelete,
                                )
                            },
                            icon = Icons.Default.DeleteSweep,
                            shouldFilledIconBeUsed = true,
                        ),
                    )
                }
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 15.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Selective Asset Stripping",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp,
                            )
                            Text(
                                text =
                                "Choose which components to embed. Unchecking items reduces file sizes and local storage footprint but may alter page rendering.",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 20.sp,
                            )
                        }
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            AssetStripOption(
                                label = "Include Images",
                                checked = preferences.webCaptureSaveImages,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey =
                                        booleanPreferencesKey(AppPreferences.WEB_CAPTURE_SAVE_IMAGES.key),
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = "Include Fonts",
                                checked = preferences.webCaptureSaveFonts,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey =
                                        booleanPreferencesKey(AppPreferences.WEB_CAPTURE_SAVE_FONTS.key),
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = "Include CSS Stylesheets",
                                checked = preferences.webCaptureSaveCss,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey =
                                        booleanPreferencesKey(AppPreferences.WEB_CAPTURE_SAVE_CSS.key),
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = "Include Audio Elements",
                                checked = preferences.webCaptureSaveAudio,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey =
                                        booleanPreferencesKey(AppPreferences.WEB_CAPTURE_SAVE_AUDIO.key),
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = "Include Video Elements",
                                checked = preferences.webCaptureSaveVideo,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey =
                                        booleanPreferencesKey(AppPreferences.WEB_CAPTURE_SAVE_VIDEO.key),
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = "Include Page Metadata",
                                checked = preferences.webCaptureSaveMetadata,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey =
                                        booleanPreferencesKey(AppPreferences.WEB_CAPTURE_SAVE_METADATA.key),
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = "Execute JavaScript",
                                checked = preferences.webCaptureExecuteJs,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey =
                                        booleanPreferencesKey(AppPreferences.WEB_CAPTURE_EXECUTE_JS.key),
                                        newValue = it,
                                    )
                                },
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 15.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(15.dp),
                    ) {
                        TextField(
                            value = whitelistDomains,
                            onValueChange = {
                                whitelistDomains = it
                            },
                            supportingText = {
                                Text(
                                    text = "Separate multiple domains with commas",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            trailingIcon = {
                                FilledTonalIconButton(
                                    modifier =
                                    Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                        .pressScaleEffect()
                                        .padding(end = 5.dp),
                                    onClick = {
                                        dataSettingsScreenVM.changeSettingPreferenceValue(
                                            preferenceKey =
                                            stringPreferencesKey(
                                                AppPreferences.WEB_CAPTURE_WHITELIST_DOMAINS.key,
                                            ),
                                            newValue = whitelistDomains,
                                        )
                                        localFocusManager.clearFocus()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = "Only capture from specific domains",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "example.com, wikipedia.org",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            textStyle = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextField(
                            value = blacklistDomains,
                            onValueChange = {
                                blacklistDomains = it
                            },
                            supportingText = {
                                Text(
                                    text = "Separate multiple domains with commas",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            trailingIcon = {
                                FilledTonalIconButton(
                                    modifier =
                                    Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                        .pressScaleEffect()
                                        .padding(end = 5.dp),
                                    onClick = {
                                        dataSettingsScreenVM.changeSettingPreferenceValue(
                                            preferenceKey =
                                            stringPreferencesKey(
                                                AppPreferences.WEB_CAPTURE_BLACKLIST_DOMAINS.key,
                                            ),
                                            newValue = blacklistDomains,
                                        )
                                        localFocusManager.clearFocus()
                                    },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = null,
                                    )
                                }
                            },
                            label = {
                                Text(
                                    "Never auto capture from",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            placeholder = {
                                Text(
                                    "github.com, twitter.com",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            textStyle = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                item {
                    SettingComponent(
                        SettingComponentParam(
                            isIconNeeded = true,
                            title = "Save as versions",
                            doesDescriptionExists = true,
                            description =
                            "Retain historical page snapshots instead of overwriting the existing file when saving a duplicate link or refreshing.",
                            isSwitchNeeded = true,
                            isSwitchEnabled = preferences.webCaptureSaveAsVersions,
                            onSwitchStateChange = {
                                dataSettingsScreenVM.changeSettingPreferenceValue(
                                    preferenceKey =
                                    booleanPreferencesKey(AppPreferences.WEB_CAPTURE_SAVE_AS_VERSIONS.key),
                                    newValue = !preferences.webCaptureSaveAsVersions,
                                )
                            },
                            icon = Icons.Default.History,
                            shouldFilledIconBeUsed = true,
                        ),
                    )
                }

                if (preferences.webCaptureSaveAsVersions) {
                    item {
                        SettingComponent(
                            SettingComponentParam(
                                isIconNeeded = true,
                                title = "Retain all versions",
                                doesDescriptionExists = false,
                                description =
                                "Retain historical page snapshots instead of overwriting the existing file when saving a duplicate link or pulling a fresh updates.",
                                isSwitchNeeded = true,
                                isSwitchEnabled = preferences.webCaptureRetainAllVersions,
                                onSwitchStateChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey =
                                        booleanPreferencesKey(
                                            AppPreferences.WEB_CAPTURE_RETAIN_ALL_VERSIONS.key,
                                        ),
                                        newValue = !preferences.webCaptureRetainAllVersions,
                                    )
                                },
                                icon = Icons.Default.AllInclusive,
                                shouldFilledIconBeUsed = true,
                            ),
                        )
                    }
                    item {
                        SliderOption(
                            modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                            label = "Max versions per page",
                            value = preferences.webCaptureMaxVersions.toFloat(),
                            onValueChange = {
                                dataSettingsScreenVM.changeSettingPreferenceValue(
                                    preferenceKey =
                                    intPreferencesKey(AppPreferences.WEB_CAPTURE_MAX_VERSIONS.key),
                                    newValue = it.toInt(),
                                )
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            enabled = !preferences.webCaptureRetainAllVersions,
                            displayValue = preferences.webCaptureMaxVersions.toString(),
                        )
                    }
                    if (!preferences.webCaptureRetainAllVersions) {
                        item {
                            Text(
                                text =
                                "When the maximum version limit per page is reached, older captures will be automatically deleted to make room for new ones.",
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(horizontal = 15.dp).padding(top = 5.dp),
                            )
                        }
                    }
                }
            }
            item {
                HorizontalDivider()
            }
            item {
                Text(
                    text =
                    "Even when web-captures is disabled, you can save individual pages anytime from a link’s menu. Alternatively, use the main screen’s + button to download a page locally without adding a link.",
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(horizontal = 15.dp),
                )
            }
            item {
                Spacer(Modifier.height(150.dp))
            }
        }
    }
}

@Composable
private fun AssetStripOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .clickable(
                onClick = {
                    onCheckedChange(!checked)
                },
                indication = null,
                interactionSource = null,
            )
            .pointerHoverIcon(icon = PointerIcon.Hand),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SliderOption(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    enabled: Boolean,
    displayValue: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)) {
        Text(
            text = "$label: $displayValue",
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = value,
            onValueChange = { if (enabled) onValueChange(it) },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
        )
    }
}
