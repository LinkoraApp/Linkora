package com.sakethh.linkora.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.shared.R
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.utils.replaceActual
import com.sakethh.linkora.worker.AllLinksWebCaptureWorker

class WebCaptureNotificationService(
    private val context: Context,
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val cancelCaptureIntent = Intent(context, CancelWebCaptureActionReceiver::class.java)
    private val cancelCapturePendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            cancelCaptureIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun clearNotifications() {
        notificationManager.cancelAll()
    }

    private val localizedStrings = DependencyContainer.localizationRepo.localizedStrings

    fun showNotification() {
        val webCaptureState = DataSettingsScreenVM.onGoingWebCaptureState
        val notification =
            NotificationCompat.Builder(context, "1")
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(localizedStrings.value.CapturingWebPages)
                .setContentText(
                    localizedStrings.value.CapturedCount.replaceActual(
                        webCaptureState.currentIteration.toString(),
                        webCaptureState.total.toString()
                    )
                )
                .setProgress(
                    webCaptureState.total,
                    webCaptureState.currentIteration,
                    false,
                )
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .addAction(
                    R.drawable.notification_icon,
                    localizedStrings.value.Cancel,
                    cancelCapturePendingIntent,
                )
                .build()

        notificationManager.notify(1, notification)
    }
}

class CancelWebCaptureActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (context != null) {
            AllLinksWebCaptureWorker.cancelWork(context)
        }
    }
}
