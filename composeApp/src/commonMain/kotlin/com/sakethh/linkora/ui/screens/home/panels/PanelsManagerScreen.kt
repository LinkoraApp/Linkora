package com.sakethh.linkora.ui.screens.home.panels

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ViewArray
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.HomeScreenVMAssistedFactory
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.ui.LocalFabController
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.AddANewPanelDialogBox
import com.sakethh.linkora.ui.components.AddANewPanelParam
import com.sakethh.linkora.ui.components.DeleteAPanelDialogBox
import com.sakethh.linkora.ui.components.DeleteAPanelDialogBoxParam
import com.sakethh.linkora.ui.components.RenameAShelfPanelDialogBox
import com.sakethh.linkora.ui.components.menu.IndividualMenuComponent
import com.sakethh.linkora.ui.domain.CurrentFABContext
import com.sakethh.linkora.ui.domain.FABContext
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.home.HomeScreenVM
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import com.sakethh.linkora.utils.rememberLocalizedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelsManagerScreen(
    specificPanelManagerScreenParam: SpecificPanelManagerScreenParam,
    performAction: (PanelsAction) -> Unit
) {
    val localFABContext = LocalFabController.current
    LifecycleResumeEffect(Unit) {
        localFABContext.updateState(CurrentFABContext(FABContext.HIDE))
        onPauseOrDispose {}
    }
    val navController = LocalNavController.current
    val topAppBarState = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val homeScreenVM: HomeScreenVM =
        viewModel(factory = HomeScreenVMAssistedFactory.createForPanelsManagerScreen())
    val panels = homeScreenVM.existingPanels.collectAsStateWithLifecycle()
    val selectedPanelForDetailView = rememberDeserializableMutableObject {
        mutableStateOf(Panel(localId = -1, panelName = ""))
    }
    val selectedPanelForDialogBoxes = rememberDeserializableMutableObject {
        mutableStateOf(Panel(localId = -1, panelName = ""))
    }
    val isAddANewPanelDialogBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val isDeleteAPanelDialogBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val isRenameAPanelDialogBoxVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val onAndroidMobile = Platform.Android.onMobile()
    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
        BottomAppBar(
            modifier = Modifier.fillMaxWidth(if (onAndroidMobile) 1f else 0.5f)
        ) {
            Button(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(15.dp)
                    .navigationBarsPadding().fillMaxWidth().pressScaleEffect(0.9f), onClick = {
                    isAddANewPanelDialogBoxVisible.value = true
                }) {
                Text(
                    text = Localization.Key.AddANewPanel.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 16.sp
                )
            }
        }
    }, topBar = {
        Column {
            MediumTopAppBar(navigationIcon = {
                IconButton(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand), onClick = {
                        navController.navigateUp()
                    }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "")
                }
            }, scrollBehavior = topAppBarState, title = {
                Text(
                    text = Localization.Key.Panels.rememberLocalizedString(),
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium,
                )
            })
            HorizontalDivider()
        }
    }) {
        Row(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.padding(it)
                    .fillMaxWidth(if (onAndroidMobile) 1f else 0.5f)
                    .animateContentSize().nestedScroll(topAppBarState.nestedScrollConnection)
            ) {
                if (panels.value.drop(1).isEmpty()) {
                    item {
                        DataEmptyScreen(text = Localization.Key.NoPanelsFound.rememberLocalizedString())
                    }
                }
                items(panels.value.drop(1)) { panel ->
                    IndividualMenuComponent(
                        onClick = {
                            SpecificPanelManagerScreenVM.updateSelectedPanel(panel)
                            selectedPanelForDetailView.value = panel
                            if (onAndroidMobile) {
                                navController.navigate(Navigation.Home.SpecificPanelManagerScreen)
                            }
                        },
                        elementName = panel.panelName,
                        elementImageVector = Icons.Default.ViewArray,
                        inPanelsScreen = true,
                        isSelected = selectedPanelForDetailView.value.localId == panel.localId,
                        onDeleteClick = {
                            selectedPanelForDialogBoxes.value = panel
                            isDeleteAPanelDialogBoxVisible.value = true
                        },
                        onRenameClick = {
                            selectedPanelForDialogBoxes.value = panel
                            isRenameAPanelDialogBoxVisible.value = true
                        })
                }
            }
            VerticalDivider()
            if (selectedPanelForDetailView.value.localId <= 0) {
                Box(
                    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = Localization.Key.SelectAPanel.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                SpecificPanelManagerScreen(
                    specificPanelManagerScreenParam = specificPanelManagerScreenParam
                )
            }
        }
    }
    AddANewPanelDialogBox(
        addANewPanelParam = AddANewPanelParam(
            isDialogBoxVisible = isAddANewPanelDialogBoxVisible,
            onCreateClick = { panelName, onCompletion ->
                performAction(
                    PanelsAction.AddANewAPanel(
                        panel = Panel(panelName = panelName), onCompletion = {
                            onCompletion()
                            isAddANewPanelDialogBoxVisible.value = false
                        })
                )
            })
    )

    DeleteAPanelDialogBox(
        deleteAPanelDialogBoxParam = DeleteAPanelDialogBoxParam(
            isDialogBoxVisible = isDeleteAPanelDialogBoxVisible, onDeleteClick = { onCompletion ->
                performAction(
                    PanelsAction.DeleteAPanel(
                        panelId = selectedPanelForDialogBoxes.value.localId, onCompletion = {
                            onCompletion()
                            selectedPanelForDetailView.value = Panel(localId = -45, panelName = "")
                            isDeleteAPanelDialogBoxVisible.value = false
                        })
                )
            }, panelName = selectedPanelForDialogBoxes.value.panelName
        )
    )

    RenameAShelfPanelDialogBox(
        isDialogBoxVisible = isRenameAPanelDialogBoxVisible,
        onRenameClick = { newPanelName, onCompletion ->
            performAction(
                PanelsAction.RenameAPanel(
                    panelId = selectedPanelForDialogBoxes.value.localId,
                    newName = newPanelName, onCompletion = {
                        onCompletion()
                        SpecificPanelManagerScreenVM.updateSelectedPanel(
                            Panel(
                                selectedPanelForDialogBoxes.value.localId, newPanelName
                            )
                        )
                        isRenameAPanelDialogBoxVisible.value = false
                    })
            )
        },
        panelName = selectedPanelForDialogBoxes.value.panelName
    )
}