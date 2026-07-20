package com.sakethh.linkora.worker

import AndroidDesktopWebCapture
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.sakethh.linkora.R
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.LinkoraResultFailure
import com.sakethh.linkora.domain.model.CaptureTrack
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.utils.getOrCreateFolderUuid
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

        val initResult = LinkoraSDK.getInstance().webCapture.init()
        if (initResult is LinkoraResultFailure) {
            return@coroutineScope Result.failure()
        }
        val linkUrl = inputData.getString(LINK) ?: return@coroutineScope Result.failure()
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

            if (!linkUrl.isAllowedByWebCapturePolicies(whitelist, blacklist)) {
                return@coroutineScope Result.success()
            }

            val baseCaptureDir = DocumentFile.fromTreeUri(
                applicationContext,
                preferences.webCapturesLocation.toUri(),
            ) ?: return@coroutineScope Result.failure()
            val folderUuid = webCaptureRepo.getOrCreateFolderUuid(linkUrl)
            val linkWebCaptureFolder = baseCaptureDir.prepareWebCaptureFolder(
                folderUuid = folderUuid,
                saveAsVersions = preferences.webCaptureSaveAsVersions,
                retainAllVersions = preferences.webCaptureRetainAllVersions,
                maxVersions = preferences.webCaptureMaxVersions,
            )

            val folderUri =
                linkWebCaptureFolder?.uri?.toString() ?: return@coroutineScope Result.failure()

            val webCaptureFd = applicationContext.createWebCaptureFileDescriptor(folderUri)
                ?: return@coroutineScope Result.failure()

            var isSuccess = false

            withContext(Dispatchers.IO) {
                androidDesktopWebCapture.saveHTMLPage(
                    url = linkUrl,
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
            }.onSuccess { (captureSuccess) ->
                isSuccess = captureSuccess
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
