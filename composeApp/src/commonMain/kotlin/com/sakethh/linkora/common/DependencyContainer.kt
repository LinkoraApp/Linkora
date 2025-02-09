package com.sakethh.linkora.common

import com.sakethh.linkora.common.network.Network
import com.sakethh.linkora.common.network.repository.NetworkRepoImpl
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.data.ExportDataRepoImpl
import com.sakethh.linkora.data.ImportDataRepoImpl
import com.sakethh.linkora.data.LocalizationRepoImpl
import com.sakethh.linkora.data.local.repository.LocalFoldersRepoImpl
import com.sakethh.linkora.data.local.repository.LocalLinksRepoImpl
import com.sakethh.linkora.data.local.repository.LocalPanelsRepoImpl
import com.sakethh.linkora.data.local.repository.PendingSyncQueueRepoImpl
import com.sakethh.linkora.data.local.repository.PreferencesImpl
import com.sakethh.linkora.data.remote.repository.GitHubReleasesRepoImpl
import com.sakethh.linkora.data.remote.repository.RemoteFoldersRepoImpl
import com.sakethh.linkora.data.remote.repository.RemoteLinksRepoImpl
import com.sakethh.linkora.data.remote.repository.RemotePanelsRepoImpl
import com.sakethh.linkora.data.remote.repository.RemoteSyncRepoImpl
import com.sakethh.linkoraDataStore
import com.sakethh.localDatabase

object DependencyContainer {

    val preferencesRepo = lazy {
        PreferencesImpl(linkoraDataStore)
    }

    val localizationRepo = lazy {
        LocalizationRepoImpl(
            Network.client, {
                AppPreferences.localizationServerURL.value
            },
            localDatabase!!.localizationDao
        )
    }

    val networkRepo = lazy {
        NetworkRepoImpl(Network.client)
    }

    val remoteFoldersRepo = lazy {
        RemoteFoldersRepoImpl(
            Network.client,
            baseUrl = { AppPreferences.serverBaseUrl.value },
            authToken = { AppPreferences.serverSecurityToken.value },
        )
    }

    val remoteSyncRepo = lazy {
        RemoteSyncRepoImpl(
            localFoldersRepo = localFoldersRepo.value,
            localLinksRepo = localLinksRepo.value,
            localPanelsRepo = localPanelsRepo.value,
            authToken = {
                AppPreferences.serverSecurityToken.value
            },
            baseUrl = {
                AppPreferences.serverBaseUrl.value
            },
            pendingSyncQueueRepo = pendingSyncQueueRepo.value,
            remoteFoldersRepo = remoteFoldersRepo.value,
            remoteLinksRepo = remoteLinksRepo.value,
            remotePanelsRepo = remotePanelsRepo.value,
            preferencesRepository = preferencesRepo.value
        )
    }
    val pendingSyncQueueRepo = lazy {
        PendingSyncQueueRepoImpl(localDatabase?.pendingSyncQueueDao!!)
    }
    val localFoldersRepo = lazy {
        LocalFoldersRepoImpl(
            foldersDao = localDatabase?.foldersDao!!,
            remoteFoldersRepo = remoteFoldersRepo.value,
            localLinksRepo = localLinksRepo.value, localPanelsRepo = localPanelsRepo.value,
            pendingSyncQueueRepo = pendingSyncQueueRepo.value,
            preferencesRepository = preferencesRepo.value
        )
    }

    val localLinksRepo = lazy {
        LocalLinksRepoImpl(
            linksDao = localDatabase?.linksDao!!,
            primaryUserAgent = {
                AppPreferences.primaryJsoupUserAgent.value
            },
            httpClient = Network.client,
            remoteLinksRepo = remoteLinksRepo.value,
            foldersDao = localDatabase?.foldersDao!!,
            pendingSyncQueueRepo = pendingSyncQueueRepo.value,
            preferencesRepository = preferencesRepo.value
        )
    }

    val remoteLinksRepo = lazy {
        RemoteLinksRepoImpl(httpClient = Network.client, baseUrl = {
            AppPreferences.serverBaseUrl.value
        }, authToken = {
            AppPreferences.serverSecurityToken.value
        })
    }

    val gitHubReleasesRepo = lazy {
        GitHubReleasesRepoImpl(Network.client)
    }

    val remotePanelsRepo = lazy {
        RemotePanelsRepoImpl(Network.client, baseUrl = {
            AppPreferences.serverBaseUrl.value
        }, authToken = {
            AppPreferences.serverSecurityToken.value
        })
    }

    val localPanelsRepo = lazy {
        LocalPanelsRepoImpl(
            panelsDao = localDatabase?.panelsDao!!,
            remotePanelsRepo = remotePanelsRepo.value,
            foldersDao = localDatabase?.foldersDao!!,
            pendingSyncQueueRepo = pendingSyncQueueRepo.value,
            preferencesRepository = preferencesRepo.value
        )
    }

    val exportDataRepo = lazy {
        ExportDataRepoImpl(localLinksRepo.value, localFoldersRepo.value, localPanelsRepo.value)
    }

    val importDataRepo = lazy {
        ImportDataRepoImpl(
            localLinksRepo.value, localFoldersRepo.value, localPanelsRepo.value, canPushToServer = {
                AppPreferences.canPushToServer()
            }, remoteSyncRepo = remoteSyncRepo.value
        )
    }
}