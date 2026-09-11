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
import com.sakethh.linkora.ui.LocalizedStrings

@Composable
fun NotificationPermissionDialogBox(
    isVisible: Boolean,
    launchRuntimePermission: () -> Unit,
    hideDialog: () -> Unit,
) {
    val localizedStrings = LocalizedStrings.current
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
                        text = localizedStrings.EnableNotifications,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            },
            title = {
                Text(
                    text = localizedStrings.NotificationPermissionRequired,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp,
                )
            },
            text = {
                Text(
                    text = localizedStrings.NotificationPermissionDesc,
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
                        text = localizedStrings.Cancel,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            },
        )
    }
}
