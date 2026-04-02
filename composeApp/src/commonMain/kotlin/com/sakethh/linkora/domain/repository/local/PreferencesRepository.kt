package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.PreferenceKey
import kotlinx.coroutines.flow.StateFlow


/* This looks like an additional layer since `PlatformPreference` already
implements the logic. It's mainly helpful for tests so we can just swap in
dummy data, but otherwise I don't see a useful use case for this extra
`PreferencesRepository`. It's not required, but to keep things similar to
other repos, I'm leaving it as-is.*/
interface PreferencesRepository {

    val preferencesAsFlow: StateFlow<AppPreferences>

    fun getPreferences(): AppPreferences

    suspend fun loadPersistedPreferences()

    suspend fun <T> changePreferenceValue(
        preferenceKey: PreferenceKey<T>,
        newValue: T,
    )

    suspend fun <T> readPreferenceValue(
        preferenceKey: PreferenceKey<T>,
    ): T?
}