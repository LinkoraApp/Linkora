package com.sakethh.linkora.domain.repository.remote

import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.ServerConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SyncSessionService {

    /**
     * Can be used to check whether batch sync operations are currently running. Does not reflect
     * socket status.
     */
    val isBatchSyncRunning: StateFlow<Boolean>

    /**
     * Starts a sync session, initiating both batch syncing and event collection via WebSocket, and
     * returns a hot flow of sync states.
     *
     * The returned flow does not complete when batch syncing finishes. If you collect it,
     * collection suspends indefinitely until the session is explicitly cancelled via [stop] or a
     * new session via [start] again, which will cancel all the background sync works including the
     * WebSocket.
     *
     * Use [isBatchSyncRunning] to check whether batch sync operations are currently running. Use
     * the returned flow for sync logs and errors. For explicit completion outcomes, look for
     * [SyncState.SyncBatchHadFailure] and [SyncState.SyncBatchSuccessful] in the emitted
     * [SyncState].
     */
    suspend fun start(
        lastSynced: Long,
        correlation: Correlation,
        serverConnection: ServerConnection,
        bypassCertCheck: Boolean,
    ): Flow<SyncState>

    fun stop()

    sealed interface SyncState {
        data class SocketFailure(val throwable: Throwable) : SyncState

        data object InvalidServerConfiguration : SyncState

        data object InvalidSyncServerClientConfiguration : SyncState

        data object ConnectionFailed : SyncState

        data class SyncOpFailed(val throwable: Throwable) : SyncState

        data class SyncLog(val str: String) : SyncState

        data object SyncBatchSuccessful : SyncState

        data object SyncBatchHadFailure : SyncState
    }
}
