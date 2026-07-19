package com.sakethh.linkora

import AndroidDesktopWebCapture
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.local.WebCaptureRepo
import com.sakethh.linkora.model.WebCaptureRequest
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.utils.getOrCreateFolderUuid
import getFileNameWithTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

object WebCaptureService {
    private val requestChannel = Channel<WebCaptureRequest>(Channel.UNLIMITED)
    private val activeCapturesCount = MutableStateFlow(0)
    private var processingJob: Job? = null
    private var serviceScope = CoroutineScope(Dispatchers.IO)

    val isProcessing = activeCapturesCount
        .map { it > 0 }
        .stateIn(
            scope = serviceScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false,
        )

    private val androidDesktopWebCapture = AndroidDesktopWebCapture()

    init {
        startService(
            preferencesRepository = DependencyContainer.preferencesRepo,
            webCaptureRepo = DependencyContainer.webCaptureRepo,
        )
    }

    private fun startService(
        preferencesRepository: PreferencesRepository,
        webCaptureRepo: WebCaptureRepo,
    ) {
        if (processingJob?.isActive == true) return

        processingJob = serviceScope.launch {
            requestChannel.receiveAsFlow()
                .flatMapMerge(concurrency = preferencesRepository.getPreferences().webCaptureMaxConcurrency) { request ->
                    flow {
                        activeCapturesCount.emit(activeCapturesCount.value + 1)
                        val prefs = preferencesRepository.getPreferences()
                        try {
                            val baseCaptureDir = File(request.nativeFolderPath)
                            val folderUuid = webCaptureRepo.getOrCreateFolderUuid(request.url)
                            val targetFolder =
                                baseCaptureDir.prepareWebCaptureDir(folderUuid, prefs)

                            val captureFile = File(
                                targetFolder,
                                getFileNameWithTimestamp(
                                    ExportFileType.HTML,
                                    ExportLocationType.WEB_CAPTURE,
                                ),
                            )
                            captureFile.createNewFile()

                            androidDesktopWebCapture.saveHTMLPage(
                                url = request.url,
                                userAgent = request.userAgent,
                                timeout = request.timeout,
                                allowInsecureProtocol = request.allowInsecureProtocol,
                                ignoreDocErrors = request.ignoreDocErrors,
                                useCss = request.useCss,
                                embedFonts = request.embedFonts,
                                embedImages = request.embedImages,
                                restrictJs = request.restrictJs,
                                includeAudioElements = request.includeAudioElements,
                                includeVideoElements = request.includeVideoElements,
                                includeMetadata = request.includeMetadata,
                                logStuff = request.logStuff,
                                fileDescriptor = -1,
                                filePath = captureFile.absolutePath,
                            )
                            emit(Unit)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            activeCapturesCount.emit(activeCapturesCount.value - 1)
                        }
                    }
                }.flowOn(Dispatchers.IO).collect()
        }
    }

    suspend fun queueCapture(request: WebCaptureRequest) {
        requestChannel.send(request)
    }
}
