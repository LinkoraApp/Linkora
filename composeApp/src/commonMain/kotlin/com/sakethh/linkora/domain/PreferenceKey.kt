package com.sakethh.linkora.domain

sealed interface PreferenceKey<T> {
    data class BooleanPreferencesKey(
        val key: String,
    ) : PreferenceKey<Boolean>

    data class LongPreferencesKey(
        val key: String,
    ) : PreferenceKey<Long>

    data class IntPreferencesKey(
        val key: String,
    ) : PreferenceKey<Int>

    data class StringPreferencesKey(
        val key: String,
    ) : PreferenceKey<String>
}