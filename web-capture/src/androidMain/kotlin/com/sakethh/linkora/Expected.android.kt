package com.sakethh.linkora

actual object WebCapture {
    actual suspend fun init(): Result<Boolean> = JVMAndAndroidWebCapture.init()
    actual suspend fun nuke(): Result<Boolean> = JVMAndAndroidWebCapture.nuke()
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
        logStuff: Boolean,
    ): Boolean = JVMAndAndroidWebCapture.saveHTMLPage(
        fileDescriptor = fileDescriptor,
        filePath = filePath,
        url = url,
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
    )
}
