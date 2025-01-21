package com.sakethh.linkora.common

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sakethh.linkora.common.network.Network
import com.sakethh.linkora.common.network.repository.NetworkRepoImpl
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.data.ExportDataRepoImpl
import com.sakethh.linkora.data.ImportDataRepoImpl
import com.sakethh.linkora.data.LocalizationRepoImpl
import com.sakethh.linkora.data.local.repository.LocalFoldersRepoImpl
import com.sakethh.linkora.data.local.repository.LocalLinksRepoImpl
import com.sakethh.linkora.data.local.repository.PanelsRepoImpl
import com.sakethh.linkora.data.local.repository.PreferencesImpl
import com.sakethh.linkora.data.remote.repository.GitHubReleasesRepoImpl
import com.sakethh.linkora.data.remote.repository.RemoteFoldersRepoImpl
import com.sakethh.localDatabase

object DependencyContainer {
    lateinit var dataStorePref: DataStore<Preferences>

    val preferencesRepo = lazy {
        PreferencesImpl(dataStorePref)
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

    val localFoldersRepo = lazy {
        LocalFoldersRepoImpl(
            foldersDao = localDatabase?.foldersDao!!,
            remoteFoldersRepo = remoteFoldersRepo.value,
            localLinksRepo = localLinksRepo.value,
            panelsRepo = panelsRepo.value
        )
    }

    val localLinksRepo = lazy {
        LocalLinksRepoImpl(
            linksDao = localDatabase?.linksDao!!,
            primaryUserAgent = {
                AppPreferences.primaryJsoupUserAgent.value
            },
            httpClient = Network.client
        )
    }

    val gitHubReleasesRepo = lazy {
        GitHubReleasesRepoImpl(Network.client)
    }

    val panelsRepo = lazy {
        PanelsRepoImpl(panelsDao = localDatabase?.panelsDao!!)
    }

    val exportDataRepo = lazy {
        ExportDataRepoImpl(localLinksRepo.value, localFoldersRepo.value, panelsRepo.value)
    }

    val importDataRepo = lazy {
        ImportDataRepoImpl(localLinksRepo.value, localFoldersRepo.value, panelsRepo.value)
    }
}