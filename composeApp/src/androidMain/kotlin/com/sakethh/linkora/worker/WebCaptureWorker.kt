package com.sakethh.linkora.worker

import AndroidDesktopWebCapture
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.sakethh.linkora.KaptureOptions
import com.sakethh.linkora.R
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.model.CaptureTrack
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.utils.createPOSIXOwnedFile
import com.sakethh.linkora.utils.getDefaultFolder
import com.sakethh.linkora.utils.getOrCreateFolderUuid
import com.sakethh.linkora.utils.isAllowedByWebCapturePolicies
import com.sakethh.linkora.utils.onTV
import com.sakethh.linkora.utils.prepareWebCaptureFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.UUID

class WebCaptureWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    private val androidDesktopWebCapture = AndroidDesktopWebCapture()

    companion object {
        const val LINK = "LINK"
        const val WORKER_ID = "WORKER_ID"
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        1,
        NotificationCompat.Builder(applicationContext, "1").setSmallIcon(R.drawable.ic_stat_name)
            .build(),
    )

    override suspend fun doWork(): Result = coroutineScope {
        val preferences = DependencyContainer.preferencesRepo.getPreferences()

        if (isStopped) {
            return@coroutineScope Result.success()
        }

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
        } catch (e: Exception) {
            e.printStackTrace()
            return@coroutineScope Result.failure()
        }

        val url = inputData.getString(LINK) ?: return@coroutineScope Result.failure()
        val captureWorkerId =
            inputData.getString(WORKER_ID) ?: return@coroutineScope Result.failure()

        val webCaptureRepo = DependencyContainer.webCaptureRepo

        DependencyContainer.webCaptureRepo.insertAProcessedId(
            CaptureTrack(
                capturedLinkId = CaptureTrack.getCaptureLinkId(
                    inAllLinksWorker = false,
                    linkId = null,
                    // we don't care about the link, we only care about the captureWorkerId on Android;
                    // this should generally be in its own table, so the data won't be scattered around,
                    // but for our case, this should be fine
                    link = UUID.randomUUID().toString(),
                ),
                captureWorkerId = captureWorkerId,
            ),
        )

        return@coroutineScope try {
            val whitelist = preferences.webCaptureWhitelistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }
            val blacklist = preferences.webCaptureBlacklistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }

            if (!url.isAllowedByWebCapturePolicies(whitelist, blacklist)) {
                return@coroutineScope Result.success()
            }

            val folderUuid = webCaptureRepo.getOrCreateFolderUuid(url)

            val isOnTV = with(applicationContext) {
                onTV()
            }

            val captureFilePOSIXPath = if (isOnTV) {
                val linkWebCaptureFolder =
                    getDefaultFolder(ExportLocationType.WEB_CAPTURE).prepareWebCaptureFolder(
                        folderUuid,
                        saveAsVersions = preferences.webCaptureSaveAsVersions,
                        retainAllVersions = preferences.webCaptureRetainAllVersions,
                        maxVersions = preferences.webCaptureMaxVersions,
                    )
                createPOSIXOwnedFile(
                    folder = linkWebCaptureFolder,
                    exportFileType = ExportFileType.HTML,
                    exportLocationType = ExportLocationType.WEB_CAPTURE
                ) ?: return@coroutineScope Result.failure()
            } else {
                val baseCaptureDir = DocumentFile.fromTreeUri(
                    applicationContext,
                    preferences.webCapturesLocation.toUri(),
                ) ?: return@coroutineScope Result.failure()

                val linkWebCaptureFolder = baseCaptureDir.prepareWebCaptureFolder(
                    folderUuid = folderUuid,
                    saveAsVersions = preferences.webCaptureSaveAsVersions,
                    retainAllVersions = preferences.webCaptureRetainAllVersions,
                    maxVersions = preferences.webCaptureMaxVersions,
                )

                val folderUri =
                    linkWebCaptureFolder?.uri?.toString() ?: return@coroutineScope Result.failure()
                createPOSIXOwnedFile(
                    context = applicationContext,
                    folderUriString = folderUri,
                    exportFileType = ExportFileType.HTML,
                    exportLocationType = ExportLocationType.WEB_CAPTURE
                ) ?: return@coroutineScope Result.failure()
            }

            var isSuccess = false

            try {
                withContext(Dispatchers.IO) {
                    androidDesktopWebCapture.saveHTMLPage(
                        url = url,
                        filePath = captureFilePOSIXPath,
                    )
                }
                isSuccess = true
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (isSuccess) Result.success() else Result.failure()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        } finally {
            if (isActive) {
                DependencyContainer.webCaptureRepo.deleteByWorkerId(
                    id = captureWorkerId,
                )
            }
        }
    }
}
