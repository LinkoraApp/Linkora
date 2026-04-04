package com.sakethh.linkora

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.LocalDatabase
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
                        applicationContext, name = dbFile.absolutePath
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
                            LocalDatabase.MIGRATION_13_14
                        ).build()
                },
                dataSyncingNotificationService = NativeUtils.DataSyncingNotificationService(
                    applicationContext
                ),
                network = Network(applicationContext),
                platformPreference = PlatformPreference(
                    dataStore = PreferenceDataStoreFactory.createWithPath(
                        produceFile = {
                            applicationContext.filesDir.resolve(Constants.DATA_STORE_NAME).absolutePath.toPath()
                        })
                )
            )
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
                "1", "Data Syncing", NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description =
                "Used to notify about the data syncing status, link refreshes, and auto-save status."
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}