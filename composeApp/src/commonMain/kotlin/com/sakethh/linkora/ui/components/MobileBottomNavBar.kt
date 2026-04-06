package com.sakethh.linkora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.supportsWideDisplay
import kotlin.time.Clock

@Composable
fun MobileBottomNavBar(
    preferences: AppPreferences,
    rootRouteList: List<Navigation.Root>,
    isPerformingStartupSync: Boolean,
    inRootScreen: Boolean?,
    onDoubleTap: (Navigation.Root) -> Unit
) {
    val localNavController = LocalNavController.current
    val mobileBottomNavBarVM: MobileBottomNavBarVM = viewModel(initializer = {
        MobileBottomNavBarVM()
    })
    val currentBackStackEntryState by localNavController.currentBackStackEntryAsState()
    val navDestination = currentBackStackEntryState?.destination
    val inCollectionScreen = navDestination?.hasRoute<Navigation.Root.CollectionsScreen>() == true
    AnimatedVisibility(
        visible = (!inCollectionScreen || (inCollectionScreen && CollectionsScreenVM.inCollectionsListPane)) && !supportsWideDisplay() && inRootScreen == true && !CollectionsScreenVM.isSelectionEnabled.value,
        exit = slideOutVertically(targetOffsetY = { it }),
        enter = slideInVertically(initialOffsetY = { it })
    ) {
        Column(
            modifier = Modifier.animateContentSize().fillMaxWidth()
        ) {
            if (isPerformingStartupSync) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            NavigationBar {
                rootRouteList.forEach { navRouteItem ->
                    if (!preferences.isHomeScreenEnabled && navRouteItem.toString() == Navigation.Root.HomeScreen.toString()) return@forEach

                    val isSelected = navDestination?.hasRoute(navRouteItem::class) == true
                    NavigationBarItem(
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                        selected = isSelected,
                        onClick = {
                            mobileBottomNavBarVM.incrementTapCount(
                                navRouteItem, onDoubleTapSucceeded = {
                                    onDoubleTap(navRouteItem)
                                })
                            if (!isSelected) {
                                localNavController.navigate(navRouteItem) {
                                    // pop up to the initial route on every navigation via bottom nav bar
                                    popUpTo(localNavController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) {
                                    when (navRouteItem) {
                                        Navigation.Root.HomeScreen -> Icons.Filled.Home
                                        Navigation.Root.SearchScreen -> Icons.Filled.Search
                                        Navigation.Root.CollectionsScreen -> Icons.Filled.Folder
                                        Navigation.Root.SettingsScreen -> Icons.Filled.Settings
                                        else -> return@NavigationBarItem
                                    }
                                } else {
                                    when (navRouteItem) {
                                        Navigation.Root.HomeScreen -> Icons.Outlined.Home
                                        Navigation.Root.SearchScreen -> Icons.Outlined.Search
                                        Navigation.Root.CollectionsScreen -> Icons.Outlined.Folder
                                        Navigation.Root.SettingsScreen -> Icons.Outlined.Settings
                                        else -> return@NavigationBarItem
                                    }
                                }, contentDescription = null
                            )
                        },
                        label = {
                            Text(
                                text = navRouteItem.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                fontWeight = if (isSelected) FontWeight.SemiBold
                                else FontWeight.Normal
                            )
                        },
                    )
                }
            }
        }
    }
}

class MobileBottomNavBarVM : ViewModel() {

    private var lastTapTime: Long = 0
    private var lastTappedRoute: Navigation.Root? = null

    fun incrementTapCount(currentRoute: Navigation.Root, onDoubleTapSucceeded: () -> Unit) {
        val currentMilli = Clock.System.now().epochSeconds
        if (currentMilli - lastTapTime <= Constants.DOUBLE_TAP_DELAY && lastTappedRoute == currentRoute) {
            onDoubleTapSucceeded()
            lastTapTime = 0
            lastTappedRoute = null
        } else {
            lastTapTime = currentMilli
            lastTappedRoute = currentRoute
        }
    }

    /* the "HARD" way (the "Rube Goldberg" way); idk why i do this
    private val searchItemTapTimes = MutableStateFlow(0)

    var tappedNavRoute: Navigation.Root = Navigation.Root.HomeScreen

    fun incrementTapCount(currentRoute: Navigation.Root) {
        if (searchItemTapTimes.value >= 2) return

        tappedNavRoute = currentRoute
        ++searchItemTapTimes.value
    }

    init {
        viewModelScope.launch {
            startWatchingForDoubleTap()
        }
    }

    private var doubleTapJob: Job? = null

    private fun CoroutineScope.startWatchingForDoubleTap() {
        doubleTapJob?.cancel()

        doubleTapJob = launch {
            searchItemTapTimes.filter {
                it == 1
            }.collect {
                handleSecondTapIfConfirmed()
                doubleTapJob?.cancel()
            }
        }
    }

    private var secondTapJob: Job? = null
    fun handleSecondTapIfConfirmed() {
        secondTapJob?.cancel()

        secondTapJob = viewModelScope.launch {
            withTimeoutOrNull(500L) {
                searchItemTapTimes.first { it == 2 }
            }.run {
                if (this != null) {
                    onDoubleTapSucceeded(tappedNavRoute)
                }
            }
            searchItemTapTimes.value = 0
        }.also {
            it.invokeOnCompletion {
                viewModelScope.startWatchingForDoubleTap()
            }
        }
    }
*/
}