package com.sakethh.linkora.ui.screens.collections

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.DatasetLinked
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Platform
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.ui.components.AddItemFABParam
import com.sakethh.linkora.ui.components.AddItemFab
import com.sakethh.linkora.ui.components.folder.FolderComponent
import com.sakethh.linkora.ui.domain.model.FolderComponentParam
import com.sakethh.linkora.ui.navigation.NavigationRoute
import com.sakethh.linkora.ui.utils.pulsateEffect
import com.sakethh.platform
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalComposeUiApi::class
)
@Composable
fun CollectionsScreen() {
    val shouldRenameDialogBoxBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldDeleteDialogBoxBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    val btmModalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clickedItemName = rememberSaveable { mutableStateOf("") }
    val clickedItemNote = rememberSaveable { mutableStateOf("") }
    val btmModalSheetStateForSavingLinks = rememberModalBottomSheetState()
    val shouldOptionsBtmModalSheetBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val isMainFabRotated = rememberSaveable {
        mutableStateOf(false)
    }
    val rotationAnimation = remember {
        Animatable(0f)
    }
    val shouldScreenTransparencyDecreasedBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val areFoldersSelectable = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldDialogForNewLinkAppear = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldDialogForNewFolderAppear = rememberSaveable {
        mutableStateOf(false)
    }
    val shouldSortingBottomSheetAppear = rememberSaveable {
        mutableStateOf(false)
    }
    val sortingBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shouldBtmSheetForNewLinkAdditionBeEnabled = rememberSaveable {
        mutableStateOf(false)
    }
    Scaffold(
        floatingActionButton = {
            AddItemFab(
                AddItemFABParam(
                    newLinkBottomModalSheetState = btmModalSheetStateForSavingLinks,
                    shouldBtmSheetForNewLinkAdditionBeEnabled = shouldBtmSheetForNewLinkAdditionBeEnabled,
                    shouldScreenTransparencyDecreasedBoxVisible = shouldScreenTransparencyDecreasedBoxVisible,
                    shouldDialogForNewFolderAppear = shouldDialogForNewFolderAppear,
                    shouldDialogForNewLinkAppear = shouldDialogForNewLinkAppear,
                    isMainFabRotated = isMainFabRotated,
                    rotationAnimation = rotationAnimation,
                    inASpecificScreen = false
                )
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
        topBar = {
            Column {
                TopAppBar(actions = {}, navigationIcon = {}, title = {
                    Text(
                        text = NavigationRoute.CollectionsScreen.toString(),
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 18.sp
                    )
                })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(0.25f))
            }
        }) { padding ->
        val listDetailPaneNavigator =
            rememberListDetailPaneScaffoldNavigator<SelectedCollectionInfo>()
        ListDetailPaneScaffold(
            modifier = Modifier.padding(padding).fillMaxSize(),
            directive = listDetailPaneNavigator.scaffoldDirective,
            value = listDetailPaneNavigator.scaffoldValue,
            listPane = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        DefaultFolderComponent(
                            name = "All Links",
                            icon = Icons.Outlined.DatasetLinked,
                            onClick = {
                                listDetailPaneNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail, SelectedCollectionInfo(
                                        name = "All Links", id = -1
                                    )
                                )
                            },
                            listDetailPaneNavigator.currentDestination?.content?.id == (-1).toLong()
                        )
                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = 20.dp, top = 15.dp, bottom = 10.dp
                            ),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(0.25f)
                        )
                    }
                    item {
                        DefaultFolderComponent(
                            name = "Saved Links",
                            icon = Icons.Outlined.Link,
                            onClick = { ->
                                listDetailPaneNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail, SelectedCollectionInfo(
                                        name = "Saved Links", id = -2
                                    )
                                )
                            },
                            listDetailPaneNavigator.currentDestination?.content?.id == (-2).toLong()
                        )
                    }
                    item {
                        DefaultFolderComponent(
                            name = "Important Links",
                            icon = Icons.Outlined.StarOutline,
                            onClick = { ->
                                listDetailPaneNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail, SelectedCollectionInfo(
                                        name = "Important Links", id = -3
                                    )
                                )
                            },
                            isSelected = listDetailPaneNavigator.currentDestination?.content?.id == (-3).toLong()
                        )
                    }
                    item {
                        DefaultFolderComponent(
                            name = "Archive",
                            icon = Icons.Outlined.Archive,
                            onClick = { ->
                                listDetailPaneNavigator.navigateTo(
                                    ListDetailPaneScaffoldRole.Detail, SelectedCollectionInfo(
                                        name = "Archive", id = -4
                                    )
                                )
                            },
                            isSelected = listDetailPaneNavigator.currentDestination?.content?.id == (-4).toLong()
                        )

                    }
                    item {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                start = 20.dp, top = 15.dp, bottom = 10.dp
                            ),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(0.25f)
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Folders",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(start = 15.dp)
                            )
                            IconButton(modifier = Modifier.pulsateEffect(), onClick = {

                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Sort,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                    items(25) {
                        FolderComponent(
                            FolderComponentParam(
                                folder = Folder(
                                    name = "Folder $it", note = "", parentFolderId = null, id = 0L
                                ),
                                onClick = { ->
                                    listDetailPaneNavigator.navigateTo(
                                        ListDetailPaneScaffoldRole.Detail, SelectedCollectionInfo(
                                            name = "Folder $it", id = it.toLong()
                                        )
                                    )
                                },
                                onLongClick = { -> },
                                onMoreIconClick = { -> },
                                isCurrentlyInDetailsView = remember(listDetailPaneNavigator.currentDestination?.content?.id) {
                                    mutableStateOf(listDetailPaneNavigator.currentDestination?.content?.id == it.toLong())
                                },
                                showMoreIcon = rememberSaveable {
                                    mutableStateOf(true)
                                })
                        )
                    }
                }
            },
            detailPane = {
                Row(modifier = Modifier.fillMaxSize()) {
                    VerticalDivider()
                    if (listDetailPaneNavigator.currentDestination?.content == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a Collection",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    } else {
                        CollectionDetailPane(
                            selectedCollectionInfo = listDetailPaneNavigator.currentDestination?.content!!,
                            paneNavigator = listDetailPaneNavigator
                        )
                    }
                }
            })
        if (shouldScreenTransparencyDecreasedBoxVisible.value) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(0.95f)).clickable {
                        shouldScreenTransparencyDecreasedBoxVisible.value = false
                        coroutineScope.launch {
                            awaitAll(async {
                                rotationAnimation.animateTo(
                                    -360f, animationSpec = tween(300)
                                )
                            }, async { isMainFabRotated.value = false })
                        }.invokeOnCompletion {
                            coroutineScope.launch {
                                rotationAnimation.snapTo(0f)
                            }
                        }
                    })
        }
    }
}


@Composable
private fun DefaultFolderComponent(
    name: String, icon: ImageVector, onClick: () -> Unit, isSelected: Boolean
) {
    Card(
        modifier = Modifier.padding(
            end = if (platform() == Platform.Android.Mobile) 20.dp else 0.dp,
            start = 20.dp,
            top = 15.dp
        ).wrapContentHeight().fillMaxWidth().clickable(interactionSource = remember {
            MutableInteractionSource()
        }, indication = null, onClick = {
            onClick()
        }).pulsateEffect().then(
            if (isSelected) Modifier.border(
                width = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CardDefaults.shape
            ) else Modifier
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.padding(20.dp), imageVector = icon, contentDescription = null
            )
            Text(
                text = name, style = MaterialTheme.typography.titleSmall, fontSize = 16.sp
            )
        }
    }
}