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
import com.sakethh.linkora.utils.getAbsolutePathFromSafUri
import com.sakethh.linkora.utils.getLocalizedString
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
import java.io.FileOutputStream
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

        actual suspend fun init(): Result<Boolean> = androidDesktopWebCapture.init()

        actual suspend fun saveHTMLPage(
            nativeFolderPath: String,
            url: String,
            userAgent: String,
            timeout: Long,
            allowInsecureProtocol: Boolean,
            ignoreDocErrors: Boolean,
            useCss: Boolean,
            embedFonts: Boolean,
            embedImages: Boolean,
            restrictJs: Boolean,
            logStuff: Boolean,
            includeAudioElements: Boolean,
            includeVideoElements: Boolean,
            includeMetadata: Boolean,
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

        actual suspend fun prepareExternalDatabase(
            captureLocation: String,
            webCaptureDatabaseManager: WebCaptureDatabaseManager,
        ): Unit = withContext(Dispatchers.IO) {
            val rawDirPath = getAbsolutePathFromSafUri(
                context.applicationContext,
                captureLocation.toUri(),
            ).toString()
            val dbFilePath = "$rawDirPath/${WebCaptureDatabase.NAME}.db"
            try {
                RandomAccessFile(
                    dbFilePath,
                    "r",
                ).use { it.close() }
            } catch (e: FileNotFoundException) {
                // we get the same exception when file literally doesn't exist
                // or even if we are blocked from accessing the file

                if (e.message?.contains("EACCES") == true) {
                    val webCaptureLocation =
                        DocumentFile.fromTreeUri(context, captureLocation.toUri())
                            ?: return@withContext
                    val existingDbFile =
                        webCaptureLocation.findFile("${WebCaptureDatabase.NAME}.db")
                    val tempDbFile = File(context.cacheDir, "temp.db")

                    if (existingDbFile != null && existingDbFile.exists()) {
                        context.contentResolver.openInputStream(existingDbFile.uri)?.use { input ->
                            tempDbFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    listOf(
                        "${WebCaptureDatabase.NAME}.db",
                        "${WebCaptureDatabase.NAME}.db-wal",
                        "${WebCaptureDatabase.NAME}.db-shm",
                        "${WebCaptureDatabase.NAME}.db.lck",
                    ).forEach { fileName ->
                        webCaptureLocation.findFile(fileName)?.delete()
                    }

                    // ownership of the database is now set to app UID and not the underlying system handling it;
                    // SAF WILL NOT WORK IN OUR CASE
                    val newDbFile = File(dbFilePath)
                    newDbFile.parentFile?.mkdirs()
                    newDbFile.createNewFile()

                    if (tempDbFile.exists()) {
                        tempDbFile.inputStream().use { input ->
                            FileOutputStream(newDbFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        tempDbFile.delete()
                    }
                }
            } finally {
                webCaptureDatabaseManager.initAndGetDatabase(captureLocation)
            }
        }
    }
}
