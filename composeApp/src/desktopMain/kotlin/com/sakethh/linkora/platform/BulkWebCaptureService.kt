package com.sakethh.linkora.platform

import com.sakethh.linkora.WebCaptureMetadata
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.WebCaptureRepo
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
import java.net.URI
import java.util.UUID

object BulkWebCaptureService {
    private var captureJob: Job? = null

    fun cancel() {
        captureJob?.cancel()
        DataSettingsScreenVM.webCaptureState = WebCaptureState(
            isInProgress = false,
            currentIteration = 0,
            total = 0,
        )
    }

    fun captureAllWebPages(
        preferences: AppPreferences,
        localLinksRepo: LocalLinksRepo,
        webCaptureRepo: WebCaptureRepo,
        webCapture: NativeUtils.WebCapture,
    ) {
        captureJob = CoroutineScope(PlatformIODispatcher).launch {
            val initResult = LinkoraSDK.getInstance().webCapture.init()
            if (initResult is Result.Failure) {
                return@launch
            }

            val allLinks = localLinksRepo.getAllLinks()

            val whitelist = preferences.webCaptureWhitelistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }
            val blacklist = preferences.webCaptureBlacklistDomains.split(",").map { it.trim() }
                .filter { it.isNotBlank() }

            DataSettingsScreenVM.webCaptureState = WebCaptureState(
                isInProgress = true,
                currentIteration = 0,
                total = allLinks.size,
            )

            if (allLinks.isEmpty()) return@launch

            var processedCount = 0
            val baseCaptureDir = File(preferences.webCapturesLocation)
            if (!baseCaptureDir.exists()) baseCaptureDir.mkdirs()

            allLinks.asFlow()
                .flatMapMerge(concurrency = preferences.webCaptureMaxConcurrency) { link ->
                    flow {
                        val host = try {
                            URI(link.url).host
                        } catch (_: Exception) {
                            emit(link.localId)
                            return@flow
                        }
                        if (whitelist.isNotEmpty() && whitelist.none { host.endsWith(it) }) {
                            emit(link.localId)
                            return@flow
                        }
                        if (blacklist.any { host.endsWith(it) }) {
                            emit(link.localId)
                            return@flow
                        }

                        val folderUuid = webCaptureRepo.getFolderNameByLink(link.url) ?: run {
                            val newUuid = UUID.randomUUID().toString()
                            webCaptureRepo.insertMetadata(
                                WebCaptureMetadata(
                                    link = link.url,
                                    uuid = newUuid,
                                ),
                            )
                            newUuid
                        }

                        val linkWebCaptureFolder = File(baseCaptureDir, folderUuid)

                        if (!preferences.webCaptureSaveAsVersions) {
                            if (linkWebCaptureFolder.exists()) {
                                linkWebCaptureFolder.deleteRecursively()
                            }
                            linkWebCaptureFolder.mkdirs()
                        } else {
                            if (!linkWebCaptureFolder.exists()) linkWebCaptureFolder.mkdirs()
                            if (!preferences.webCaptureRetainAllVersions) {
                                val existingFiles =
                                    linkWebCaptureFolder.listFiles()?.filter { it.isFile }
                                        ?.sortedBy { it.lastModified() }.orEmpty()
                                if (existingFiles.size >= preferences.webCaptureMaxVersions) {
                                    existingFiles.take(existingFiles.size - preferences.webCaptureMaxVersions + 1)
                                        .forEach { it.delete() }
                                }
                            }
                        }

                        webCapture.saveHTMLPage(
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
                            nativeFolderPath = linkWebCaptureFolder.absolutePath,
                        )

                        emit(link.localId)
                    }
                }.catch { it.printStackTrace() }.collect {
                    processedCount++
                    DataSettingsScreenVM.webCaptureState =
                        DataSettingsScreenVM.webCaptureState.copy(currentIteration = processedCount)
                }
        }

        captureJob?.invokeOnCompletion {
            println("Completed bulk web capture")
            DataSettingsScreenVM.webCaptureState = WebCaptureState(
                isInProgress = false,
                currentIteration = 0,
                total = 0,
            )
            captureJob = null
        }
    }
}
