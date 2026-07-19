package com.sakethh.linkora.model

data class WebCaptureRequest(
    val nativeFolderPath: String,
    val url: String,
    val userAgent: String,
    val timeout: Long,
    val allowInsecureProtocol: Boolean,
    val ignoreDocErrors: Boolean,
    val useCss: Boolean,
    val embedFonts: Boolean,
    val embedImages: Boolean,
    val restrictJs: Boolean,
    val logStuff: Boolean,
    val includeAudioElements: Boolean,
    val includeVideoElements: Boolean,
    val includeMetadata: Boolean,
)
