package com.sakethh.linkora.platform

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sakethh.linkora.Localization
import com.sakethh.linkora.R
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.local.RefreshLinksRepo
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.longPreferencesKey
import com.sakethh.linkora.utils.stringPreferencesKey
import com.sakethh.linkora.worker.RefreshAllLinksWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import java.util.UUID

actual class NativeUtils(private val context: Context) {
    actual fun onShare(url: String) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(intent, null)
        shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(shareIntent)
    }

    actual suspend fun onRefreshAllLinks(
        localLinksRepo: LocalLinksRepo,
        preferencesRepository: PreferencesRepository,
        refreshLinksRepo: RefreshLinksRepo
    ) {
        val preferences = DependencyContainer.preferencesRepo.getPreferences()
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<RefreshAllLinksWorker>().setConstraints(
            Constraints(requiredNetworkType = NetworkType.CONNECTED)
        ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

        preferencesRepository.changePreferenceValue(
            preferenceKey = stringPreferencesKey(
                AppPreferences.CURRENT_WORK_MANAGER_WORK_UUID.key
            ), newValue = preferences.refreshLinksWorkerTag
        )
        preferencesRepository.changePreferenceValue(
            preferenceKey = longPreferencesKey(AppPreferences.REFRESHED_LINKS_COUNT.key),
            newValue = 0
        )
        refreshLinksRepo.deleteAllIds()
        workManager.enqueueUniqueWork(
            preferences.refreshLinksWorkerTag, ExistingWorkPolicy.KEEP, request
        )
    }

    actual suspend fun isAnyRefreshingScheduled(): Flow<Boolean?> {
        val preferences = DependencyContainer.preferencesRepo.getPreferences()
        return channelFlow {
            WorkManager.getInstance(context)
                .getWorkInfoByIdFlow(UUID.fromString(preferences.refreshLinksWorkerTag))
                .collectLatest {
                    if (it != null) {
                        send(it.state == WorkInfo.State.ENQUEUED)
                    } else {
                        send(null)
                    }
                }
        }
    }

    actual fun cancelRefreshingLinks() {
        RefreshAllLinksWorker.cancelLinksRefreshing(
            context,
            refreshLinksWorkerTag = DependencyContainer.preferencesRepo.getPreferences().refreshLinksWorkerTag
        )
    }

    actual class DataSyncingNotificationService(private val context: Context) {

        private val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        actual fun showNotification() {
            val notification =
                NotificationCompat.Builder(context, "1").setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(Localization.Key.SyncingDataLabel.getLocalizedString())
                    .setProgress(
                        0, 0, true
                    ).setPriority(NotificationCompat.PRIORITY_LOW).setSilent(true).build()

            notificationManager.notify(1, notification)
        }

        actual fun clearNotification() {
            notificationManager.cancelAll()
        }
    }

    private val packageManager = context.packageManager
    private val packageName = context.applicationContext.packageName

    actual fun onIconChange(
        allIconCodes: List<String>, newIconCode: String, onCompletion: () -> Unit
    ) {
        allIconCodes.forEach {
            if (it != newIconCode) {
                packageManager.setComponentEnabledSetting(
                    ComponentName(packageName, "$packageName.$it"),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }

        val newAppIconComponent = ComponentName(packageName, "$packageName.$newIconCode")

        packageManager.setComponentEnabledSetting(
            newAppIconComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        onCompletion()
    }

    actual fun <T> platformRunBlocking(block: suspend () -> T): T? = runBlocking {
        block()
    }
}