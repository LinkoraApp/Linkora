package com.sakethh.linkora.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sakethh.linkora.Localization
import com.sakethh.linkora.R
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.worker.RefreshAllLinksWorker

class RefreshAllLinksNotificationService(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val cancelRefreshingIntent = Intent(context, CancelRefreshingActionReceiver::class.java)
    private val cancelRefreshingPendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        cancelRefreshingIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun clearNotifications() {
        notificationManager.cancelAll()
    }

    fun showNotification() {
        val notification = NotificationCompat.Builder(context, "1")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(Localization.Key.RefreshingLinks.getLocalizedString())
            .setContentText(
                Localization.Key.NoOfLinksRefreshed.getLocalizedString()
                    .replace(
                        LinkoraPlaceHolder.First.value,
                        DataSettingsScreenVM.refreshLinksState.value.currentIteration.toString()
                    ).replace(
                        LinkoraPlaceHolder.Second.value,
                        DataSettingsScreenVM.totalLinksForRefresh.value.toString()
                    )
            )
            .setProgress(
                DataSettingsScreenVM.totalLinksForRefresh.value,
                DataSettingsScreenVM.refreshLinksState.value.currentIteration,
                false
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .addAction(
                R.drawable.ic_stat_name,
                Localization.Key.Cancel.getLocalizedString(),
                cancelRefreshingPendingIntent
            )
            .build()

        notificationManager.notify(1, notification)
    }
}

class CancelRefreshingActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            RefreshAllLinksWorker.cancelLinksRefreshing(
                context,
                refreshLinksWorkerTag = DependencyContainer.preferencesRepo.getPreferences().refreshLinksWorkerTag
            )
        }
    }
}