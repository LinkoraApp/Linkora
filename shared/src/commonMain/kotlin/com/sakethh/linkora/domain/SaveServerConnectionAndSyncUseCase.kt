package com.sakethh.linkora.domain

import com.sakethh.linkora.domain.model.ServerConnection
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.SyncSessionService
import com.sakethh.linkora.utils.currentSavedServerConfig
import kotlinx.coroutines.flow.Flow

class SaveServerConnectionAndSyncUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val syncSessionService: SyncSessionService,
) {
    suspend operator fun invoke(
        lastSynced: Long,
        serverConnection: ServerConnection,
    ): Flow<SyncSessionService.SyncState> {
        syncSessionService.stop()

        preferencesRepository.writePreferenceToDisk(
            preference = AppPreferences.SERVER_URL,
            newValue = serverConnection.serverUrl,
        )

        preferencesRepository.writePreferenceToDisk(
            preference = AppPreferences.SERVER_AUTH_TOKEN,
            newValue = serverConnection.authToken,
        )

        preferencesRepository.writePreferenceToDisk(
            preference = AppPreferences.SERVER_SYNC_TYPE,
            newValue = serverConnection.syncType.name,
        )

        val preferences = preferencesRepository.getCurrentPreferencesState()

        return syncSessionService.start(
            lastSynced = lastSynced,
            correlation = preferences.correlation,
            serverConnection = preferences.currentSavedServerConfig(),
            bypassCertCheck = preferences.skipCertCheckForSync,
        )
    }
}
