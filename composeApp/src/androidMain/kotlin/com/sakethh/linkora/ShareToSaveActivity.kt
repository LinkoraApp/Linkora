package com.sakethh.linkora

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.rememberNavController
import com.sakethh.linkora.di.CollectionScreenVMAssistedFactory
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.LocalTagsRepo
import com.sakethh.linkora.domain.repository.local.SnapshotRepo
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.service.AutoSaveLinkService
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.components.AddANewLinkDialogBox
import com.sakethh.linkora.ui.domain.model.AddNewLinkDialogParams
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.theme.DarkColors
import com.sakethh.linkora.ui.theme.LightColors
import com.sakethh.linkora.ui.theme.LinkoraTheme
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.getLocalizedString
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ShareToSaveActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AppPreferences.autoSaveOnShareIntent.value) {

            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    applicationContext,
                    Localization.Key.AutoSaveNotificationPermission.getLocalizedString(),
                    Toast.LENGTH_SHORT
                ).show()
                finishAndRemoveTask()
            }

            linkoraLog("Redirecting the intent action to AutoSaveLinkService")
            val autoSaveServiceIntent = Intent(this, AutoSaveLinkService::class.java).putExtra(
                Intent.EXTRA_TEXT, this@ShareToSaveActivity.intent?.getStringExtra(
                    Intent.EXTRA_TEXT
                ).toString()
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(autoSaveServiceIntent)
            } else {
                startService(autoSaveServiceIntent)
            }

            linkoraLog("Redirected the intent action to AutoSaveLinkService")
            finishAndRemoveTask()
        }

        setContent {
            val localConfiguration = LocalConfiguration.current
            val navController = rememberNavController()
            val context = LocalContext.current
            val intentActivityVM = viewModel<IntentActivityVM>(factory = viewModelFactory {
                initializer {
                    IntentActivityVM(
                        localLinksRepo = DependencyContainer.localLinksRepo,
                        localFoldersRepo = DependencyContainer.localFoldersRepo,
                        localPanelsRepo = DependencyContainer.localPanelsRepo,
                        localTagsRepo = DependencyContainer.localTagsRepo,
                        snapshotRepo = DependencyContainer.snapshotRepo,
                        showToast = { message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        })
                }
            })
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalPlatform provides Platform.Android
            ) {
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
                val collectionsScreenVM: CollectionsScreenVM =
                    viewModel(factory = CollectionScreenVMAssistedFactory.createForIntentActivity())
                LinkoraTheme(
                    colorScheme = colors
                ) {
                    AddANewLinkDialogBox(
                        addNewLinkDialogParams = AddNewLinkDialogParams(
                            onDismiss = {
                                if (MainActivity.wasLaunched) {
                                    this@ShareToSaveActivity.finishAndRemoveTask()
                                    return@AddNewLinkDialogParams
                                }
                                if (AppPreferences.areSnapshotsEnabled.value) {
                                    intentActivityVM.createADataSnapshot(onCompletion = {
                                        this@ShareToSaveActivity.finishAndRemoveTask()
                                    })
                                } else {
                                    this@ShareToSaveActivity.finishAndRemoveTask()
                                }
                            },
                            currentFolder = null,
                            allTags = collectionsScreenVM.allTags,
                            selectedTags = collectionsScreenVM.selectedTags,
                            foldersSearchQuery = collectionsScreenVM.foldersSearchQuery,
                            foldersSearchQueryResult = collectionsScreenVM.foldersSearchQueryResult,
                            rootRegularFolders = collectionsScreenVM.rootRegularFolders,
                            performAction = collectionsScreenVM::performAction,
                            url = this@ShareToSaveActivity.intent?.getStringExtra(
                                Intent.EXTRA_TEXT
                            ).toString()
                        ),
                    )
                }
            }
        }
    }
}

class IntentActivityVM(
    val localLinksRepo: LocalLinksRepo,
    private val localFoldersRepo: LocalFoldersRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val localTagsRepo: LocalTagsRepo,
    private val snapshotRepo: SnapshotRepo,
    showToast: (message: String) -> Unit
) : ViewModel() {
    fun createADataSnapshot(onCompletion: () -> Unit) {
        viewModelScope.launch {
            val allLinks = async { localLinksRepo.getAllLinks() }
            val allFolders = async { localFoldersRepo.getAllFoldersAsList() }
            val allPanels = async { localPanelsRepo.getAllThePanelsAsAList() }
            val allPanelFolders = async { localPanelsRepo.getAllThePanelFoldersAsAList() }
            val allTags = async { localTagsRepo.getAllTagsAsList() }
            val allLinkTagsPairs = async { localTagsRepo.getAllLinkTagsAsList() }

            snapshotRepo.createAManualSnapshot(
                allLinks = allLinks.await(),
                allFolders = allFolders.await(),
                allPanels = allPanels.await(),
                allPanelFolders = allPanelFolders.await(),
                allTags = allTags.await(),
                allLinkTagsPairs = allLinkTagsPairs.await(),
                onCompletion = onCompletion
            )
        }
    }

    init {
        viewModelScope.launch {
            UIEvent.uiEvents.collectLatest {
                if (it is UIEvent.Type.ShowSnackbar) {
                    showToast(it.message)
                }
            }
        }
    }

}