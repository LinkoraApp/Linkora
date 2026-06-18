package com.sakethh.linkora.hoarder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object MonolithHoarder {

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
                        System.loadLibrary("hoarder")
                        isLibraryLoaded = true
                        Result.success(true)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }
        }
    }

    external fun getHTMLPage(
        url: String,
        userAgent: String,
        timeout: Long,
        allowInsecureProtocol: Boolean,
        ignoreDocErrors: Boolean,
        useCss: Boolean,
        embedFonts: Boolean,
        embedImages: Boolean,
        restrictJs: Boolean,
        logStuff: Boolean
    ): ByteArray
}