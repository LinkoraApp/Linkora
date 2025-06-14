package com.sakethh

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import android.os.Environment
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sakethh.linkora.LinkoraApp
import com.sakethh.linkora.R
import com.sakethh.linkora.ReminderReceiver
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.preferences.AppPreferenceType
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.Constants
import com.sakethh.linkora.common.utils.getLocalizedString
import com.sakethh.linkora.common.utils.isNull
import com.sakethh.linkora.common.utils.suspendTryAndCatch
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.ImportFileType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.RawExportString
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.model.Snapshot
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.ui.AppVM
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.theme.poppinsFontFamily
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.utils.AndroidUIEvent
import com.sakethh.linkora.utils.isTablet
import com.sakethh.linkora.worker.RefreshAllLinksWorker
import com.sakethh.linkora.worker.SnapshotWorker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.UUID

actual val showFollowSystemThemeOption: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
actual val platform: @Composable () -> Platform = {
    if (isTablet(LocalConfiguration.current) || LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) Platform.Android.Tablet else Platform.Android.Mobile
}
actual val BUILD_FLAVOUR: String = platform.toString()
actual val localDatabase: LocalDatabase? = LinkoraApp.getLocalDb()

actual val poppinsFontFamily: FontFamily = poppinsFontFamily
actual val showDynamicThemingOption: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
actual suspend fun writeRawExportStringToFile(
    exportFileType: ExportFileType,
    rawExportString: RawExportString,
    onCompletion: suspend (String) -> Unit
) {
    val defaultFolder = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        File(Environment.getExternalStorageDirectory(), "Linkora/Exports")
    } else {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Linkora/Exports"
        )
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && defaultFolder.exists().not()) {
        File(Environment.getExternalStorageDirectory(), "Linkora/Exports").mkdirs()
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && defaultFolder.exists().not()) {
        File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "Linkora/Exports"
        ).mkdirs()
    }

    val exportFileName = "LinkoraExport-${
        DateFormat.getDateTimeInstance().format(Date()).replace(":", "").replace(" ", "")
    }.${if (exportFileType == ExportFileType.HTML) "html" else "json"}"

    val file = File(defaultFolder, exportFileName)
    file.writeText(rawExportString)
    onCompletion(exportFileName)
}

actual suspend fun isStorageAccessPermittedOnAndroid(): Boolean {
    return if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(
            LinkoraApp.getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        true
    } else {
        AndroidUIEvent.pushUIEvent(AndroidUIEvent.Type.ShowRuntimePermissionForStorage)
        false
    }
}

actual suspend fun pickAValidFileForImporting(
    importFileType: ImportFileType, onStart: () -> Unit
): File? {
    AndroidUIEvent.pushUIEvent(
        AndroidUIEvent.Type.ImportAFile(
            fileType = if (importFileType == ImportFileType.JSON) "application/json" else "text/html"
        )
    )
    val deferredFile = CompletableDeferred<File?>()
    CoroutineScope(Dispatchers.IO).launch {
        AndroidUIEvent.androidUIEventChannel.collectLatest {
            if (it is AndroidUIEvent.Type.UriOfTheFileForImporting) {
                try {
                    if (it.uri.isNull()) {
                        throw NullPointerException()
                    }
                    onStart()
                    val file = createTempFile()
                    LinkoraApp.getContext().contentResolver.openInputStream(it.uri!!).use { input ->
                        file.outputStream().use { output ->
                            input?.copyTo(output)
                        }
                    }
                    deferredFile.complete(file)
                } catch (e: Exception) {
                    e.printStackTrace()
                    deferredFile.complete(null)
                } finally {
                    this.cancel()
                }
            }
        }
    }
    return deferredFile.await()
}

actual fun onShare(url: String) {
    val intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, url)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(intent, null)
    shareIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    LinkoraApp.getContext().startActivity(shareIntent)
}

