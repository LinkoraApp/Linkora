package com.sakethh.linkora.worker

import AndroidDesktopWebCapture
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sakethh.linkora.R
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.LinkoraResultFailure
import com.sakethh.linkora.domain.model.CaptureTrack
import com.sakethh.linkora.service.WebCaptureNotificationService
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.OnGoingWebCaptureState
import com.sakethh.linkora.utils.getOrCreateFolderUuid
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import java.util.UUID

class AllLinksWebCaptureWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    companion object {
        const val WORKER_NAME = "bulk-web-capture"

        fun cancelWork(appContext: Context) {
            WorkManager.getInstance(appContext)
                .cancelWorkById(UUID.fromString(DependencyContainer.preferencesRepo.getPreferences().allLinksWebCaptureWorkerTag))
            DataSettingsScreenVM.onGoingWebCaptureState = OnGoingWebCaptureState(
                isInProgress = false,
                currentIteration = 0,
                total = 0,
            )
        }
    }

    private var webCaptureNotificationService = WebCaptureNotificationService(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        1,
        NotificationCompat.Builder(applicationContext, "1")
            .setSmallIcon(R.drawable.ic_stat_name).build(),
    )

    private val androidDesktopWebCapture = AndroidDesktopWebCapture()

    override suspend fun doWork(): Result = coroutineScope {
        webCaptureNotificationService.clearNotifications()

        if (isStopped) {
            cleanUp()
            return@coroutineScope Result.success()
        }

        val initResult = LinkoraSDK.getInstance().webCapture.init()
        if (initResult is LinkoraResultFailure) {
            return@coroutineScope Result.failure()
        }
        return@coroutineScope try {
            val preferences = DependencyContainer.preferencesRepo.getPreferences()
            val webCaptureRepo = DependencyContainer.webCaptureRepo
            val allLinks = DependencyContainer.localLinksRepo.getAllLinks()
            val processedIds = DependencyContainer.webCaptureRepo.getProcessedLinkIds().toSet()
            val linksToCapture = allLinks.filter { it.localId !in processedIds }

            val whitelist = preferences.webCaptureWhitelistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }
            val blacklist = preferences.webCaptureBlacklistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }

            DataSettingsScreenVM.onGoingWebCaptureState = OnGoingWebCaptureState(
                isInProgress = true,
                currentIteration = 0,
                total = linksToCapture.size,
            )

            if (linksToCapture.isEmpty()) return@coroutineScope Result.success()

            val baseCaptureDir = DocumentFile.fromTreeUri(
                applicationContext,
                preferences.webCapturesLocation.toUri(),
            ) ?: return@coroutineScope Result.failure()

            var processedCount = 0

            linksToCapture.asFlow()
                .flatMapMerge(concurrency = preferences.webCaptureMaxConcurrency) { link ->
                    flow {
                        if (!link.url.isAllowedByWebCapturePolicies(whitelist, blacklist)) {
                            DependencyContainer.webCaptureRepo.insertAProcessedId(CaptureTrack(link.localId))
                            emit(link.localId)
                            return@flow
                        }

                        val folderUuid = webCaptureRepo.getOrCreateFolderUuid(link.url)

                        val linkWebCaptureFolder = baseCaptureDir.prepareWebCaptureFolder(
                            folderUuid = folderUuid,
                            saveAsVersions = preferences.webCaptureSaveAsVersions,
                            retainAllVersions = preferences.webCaptureRetainAllVersions,
                            maxVersions = preferences.webCaptureMaxVersions,
                        )

                        val folderUri = linkWebCaptureFolder?.uri?.toString() ?: return@flow
                        val webCaptureFd =
                            applicationContext.createWebCaptureFileDescriptor(folderUri)

                        if (webCaptureFd != null) {
                            androidDesktopWebCapture.saveHTMLPage(
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
                                fileDescriptor = webCaptureFd,
                                filePath = "",
                            )
                        }

                        DependencyContainer.webCaptureRepo.insertAProcessedId(CaptureTrack(link.localId))
                        emit(link.localId)
                    }
                }.onEach {
                    if (isStopped) {
                        cleanUp()
                        cancel()
                    }
                }.catch { it.printStackTrace() }.collect {
                    processedCount++
                    DataSettingsScreenVM.onGoingWebCaptureState =
                        DataSettingsScreenVM.onGoingWebCaptureState.copy(currentIteration = processedCount)
                    webCaptureNotificationService.showNotification()
                }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        } finally {
            cleanUp()
        }
    }

    private fun cleanUp() {
        DataSettingsScreenVM.onGoingWebCaptureState = OnGoingWebCaptureState(
            isInProgress = false,
            currentIteration = 0,
            total = 0,
        )
        webCaptureNotificationService.clearNotifications()
    }
}
