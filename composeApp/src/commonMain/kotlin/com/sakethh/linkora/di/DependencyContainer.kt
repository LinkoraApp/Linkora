package com.sakethh.linkora.di

import androidx.room3.useWriterConnection
import com.sakethh.linkora.data.ExportDataRepoImpl
import com.sakethh.linkora.data.ImportDataRepoImpl
import com.sakethh.linkora.data.LocalizationRepoImpl
import com.sakethh.linkora.data.local.repository.LocalDatabaseUtilsImpl
import com.sakethh.linkora.data.local.repository.LocalFoldersRepoImpl
import com.sakethh.linkora.data.local.repository.LocalLinksRepoImpl
import com.sakethh.linkora.data.local.repository.LocalMultiActionRepoImpl
import com.sakethh.linkora.data.local.repository.LocalPanelsRepoImpl
import com.sakethh.linkora.data.local.repository.LocalTagsRepoImpl
import com.sakethh.linkora.data.local.repository.PendingSyncQueueRepoImpl
import com.sakethh.linkora.data.local.repository.PreferencesImpl
import com.sakethh.linkora.data.local.repository.RefreshLinksRepoImpl
import com.sakethh.linkora.data.local.repository.SnapshotRepoImpl
import com.sakethh.linkora.data.remote.repository.GitHubReleasesRepoImpl
import com.sakethh.linkora.data.remote.repository.RemoteFoldersRepoImpl
import com.sakethh.linkora.data.remote.repository.RemoteLinksRepoImpl
import com.sakethh.linkora.data.remote.repository.RemoteMultiActionRepoImpl
import com.sakethh.linkora.data.remote.repository.RemotePanelsRepoImpl
import com.sakethh.linkora.data.remote.repository.RemoteTagsRepoImpl
import com.sakethh.linkora.data.remote.repository.sync.RemoteSyncRepoImpl
import com.sakethh.linkora.network.repository.NetworkRepoImpl
import com.sakethh.linkora.utils.canPushToServer
import kotlinx.coroutines.flow.map

object DependencyContainer {

    val preferencesRepo by lazy {
        PreferencesImpl(LinkoraSDK.getInstance().platformPreference)
    }

    val localizationRepo by lazy {
        LocalizationRepoImpl(
            standardClient = LinkoraSDK.getInstance().network.standardClient,
            localizationServerURL = {
                preferencesRepo.getPreferences().localizationServerURL
            },
            localizationDao = LinkoraSDK.getInstance().localDatabase.localizationDao
        )
    }

    val networkRepo by lazy {
        NetworkRepoImpl(syncServerClient = {
            LinkoraSDK.getInstance().network.getSyncServerClient()
        })
    }

    val remoteFoldersRepo by lazy {
        RemoteFoldersRepoImpl(
            syncServerClient = {
                LinkoraSDK.getInstance().network.getSyncServerClient()
            },
            baseUrl = { preferencesRepo.getPreferences().serverBaseUrl },
            authToken = { preferencesRepo.getPreferences().serverSecurityToken },
        )
    }

    val localDatabaseUtilsImpl by lazy {
        LocalDatabaseUtilsImpl(LinkoraSDK.getInstance().localDatabase)
    }

    val refreshLinksRepo by lazy {
        RefreshLinksRepoImpl(
            refreshLinkDao = LinkoraSDK.getInstance().localDatabase.refreshDao
        )
    }

    val remoteSyncRepo by lazy {
        RemoteSyncRepoImpl(
            localFoldersRepo = localFoldersRepo,
            localLinksRepo = localLinksRepo,
            localPanelsRepo = localPanelsRepo,
            authToken = {
                preferencesRepo.getPreferences().serverSecurityToken
            },
            baseUrl = {
                preferencesRepo.getPreferences().serverBaseUrl
            },
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            remoteFoldersRepo = remoteFoldersRepo,
            remoteLinksRepo = remoteLinksRepo,
            remotePanelsRepo = remotePanelsRepo,
            preferencesRepository = preferencesRepo,
            localMultiActionRepo = localMultiActionRepo,
            remoteMultiActionRepo = remoteMultiActionRepo,
            linksDao = LinkoraSDK.getInstance().localDatabase.linksDao,
            foldersDao = LinkoraSDK.getInstance().localDatabase.foldersDao,
            websocketScheme = {
                "wss"
            },
            localTagsRepo = localTagsRepo,
            remoteTagsRepo = remoteTagsRepo,
            tagsDao = LinkoraSDK.getInstance().localDatabase.tagsDao,
            localDatabaseUtilsRepo = localDatabaseUtilsImpl,
            network = LinkoraSDK.getInstance().network
        )
    }

    val pendingSyncQueueRepo by lazy {
        PendingSyncQueueRepoImpl(LinkoraSDK.getInstance().localDatabase.pendingSyncQueueDao)
    }
    val localFoldersRepo by lazy {
        LocalFoldersRepoImpl(
            foldersDao = LinkoraSDK.getInstance().localDatabase.foldersDao,
            remoteFoldersRepo = remoteFoldersRepo,
            localLinksRepo = localLinksRepo,
            localPanelsRepo = localPanelsRepo,
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            preferencesRepository = preferencesRepo,
            withWriterConnection = {
                LinkoraSDK.getInstance().localDatabase.useWriterConnection(it)
            })
    }

