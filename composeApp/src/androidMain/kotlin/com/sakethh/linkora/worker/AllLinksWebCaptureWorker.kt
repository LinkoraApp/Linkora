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
import com.sakethh.linkora.KaptureOptions
import com.sakethh.linkora.R
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.model.CaptureTrack
import com.sakethh.linkora.service.WebCaptureNotificationService
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.ui.screens.settings.section.data.OnGoingWebCaptureState
import com.sakethh.linkora.utils.createPOSIXOwnedFile
import com.sakethh.linkora.utils.getOrCreateFolderUuid
import com.sakethh.linkora.utils.isAllowedByWebCapturePolicies
import com.sakethh.linkora.utils.prepareWebCaptureFolder
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
            cleanup()
            return@coroutineScope Result.success()
        }

        val preferences = DependencyContainer.preferencesRepo.getPreferences()

        try {
            LinkoraSDK.getInstance().webCapture.init(
                options = KaptureOptions(
                    userAgent = preferences.primaryJsoupUserAgent,
                    includeCss = preferences.webCaptureSaveCss,
                    includeImages = preferences.webCaptureSaveImages,
                    includeJs = preferences.webCaptureExecuteJs,
                    includeAudio = preferences.webCaptureSaveAudio,
                    includeVideo = preferences.webCaptureSaveVideo,
                    includeFonts = preferences.webCaptureSaveFonts,
                    includeMetadata = preferences.webCaptureSaveMetadata,
                )
            )
        } catch (_: Exception) {
            return@coroutineScope Result.failure()
        }

        return@coroutineScope try {
            val webCaptureRepo = DependencyContainer.webCaptureRepo
            val allLinks = DependencyContainer.localLinksRepo.getAllLinks()
            val processedIds =
                DependencyContainer.webCaptureRepo.getAllLinksCaptureProcessedLinkIds().toSet()
            val linksToCapture = allLinks.filter { "all_${it.localId}" !in processedIds }

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
                            DependencyContainer.webCaptureRepo.insertAProcessedId(
                                CaptureTrack(
                                    capturedLinkId = CaptureTrack.getCaptureLinkId(
                                        inAllLinksWorker = true,
                                        linkId = link.localId,
                                    ),
                                ),
                            )
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

                        val captureFilePOSIXPath = createPOSIXOwnedFile(
                            context = applicationContext,
                            folderUriString = folderUri,
                            exportFileType = ExportFileType.HTML,
                            exportLocationType = ExportLocationType.WEB_CAPTURE
                        ) ?: return@flow

                        androidDesktopWebCapture.saveHTMLPage(
                            url = link.url,
                            filePath = captureFilePOSIXPath,
                        )

                        DependencyContainer.webCaptureRepo.insertAProcessedId(
                            CaptureTrack(
                                capturedLinkId = CaptureTrack.getCaptureLinkId(
                                    inAllLinksWorker = true,
                                    linkId = link.localId,
                                ),
                            ),
                        )
                        emit(link.localId)
                    }
                }.onEach {
                    if (isStopped) {
                        cleanup()
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
            cleanup()
        }
    }

    private fun cleanup() {
        DataSettingsScreenVM.onGoingWebCaptureState = OnGoingWebCaptureState(
            isInProgress = false,
            currentIteration = 0,
            total = 0,
        )
        webCaptureNotificationService.clearNotifications()
    }
}
