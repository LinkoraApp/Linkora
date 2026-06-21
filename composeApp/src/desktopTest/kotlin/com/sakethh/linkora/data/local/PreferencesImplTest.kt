package com.sakethh.linkora.data.local

import com.sakethh.linkora.data.local.repository.PreferencesImpl
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.platform.PlatformPreference
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Modifier
import kotlin.math.abs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PreferencesImplTest {

    private lateinit var platformPreference: PlatformPreference
    private lateinit var preferencesImpl: PreferencesImpl

    private val ignoredStateKeys = listOf(
        "SERVER_CORRELATION",
        "LAST_TIME_SYNCED_WITH_SERVER",
        "SHOULD_SHOW_ONBOARDING",
        "REFRESHED_LINKS_COUNT",
        "LAST_SELECTED_PANEL_ID",
        "IS_DATA_MIGRATION_COMPLETED_FROM_V9",
        "TOTAL_REMOTE_STRINGS",
        "REMOTE_STRINGS_LAST_UPDATED_ON",
        "SHELF_VISIBLE_STATE",
        "AUTO_CHECK_UPDATES",
        "SEND_CRASH_REPORTS",
        "SETTING_COMPONENT_DESCRIPTION_STATE",
        "CUSTOM_TABS",
        "SECONDARY_JSOUP_USER_AGENT",
        "CUSTOM_VERSION_APP_LABEL"
    )

    private val nonUpdatablePreferenceVariables = listOf(
        "correlation"
    )

    @BeforeTest
    fun setup() {
        clearAllMocks()
        platformPreference = mockk(relaxed = true)

        coEvery { platformPreference.readAllPreferences() } returns AppPreferences()

        preferencesImpl = PreferencesImpl(platformPreference)
    }

    @Test
    fun `every state-tracked variable in AppPreferences must have exactly one corresponding PreferenceKey`() {
        val dataClassProperties = AppPreferences::class.java.declaredFields
            .filter { !Modifier.isStatic(it.modifiers) }
            .filter { !it.name.startsWith("$") && it.name != "Companion" }
            .map { it.name }
            .filter { it !in nonUpdatablePreferenceVariables }

        val companion = AppPreferences.Companion
        val companionKeys = companion::class.java.declaredMethods
            .mapNotNull { method ->
                method.isAccessible = true
                try {
                    method.invoke(companion) as? PreferenceKey<*>
                } catch (e: Exception) {
                    null
                }
            }
            .filter { it.key !in ignoredStateKeys }

        val difference = abs(dataClassProperties.size - companionKeys.size)

        assertEquals(
            dataClassProperties.size, companionKeys.size,
            "FATAL MISMATCH: Data Class Variables: ${dataClassProperties.size}, Companion Object Keys: ${companionKeys.size}. Difference: $difference.\n\n" +
                    "Please apply the following rules:\n" +
                    "1. Include in 'nonUpdatablePreferenceVariables' if any value is only stored for once in the entirety and not at all changed, so it doesn't have any implementation in updation.\n" +
                    "2. Include in 'ignoredStateKeys' if the extra keys aren't meant for UI state or typical state across the app, or are just one-shot operations that don't need a variable in the dataclass."
        )
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `every PreferenceKey must be mapped in updateStateMemory to trigger a StateFlow mutation`() =
        runTest {
            val companion = AppPreferences.Companion
            val keys = companion::class.java.declaredMethods
                .mapNotNull { method ->
                    method.isAccessible = true
                    try {
                        method.invoke(companion) as? PreferenceKey<*>
                    } catch (e: Exception) {
                        null
                    }
                }
                .filter { it.key !in ignoredStateKeys }

            assertTrue(
                keys.isNotEmpty(),
                "Reflection failed to extract keys from Companion object."
            )

            val unmappedKeys = mutableListOf<String>()

            for (key in keys) {
                var isMapped = false

                try {
                    val baseState = preferencesImpl.getPreferences()
                    preferencesImpl.changePreferenceValue(
                        key as PreferenceKey<Any>,
                        generateTestValue(key, 1)
                    )
                    if (preferencesImpl.getPreferences() != baseState) isMapped = true
                } catch (e: Exception) {
                    isMapped = true
                }

                if (!isMapped) {
                    try {
                        val baseState = preferencesImpl.getPreferences()
                        preferencesImpl.changePreferenceValue(
                            key as PreferenceKey<Any>,
                            generateTestValue(key, 2)
                        )
                        if (preferencesImpl.getPreferences() != baseState) isMapped = true
                    } catch (e: Exception) {
                        isMapped = true
                    }
                }

                if (!isMapped) {
                    unmappedKeys.add(key.key)
                }
            }

            assertTrue(
                unmappedKeys.isEmpty(),
                "CRITICAL BUG: The following Keys are MISSING from the 'when' block in PreferencesImpl.updateStateMemory(), meaning their UI states will never update:\n" +
                        unmappedKeys.joinToString("\n")
            )
        }

    @Test
    fun `loadPersistedPreferences accurately delegates to disk reader and pushes result to Flow`() =
        runTest {
            val diskData =
                AppPreferences(useDarkTheme = false, primaryJsoupUserAgent = "DiskAgent123")
            coEvery { platformPreference.readAllPreferences() } returns diskData

            preferencesImpl.loadPersistedPreferences()

            assertEquals(diskData, preferencesImpl.getPreferences())
            assertEquals(diskData, preferencesImpl.preferencesAsFlow.value)

            coVerify(exactly = 1) { platformPreference.readAllPreferences() }
        }

    @Test
    fun `changePreferenceValue accurately delegates write operation to disk writer`() = runTest {
        preferencesImpl.changePreferenceValue(AppPreferences.DARK_THEME, false)

        coVerify(exactly = 1) {
            platformPreference.writePreferenceValue(AppPreferences.DARK_THEME, false)
        }
    }

    @Test
    fun `readPreferenceValue directly requests value from platform disk layer`() = runTest {
        coEvery { platformPreference.readPreferenceValue(AppPreferences.DARK_THEME) } returns false

        val result = preferencesImpl.readPreferenceValue(AppPreferences.DARK_THEME)

        assertEquals(false, result)
        coVerify(exactly = 1) { platformPreference.readPreferenceValue(AppPreferences.DARK_THEME) }
    }

    private fun generateTestValue(key: PreferenceKey<*>, variant: Int): Any {
        return when (key) {
            is PreferenceKey.BooleanPreferencesKey -> variant == 1
            is PreferenceKey.StringPreferencesKey -> "automated_test_string_$variant"
            is PreferenceKey.IntPreferencesKey -> variant * 100
            is PreferenceKey.LongPreferencesKey -> variant * 100L
        }
    }
}