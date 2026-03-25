package com.sakethh.linkora.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.bottomNavPaddingAcrossPlatforms
import com.sakethh.linkora.utils.rememberLocalizedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateATagBtmSheet(
    sheetState: SheetState,
    showBtmSheet: Boolean,
    onCancel: () -> Unit,
    onCreateClick: (tagName: String) -> Unit
) {
    val focusRequester = remember {
        FocusRequester()
    }
    if (showBtmSheet) {
        var newTag by rememberSaveable {
            mutableStateOf("")
        }
        var showLinearProgressBar by rememberSaveable {
            mutableStateOf(false)
        }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
        ModalBottomSheet(sheetState = sheetState, onDismissRequest = {
            if (!showLinearProgressBar) {
                onCancel()
            }
        }) {
            Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Text(
                    text = Localization.Key.CreateANewTag.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(start = 15.dp)
                )
                TextField(
                    enabled = !showLinearProgressBar,
                    label = {
                        Text(
                            text = Localization.Key.TagName.rememberLocalizedString(), style = MaterialTheme.typography.titleSmall
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    value = newTag,
                    onValueChange = {
                        newTag = it
                    },
                    modifier = Modifier.fillMaxWidth().padding(15.dp)
                        .focusRequester(focusRequester = focusRequester)
                )
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
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).pressScaleEffect()
                        .fillMaxWidth().padding(
                            start = 15.dp,
                            end = 15.dp,
                        )
                ) {
                    Text(
                        text = Localization.Key.Cancel.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                Button(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                        .pressScaleEffect().padding(start = 15.dp, end = 15.dp, bottom = 5.dp)
                        .bottomNavPaddingAcrossPlatforms(), onClick = {
                        showLinearProgressBar = true
                        onCreateClick(newTag)
                    }) {
                    Text(
                        text = Localization.Key.Create.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}