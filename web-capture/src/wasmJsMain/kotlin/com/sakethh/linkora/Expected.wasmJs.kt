package com.sakethh.linkora

actual object WebCapture {
    actual suspend fun init(): Result<Boolean> {
        return Result.failure(IllegalStateException("this is not supposed to be called on the web"))
    }

    actual suspend fun saveHTMLPage(
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
    ): Boolean {
        return false
    }
}