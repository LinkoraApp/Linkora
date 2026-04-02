package com.sakethh.linkora.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.sakethh.linkora.IntentActivityVM
import com.sakethh.linkora.Localization
import com.sakethh.linkora.MainActivity
import com.sakethh.linkora.R
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.replaceFirstPlaceHolderWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AutoSaveLinkService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun toast(msg: String) = mainHandler.post {
        Toast.makeText(
            applicationContext, msg, Toast.LENGTH_SHORT
        ).show()
    }

    private val intentActivityVM = IntentActivityVM(
        localLinksRepo = DependencyContainer.localLinksRepo,
        localFoldersRepo = DependencyContainer.localFoldersRepo,
        localPanelsRepo = DependencyContainer.localPanelsRepo,
        localTagsRepo = DependencyContainer.localTagsRepo,
        snapshotRepo = DependencyContainer.snapshotRepo,
        preferencesRepository = DependencyContainer.preferencesRepo,
        showToast = ::toast
    )

    private var areSnapshotsEnabled = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val url = intent?.getStringExtra(
            Intent.EXTRA_TEXT
        ).toString()
        val notification = NotificationCompat.Builder(applicationContext, "1")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle(Localization.Key.AutoSavingTheLinkNotifyLabel.getLocalizedString())
            .setContentText(
                Localization.Key.AutoSavingTheLinkNotifyLabel.getLocalizedString()
                    .replaceFirstPlaceHolderWith(url)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH).build()

        startForeground(1, notification)

        CoroutineScope(Dispatchers.Default).launch {
            val preferences = intentActivityVM.preferencesRepository.getPreferences()
            areSnapshotsEnabled = preferences.areSnapshotsEnabled
            intentActivityVM.localLinksRepo.addANewLink(
                selectedTagIds = null, linkSaveConfig = LinkSaveConfig(
                    forceAutoDetectTitle = true, forceSaveWithoutRetrievingData = false,
                    useProxy = preferences.useProxy,
                    skipSavingIfExists = preferences.skipSavingExistingLink,
                    forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails,
                ), viaSocket = false, link = Link(
                    linkType = LinkType.SAVED_LINK,
                    title = "",
                    url = url,
                    imgURL = "",
                    note = "",
                    idOfLinkedFolder = Constants.SAVED_LINKS_ID,
                    userAgent = preferences.primaryJsoupUserAgent,
                )
            ).collectLatest {
                withContext(Dispatchers.Main) {
                    it.onSuccess {
                        toast(Localization.Key.AutoSavedTheLinkSuccessfully.getLocalizedString())
                    }.onFailure {
                        toast(it)
                    }
                }
            }
        }.invokeOnCompletion {
            if (!MainActivity.wasLaunched && areSnapshotsEnabled) {
                intentActivityVM.createADataSnapshot(onCompletion = {
                    toast(Localization.Key.SnapshotCreatedSuccessfully.getLocalizedString())
                })
            }
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        linkoraLog("Nuking the AutoSaveLinkService")
        super.onDestroy()
    }
}