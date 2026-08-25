package com.sakethh.linkora

import io.github.sakethpathike.kapture.Kapture

object WebCapture {
    private var isLibraryLoaded = false

    /**
     * Kapture uses Mutex for initializing the options and will only be initialized once
     * */
    suspend fun init(options: KaptureOptions) {
        if (isLibraryLoaded) return
        Kapture.init(options.toKaptureLibOptions())
        isLibraryLoaded = true
    }

    suspend fun saveHTMLPage(
        filePath: String,
        url: String,
    ) {
        Kapture.archive(url, destinationFilePath = filePath)
    }
}

// maybe i should reintroduce options in shared module (in kapture), so this redundancy can be avoided
private fun KaptureOptions.toKaptureLibOptions(): io.github.sakethpathike.kapture.KaptureOptions = io.github.sakethpathike.kapture.KaptureOptions(
        includeJs = includeJs,
        includeCss = includeCss,
        includeImages = includeImages,
        includeVideo = includeVideo,
        includeAudio = includeAudio,
        includeFonts = includeFonts,
        includeMetadata = includeMetadata,
        timeoutMillis = timeoutMillis,
        userAgent = userAgent,
        base64StreamSize = base64StreamSize
    )

data class KaptureOptions(
    val includeJs: Boolean = true,
    val includeCss: Boolean = true,
    val includeImages: Boolean = true,
    val includeVideo: Boolean = true,
    val includeAudio: Boolean = true,
    val includeFonts: Boolean = true,
    val includeMetadata: Boolean = true,
    val timeoutMillis: Long = 30000L,
    val userAgent: String = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    val base64StreamSize: Int = 3000
)
