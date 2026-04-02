package com.sakethh.linkora.domain

sealed interface PreferenceKey<T> {
    val key: String
    data class BooleanPreferencesKey(
        override val key: String,
    ) : PreferenceKey<Boolean>

    data class LongPreferencesKey(
        override val key: String,
    ) : PreferenceKey<Long>

    data class IntPreferencesKey(
        override val key: String,
    ) : PreferenceKey<Int>

    data class StringPreferencesKey(
        override val key: String,
    ) : PreferenceKey<String>
}