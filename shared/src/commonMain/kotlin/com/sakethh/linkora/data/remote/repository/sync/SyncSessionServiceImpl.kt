package com.sakethh.linkora.data.remote.repository.sync

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.SyncType
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.ServerConnection
import com.sakethh.linkora.domain.repository.NetworkRepo
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.domain.repository.remote.SyncSessionService
import com.sakethh.linkora.platform.Network
import com.sakethh.linkora.platform.PlatformIODispatcher
import com.sakethh.linkora.utils.runSuspendCatching
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class SyncSessionServiceImpl(
    private val remoteSyncRepo: RemoteSyncRepo,
    private val networkRepo: NetworkRepo,
    private val network: Network,
    private val syncDispatcher: CoroutineDispatcher = PlatformIODispatcher,
) : SyncSessionService {

    private val _isBatchSyncRunning = MutableStateFlow(false)
    override val isBatchSyncRunning: StateFlow<Boolean> = _isBatchSyncRunning.asStateFlow()

    private val syncScope = CoroutineScope(SupervisorJob() + syncDispatcher)

    private class SyncSession

    private val sessions = MutableStateFlow<SyncSession?>(null)

    private fun <T> Flow<T>.forceMarkBatchSyncComplete() = onCompletion {
        _isBatchSyncRunning.value = false
    }

    override suspend fun start(
        lastSynced: Long,
        correlation: Correlation,
        serverConnection: ServerConnection,
        bypassCertCheck: Boolean,
    ): Flow<SyncSessionService.SyncState> {
        syncScope.coroutineContext.cancelChildren()

        val currentSession = SyncSession()
        sessions.value = currentSession

        val syncType = serverConnection.syncType
        val canPush = syncType == SyncType.TwoWay || syncType == SyncType.ClientToServer
        val canPull = syncType == SyncType.TwoWay || syncType == SyncType.ServerToClient

        if (serverConnection.serverUrl.isBlank()) {
            return flow {
                emit(SyncSessionService.SyncState.InvalidServerConfiguration)
            }
                .forceMarkBatchSyncComplete()
        }

        _isBatchSyncRunning.value = true

        runSuspendCatching {
            network.configureSyncServerClient(bypassCertCheck = bypassCertCheck)
        }
            .onFailure {
                it.printStackTrace()
                return flow {
                    emit(SyncSessionService.SyncState.InvalidSyncServerClientConfiguration)
                }
                    .forceMarkBatchSyncComplete()
            }

        val syncServerReachable = runSuspendCatching {
            networkRepo.isSyncServerReachable(
                serverUrl = serverConnection.serverUrl + SyncServerRoute.TEST_BEARER.name,
                token = serverConnection.authToken,
            )
        }
            .getOrElse { false }

        if (!syncServerReachable) {
            return flow {
                emit(SyncSessionService.SyncState.ConnectionFailed)
            }
                .forceMarkBatchSyncComplete()
        }

        val batchSyncFlow = channelFlow {
            val batchHadFailure = MutableStateFlow(false)
            val outerScope = this
            val syncJobs = mutableListOf<Job>()
            if (canPush) {
                launch {
                    runSuspendCatching {
                        with(remoteSyncRepo) {
                            channelFlow {
                                pushPendingSyncQueueToServer<Unit>().collect { result ->
                                    when (result) {
                                        is Result.Failure<*> -> {
                                            batchHadFailure.compareAndSet(
                                                expect = false,
                                                update = true,
                                            )
                                            result.e.printStackTrace()
                                            outerScope.send(
                                                SyncSessionService.SyncState.SyncOpFailed(
                                                    result.e,
                                                ),
                                            )
                                        }

                                        is Result.Loading<*> -> {
                                            outerScope.send(
                                                SyncSessionService.SyncState.SyncLog(
                                                    result.message,
                                                ),
                                            )
                                        }

                                        else -> Unit
                                    }
                                }
                            }
                                .collect()
                        }
                    }
                        .onFailure { e ->
                            batchHadFailure.compareAndSet(expect = false, update = true)
                            e.printStackTrace()
                            outerScope.send(
                                SyncSessionService.SyncState.SyncOpFailed(e),
                            )
                        }
                }
                    .also { job ->
                        syncJobs.add(job)
                    }
            }

            if (canPull) {
                launch {
                    runSuspendCatching {
                        remoteSyncRepo
                            .applyUpdatesBasedOnRemoteTombstones(lastSynced)
                            .collectAndSendState(
                                batchHadFailure = {
                                    batchHadFailure.compareAndSet(expect = false, update = true)
                                },
                            )
                    }
                        .onFailure { e ->
                            batchHadFailure.compareAndSet(expect = false, update = true)
                            e.printStackTrace()
                            send(SyncSessionService.SyncState.SyncOpFailed(e))
                        }
                }
                    .also { job ->
                        syncJobs.add(job)
                    }

                launch {
                    runSuspendCatching {
                        remoteSyncRepo
                            .applyUpdatesFromRemote(lastSynced)
                            .collectAndSendState(
                                batchHadFailure = {
                                    batchHadFailure.compareAndSet(expect = false, update = true)
                                },
                            )
                    }
                        .onFailure { e ->
                            batchHadFailure.compareAndSet(expect = false, update = true)
                            e.printStackTrace()
                            send(SyncSessionService.SyncState.SyncOpFailed(e))
                        }
                }
                    .also { job ->
                        syncJobs.add(job)
                    }
            }
            syncJobs.joinAll()
            if (batchHadFailure.value) {
                send(SyncSessionService.SyncState.SyncBatchHadFailure)
            } else {
                send(SyncSessionService.SyncState.SyncBatchSuccessful)
            }
        }
            .flowOn(syncDispatcher)
            .onCompletion {
                // WE DO NOT CARE ABOUT THE UPSTREAM OUTCOME
                // THIS CHECK MUST HAPPEN HERE SIMPLY BECAUSE WE CONSIDER THE "BATCH SYNC" IS NOT
                // RUNNING ANYMORE IF THE UPSTREAM RETURNS/COMPLETES
                if (currentSession === sessions.value) {
                    _isBatchSyncRunning.value = false
                }
            }

        val socketFlow = channelFlow {
            if (canPull) {
                launch {
                    runSuspendCatching {
                        remoteSyncRepo.readSocketEvents(correlation)
                    }
                        .onFailure { e ->
                            e.printStackTrace()
                            send(SyncSessionService.SyncState.SocketFailure(e))
                        }
                }
            }
        }

        return merge(batchSyncFlow, socketFlow)
            // we own the workers
            .shareIn(syncScope, started = SharingStarted.Eagerly, replay = 1)
    }

    override fun stop() {
        sessions.value = null
        syncScope.coroutineContext.cancelChildren()
        _isBatchSyncRunning.value = false
    }

    context(scope: ProducerScope<SyncSessionService.SyncState>)
    private suspend fun Flow<Result<Unit>>.collectAndSendState(batchHadFailure: () -> Unit) {
        this.collect { result ->
            when (result) {
                is Result.Failure<*> -> {
                    batchHadFailure()
                    scope.send(SyncSessionService.SyncState.SyncOpFailed(result.e))
                }

                is Result.Loading<*> -> {
                    scope.send(SyncSessionService.SyncState.SyncLog(result.message))
                }

                else -> Unit
            }
        }
    }
}