actual suspend fun onRefreshAllLinks(
    localLinksRepo: LocalLinksRepo, preferencesRepository: PreferencesRepository
) {
    val workManager = WorkManager.getInstance(LinkoraApp.getContext())
    val request = OneTimeWorkRequestBuilder<RefreshAllLinksWorker>().setConstraints(
        Constraints(requiredNetworkType = NetworkType.CONNECTED)
    ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

    AppPreferences.refreshLinksWorkerTag.value = request.id.toString()
    preferencesRepository.changePreferenceValue(
        preferenceKey = stringPreferencesKey(
            AppPreferenceType.CURRENT_WORK_MANAGER_WORK_UUID.name
        ), newValue = AppPreferences.refreshLinksWorkerTag.value
    )
    preferencesRepository.changePreferenceValue(
        preferenceKey = longPreferencesKey(AppPreferenceType.LAST_REFRESHED_LINK_INDEX.name),
        newValue = -1
    )
    workManager.enqueueUniqueWork(
        AppPreferences.refreshLinksWorkerTag.value, ExistingWorkPolicy.KEEP, request
    )
}

actual fun cancelRefreshingLinks() {
    RefreshAllLinksWorker.cancelLinksRefreshing()
}

actual suspend fun isAnyRefreshingScheduled(): Flow<Boolean?> {
    return channelFlow {
        WorkManager.getInstance(LinkoraApp.getContext())
            .getWorkInfoByIdFlow(UUID.fromString(AppPreferences.refreshLinksWorkerTag.value))
            .collectLatest {
                if (it != null) {
                    send(it.state == WorkInfo.State.ENQUEUED)
                } else {
                    send(null)
                }
            }
    }
}

@Composable
actual fun PlatformSpecificBackHandler(init: () -> Unit) {
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    BackHandler(onBack = {
        if (AppVM.isMainFabRotated.value) {
            AppVM.isMainFabRotated.value = false
        } else if (navController.previousBackStackEntry == null) {
            coroutineScope.launch {
                UIEvent.pushUIEvent(UIEvent.Type.MinimizeTheApp)
            }
        } else {
            init()
        }
    })
}

actual suspend fun permittedToShowNotification(): Boolean {
    return if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
            LinkoraApp.getContext(), Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        true
    } else {
        AndroidUIEvent.pushUIEvent(AndroidUIEvent.Type.ShowRuntimePermissionForNotifications)
        false
    }
}

actual fun platformSpecificLogging(string: String) {
    Log.d("Linkora Log", string)
}

actual class DataSyncingNotificationService actual constructor() {

    private val context = LinkoraApp.getContext()
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

actual val linkoraDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    produceFile = {
        LinkoraApp.getContext().applicationContext.filesDir.resolve(Constants.DATA_STORE_NAME).absolutePath.toPath()
    })

actual suspend fun exportSnapshotData(
    rawExportString: String, fileType: ExportFileType, onCompletion: suspend (String) -> Unit
) {
    val snapshotWorker = OneTimeWorkRequestBuilder<SnapshotWorker>()
    val rawExportStringID: Long =
        DependencyContainer.snapshotRepo.value.addASnapshot(Snapshot(content = rawExportString))

    val parameters = Data.Builder().putLong(key = "rawExportStringID", value = rawExportStringID)
        .putString(key = "fileType", value = fileType.name).build()
    snapshotWorker.setInputData(parameters)
    WorkManager.getInstance(LinkoraApp.getContext()).enqueue(snapshotWorker.build())
}


