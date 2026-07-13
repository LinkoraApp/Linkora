package com.sakethh.linkora.ui.screens.settings.common.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.utils.pressScaleEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSectionScaffold(
    topAppBarText: String,
    floatingActionButton: ComposableContent = {},
    actions: ComposableContent = {},
    bottomBar: ComposableContent = {},
    content:
    @Composable (paddingValues: PaddingValues, topAppBarScrollBehaviour: TopAppBarScrollBehavior) -> Unit,
) {
    val navController = LocalNavController.current
    val topAppBarScrollBehaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        bottomBar = bottomBar,
        floatingActionButton = {
            floatingActionButton()
        },
        floatingActionButtonPosition = FabPosition.Center,
        topBar = {
            Column {
                LargeTopAppBar(
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            actions()
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            modifier =
                            Modifier.pointerHoverIcon(icon = PointerIcon.Hand).pressScaleEffect(),
                            onClick = {
                                navController.navigateUp()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    scrollBehavior = topAppBarScrollBehaviour,
                    title = {
                        Text(
                            text = topAppBarText,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp,
                            modifier = Modifier.fillMaxWidth().padding(end = 25.dp),
                        )
                    },
                )
            }
        },
    ) {
        content(it, topAppBarScrollBehaviour)
    }
}
