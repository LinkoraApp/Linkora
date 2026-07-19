package com.sakethh.linkora.worker

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.sakethh.linkora.R
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.model.RefreshLink
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.service.RefreshAllLinksNotificationService
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.RefreshLinksState
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.longPreferencesKey
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
    }

    private var refreshAllLinksNotificationService = RefreshAllLinksNotificationService(appContext)

    override suspend fun getForegroundInfo(): ForegroundInfo = ForegroundInfo(
        1,
        NotificationCompat.Builder(applicationContext, "1").setSmallIcon(R.drawable.ic_stat_name)
            .build(),
    )

    private var linksProcessedChannel: Channel<Long>? = null
    private var linksProcessedChannelJob: Job? = null

    private var processedLinksCount: Long = -1

    override suspend fun doWork(): Result = coroutineScope {
        val preferences = DependencyContainer.preferencesRepo.getPreferences()
        processedLinksCount = DependencyContainer.preferencesRepo.readPreferenceValue(
            longPreferencesKey(AppPreferences.REFRESHED_LINKS_COUNT.key),
        ) ?: 0

        refreshAllLinksNotificationService.clearNotifications()
        linksProcessedChannel?.cancel()
        linksProcessedChannelJob?.cancel()

        linksProcessedChannel = Channel(Channel.BUFFERED)
        linksProcessedChannelJob = launch {
            linksProcessedChannel?.consumeAsFlow()?.cancellable()?.collect { refreshedLinkId ->
                DependencyContainer.preferencesRepo.changePreferenceValue(
                    preferenceKey = longPreferencesKey(AppPreferences.REFRESHED_LINKS_COUNT.key),
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
                        preferences.captureWhenRefreshAllLink,
                    ).map { result ->
                        when (result) {
                            is com.sakethh.linkora.domain.Result.Failure -> com.sakethh.linkora.domain.Result.Failure(
                                result.message,
                            )

                            is com.sakethh.linkora.domain.Result.Loading -> com.sakethh.linkora.domain.Result.Loading()

                            is com.sakethh.linkora.domain.Result.Success -> com.sakethh.linkora.domain.Result.Success(
                                link.localId,
                            )
                        }
                    }
                }.onEach {
                    if (isStopped) {
                        cleanUp()
                        cancel()
                    }
                }.catch {
                    it.printStackTrace()
                }.collect { result ->
                    result.onSuccess { (processedLinkId) ->
                        linksProcessedChannel?.send(
                            processedLinkId,
                        )
                        linkoraLog("Processed $processedLinkId")
                    }
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
