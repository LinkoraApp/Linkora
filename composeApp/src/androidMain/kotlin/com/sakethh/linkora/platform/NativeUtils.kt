package com.sakethh.linkora.platform

import AndroidDesktopWebCapture
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sakethh.linkora.KaptureOptions
import com.sakethh.linkora.Localization
import com.sakethh.linkora.R
import com.sakethh.linkora.WebCaptureDatabase
import com.sakethh.linkora.data.local.WebCaptureDatabaseManager
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.local.RefreshLinksRepo
import com.sakethh.linkora.domain.repository.local.WebCaptureRepo
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.utils.getDefaultFolder
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.getPOSIXPathFromSafUri
import com.sakethh.linkora.utils.onTV
import com.sakethh.linkora.worker.AllLinksWebCaptureWorker
import com.sakethh.linkora.worker.RefreshAllLinksWorker
import com.sakethh.linkora.worker.WebCaptureWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.util.UUID

actual class NativeUtils(
    private val context: Context,
) {
    actual fun onShare(url: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(intent, null)
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    actual suspend fun onRefreshAllLinks(
        localLinksRepo: LocalLinksRepo,
        preferencesRepository: PreferencesRepository,
        refreshLinksRepo: RefreshLinksRepo,
    ) {
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<RefreshAllLinksWorker>().setConstraints(
            Constraints(requiredNetworkType = NetworkType.CONNECTED),
        ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

        preferencesRepository.changePreferenceValue(
            preferenceKey = AppPreferences.REFRESH_ALL_LINKS_WORKER_UUID,
            newValue = request.id.toString(),
        )
        preferencesRepository.changePreferenceValue(
            preferenceKey = AppPreferences.REFRESHED_LINKS_COUNT,
            newValue = 0,
        )
        refreshLinksRepo.deleteAllIds()
        workManager.enqueueUniqueWork(
            request.id.toString(),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    actual fun isAnyRefreshingEnqueued(): Flow<Boolean?> {
        val preferences = DependencyContainer.preferencesRepo.getPreferences()
        return channelFlow {
            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(UUID.fromString(preferences.refreshLinksWorkerTag))
                .collectLatest {
                    if (it != null) {
                        send(it.state == WorkInfo.State.ENQUEUED)
                    } else {
                        send(null)
                    }
                }
        }
    }

    actual fun cancelRefreshingLinks() {
        RefreshAllLinksWorker.cancelLinksRefreshing(
            context,
            refreshLinksWorkerTag = DependencyContainer.preferencesRepo.getPreferences().refreshLinksWorkerTag,
        )
    }

    actual class DataSyncingNotificationService(
        private val context: Context,
    ) {
        private val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        actual fun showNotification() {
            val notification =
                NotificationCompat.Builder(context, "1").setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(Localization.Key.SyncingDataLabel.getLocalizedString())
                    .setProgress(
                        0,
                        0,
                        true,
                    ).setPriority(NotificationCompat.PRIORITY_LOW).setSilent(true).build()

            notificationManager.notify(1, notification)
        }

        actual fun clearNotification() {
            notificationManager.cancelAll()
        }
    }

    private val packageManager = context.packageManager
    private val packageName = context.applicationContext.packageName

    actual fun onIconChange(
        allIconCodes: List<String>,
        newIconCode: String,
        onCompletion: () -> Unit,
    ) {
        allIconCodes.forEach {
            if (it != newIconCode) {
                packageManager.setComponentEnabledSetting(
                    ComponentName(packageName, "$packageName.$it"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP,
                )
            }
        }

        val newAppIconComponent = ComponentName(packageName, "$packageName.$newIconCode")

        packageManager.setComponentEnabledSetting(
            newAppIconComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )

        onCompletion()
    }

    actual fun <T> platformRunBlocking(block: suspend () -> T): T? = runBlocking {
        block()
    }

    actual class WebCapture(
        private val context: Context,
    ) {
        private val androidDesktopWebCapture = AndroidDesktopWebCapture()

        actual suspend fun init(options: KaptureOptions) = androidDesktopWebCapture.init(options)

        actual suspend fun saveHTMLPage(
            nativeFolderPath: String,
            url: String,
        ): Result<Boolean> = withContext(Dispatchers.IO) {
            val workerUUID = UUID.randomUUID()

            val captureData =
                workDataOf(
                    WebCaptureWorker.LINK to url,
                    WebCaptureWorker.WORKER_ID to workerUUID.toString(),
                )

            val workManager = WorkManager.getInstance(context)
            val request = OneTimeWorkRequestBuilder<WebCaptureWorker>().setConstraints(
                Constraints(requiredNetworkType = NetworkType.CONNECTED),
            ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setInputData(captureData)
                .setId(workerUUID)
                .build()

            workManager.enqueueUniqueWork(
                UUID.randomUUID().toString(),
                ExistingWorkPolicy.REPLACE,
                request,
            )
            Result.Success(true)
        }

        actual suspend fun onCaptureAllWebPages(
            preferences: AppPreferences,
            localLinksRepo: LocalLinksRepo,
            webCaptureRepo: WebCaptureRepo,
            webCapture: WebCapture,
        ) {
            webCaptureRepo.deleteAllProcessedIds()

            val workManager = WorkManager.getInstance(context)
            val request = OneTimeWorkRequestBuilder<AllLinksWebCaptureWorker>().setConstraints(
                Constraints(requiredNetworkType = NetworkType.CONNECTED),
            ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

            DependencyContainer.preferencesRepo.changePreferenceValue(
                preferenceKey = AppPreferences.WEB_CAPTURE_ALL_LINKS_WORKER_UUID,
                newValue = request.id.toString(),
            )

            workManager.enqueueUniqueWork(
                AllLinksWebCaptureWorker.WORKER_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }

        actual fun cancelAllWebPagesBulkCaptures() {
            AllLinksWebCaptureWorker.cancelWork(context)
        }

        actual fun isWebCaptureWorkerEnqueued(): Flow<Boolean?> = channelFlow {
            val workerId = DependencyContainer.preferencesRepo.readPreferenceValue(
                preferenceKey = AppPreferences.WEB_CAPTURE_ALL_LINKS_WORKER_UUID,
            ) ?: DependencyContainer.preferencesRepo.getPreferences().allLinksWebCaptureWorkerTag

            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(UUID.fromString(workerId))
                .collectLatest {
                    if (it != null) {
                        send(it.state == WorkInfo.State.ENQUEUED)
                    } else {
                        send(null)
                    }
                }
        }

        actual suspend fun nuke() {
            val workManager = WorkManager.getInstance(context = context.applicationContext)
            DependencyContainer.webCaptureRepo.getAllWorkerIds().forEach { workerId ->
                workManager.cancelWorkById(UUID.fromString(workerId))
            }
            androidDesktopWebCapture.nuke()
        }

        private val captureDBFiles = listOf(
            "${WebCaptureDatabase.NAME}.db",
            "${WebCaptureDatabase.NAME}.db-wal",
            "${WebCaptureDatabase.NAME}.db-shm",
            "${WebCaptureDatabase.NAME}.db.lck",
        )

        private suspend fun checkAndFixDBPermissions(
            dbFilePath: String,
            webCaptureDatabaseManager: WebCaptureDatabaseManager,
            initPath: String,
            onAccessError: suspend () -> Unit
        ) = withContext(Dispatchers.IO) {
            try {
                RandomAccessFile(dbFilePath, "r").use { it.close() }
            } catch (e: FileNotFoundException) {
                if (e.message?.contains("EACCES") == true) {
                    onAccessError()
                }
            } finally {
                webCaptureDatabaseManager.initAndGetDatabase(initPath)
            }
        }

        private fun recreateDB(
            dbFilePath: String,
            readExistingDb: (File) -> Unit,
            deleteExistingFiles: () -> Unit
        ) {
            val tempDbFile = File(context.cacheDir, "temp_db_restore.db")

            readExistingDb(tempDbFile)
            deleteExistingFiles()

            val newDbFile = File(dbFilePath)
            newDbFile.parentFile?.mkdirs()

            if (!newDbFile.exists()) {
                newDbFile.createNewFile()
            }

            if (tempDbFile.exists()) {
                tempDbFile.copyTo(newDbFile, overwrite = true)
                tempDbFile.delete()
            }
        }

        private suspend fun prepareExternalDatabaseViaSAF(
            captureLocation: String,
            webCaptureDatabaseManager: WebCaptureDatabaseManager
        ) {
            val rawDirPath = getPOSIXPathFromSafUri(
                context.applicationContext,
                captureLocation.toUri()
            ).toString()
            val dbFilePath = "$rawDirPath/${WebCaptureDatabase.NAME}.db"

            checkAndFixDBPermissions(dbFilePath, webCaptureDatabaseManager, captureLocation, onAccessError = {
                val webCaptureFolder = DocumentFile.fromTreeUri(context, captureLocation.toUri())
                    ?: return@checkAndFixDBPermissions

                recreateDB(
                    dbFilePath = dbFilePath,
                    readExistingDb = { tempFile ->
                        val existingDbDoc = webCaptureFolder.findFile("${WebCaptureDatabase.NAME}.db")
                        if (existingDbDoc != null && existingDbDoc.exists()) {
                            context.contentResolver.openInputStream(existingDbDoc.uri)?.use { input ->
                                tempFile.outputStream().use { output -> input.copyTo(output) }
                            }
                        }
                    },
                    deleteExistingFiles = {
                        captureDBFiles.forEach { fileName ->
                            webCaptureFolder.findFile(fileName)?.delete()
                        }
                    }
                )
            })
        }

        private suspend fun prepareExternalDatabaseViaPOSIX(
            webCaptureFolder: File,
            webCaptureDatabaseManager: WebCaptureDatabaseManager
        ) {
            val dbFilePath = "${webCaptureFolder.absolutePath}/${WebCaptureDatabase.NAME}.db"
            val defaultPath = getDefaultFolder(ExportLocationType.WEB_CAPTURE).absolutePath

            checkAndFixDBPermissions(dbFilePath, webCaptureDatabaseManager, defaultPath, onAccessError = {
                recreateDB(
                    dbFilePath = dbFilePath,
                    readExistingDb = { tempFile ->
                        val existingDbFile = File(webCaptureFolder, "${WebCaptureDatabase.NAME}.db")
                        if (existingDbFile.exists()) {
                            existingDbFile.copyTo(tempFile, overwrite = true)
                        }
                    },
                    deleteExistingFiles = {
                        captureDBFiles.forEach { fileName ->
                            File(webCaptureFolder, fileName).delete()
                        }
                    }
                )
            })
        }

        actual suspend fun prepareExternalDatabase(
            captureLocation: String,
            webCaptureDatabaseManager: WebCaptureDatabaseManager,
        ): Unit = withContext(Dispatchers.IO) {
            val isOnTV = with(context) {
                onTV()
            }
            if (isOnTV) {
                prepareExternalDatabaseViaPOSIX(
                    getDefaultFolder(ExportLocationType.WEB_CAPTURE),
                    webCaptureDatabaseManager
                )
            } else {
                prepareExternalDatabaseViaSAF(captureLocation, webCaptureDatabaseManager)
            }
        }
    }
}