    val localLinksRepo by lazy {
        LocalLinksRepoImpl(
            linksDao = LinkoraSDK.getInstance().localDatabase.linksDao,
            primaryUserAgent = {
                preferencesRepo.getPreferences().primaryJsoupUserAgent
            },
            remoteLinksRepo = remoteLinksRepo,
            foldersDao = LinkoraSDK.getInstance().localDatabase.foldersDao,
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            preferencesRepository = preferencesRepo,
            standardClient = LinkoraSDK.getInstance().network.standardClient,
            tagsDao = LinkoraSDK.getInstance().localDatabase.tagsDao,
            proxyUrl = {
                preferencesRepo.getPreferences().proxyUrl
            }
        )
    }

    val remoteTagsRepo by lazy {
        RemoteTagsRepoImpl(syncServerClient = {
            LinkoraSDK.getInstance().network.getSyncServerClient()
        }, baseUrl = {
            preferencesRepo.getPreferences().serverBaseUrl
        }, authToken = {
            preferencesRepo.getPreferences().serverSecurityToken
        })
    }

    val localTagsRepo by lazy {
        LocalTagsRepoImpl(
            tagsDao = LinkoraSDK.getInstance().localDatabase.tagsDao,
            remoteTagsRepo = remoteTagsRepo,
            preferencesRepository = preferencesRepo,
            pendingSyncQueueRepo = pendingSyncQueueRepo
        )
    }

    val remoteLinksRepo by lazy {
        RemoteLinksRepoImpl(
            syncServerClient = { LinkoraSDK.getInstance().network.getSyncServerClient() },
            baseUrl = {
                preferencesRepo.getPreferences().serverBaseUrl
            },
            authToken = {
                preferencesRepo.getPreferences().serverSecurityToken
            })
    }

    val gitHubReleasesRepo by lazy {
        GitHubReleasesRepoImpl(standardClient = LinkoraSDK.getInstance().network.standardClient)
    }

    val remotePanelsRepo by lazy {
        RemotePanelsRepoImpl(
            syncServerClient = { LinkoraSDK.getInstance().network.getSyncServerClient() },
            baseUrl = {
                preferencesRepo.getPreferences().serverBaseUrl
            },
            authToken = {
                preferencesRepo.getPreferences().serverSecurityToken
            })
    }

    val localPanelsRepo by lazy {
        LocalPanelsRepoImpl(
            panelsDao = LinkoraSDK.getInstance().localDatabase.panelsDao,
            remotePanelsRepo = remotePanelsRepo,
            foldersDao = LinkoraSDK.getInstance().localDatabase.foldersDao,
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            preferencesRepository = preferencesRepo
        )
    }

    val exportDataRepo by lazy {
        ExportDataRepoImpl(localLinksRepo, localFoldersRepo, localPanelsRepo, localTagsRepo)
    }

    val importDataRepo by lazy {
        ImportDataRepoImpl(
            localLinksRepo,
            localFoldersRepo,
            localPanelsRepo,
            canPushToServer = {
                preferencesRepo.getPreferences().canPushToServer()
            },
            remoteSyncRepo = remoteSyncRepo,
            localTagsRepo = localTagsRepo,
            withWriterConnection = {
                LinkoraSDK.getInstance().localDatabase.useWriterConnection(it)
            })
    }

    private val remoteMultiActionRepo by lazy {
        RemoteMultiActionRepoImpl(
            syncServerClient = { LinkoraSDK.getInstance().network.getSyncServerClient() },
            baseUrl = {
                preferencesRepo.getPreferences().serverBaseUrl
            },
            authToken = {
                preferencesRepo.getPreferences().serverSecurityToken
            })
    }

    val localMultiActionRepo by lazy {
        LocalMultiActionRepoImpl(
            linksDao = LinkoraSDK.getInstance().localDatabase.linksDao,
            foldersDao = LinkoraSDK.getInstance().localDatabase.foldersDao,
            preferencesRepository = preferencesRepo,
            remoteMultiActionRepo = remoteMultiActionRepo,
            pendingSyncQueueRepo = pendingSyncQueueRepo,
            localFoldersRepo = localFoldersRepo,
            localTagsRepo = localTagsRepo,
            withWriterConnection = {
                LinkoraSDK.getInstance().localDatabase.useWriterConnection(it)
            })
    }

    val snapshotRepo by lazy {
        SnapshotRepoImpl(
            snapshotDao = LinkoraSDK.getInstance().localDatabase.snapshotDao,
            linksRepo = localLinksRepo,
            foldersRepo = localFoldersRepo,
            localPanelsRepo = localPanelsRepo,
            exportDataRepo = exportDataRepo,
            localTagsRepo = localTagsRepo,
            fileManager = LinkoraSDK.getInstance().fileManager,
            preferencesRepository = preferencesRepo
        )
    }
}