actual suspend fun scheduleAReminder(
    reminder: Reminder,
    graphicsLayer: GraphicsLayer,
    onFailure: suspend (String) -> Unit,
    onSuccess: suspend (base64String: String) -> Unit
) {

    // imageBitmap should be converted to base64,
    // it should have been done in the common module,
    // but SkiaBitmap doesn't support it on Android,
    // so it's handled here using Android's Bitmap.
    val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()

    if (reminder.reminderType == Reminder.Type.STICKY) {
        val notificationManager = LinkoraApp.getContext()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notification = NotificationCompat.Builder(LinkoraApp.getContext(), "2")
            .setSmallIcon(R.drawable.ic_stat_name).setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(bitmap)
            ).setContentTitle(reminder.title).setContentText(reminder.description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSilent(reminder.reminderMode == Reminder.Mode.SILENT).apply {
                if (reminder.reminderMode == Reminder.Mode.VIBRATE) {
                    setVibrate(LongArray(5) { 1000 })
                }
                if (reminder.reminderMode == Reminder.Mode.CRUCIAL) {
                    setSound(notificationSound)
                }
            }.setOngoing(true).build()

        notificationManager.notify(2, notification)
        return
    }

    val base64String: String = ByteArrayOutputStream().use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        android.util.Base64.encodeToString(it.toByteArray(), android.util.Base64.DEFAULT)
    }

    val alarmManager: AlarmManager =
        LinkoraApp.getContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

    val catchInit: suspend (Exception) -> Unit = { exception: Exception ->
        if (exception is SecurityException) {
            if (Build.VERSION.SDK_INT >= 31) {
                LinkoraApp.getContext().startActivity(
                    Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        exception.printStackTrace()
        onFailure(exception.message.toString())
    }
    val now = Calendar.getInstance().timeInMillis

    when (reminder.reminderType) {
        Reminder.Type.ONCE -> {
            suspendTryAndCatch(onCatch = {
                catchInit(it)
            }) {
                if (reminder.date == null || reminder.time == null) return@suspendTryAndCatch onFailure(
                    "reminder.date == null || reminder.time == null"
                )

                val pendingIntent = PendingIntent.getBroadcast(
                    LinkoraApp.getContext(),
                    reminder.id.toInt(),
                    Intent(LinkoraApp.getContext(), ReminderReceiver::class.java).apply {
                        putExtra("id", reminder.id)
                    },
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val reminderTime = Calendar.getInstance().apply {
                    set(
                        reminder.date.year.toInt(),
                        reminder.date.month.toInt() - 1,
                        reminder.date.dayOfMonth.toInt(),
                        reminder.time.hour.toInt(),
                        reminder.time.minute.toInt(),
                        reminder.time.second.toInt()
                    )
                }.timeInMillis

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, reminderTime, pendingIntent
                )
                onSuccess(base64String)
            }
        }

        Reminder.Type.PERIODIC.WEEKLY -> {
            if (reminder.time == null) return
            suspendTryAndCatch(onCatch = {
                catchInit(it)
            }) {
                reminder.daysOfWeek?.forEachIndexed { index, dayOfWeek ->
                    val weekDay = when (dayOfWeek.uppercase()) {
                        "SUN" -> Calendar.SUNDAY
                        "MON" -> Calendar.MONDAY
                        "TUE" -> Calendar.TUESDAY
                        "WED" -> Calendar.WEDNESDAY
                        "THU" -> Calendar.THURSDAY
                        "FRI" -> Calendar.FRIDAY
                        "SAT" -> Calendar.SATURDAY
                        else -> return@forEachIndexed
                    }

                    val scheduledCalendar = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, weekDay)
                        set(Calendar.HOUR_OF_DAY, reminder.time.hour.toInt())
                        set(Calendar.MINUTE, reminder.time.minute.toInt())
                        set(Calendar.SECOND, reminder.time.second.toInt())
                        if (timeInMillis <= now) add(Calendar.WEEK_OF_YEAR, 1)
                    }

                    val intent =
                        Intent(LinkoraApp.getContext(), ReminderReceiver::class.java).apply {
                            putExtra("id", reminder.id)
                        }
                    val requestCode = reminder.id.toInt() * 100 + weekDay * 10 + index
                    val pendingIntent = PendingIntent.getBroadcast(
                        LinkoraApp.getContext(),
                        requestCode,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        scheduledCalendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY * 7,
                        pendingIntent
                    )
                }

                onSuccess(base64String)
            }
        }

        Reminder.Type.PERIODIC.MONTHLY -> {
            if (reminder.time == null) return

            suspendTryAndCatch(onCatch = {
                catchInit(it)
            }) {
                reminder.datesOfMonth?.forEachIndexed { index, dateOfMonth ->
                    val scheduledCalendar = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, dateOfMonth)
                        set(Calendar.HOUR_OF_DAY, reminder.time.hour.toInt())
                        set(Calendar.MINUTE, reminder.time.minute.toInt())
                        set(Calendar.SECOND, reminder.time.second.toInt())
                        if (timeInMillis <= now) add(Calendar.MONTH, 1)
                    }

                    val intent = Intent(LinkoraApp.getContext(), ReminderReceiver::class.java).apply {
                        putExtra("id", reminder.id)
                    }

                    val requestCode = reminder.id.toInt() * 100 + dateOfMonth * 10 + index
                    val pendingIntent = PendingIntent.getBroadcast(
                        LinkoraApp.getContext(),
                        requestCode,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        scheduledCalendar.timeInMillis,
                        AlarmManager.INTERVAL_DAY * 30,
                        pendingIntent
                    )
                }

                onSuccess(base64String)
            }
        }

        else -> Unit
    }
}

actual fun canScheduleReminders(): Boolean {
    val alarmManager =
        LinkoraApp.getContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return if (Build.VERSION.SDK_INT >= 31 && alarmManager.canScheduleExactAlarms().not()) {
        LinkoraApp.getContext().startActivity(
            Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(
                FLAG_ACTIVITY_NEW_TASK
            )
        )
        false
    } else {
        true
    }
}

actual fun cancelAReminder(requestCode: Int) {
    val alarmManager: AlarmManager =
        LinkoraApp.getContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // this should exactly match with the pendingIntent that was previously set using @scheduleAReminder
    // https://stackoverflow.com/questions/28922521/how-to-cancel-alarm-from-alarmmanager/28922621#28922621
    val pendingIntent = PendingIntent.getBroadcast(
        LinkoraApp.getContext(),
        requestCode,
        Intent(LinkoraApp.getContext(), ReminderReceiver::class.java).apply {
            putExtra("id", requestCode)
        },
        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    pendingIntent.cancel()
}