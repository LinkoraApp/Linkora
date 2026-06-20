package com.sakethh.linkora.data.local

import com.sakethh.linkora.WebCaptureDatabase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// creating a database even when web-capture isn't opted-in is bad.
// to get around that, we will only create the database when the user opts-in to the feature for the first time.
// for the current custom DI setup, this lazy approach is the simplest way to implement this without blowing things up.
// if this app used koin, this could be dynamically registered on the fly (i guess), but the current DI handles it just fine.
class WebCaptureDatabaseManager(
    private val databaseBuilder: (path: String) -> WebCaptureDatabase
) {
    private var database: WebCaptureDatabase? = null
    private val mutex = Mutex()

    suspend fun getDatabase(webCaptureDirPath: String): WebCaptureDatabase {
        database?.let { return it }

        return mutex.withLock {
            val currentDb = database
            if (currentDb != null) {
                currentDb
            } else {
                val newDb = databaseBuilder(webCaptureDirPath)
                database = newDb
                newDb
            }
        }
    }

    suspend fun nukeDatabaseConnection() {
        mutex.withLock {
            database?.close()
            database = null
        }
    }
}