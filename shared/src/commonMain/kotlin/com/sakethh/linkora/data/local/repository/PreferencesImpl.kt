package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Preference
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.platform.PlatformPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PreferencesImpl(
    private val platformPreference: PlatformPreference,
) : PreferencesRepository {
    private val _preferences = MutableStateFlow(AppPreferences())
    override val currentPreferencesStateAsFlow: StateFlow<AppPreferences> =
        _preferences.asStateFlow()

    override fun getCurrentPreferencesState(): AppPreferences = _preferences.value

    override suspend fun readAllPreferencesFromDisk() {
        _preferences.value = platformPreference.readAllPreferencesFromDisk()
    }

    override suspend fun <T> writePreferenceToDisk(
        preference: Preference<T>,
        newValue: T,
    ) {
        platformPreference.writePreferenceToDisk(
            preference = preference,
            newValue = newValue,
        )
        updateStateMemory(preference, newValue)
    }

    override suspend fun <T> readPreferenceFromDisk(preference: Preference<T>): T? =
        platformPreference.readPreferenceFromDisk(
            preference = preference,
        )

    private fun <T> updateStateMemory(
        key: Preference<T>,
        newValue: T,
    ) {
        _preferences.update { currentPref ->
            runCatching {
                key.writeToCurrentState(currentPref, newValue)
            }
                .getOrElse { throwable ->
                    if (throwable is Preference.WriteToDiskOnlyException) {
                        currentPref
                    } else {
                        throw throwable
                    }
                }
        }
    }
}
