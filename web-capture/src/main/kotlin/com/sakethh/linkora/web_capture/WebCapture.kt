package com.sakethh.linkora.web_capture

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object WebCapture {

    private var isLibraryLoaded = false
    private val initMutex = Mutex()

    suspend fun init(): Result<Boolean> {
        if (isLibraryLoaded) return Result.success(true)

        return withContext(Dispatchers.IO) {
            initMutex.withLock {
                if (isLibraryLoaded) {
                    Result.success(true)
                } else {
                    try {
                        System.loadLibrary("web_capture")
                        isLibraryLoaded = true
                        Result.success(true)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }
        }
    }

    external fun saveHTMLPage(
        fileDescriptor: Int,
        filePath: String,
        url: String,
        userAgent: String,
        timeout: Long,
        allowInsecureProtocol: Boolean,
        ignoreDocErrors: Boolean,
        useCss: Boolean,
        embedFonts: Boolean,
        embedImages: Boolean,
        restrictJs: Boolean,
        includeAudioElements: Boolean,
        includeVideoElements: Boolean,
        includeMetadata: Boolean,
        logStuff: Boolean
    ): Boolean
}