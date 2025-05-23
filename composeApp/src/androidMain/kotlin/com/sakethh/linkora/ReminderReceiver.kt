package com.sakethh.linkora

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.domain.model.Reminder
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
                    .setStyle(
                        NotificationCompat.BigPictureStyle().bigPicture(
                            android.util.Base64.decode(
                                reminder.linkView, android.util.Base64.DEFAULT
                            ).run {
                                BitmapFactory.decodeByteArray(this, 0, this.size)
                            })
                    ).setContentTitle(reminder.title).setContentText(reminder.description)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSilent(reminder.reminderMode == Reminder.Mode.SILENT).apply {
                        if (reminder.reminderMode == Reminder.Mode.VIBRATE) {
                            setVibrate(LongArray(5) { 1000 })
                        }
                        if (reminder.reminderMode == Reminder.Mode.CRUCIAL) {
                            setSound(notificationSound)
                        }
                    }.build()

            notificationManager.notify(2, notification)
            if (reminder.reminderType == Reminder.Type.ONCE) {
                DependencyContainer.remindersRepo.value.deleteAReminder(reminderId)
            }
        }
    }
}