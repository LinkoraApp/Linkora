package com.sakethh.linkora.ui.screens.collections.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameTagComponent(
    showComponent: Boolean,
    sheetState: SheetState,
    existingName: String,
    onHide: () -> Unit,
    onSave: (newName: String) -> Unit
) {
    var newTagName by rememberSaveable(existingName) {
        mutableStateOf(existingName)
    }
    val tagFieldFocusRequester = remember {
        FocusRequester()
    }
    var showLinearProgressBar by rememberSaveable {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    val hideComponent: () -> Unit = {
        if (!showLinearProgressBar) {
            coroutineScope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                onHide()
            }
        }
    }
    if (showComponent) {
        LaunchedEffect(Unit) {
            showLinearProgressBar = false
        }
        ModalBottomSheet(sheetState = sheetState, onDismissRequest = hideComponent) {
            Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Text(
                    text = Localization.Key.RenameTagName.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(start = 15.dp)
                )
                TextField(
                    enabled = !showLinearProgressBar,
                    value = newTagName,
                    textStyle = MaterialTheme.typography.titleSmall,
                    onValueChange = {
                        newTagName = it
                    },
                    modifier = Modifier.padding(15.dp).fillMaxWidth()
                        .focusRequester(tagFieldFocusRequester),
                    label = {
                        Text(text = Localization.Key.NewTagName.rememberLocalizedString(), style = MaterialTheme.typography.titleSmall)
                    })
                if (showLinearProgressBar) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(
                            start = 15.dp,
                            end = 15.dp,
                            bottom = if (!Platform.Android.onMobile()) 15.dp else 0.dp
                        )
                    )
                    return@Column
                }
                Button(
                    modifier = Modifier.pressScaleEffect().pointerHoverIcon(icon = PointerIcon.Hand)
                        .fillMaxWidth().padding(start = 15.dp, end = 15.dp), onClick = {
                        onSave(newTagName)
                    }) {
                    Text(text = Localization.Key.Update.rememberLocalizedString(), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        LaunchedEffect(Unit) {
            tagFieldFocusRequester.requestFocus()
        }
    }
}