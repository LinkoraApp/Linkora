package com.sakethh.linkora.ui.screens.collections.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.rememberLocalizedString

@Composable
fun TagDeletionConfirmation(
    showConfirmation: Boolean, onHide: () -> Unit, onDelete: () -> Unit
) {
    if (showConfirmation) {
        var showLinearProgressBar by rememberSaveable {
            mutableStateOf(false)
        }
        AlertDialog(
            modifier = Modifier.animateContentSize(),
            onDismissRequest = {
                if (!showLinearProgressBar){
                    onHide()
                }
            },
            confirmButton = {
                if (!showLinearProgressBar) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.pressScaleEffect()
                            .pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                    ) {
                        Text(
                            text = Localization.Key.Delete.getLocalizedString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            },
            dismissButton = {
                if (!showLinearProgressBar) {
                    OutlinedButton(
                        onClick = onHide,
                        modifier = Modifier.pressScaleEffect()
                            .pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                    ) {
                        Text(
                            text = Localization.Key.Cancel.getLocalizedString(),
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    modifier = Modifier.size(48.dp),
                    contentDescription = null
                )
            },
            title = {
                Text(
                    text = Localization.Key.TagDeletionConfirmation.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 24.sp
                )
            },
            text = {
                if (showLinearProgressBar) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(
                            start = 15.dp,
                            end = 15.dp,
                            bottom = if (!Platform.Android.onMobile()) 15.dp else 0.dp
                        )
                    )
                }
            })
    }
}