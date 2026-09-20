package com.sakethh.linkora.ui.screens.settings.section.data.capture

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.VerticalInfoCard
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.highlightOnFocused
import com.sakethh.linkora.utils.replaceActual

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebPageCaptureScreen() {
    val dataSettingsScreenVM = linkoraViewModel<DataSettingsScreenVM>()
    val preferences by dataSettingsScreenVM.preferencesAsFlow.collectAsStateWithLifecycle()

    val localFocusManager = LocalFocusManager.current
    var whitelistDomains by rememberSaveable(preferences.webCaptureWhitelistDomains) {
        mutableStateOf(preferences.webCaptureWhitelistDomains)
    }
    var blacklistDomains by rememberSaveable(preferences.webCaptureBlacklistDomains) {
        mutableStateOf(preferences.webCaptureBlacklistDomains)
    }
    var webCaptureLocation by rememberSaveable(preferences.webCapturesLocation) {
        mutableStateOf(preferences.webCapturesLocation)
    }
    val platform = LocalPlatform.current
    var maxConcurrentWebCaptureCount by rememberSaveable(preferences.webCaptureMaxConcurrency) {
        mutableIntStateOf(preferences.webCaptureMaxConcurrency)
    }

    val webCaptureState by dataSettingsScreenVM.webCaptureAllLinksState.collectAsStateWithLifecycle()
    val localizedStrings = LocalizedStrings.current
    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.Data.WebPageCapturesScreen.toString(),
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.animateContentSize().fillMaxSize()
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
                        title = localizedStrings.UseWebCaptures,
                        doesDescriptionExists = true,
                        description = localizedStrings.UseWebCapturesDesc,
                        isSwitchNeeded = true,
                        isSwitchEnabled = preferences.useWebCaptures,
                        onSwitchStateChange = {
                            if (preferences.webCapturesLocation.isBlank() && platform !is Platform.Android.TV) return@SettingComponentParam

                            dataSettingsScreenVM.changeSettingPreferenceValue(
                                preferenceKey = AppPreferences.USE_WEB_CAPTURES,
                                newValue = !preferences.useWebCaptures,
                            )
                            if (it) {
                                dataSettingsScreenVM.initWebCapture(
                                    preferences = preferences,
                                    onCompletion = {}
                                )
                            } else {
                                dataSettingsScreenVM.nukeWebCapture()
                            }
                        },
                        icon = Icons.Default.Web,
                        shouldFilledIconBeUsed = false,
                    ),
                )
                if (preferences.webCapturesLocation.isNotBlank() || platform is Platform.Android.TV) {
                    VerticalInfoCard(
                        info = localizedStrings.WebCapturesLocationDesc.replaceActual(if (platform is Platform.Android.TV) "Documents/Linkora/${ExportLocationType.WEB_CAPTURE.dirRef}" else preferences.webCapturesLocation),
                        paddingValues = PaddingValues(start = 15.dp, end = 15.dp, top = 15.dp),
                    )
                }
            }

            if (preferences.webCapturesLocation.isBlank() && platform !is Platform.Android.TV) {
                item {
                    VerticalInfoCard(
                        info = localizedStrings.WebCapturesDesc,
                        paddingValues = PaddingValues(start = 15.dp, end = 15.dp, bottom = 15.dp),
                    )
                    TextField(
                        textStyle = MaterialTheme.typography.titleSmall,
                        trailingIcon = {
                            if (platform !is Platform.Android.TV) {
                                FilledTonalIconButton(
                                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                        .pressScaleEffect().padding(end = 5.dp),
                                    onClick = {
                                        dataSettingsScreenVM.changeExportLocation(
                                            exportLocation = webCaptureLocation,
                                            platform = platform,
                                            exportLocationType = ExportLocationType.WEB_CAPTURE,
                                        )
                                    },
                                ) {
                                    Icon(
                                        imageVector = if (platform is Platform.Android) {
                                            Icons.Default.FolderOpen
                                        } else {
                                            Icons.Default.Save
                                        },
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        readOnly = platform is Platform.Android,
                        label = {
                            Text(
                                text = "Select Web-captures directory",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Start,
                            )
                        },
                        value = webCaptureLocation,
                        onValueChange = {
                            webCaptureLocation = it
                        },
                        modifier = Modifier.padding(horizontal = 15.dp).fillMaxWidth()
                            .highlightOnFocused().clickable(onClick = {
                                if (platform is Platform.Android.TV) {
                                    dataSettingsScreenVM.changeExportLocation(
                                        exportLocation = webCaptureLocation,
                                        platform = platform,
                                        exportLocationType = ExportLocationType.WEB_CAPTURE,
                                    )
                                }
                            }, indication = null, interactionSource = null),
                    )
                    Spacer(modifier = Modifier.height(150.dp))
                }
                return@LazyColumn
            }
            if (preferences.useWebCaptures) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 15.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = localizedStrings.SelectiveAssetStrippingLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp,
                            )
                            Text(
                                text = localizedStrings.SelectiveAssetStrippingDesc,
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
                                label = localizedStrings.WebCaptureIncludeImages,
                                checked = preferences.webCaptureSaveImages,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey = AppPreferences.WEB_CAPTURE_SAVE_IMAGES,
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = localizedStrings.WebCaptureIncludeFonts,
                                checked = preferences.webCaptureSaveFonts,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey = AppPreferences.WEB_CAPTURE_SAVE_FONTS,
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = localizedStrings.WebCaptureIncludeCSS,
                                checked = preferences.webCaptureSaveCss,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey = AppPreferences.WEB_CAPTURE_SAVE_CSS,
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = localizedStrings.WebCaptureIncludeAudio,
                                checked = preferences.webCaptureSaveAudio,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey = AppPreferences.WEB_CAPTURE_SAVE_AUDIO,
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = localizedStrings.WebCaptureIncludeVideo,
                                checked = preferences.webCaptureSaveVideo,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey = AppPreferences.WEB_CAPTURE_SAVE_VIDEO,
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = localizedStrings.WebCaptureIncludeMetadata,
                                checked = preferences.webCaptureSaveMetadata,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey = AppPreferences.WEB_CAPTURE_SAVE_METADATA,
                                        newValue = it,
                                    )
                                },
                            )
                            AssetStripOption(
                                label = localizedStrings.WebCaptureIncludeJS,
                                checked = preferences.webCaptureExecuteJs,
                                onCheckedChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey = AppPreferences.WEB_CAPTURE_EXECUTE_JS,
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
                                    text = localizedStrings.SeparateDomainsDesc,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            trailingIcon = {
                                FilledTonalIconButton(
                                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                        .pressScaleEffect().padding(end = 5.dp),
                                    onClick = {
                                        dataSettingsScreenVM.changeSettingPreferenceValue(
                                            preferenceKey = AppPreferences.WEB_CAPTURE_WHITELIST_DOMAINS,
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
                                    text = localizedStrings.CaptureFromSpecificDomains,
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
                                    text = localizedStrings.SeparateDomainsDesc,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            },
                            trailingIcon = {
                                FilledTonalIconButton(
                                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                        .pressScaleEffect().padding(end = 5.dp),
                                    onClick = {
                                        dataSettingsScreenVM.changeSettingPreferenceValue(
                                            preferenceKey = AppPreferences.WEB_CAPTURE_BLACKLIST_DOMAINS,
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
                                    text = localizedStrings.NeverAutoCaptureFrom,
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
                            title = localizedStrings.WebCaptureSaveAsVersionsLabel,
                            doesDescriptionExists = true,
                            description = localizedStrings.WebCaptureSaveAsVersionsDesc,
                            isSwitchNeeded = true,
                            isSwitchEnabled = preferences.webCaptureSaveAsVersions,
                            onSwitchStateChange = {
                                dataSettingsScreenVM.changeSettingPreferenceValue(
                                    preferenceKey = AppPreferences.WEB_CAPTURE_SAVE_AS_VERSIONS,
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
                                title = localizedStrings.WebCaptureRetainAllVersionsLabel,
                                doesDescriptionExists = false,
                                description = localizedStrings.WebCaptureRetainAllVersionsDesc,
                                isSwitchNeeded = true,
                                isSwitchEnabled = preferences.webCaptureRetainAllVersions,
                                onSwitchStateChange = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey = AppPreferences.WEB_CAPTURE_RETAIN_ALL_VERSIONS,
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
                            label = localizedStrings.WebCaptureRetainMaxVersionsPerPageLabel,
                            value = preferences.webCaptureMaxVersions.toFloat(),
                            onValueChange = {
                                dataSettingsScreenVM.changeSettingPreferenceValue(
                                    preferenceKey = AppPreferences.WEB_CAPTURE_MAX_VERSIONS,
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
                                text = localizedStrings.WebCaptureRetainMaxVersionsPerPageDesc,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(horizontal = 15.dp).padding(top = 5.dp),
                            )
                        }
                    }
                }
                item {
                    TextField(
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.titleSmall,
                        trailingIcon = {
                            FilledTonalIconButton(
                                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                    .pressScaleEffect().padding(end = 5.dp),
                                onClick = {
                                    dataSettingsScreenVM.changeSettingPreferenceValue(
                                        preferenceKey = AppPreferences.WEB_CAPTURE_MAX_CONCURRENCY,
                                        newValue = maxConcurrentWebCaptureCount,
                                    )
                                    localFocusManager.clearFocus(force = true)
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
                                text = localizedStrings.MaxConcurrentCapturesLabel,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Start,
                            )
                        },
                        supportingText = {
                            Text(
                                text = localizedStrings.MaxConcurrentCapturesDesc,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        },
                        value = maxConcurrentWebCaptureCount.toString(),
                        onValueChange = {
                            maxConcurrentWebCaptureCount = try {
                                it.toInt()
                            } catch (_: Exception) {
                                0
                            } catch (_: Error) {
                                0
                            }.coerceAtLeast(minimumValue = 1)
                        },
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 5.dp)
                            .fillMaxWidth(),
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(5.dp))

                    AnimatedContent(targetState = webCaptureState) { state ->
                        when (state) {
                            WorkerState.ENQUEUED -> {
                                if (platform is Platform.Android) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 15.dp)
                                            .fillMaxWidth()
                                            .wrapContentHeight(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(
                                            text = localizedStrings.WorkManagerSchedulingCaptures,
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    }
                                }
                            }

                            WorkerState.IN_PROGRESS -> {
                                Column(
                                    modifier = Modifier.padding(horizontal = 15.dp).fillMaxWidth()
                                        .wrapContentHeight(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Text(
                                        text = localizedStrings.CapturingWebPages,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth(0.85f),
                                            progress = {
                                                val total =
                                                    DataSettingsScreenVM.onGoingWebCaptureState.total
                                                if (total == 0) 0f else DataSettingsScreenVM.onGoingWebCaptureState.currentIteration.toFloat() / total
                                            },
                                        )
                                        IconButton(
                                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                                            onClick = { dataSettingsScreenVM.cancelBulkWebCapture() },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Cancel,
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                    Text(
                                        text = localizedStrings.CapturedCount.replaceActual(
                                            DataSettingsScreenVM.onGoingWebCaptureState.currentIteration.toString(),
                                            DataSettingsScreenVM.onGoingWebCaptureState.total.toString()
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }

                            WorkerState.IDLE -> {
                                SettingComponent(
                                    SettingComponentParam(
                                        title = localizedStrings.CaptureAllLinksLabel,
                                        doesDescriptionExists = true,
                                        description = localizedStrings.CaptureAllLinksDesc,
                                        isSwitchNeeded = false,
                                        isIconNeeded = true,
                                        icon = Icons.Default.Web,
                                        isSwitchEnabled = false,
                                        onSwitchStateChange = {
                                            dataSettingsScreenVM.captureAllWebPages()
                                        },
                                        shouldFilledIconBeUsed = true,
                                    ),
                                )
                            }
                        }
                    }
                }
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
        modifier = modifier.fillMaxWidth().clickable(
            onClick = {
                onCheckedChange(!checked)
            },
            indication = null,
            interactionSource = null,
        ).pointerHoverIcon(icon = PointerIcon.Hand),
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
