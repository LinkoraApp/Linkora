package com.sakethh.linkora.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackupTable
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.domain.AppAction
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.utils.currentSavedServerConfig

@Composable
fun DesktopNavigationRail(
    preferences: AppPreferences,
    rootRouteList: List<Navigation.Root>,
    currentRoute: NavDestination?,
    isDataSyncingFromPullRefresh: MutableState<Boolean>,
    onNavigate: () -> Unit,
    isPerformingStartupSync: Boolean,
    getLastSyncedTime: suspend () -> Long,
    isAnySnapshotOngoing: Boolean,
    performAction: (AppAction) -> Unit
) {
    val localNavController = LocalNavController.current
    Row {
        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rootRouteList.forEach { navRouteItem ->
                    if (!preferences.isHomeScreenEnabled && navRouteItem.toString() == Navigation.Root.HomeScreen.toString()) return@forEach

                    val isSelected = currentRoute?.hasRoute(navRouteItem::class) == true
                    NavigationRailItem(
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(
                            start = 15.dp, end = 15.dp, top = 15.dp
                        ), selected = isSelected, onClick = {
                            if (currentRoute?.hasRoute(navRouteItem::class) == false) {
                                localNavController.navigate(navRouteItem)
                                onNavigate()
                            }
                        }, icon = {
                            Icon(
                                imageVector = if (isSelected) {
                                    when (navRouteItem) {
                                        Navigation.Root.HomeScreen -> Icons.Filled.Home
                                        Navigation.Root.SearchScreen -> Icons.Filled.Search
                                        Navigation.Root.CollectionsScreen -> Icons.Filled.Folder
                                        Navigation.Root.SettingsScreen -> Icons.Filled.Settings
                                        else -> return@NavigationRailItem
                                    }
                                } else {
                                    when (navRouteItem) {
                                        Navigation.Root.HomeScreen -> Icons.Outlined.Home
                                        Navigation.Root.SearchScreen -> Icons.Outlined.Search
                                        Navigation.Root.CollectionsScreen -> Icons.Outlined.Folder
                                        Navigation.Root.SettingsScreen -> Icons.Outlined.Settings
                                        else -> return@NavigationRailItem
                                    }
                                }, contentDescription = null
                            )
                        }, label = {
                            Text(
                                text = navRouteItem.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        })
                }
            }
            Box(
                Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter
            ) {
                if (!Platform.Android.onMobile() && preferences.serverBaseUrl.isNotBlank()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
                    ) {
                        IconButton(onClick = {
                            if (!isPerformingStartupSync && !isDataSyncingFromPullRefresh.value) {
                                performAction(
                                    AppAction.SaveServerConnectionAndSync(
                                        serverConnection = preferences.currentSavedServerConfig(),
                                        timeStampAfter = getLastSyncedTime,
                                        onSyncStart = {
                                            isDataSyncingFromPullRefresh.value = true
                                        },
                                        onCompletion = {
                                            isDataSyncingFromPullRefresh.value = false
                                        })
                                )
                            }
                        }, modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)) {
                            Icon(
                                imageVector = Icons.Default.CloudSync, contentDescription = null
                            )
                        }
                        if (isPerformingStartupSync || isDataSyncingFromPullRefresh.value) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (preferences.areSnapshotsEnabled && platform == Platform.Desktop) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
                            .alpha(if (!Platform.Android.onMobile() && isAnySnapshotOngoing) 1f else 0.25f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BackupTable, contentDescription = null
                        )

                        if (isAnySnapshotOngoing) CircularProgressIndicator()
                    }
                }
            }
        }
        VerticalDivider()
    }
}