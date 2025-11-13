package com.sakethh.linkora

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.utils.linkoraLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoSaveLinkService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private val intentActivityVM = IntentActivityVM(
        localLinksRepo = DependencyContainer.localLinksRepo,
        localFoldersRepo = DependencyContainer.localFoldersRepo,
        localPanelsRepo = DependencyContainer.localPanelsRepo,
        localTagsRepo = DependencyContainer.localTagsRepo,
        snapshotRepo = DependencyContainer.snapshotRepo
    )

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val url = intent?.getStringExtra(
            Intent.EXTRA_TEXT
        ).toString()
        val notification = NotificationCompat.Builder(applicationContext, "1")
            .setSmallIcon(R.drawable.ic_stat_name).setContentTitle("Auto-saving the link...")
            .setContentText("Retrieving metadata to save the link: $url")
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()

        startForeground(1, notification)

        CoroutineScope(Dispatchers.Default).launch {
            intentActivityVM.localLinksRepo.addANewLink(
                selectedTagIds = null, linkSaveConfig = LinkSaveConfig(
                    forceAutoDetectTitle = true, forceSaveWithoutRetrievingData = false
                ), viaSocket = false, link = Link(
                    linkType = LinkType.SAVED_LINK,
                    title = "",
                    url = url,
                    imgURL = "",
                    note = "",
                    idOfLinkedFolder = null
                )
            ).collectLatest {
                withContext(Dispatchers.Main) {
                    it.onSuccess {
                        Toast.makeText(
                            applicationContext,
                            "Auto-saved the link successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }.onFailure {
                        Toast.makeText(
                            applicationContext, it, Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }.invokeOnCompletion {
            if (!MainActivity.wasLaunched && AppPreferences.areSnapshotsEnabled.value) {
                intentActivityVM.createADataSnapshot(onCompletion = {
                    Toast.makeText(
                        applicationContext, "Snapshot created successfully", Toast.LENGTH_SHORT
                    ).show()
                })
            }
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        linkoraLog("Destroying the AutoSaveLinkService")
        super.onDestroy()
    }
}