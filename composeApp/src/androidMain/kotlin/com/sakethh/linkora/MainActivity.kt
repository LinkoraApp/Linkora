package com.sakethh.linkora

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.App
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.components.NotificationPermissionDialogBox
import com.sakethh.linkora.ui.theme.DarkColors
import com.sakethh.linkora.ui.theme.LightColors
import com.sakethh.linkora.ui.theme.LinkoraTheme
import com.sakethh.linkora.utils.AndroidUIEvent
import com.sakethh.linkora.utils.AndroidUIEvent.pushUIEvent

class MainActivity : ComponentActivity() {

    companion object {
        var wasLaunched = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val navController = rememberNavController()
            val coroutineScope = rememberCoroutineScope()
            val storageRuntimePermission = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { permissionGranted ->
                    coroutineScope.pushUIEvent(
                        AndroidUIEvent.Type.StoragePermissionGrantedForAndBelowQ(
                            isGranted = permissionGranted
                        )
                    )
                })
            val notificationRuntimePermission = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { permissionGranted ->
                    coroutineScope.pushUIEvent(
                        AndroidUIEvent.Type.NotificationPermissionState(
                            isGranted = permissionGranted
                        )
                    )
                })

            var showNotificationPermissionDialog by rememberSaveable {
                mutableStateOf(false)
            }

            val activityResultLauncherForFileImport =
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                    coroutineScope.pushUIEvent(
                        AndroidUIEvent.Type.UriOfTheFileForImporting(
                            uri
                        )
                    )
                }
            val localContext = LocalContext.current
            val activityResultLauncherForPickingADirectory =
                rememberLauncherForActivityResult(contract = OpenDocumentTreeWithPermissionsContract()) { uri: Uri? ->
                    uri?.let {
                        val flags =
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                        val isUriPermissionPersisted =
                            contentResolver.persistedUriPermissions.any { uriPermission -> uriPermission.uri == uri }
                        if (isUriPermissionPersisted) {
                            localContext.contentResolver.releasePersistableUriPermission(uri, flags)
                        }
                        localContext.contentResolver.takePersistableUriPermission(uri, flags)
                    }
                    coroutineScope.pushUIEvent(
                        AndroidUIEvent.Type.PickedDirectory(
                            uri
                        )
                    )
                }
            viewModel<MainVM>(factory = viewModelFactory {
                initializer {
                    MainVM(launchAction = {
                        when (it) {
                            Action.LaunchDirectoryPicker -> {
                                activityResultLauncherForPickingADirectory.launch(null)
                            }

                            is Action.LaunchFileImport -> {
                                activityResultLauncherForFileImport.launch(it.fileType)
                            }

                            Action.LaunchWriteExternalStoragePermission -> {
                                storageRuntimePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }

                            Action.Minimize -> moveTaskToBack(true)
                            Action.ShowNotificationPermissionDialog -> {
                                showNotificationPermissionDialog = true
                            }
                        }
                    })
                }
            })
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalPlatform provides Platform.Android
            ) {
                val context = LocalContext.current
                val darkColors = DarkColors.copy(
                    background = if (AppPreferences.useAmoledTheme.value) Color(0xFF000000) else DarkColors.background,
                    surface = if (AppPreferences.useAmoledTheme.value) Color(0xFF000000) else DarkColors.surface
                )
                val colors = when {
                    AppPreferences.useDynamicTheming.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        if (AppPreferences.useSystemTheme.value) {
                            if (isSystemInDarkTheme()) dynamicDarkColorScheme(context).copy(
                                background = if (AppPreferences.useAmoledTheme.value) Color(
                                    0xFF000000
                                ) else dynamicDarkColorScheme(context).background,
                                surface = if (AppPreferences.useAmoledTheme.value) Color(
                                    0xFF000000
                                ) else dynamicDarkColorScheme(
                                    context
                                ).surface
                            ) else dynamicLightColorScheme(
                                context
                            )
                        } else {
                            if (AppPreferences.useDarkTheme.value) dynamicDarkColorScheme(
                                context
                            ).copy(
                                background = if (AppPreferences.useAmoledTheme.value) Color(
                                    0xFF000000
                                ) else dynamicDarkColorScheme(context).background,
                                surface = if (AppPreferences.useAmoledTheme.value) Color(
                                    0xFF000000
                                ) else dynamicDarkColorScheme(
                                    context
                                ).surface
                            ) else dynamicLightColorScheme(context)
                        }
                    }

                    else -> if (AppPreferences.useSystemTheme.value) {
                        if (isSystemInDarkTheme()) darkColors else LightColors
                    } else {
                        if (AppPreferences.useDarkTheme.value) darkColors else LightColors
                    }
                }
                LinkoraTheme(
                    colorScheme = colors
                ) {
                    Surface {
                        App()
                    }
                    NotificationPermissionDialogBox(
                        isVisible = showNotificationPermissionDialog,
                        hideDialog = {
                            showNotificationPermissionDialog = false
                        },
                        launchRuntimePermission = {
                            notificationRuntimePermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        })
                }
            }
        }
        wasLaunched = true
    }
}

class OpenDocumentTreeWithPermissionsContract() : ActivityResultContracts.OpenDocumentTree() {
    override fun createIntent(context: Context, input: Uri?): Intent {
        return super.createIntent(context, input).apply {
            listOf(
                Intent.FLAG_GRANT_PREFIX_URI_PERMISSION,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            ).forEach {
                addFlags(it)
            }
        }
    }
}