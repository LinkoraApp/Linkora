package com.sakethh.linkora.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sakethh.linkora.R
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.model.RefreshLink
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.service.RefreshAllLinksNotificationService
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
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class RefreshAllLinksWorker(appContext: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(appContext, workerParameters) {

    companion object {

        fun cancelLinksRefreshing(appContext: Context) {
            WorkManager.getInstance(appContext)
                .cancelWorkById(UUID.fromString(AppPreferences.refreshLinksWorkerTag.value))
            DataSettingsScreenVM.refreshLinksState.value = RefreshLinksState(
                isInRefreshingState = false, currentIteration = 0
            )
            linkoraLog("cancelLinksRefreshing")
        }

    }

    private var refreshAllLinksNotificationService = RefreshAllLinksNotificationService(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            1,
            NotificationCompat.Builder(applicationContext, "1")
                .setSmallIcon(R.drawable.ic_stat_name)
                .build()
        )
    }

    private var linksProcessedChannel: Channel<Long>? = null
    private var linksProcessedChannelJob: Job? = null

    private var processedLinksCount: Long = -1

    private val maxConcurrentRefreshCount = AppPreferences.maxConcurrentRefreshCount

    override suspend fun doWork(): Result = coroutineScope {

        processedLinksCount = DependencyContainer.preferencesRepo.readPreferenceValue(
            longPreferencesKey(AppPreferenceType.REFRESHED_LINKS_COUNT.name)
        ) ?: 0

        refreshAllLinksNotificationService.clearNotifications()
        linksProcessedChannel?.cancel()
        linksProcessedChannelJob?.cancel()

        linksProcessedChannel = Channel(Channel.BUFFERED)
        linksProcessedChannelJob = launch {
            linksProcessedChannel?.consumeAsFlow()?.cancellable()
                ?.collect { refreshedLinkIndex ->
                    DependencyContainer.preferencesRepo.changePreferenceValue(
                        preferenceKey = longPreferencesKey(AppPreferenceType.REFRESHED_LINKS_COUNT.name),
                        newValue = ++processedLinksCount
                    )

                    LinkoraSDK.getInstance().localDatabase.refreshDao.insertAProcessedId(
                        RefreshLink(
                            refreshedLinkIndex
                        )
                    )
                    DataSettingsScreenVM.refreshLinksState.value =
                        DataSettingsScreenVM.refreshLinksState.value.copy(
                            currentIteration = processedLinksCount.toInt()
                        )
                    refreshAllLinksNotificationService.showNotification()
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
                    currentIteration = 0
                )
            DataSettingsScreenVM.totalLinksForRefresh.value = allLinks.size

            val processedLinkIds = DependencyContainer.refreshLinksRepo.getProcessedLinkIds()

            val linksToBeRefreshed = allLinks.filter {
                it.localId !in processedLinkIds
            }

            if (linksToBeRefreshed.isEmpty()) return@coroutineScope Result.success()

            linksToBeRefreshed.asFlow()
                .flatMapMerge(concurrency = maxConcurrentRefreshCount) { link ->
                    channelFlow {
                        DependencyContainer.localLinksRepo.refreshLinkMetadata(
                            link,
                            refreshLinkType = AppPreferences.selectedLinkRefreshType
                        )
                            .collectLatest {
                                it.onSuccess {
                                    send(link.localId)
                                }.onFailure {
                                    send(link.localId)
                                }
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
                    linksProcessedChannel?.send(
                        processedLinkId
                    )
                    linkoraLog("Processed $processedLinkId")
                }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        } finally {
            cleanUp()
        }
    }

    private suspend fun cleanUp() {
        cancelLinksRefreshing(applicationContext)
        DataSettingsScreenVM.refreshLinksState.value =
            DataSettingsScreenVM.refreshLinksState.value.copy(
                isInRefreshingState = false, currentIteration = 0
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