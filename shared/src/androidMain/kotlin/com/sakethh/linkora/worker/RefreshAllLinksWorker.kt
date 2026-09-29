package com.sakethh.linkora.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.model.RefreshLink
import com.sakethh.linkora.service.RefreshAllLinksNotificationService
import com.sakethh.linkora.shared.R
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.RefreshLinksState
import com.sakethh.linkora.ui.utils.linkoraLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class RefreshAllLinksWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    companion object {
        fun cancelLinksRefreshing(
            appContext: Context,
            refreshLinksWorkerTag: String,
        ) {
            WorkManager.getInstance(appContext)
                .cancelWorkById(UUID.fromString(refreshLinksWorkerTag))
            DataSettingsScreenVM.refreshLinksState.value = RefreshLinksState(
                isInRefreshingState = false,
                currentIteration = 0,
                total = 0,
            )
            linkoraLog("cancelLinksRefreshing")
        }

        private const val SHUTDOWN_REFRESH_PROCESSING: Long = -2
    }

    private var refreshAllLinksNotificationService = RefreshAllLinksNotificationService(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        1,
        NotificationCompat.Builder(applicationContext, "1")
            .setSmallIcon(R.drawable.notification_icon)
            .build(),
    )

    private var linksProcessedChannel: Channel<Long>? = null
    private var linksProcessedChannelJob: Job? = null

    private var processedLinksCount: Long = -1

    override suspend fun doWork(): Result = coroutineScope {
        val preferences = DependencyContainer.preferencesRepo.getPreferences()
        processedLinksCount = DependencyContainer.preferencesRepo.readPreferenceValue(
            preferenceKey = AppPreferences.REFRESHED_LINKS_COUNT,
        ) ?: 0

        refreshAllLinksNotificationService.clearNotifications()
        linksProcessedChannel?.cancel()
        linksProcessedChannelJob?.cancel()

        linksProcessedChannel = Channel(Channel.BUFFERED)
        linksProcessedChannelJob = launch {
            linksProcessedChannel?.consumeAsFlow()?.cancellable()?.collect { refreshedLinkId ->
                if (refreshedLinkId == SHUTDOWN_REFRESH_PROCESSING) {
                    linkoraLog("processedLinksCount = SHUTDOWN_REFRESH_PROCESSING")
                    cleanUp()
                    return@collect
                }
                DependencyContainer.preferencesRepo.changePreferenceValue(
                    preferenceKey = AppPreferences.REFRESHED_LINKS_COUNT,
                    newValue = ++processedLinksCount,
                )

                LinkoraSDK.getInstance().localDatabase.refreshDao.insertAProcessedId(
                    RefreshLink(
                        refreshedLinkId,
                    ),
                )
                DataSettingsScreenVM.refreshLinksState.value =
                    DataSettingsScreenVM.refreshLinksState.value.copy(
                        currentIteration = processedLinksCount.toInt(),
                    )
                refreshAllLinksNotificationService.showNotification()
                linkoraLog("processedLinksCount = $processedLinksCount")
            }
        }

        if (isStopped) {
            cleanUp()
            return@coroutineScope Result.success()
        }

        return@coroutineScope try {
            val allLinks = DependencyContainer.localLinksRepo.getAllLinks()
            DataSettingsScreenVM.refreshLinksState.value =
                DataSettingsScreenVM.refreshLinksState.value.copy(
                    isInRefreshingState = true,
                    currentIteration = 0,
                    total = allLinks.size,
                )
            val processedLinkIds = DependencyContainer.refreshLinksRepo.getProcessedLinkIds()

            val linksToBeRefreshed = allLinks.filter {
                it.localId !in processedLinkIds
            }

            if (linksToBeRefreshed.isEmpty()) return@coroutineScope Result.success()

            linksToBeRefreshed.asFlow()
                .flatMapMerge(concurrency = preferences.maxConcurrentRefreshCount) { link ->
                    DependencyContainer.localLinksRepo.refreshLinkMetadata(
                        link,
                        refreshLinkType = preferences.selectedLinkRefreshType,
                        useWebCapture = false,
                    ).map { result ->
                        when (result) {
                            is com.sakethh.linkora.domain.Result.Failure -> link.localId
                            is com.sakethh.linkora.domain.Result.Loading -> -1
                            is com.sakethh.linkora.domain.Result.Success -> link.localId
                        }
                    }
                }.onEach {
                    if (isStopped) {
                        cleanUp()
                        cancel()
                    }
                }.catch {
                    it.printStackTrace()
                }.collect { processedLinkId ->
                    if (processedLinkId == (-1).toLong()) return@collect
                    linksProcessedChannel?.send(
                        processedLinkId,
                    )
                }
            linksProcessedChannel?.send(SHUTDOWN_REFRESH_PROCESSING)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private suspend fun cleanUp() {
        val preferences = DependencyContainer.preferencesRepo.getPreferences()
        cancelLinksRefreshing(
            applicationContext,
            refreshLinksWorkerTag = preferences.refreshLinksWorkerTag,
        )
        DataSettingsScreenVM.refreshLinksState.value =
            DataSettingsScreenVM.refreshLinksState.value.copy(
                isInRefreshingState = false,
                currentIteration = 0,
            )
        refreshAllLinksNotificationService.clearNotifications()
        linkoraLog("refreshAllLinksNotificationService.clearNotifications")
        linksProcessedChannel?.close()

        linksProcessedChannelJob?.join()
        linksProcessedChannel?.cancel()

        linksProcessedChannelJob = null
        linksProcessedChannel = null
    }
}
