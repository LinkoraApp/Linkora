package com.sakethh.linkora.platform

import com.sakethh.linkora.MetaDataDao
import com.sakethh.linkora.MetaDataEntity
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.WebCaptureState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

object BulkWebCaptureService {

    private var captureJob: Job? = null

    fun cancel() {
        captureJob?.cancel()
        DataSettingsScreenVM.webCaptureState = WebCaptureState(
            isInProgress = false, currentIteration = 0, total = 0
        )
    }

    fun captureAllWebPages(
        preferences: AppPreferences,
        localLinksRepo: LocalLinksRepo,
        metaDataDao: MetaDataDao
    ) {
        captureJob = CoroutineScope(PlatformIODispatcher).launch {
            val initResult = LinkoraSDK.getInstance().webCapture.init()
            if (initResult is Result.Failure) {
                return@launch
            }

            val linksToCapture = localLinksRepo.getAllLinks()

            DataSettingsScreenVM.webCaptureState = WebCaptureState(
                isInProgress = true, currentIteration = 0, total = linksToCapture.size
            )

            if (linksToCapture.isEmpty()) return@launch

            var processedCount = 0
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
                            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36 Edg/148.0.0.0",
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
                .catch { it.printStackTrace() }
                .collect {
                    processedCount++
                    DataSettingsScreenVM.webCaptureState =
                        DataSettingsScreenVM.webCaptureState.copy(currentIteration = processedCount)
                }
        }

        captureJob?.invokeOnCompletion {
            println("Completed bulk web capture")
            DataSettingsScreenVM.webCaptureState = WebCaptureState(
                isInProgress = false, currentIteration = 0, total = 0
            )
        }
    }
}