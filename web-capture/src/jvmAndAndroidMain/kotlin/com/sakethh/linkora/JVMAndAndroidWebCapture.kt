package com.sakethh.linkora


import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object JVMAndAndroidWebCapture {
    
    fun interface OnThrown {
        fun onThrown(message: String)
    }

    private var isLibraryLoaded = false
    private val libMutex = Mutex()
    private val pendingCaptures = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()

    suspend fun init(): Result<Boolean> {
        if (isLibraryLoaded) return Result.success(true)

        return withContext(Dispatchers.IO) {
            libMutex.withLock {
                if (isLibraryLoaded) {
                    Result.success(true)
                } else {
                    try {
                        System.loadLibrary("web_capture")
                        var spawnFailure: String? = null
                        spawnResultDaemon(
                            onThrown = { msg ->
                                spawnFailure = msg
                            }
                        )
                        if (spawnFailure != null) {
                            throw IllegalStateException(spawnFailure)
                        } else {
                            isLibraryLoaded = true
                            Result.success(true)
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }
        }
    }

    suspend fun nuke(): Result<Boolean> {
        if (!isLibraryLoaded) return Result.success(true)

        return withContext(Dispatchers.IO) {
            libMutex.withLock {
                if (!isLibraryLoaded) {
                    Result.success(true)
                } else {
                    try {
                        val capturesToCancel = pendingCaptures.values.toList()
                        pendingCaptures.clear()
                        capturesToCancel.forEach { continuation ->
                            continuation.cancel()
                        }
                        var killFailure: String? = null
                        killResultDaemon(
                            onThrown = OnThrown { msg -> killFailure = msg }
                        )
                        val failure = killFailure
                        if (failure != null) {
                            Result.failure(IllegalStateException(failure))
                        } else {
                            isLibraryLoaded = false
                            Result.success(true)
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }
        }
    }

    /**
     * Rust will call this on capture completion (only after [spawnResultDaemon]'s initialization).
     * Purely reports written-or-not; doesn't catch panics or anything else,
     * that's onThrown's job at the saveHTMLPage call site.
     * */
    private fun onCaptureResult(opKey: String, success: Boolean) {
        val continuation = pendingCaptures.remove(opKey)
        if (continuation?.isActive == true) {
            continuation.resume(success)
            println("for $opKey, from rust: $success")
        }
    }

    suspend fun saveHTMLPage(
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
        logStuff: Boolean,
        onMonolithCancellationFailure: () -> Unit = {}
    ): Boolean = suspendCancellableCoroutine { continuation ->
        if (!isLibraryLoaded) {
            throw IllegalStateException("Native library is not loaded or was nuked")
        }

        @OptIn(ExperimentalUuidApi::class) val opKey = Uuid.random().toHexString()
        pendingCaptures[opKey] = continuation

        continuation.invokeOnCancellation {
            cancelWebCapture(
                key = opKey,
                onThrown = { onMonolithCancellationFailure() }
            )
            pendingCaptures.remove(opKey)
        }

        saveHTMLPage(
            fileDescriptor = fileDescriptor,
            filePath = filePath,
            url = url,
            opKey = opKey,
            userAgent = userAgent,
            timeout = timeout,
            allowInsecureProtocol = allowInsecureProtocol,
            ignoreDocErrors = ignoreDocErrors,
            useCss = useCss,
            embedFonts = embedFonts,
            embedImages = embedImages,
            restrictJs = restrictJs,
            includeAudioElements = includeAudioElements,
            includeVideoElements = includeVideoElements,
            includeMetadata = includeMetadata,
            logStuff = logStuff,
            onThrown = { message ->
                val cont = pendingCaptures.remove(opKey)
                if (cont?.isActive == true) {
                    cont.resumeWithException(IllegalStateException(message))
                }
            }
        )
    }


    private external fun saveHTMLPage(
        fileDescriptor: Int,
        filePath: String,
        url: String,
        opKey: String,
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
        logStuff: Boolean,
        onThrown: OnThrown,
    )

    private external fun spawnResultDaemon(
        onThrown: OnThrown
    )

    private external fun killResultDaemon(onThrown: OnThrown)

    private external fun cancelWebCapture(key: String, onThrown: OnThrown)
}