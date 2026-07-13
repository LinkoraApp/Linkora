package com.sakethh.linkora

import androidx.room3.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object WebCaptureDatabaseConstructor : RoomDatabaseConstructor<WebCaptureDatabase> {
    override fun initialize(): WebCaptureDatabase
}

expect object WebCapture {
    suspend fun init(): Result<Boolean>

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
    ): Boolean
}
