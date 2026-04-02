package com.sakethh.linkora.ui.screens.settings.section.data.sync

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.utils.bottomNavPaddingAcrossPlatforms
import com.sakethh.linkora.utils.fillMaxWidthWithPadding
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerManagementBottomSheet(
    preferences: AppPreferences,
    removeTheConnection: (onDeletion: () -> Unit) -> Unit,
    sheetState: SheetState,
    isVisible: MutableState<Boolean>,
) {
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    if (isVisible.value) {
        ModalBottomSheet(sheetState = sheetState, onDismissRequest = {
            coroutineScope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                isVisible.value = false
            }
        }) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().bottomNavPaddingAcrossPlatforms()
            ) {
                item {
                    Text(
                        text = Localization.rememberLocalizedString(Localization.Key.ManageConnectedServer),
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        modifier = Modifier.fillMaxWidth().padding(end = 25.dp, start = 15.dp)
                    )
                }
                item {
                    Spacer(Modifier.height(30.dp))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidthWithPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.SettingsRemote, contentDescription = null)
                        Spacer(Modifier.width(15.dp))
                        Column {
                            Text(
                                text = Localization.rememberLocalizedString(Localization.Key.CurrentlyConnectedTo),
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = preferences.serverBaseUrl,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(Modifier.height(30.dp))
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidthWithPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.SyncAlt, contentDescription = null)
                        Spacer(Modifier.width(15.dp))
                        Column {
                            Text(
                                text = Localization.rememberLocalizedString(Localization.Key.SyncType),
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = preferences.serverSyncType.asUIString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(30.dp))
                    FilledTonalButton(
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidthWithPadding(), onClick = {
                            navController.navigate(Navigation.Settings.Data.ServerSetupScreen)
                        }) {
                        Text(
                            text = Localization.rememberLocalizedString(Localization.Key.EditServerConfiguration),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
                item {
                    Button(
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidthWithPadding(), colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ), onClick = {
                            removeTheConnection {
                                coroutineScope.launch {
                                    sheetState.hide()
                                }.invokeOnCompletion {
                                    isVisible.value = false
                                }
                            }
                        }) {
                        Text(
                            text = Localization.rememberLocalizedString(Localization.Key.DeleteTheServerConnection),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}