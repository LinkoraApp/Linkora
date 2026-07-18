package com.sakethh.linkora

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.data.local.WebCaptureDatabaseManager
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.Network
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.platform.PlatformPreference
import com.sakethh.linkora.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okio.Path.Companion.toPath

class LinkoraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LinkoraSDK.set(
            linkoraSdk = LinkoraSDK(
                nativeUtils = NativeUtils(applicationContext),
                fileManager = FileManager(applicationContext),
                permissionManager = PermissionManager(applicationContext),
                localDatabase = run {
                    val dbFile = applicationContext.getDatabasePath(LocalDatabase.NAME)
                    Room.databaseBuilder<LocalDatabase>(
                        applicationContext,
                        name = dbFile.absolutePath,
                    ).setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
                        .addMigrations(
                            LocalDatabase.MIGRATION_1_2,
                            LocalDatabase.MIGRATION_2_3,
                            LocalDatabase.MIGRATION_3_4,
                            LocalDatabase.MIGRATION_4_5,
                            LocalDatabase.MIGRATION_5_6,
                            LocalDatabase.MIGRATION_6_7,
                            LocalDatabase.MIGRATION_7_8,
                            LocalDatabase.MIGRATION_8_9,
                            LocalDatabase.MIGRATION_9_10,
                            LocalDatabase.MIGRATION_10_11,
                            LocalDatabase.MIGRATION_11_12,
                            LocalDatabase.MIGRATION_12_13,
                            LocalDatabase.MIGRATION_13_14,
                            LocalDatabase.MIGRATION_14_15,
                        ).build()
                },
                dataSyncingNotificationService = NativeUtils.DataSyncingNotificationService(
                    applicationContext,
                ),
                network = Network(applicationContext),
                platformPreference = PlatformPreference(
                    dataStore = PreferenceDataStoreFactory.createWithPath(
                        produceFile = {
                            applicationContext.filesDir.resolve(Constants.DATA_STORE_NAME).absolutePath.toPath()
                        },
                    ),
                ),
                webCapture = NativeUtils.WebCapture(applicationContext),
                webCaptureDatabaseManager = WebCaptureDatabaseManager(databaseBuilder = { webCaptureDirPath ->
                    val folderPath =
                        getAbsolutePathFromSafUri(applicationContext, webCaptureDirPath.toUri())
                    val dbFilePath = "$folderPath/${WebCaptureDatabase.NAME}.db"
                    Room.databaseBuilder<WebCaptureDatabase>(
                        name = dbFilePath,
                        context = applicationContext,
                    ).setDriver(BundledSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
                        .build()
                }),
            ),
        )
        runBlocking {
            DependencyContainer.preferencesRepo.loadPersistedPreferences()
            val preferences = DependencyContainer.preferencesRepo.getPreferences()
            Localization.loadLocalizedStrings(
                preferences,
                languageCode = preferences.preferredAppLanguageCode,
                languageName = preferences.preferredAppLanguageName,
            )?.join()
        }
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "1",
                "Data Syncing",
                NotificationManager.IMPORTANCE_HIGH,
            )
            notificationChannel.description =
                "Used to notify about the data syncing status, link refreshes, and auto-save status."
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun getAbsolutePathFromSafUri(context: Context, uri: Uri): String? {
        if (uri.authority != "com.android.externalstorage.documents") {
            return null
        }

        val rawDocId = DocumentsContract.getTreeDocumentId(uri)
        val decodedDocId = Uri.decode(rawDocId)

        val split = decodedDocId.split(":")
        val type = split[0]
        val path = if (split.size > 1) split[1] else ""

        if ("primary".equals(type, ignoreCase = true)) {
            return "${Environment.getExternalStorageDirectory().absolutePath}/$path".removeSuffix("/")
        }

        // resolve secondary storage (sd cards/usb) uuids to actual posix paths.
        // sqlite needs a real absolute path, raw saf uris will just crash it.
        // volume.directory for android 11+ and falls back to /storage/uuid for older apis.
        val storageManager = context.getSystemService(STORAGE_SERVICE) as StorageManager
        val storageVolumes = storageManager.storageVolumes

        for (volume in storageVolumes) {
            if (volume.uuid == type) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val dir = volume.directory
                    if (dir != null) {
                        return "${dir.absolutePath}/$path".removeSuffix("/")
                    }
                }
                return "/storage/$type/$path".removeSuffix("/")
            }
        }

        return null
    }
}
