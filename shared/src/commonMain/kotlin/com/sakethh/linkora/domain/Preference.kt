package com.sakethh.linkora.domain

sealed class Preference<T>(
    val key: String,
    val readFromCurrentState: (currAppPref: AppPreferences) -> T,
    val writeToCurrentState: (currAppPref: AppPreferences, newValue: T) -> AppPreferences,
) {
    class BooleanKey(
        key: String,
        readFromCurrentState: (AppPreferences) -> Boolean,
        writeToCurrentState: (AppPreferences, Boolean) -> AppPreferences,
    ) : Preference<Boolean>(key, readFromCurrentState, writeToCurrentState)

    class IntKey(
        key: String,
        readFromCurrentState: (AppPreferences) -> Int,
        writeToCurrentState: (AppPreferences, Int) -> AppPreferences,
    ) : Preference<Int>(key, readFromCurrentState, writeToCurrentState)

    class LongKey(
        key: String,
        readFromCurrentState: (AppPreferences) -> Long,
        writeToCurrentState: (AppPreferences, Long) -> AppPreferences,
    ) : Preference<Long>(key, readFromCurrentState, writeToCurrentState)

    class StringKey(
        key: String,
        readFromCurrentState: (AppPreferences) -> String,
        writeToCurrentState: (AppPreferences, String) -> AppPreferences,
    ) : Preference<String>(key, readFromCurrentState, writeToCurrentState)

    companion object {
        fun requiresDiskRead(key: String): Nothing =
            error(
                "$key is supposed to be read only from the disk, not from current state, and no implementation exists for it",
            )

        fun requiresDiskWrite(key: String): Nothing =
            error(
                "$key is supposed to be written only to the disk, not to current state, and no implementation exists for it",
            )
    }
}
