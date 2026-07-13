package com.sakethh.linkora.ui.components

import android.os.Build
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.utils.rememberLocalizedString

@Composable
fun NotificationPermissionDialogBox(
    isVisible: Boolean,
    launchRuntimePermission: () -> Unit,
    hideDialog: () -> Unit,
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT > 32) {
                            launchRuntimePermission()
                        }
                        hideDialog()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = Localization.Key.EnableNotifications.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            },
            title = {
                Text(
                    text = Localization.Key.NotificationPermissionRequired.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                )
            },
            text = {
                Text(
                    text = Localization.Key.NotificationPermissionDesc.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 15.sp,
                )
            },
            dismissButton = {
                OutlinedButton(
                    onClick = hideDialog,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = Localization.Key.Cancel.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            },
        )
    }
}
