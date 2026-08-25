package com.sakethh.linkora.platform

import AndroidDesktopWebCapture
import com.sakethh.linkora.KaptureOptions
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.WebCaptureRepo
import com.sakethh.linkora.prepareWebCaptureDir
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.ui.screens.settings.section.data.OnGoingWebCaptureState
import com.sakethh.linkora.utils.getOrCreateFolderUuid
import getFileNameWithTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI

object AllLinksWebCaptureService {
    private var captureJob: Job? = null
    val androidDesktopWebCapture = AndroidDesktopWebCapture()

    fun cancel() {
        captureJob?.cancel()
        DataSettingsScreenVM.onGoingWebCaptureState = OnGoingWebCaptureState(
            isInProgress = false,
            currentIteration = 0,
            total = 0,
        )
    }

    fun captureAllWebPages(
        preferences: AppPreferences,
        localLinksRepo: LocalLinksRepo,
        webCaptureRepo: WebCaptureRepo,
    ) {
        captureJob = CoroutineScope(PlatformIODispatcher).launch {
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
                return@launch
            }

            val allLinks = localLinksRepo.getAllLinks()
            if (allLinks.isEmpty()) return@launch

            val whitelist = preferences.webCaptureWhitelistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }
            val blacklist = preferences.webCaptureBlacklistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }

            DataSettingsScreenVM.onGoingWebCaptureState = OnGoingWebCaptureState(
                isInProgress = true,
                currentIteration = 0,
                total = allLinks.size,
            )

            var processedCount = 0
            val baseCaptureDir = File(preferences.webCapturesLocation)

            allLinks.asFlow()
                .flatMapMerge(concurrency = preferences.webCaptureMaxConcurrency) { link ->
                    flow {
                        val host = try {
                            URI(link.url).host
                        } catch (_: Exception) {
                            null
                        }

                        if (host != null) {
                            if (whitelist.isNotEmpty() && whitelist.none { host.endsWith(it) }) {
                                emit(link.localId)
                                return@flow
                            }
                            if (blacklist.any { host.endsWith(it) }) {
                                emit(link.localId)
                                return@flow
                            }
                        }

                        val folderUuid = webCaptureRepo.getOrCreateFolderUuid(link.url)
                        val targetFolder =
                            baseCaptureDir.prepareWebCaptureDir(folderUuid, preferences)
                        val captureFile = File(
                            targetFolder,
                            getFileNameWithTimestamp(
                                ExportFileType.HTML,
                                ExportLocationType.WEB_CAPTURE,
                            ),
                        )
                        withContext(Dispatchers.IO) {
                            captureFile.createNewFile()
                            androidDesktopWebCapture.saveHTMLPage(
                                url = link.url,
                                filePath = captureFile.absolutePath,
                            )
                        }

                        emit(link.localId)
                    }
                }
                .catch { it.printStackTrace() }
                .collect {
                    processedCount++
                    DataSettingsScreenVM.onGoingWebCaptureState =
                        DataSettingsScreenVM.onGoingWebCaptureState.copy(
                            currentIteration = processedCount,
                        )
                }
        }

        captureJob?.invokeOnCompletion {
            DataSettingsScreenVM.onGoingWebCaptureState = OnGoingWebCaptureState(
                isInProgress = false,
                currentIteration = 0,
                total = 0,
            )
            captureJob = null
        }
    }
}
