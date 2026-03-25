package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.platform.PlatformPreference

class PreferencesImpl(
    private val platformPreference: PlatformPreference
) : PreferencesRepository {

    override suspend fun <T> changePreferenceValue(
        preferenceKey: PreferenceKey<T>, newValue: T
    ) {
        platformPreference.writePreferenceValue(
            preferenceKey = preferenceKey, newValue = newValue
        )
    }

    override suspend fun <T> readPreferenceValue(
        preferenceKey: PreferenceKey<T>,
    ): T? {
        return platformPreference.readPreferenceValue(
            preferenceKey = preferenceKey
        )
    }

}