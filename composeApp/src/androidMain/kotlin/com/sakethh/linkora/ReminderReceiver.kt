package com.sakethh.linkora

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.ui.domain.ReminderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationManager =
            context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminderId = intent?.getLongExtra("id", 0)
        if (reminderId == null) return

        CoroutineScope(Dispatchers.Default).launch {
            val reminder = DependencyContainer.remindersRepo.value.getAReminder(reminderId)
            val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notification =
                NotificationCompat.Builder(context, "2").setSmallIcon(R.drawable.ic_stat_name)
                    .apply {
                        if (Build.VERSION.SDK_INT >= 26) {
                            setLargeIcon(
                                android.util.Base64.decode(
                                    reminder.linkView, android.util.Base64.DEFAULT
                                ).run {
                                    BitmapFactory.decodeByteArray(this, 0, this.size)
                                })
                        }
                    }.setContentTitle(reminder.title).setContentText(
                        (if (Build.VERSION.SDK_INT < 26) "Reminder for the link title: ${reminder.title}\n" else "") + reminder.description
                    ).setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSilent(reminder.reminderMode == ReminderMode.SILENT).apply {
                        if (reminder.reminderMode == ReminderMode.VIBRATE) {
                            setVibrate(LongArray(5) { 1000 })
                        }
                        if (reminder.reminderMode == ReminderMode.CRUCIAL) {
                            setSound(notificationSound)
                        }
                    }.build()

            notificationManager.notify(1, notification)
        }
    }
}