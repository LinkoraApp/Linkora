package com.sakethh.linkora

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sakethh.linkora.ui.utils.linkoraLog

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val reminderId = intent?.getLongExtra("id", 0)
        linkoraLog("from ReminderReceiver: $reminderId")
    }
}