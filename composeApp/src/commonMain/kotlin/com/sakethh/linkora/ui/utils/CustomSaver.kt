package com.sakethh.linkora.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
inline fun <reified T> rememberDeserializableMutableObject(noinline init: () -> MutableState<T>): MutableState<T> {
    return rememberSaveable(saver = Saver(save = {
        Json.encodeToString(it.value)
    }, restore = {
        mutableStateOf(Json.decodeFromString<T>(it))
    }), init = init)
}
