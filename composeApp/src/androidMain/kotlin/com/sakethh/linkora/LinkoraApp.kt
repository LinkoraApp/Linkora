package com.sakethh.linkora

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import kotlinx.coroutines.Dispatchers
import okio.Path.Companion.toPath

class LinkoraApp : Application() {

    override fun onCreate() {
        super.onCreate()
        LinkoraSDK.set(linkoraSdk = LinkoraSDK(
            nativeUtils = NativeUtils(applicationContext),
            fileManager = FileManager(applicationContext),
            permissionManager = PermissionManager(applicationContext),
            localDatabase = run {
                val dbFile = applicationContext.getDatabasePath(LocalDatabase.NAME)
                Room.databaseBuilder(
                    applicationContext,
                    LocalDatabase::class.java,
                    name = dbFile.absolutePath
                ).setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO).addMigrations(
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
                    LocalDatabase.MIGRATION_11_12
                ).build()
            },
            dataStore = PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    applicationContext.filesDir.resolve(Constants.DATA_STORE_NAME).absolutePath.toPath()
                }), dataSyncingNotificationService = NativeUtils.DataSyncingNotificationService(applicationContext)
        ))
        AppPreferences.readAll(defaultExportLocation = LinkoraSDK.getInstance().fileManager.getDefaultExportLocation(),preferencesRepository = DependencyContainer.preferencesRepo)
        Localization.loadLocalizedStrings(
            AppPreferences.preferredAppLanguageCode.value
        )
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "1", "Data Syncing", NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description =
                "Used to notify about the data syncing status, link refreshes, and auto-save status."
            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }
}