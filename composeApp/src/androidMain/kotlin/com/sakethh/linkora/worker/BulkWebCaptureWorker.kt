package com.sakethh.linkora.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sakethh.linkora.R
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.WebCaptureState
import kotlinx.coroutines.coroutineScope
import java.util.UUID

class BulkWebCaptureWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    init {
        TODO("Not yet implemented completely")
    }

    companion object {
        fun cancelWork(
            appContext: Context,
            workerTag: String,
        ) {
            WorkManager.getInstance(appContext).cancelWorkById(UUID.fromString(workerTag))
            DataSettingsScreenVM.webCaptureState =
                WebCaptureState(
                    isInProgress = false,
                    currentIteration = 0,
                    total = 0,
                )
        }
    }

    // var notificationService  = TODO()

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        1,
        NotificationCompat.Builder(applicationContext, "1")
            .setSmallIcon(R.drawable.ic_stat_name)
            .build(),
    )

    override suspend fun doWork(): Result = coroutineScope {
        Result.success()
    /* var processedCount = 0
    //   notificationService.clearNotifications()

       if (isStopped) {
           cleanUp()
           return@coroutineScope Result.success()
       }

       val initResult = LinkoraSDK.getInstance().webCapture.init()
       if (initResult is com.sakethh.linkora.domain.Result.Failure) {
           return@coroutineScope Result.failure()
       }

       return@coroutineScope try {
           val preferences = DependencyContainer.preferencesRepo.getPreferences()
           val localLinksRepo = DependencyContainer.localLinksRepo
          // val metaDataDao = LinkoraSDK.getInstance().webCaptureDatabase.metaDataDao

           val linksToCapture = localLinksRepo.getAllLinks()

           DataSettingsScreenVM.webCaptureState = WebCaptureState(
               isInProgress = true,
               currentIteration = 0,
               total = linksToCapture.size
           )

           if (linksToCapture.isEmpty()) return@coroutineScope Result.success()

          val baseCaptureDir = File(preferences.webCapturesLocation)
           if (!baseCaptureDir.exists()) baseCaptureDir.mkdirs()

           linksToCapture.asFlow()
               .flatMapMerge(concurrency = 15) { link ->
                   flow {
                       val folderUuid = metaDataDao.getFolderNameByLink(link.url) ?: run {
                           val newUuid = UUID.randomUUID().toString()
                           metaDataDao.insert(
                               MetaDataEntity(
                                   link = link.url,
                                   uuid = newUuid
                               )
                           )
                           newUuid
                       }

                       val linkWebCaptureFolder = File(baseCaptureDir, folderUuid)
                       if (!linkWebCaptureFolder.exists()) linkWebCaptureFolder.mkdirs()

                       LinkoraSDK.getInstance().webCapture.saveHTMLPage(
                           url = link.url,
                           userAgent = preferences.primaryJsoupUserAgent,
                           timeout = 15000L,
                           allowInsecureProtocol = false,
                           ignoreDocErrors = true,
                           useCss = preferences.webCaptureSaveCss,
                           embedFonts = preferences.webCaptureSaveFonts,
                           embedImages = preferences.webCaptureSaveImages,
                           restrictJs = preferences.webCaptureExecuteJs,
                           includeAudioElements = preferences.webCaptureSaveAudio,
                           includeVideoElements = preferences.webCaptureSaveVideo,
                           includeMetadata = preferences.webCaptureSaveMetadata,
                           logStuff = false,
                           nativeFolderPath = linkWebCaptureFolder.absolutePath
                       )

                       emit(link.localId)
                   }
               }
               .onEach {
                   if (isStopped) {
                       cleanUp()
                       cancel()
                   }
               }
               .catch { it.printStackTrace() }
               .collect {
                   processedCount++
                   DataSettingsScreenVM.webCaptureState =
                       DataSettingsScreenVM.webCaptureState.copy(currentIteration = processedCount)
                   notificationService.showNotification()
               }

           Result.success()
       } catch (e: Exception) {
           e.printStackTrace()
           Result.failure()
       } finally {
           cleanUp()
       }*/
    }

    private fun cleanUp() {
        DataSettingsScreenVM.webCaptureState =
            WebCaptureState(
                isInProgress = false,
                currentIteration = 0,
                total = 0,
            )
        //  notificationService.clearNotifications()
    }
}
