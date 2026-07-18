package com.sakethh.linkora.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sakethh.linkora.R
import com.sakethh.linkora.WebCaptureMetadata
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.LinkoraResultFailure
import com.sakethh.linkora.domain.model.CaptureTrack
import com.sakethh.linkora.service.WebCaptureNotificationService
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.WebCaptureState
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import java.net.URI
import java.util.UUID

class BulkWebCaptureWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    companion object {
        const val WORKER_NAME = "bulk-web-capture"

        fun cancelWork(appContext: Context) {
            WorkManager.getInstance(appContext).cancelUniqueWork(WORKER_NAME)
            DataSettingsScreenVM.webCaptureState = WebCaptureState(
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
            val metaDataDao =
                LinkoraSDK.getInstance().webCaptureDatabaseManager.getDatabase(preferences.webCapturesLocation).webCaptureMetadataDao

            val allLinks = DependencyContainer.localLinksRepo.getAllLinks()
            val processedIds = DependencyContainer.webCaptureRepo.getProcessedLinkIds().toSet()
            val linksToCapture = allLinks.filter { it.localId !in processedIds }

            val whitelist = preferences.webCaptureWhitelistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }
            val blacklist = preferences.webCaptureBlacklistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }

            DataSettingsScreenVM.webCaptureState = WebCaptureState(
                isInProgress = true,
                currentIteration = 0,
                total = linksToCapture.size,
            )

            if (linksToCapture.isEmpty()) return@coroutineScope Result.success()
            val baseCaptureDir =
                DocumentFile.fromTreeUri(
                    applicationContext,
                    preferences.webCapturesLocation.toUri(),
                )
                    ?: return@coroutineScope Result.failure()

            var processedCount = 0

            linksToCapture.asFlow()
                .flatMapMerge(concurrency = preferences.webCaptureMaxConcurrency) { link ->
                    flow {
                        val host = try {
                            URI(link.url).host
                        } catch (_: Exception) {
                            ""
                        }
                        if (whitelist.isNotEmpty() && whitelist.none { host.endsWith(it) }) {
                            DependencyContainer.webCaptureRepo.insertAProcessedId(CaptureTrack(link.localId))
                            emit(link.localId)
                            return@flow
                        }
                        if (blacklist.any { host.endsWith(it) }) {
                            DependencyContainer.webCaptureRepo.insertAProcessedId(CaptureTrack(link.localId))
                            emit(link.localId)
                            return@flow
                        }

                        val folderUuid = metaDataDao.getFolderNameByLink(link.url) ?: run {
                            val newUuid = UUID.randomUUID().toString()
                            metaDataDao.insert(
                                WebCaptureMetadata(
                                    link = link.url,
                                    uuid = newUuid,
                                ),
                            )
                            newUuid
                        }

                        var linkWebCaptureFolder = baseCaptureDir.findFile(folderUuid)

                        if (!preferences.webCaptureSaveAsVersions) {
                            if (linkWebCaptureFolder != null && linkWebCaptureFolder.exists()) {
                                linkWebCaptureFolder.delete()
                            }
                            linkWebCaptureFolder = baseCaptureDir.createDirectory(folderUuid)
                        } else {
                            if (linkWebCaptureFolder == null || !linkWebCaptureFolder.exists()) {
                                linkWebCaptureFolder = baseCaptureDir.createDirectory(folderUuid)
                            }
                            if (!preferences.webCaptureRetainAllVersions) {
                                val existingFiles = linkWebCaptureFolder?.listFiles()
                                    ?.filter { it.isFile }
                                    ?.sortedBy { it.lastModified() }
                                    .orEmpty()
                                if (existingFiles.size >= preferences.webCaptureMaxVersions) {
                                    existingFiles.take(existingFiles.size - preferences.webCaptureMaxVersions + 1)
                                        .forEach { it.delete() }
                                }
                            }
                        }

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
                            nativeFolderPath = linkWebCaptureFolder?.uri.toString(),
                        )

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
                    DataSettingsScreenVM.webCaptureState =
                        DataSettingsScreenVM.webCaptureState.copy(currentIteration = processedCount)
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
        DataSettingsScreenVM.webCaptureState = WebCaptureState(
            isInProgress = false,
            currentIteration = 0,
            total = 0,
        )
        webCaptureNotificationService.clearNotifications()
    }
}
