package com.sakethh.linkora

import androidx.room3.RoomDatabaseConstructor

@Suppress("KotlinNoActualForExpect")
expect object WebCaptureDatabaseConstructor : RoomDatabaseConstructor<WebCaptureDatabase> {
    override fun initialize(): WebCaptureDatabase
}
