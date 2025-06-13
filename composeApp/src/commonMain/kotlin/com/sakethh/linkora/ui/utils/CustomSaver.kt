package com.sakethh.linkora.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
inline fun <reified T : Any> rememberDeserializableObject(
    vararg inputs: Any?,
    noinline init: () -> T
): T {
    return rememberSaveable(inputs = inputs, saver = Saver(save = {
        Json.encodeToString(it)
    }, restore = {
        Json.decodeFromString<T>(it)
    }), init = init)
}

@Composable
inline fun <reified T> rememberDeserializableMutableObject(noinline init: () -> MutableState<T>): MutableState<T> {
    return rememberSaveable(saver = Saver(save = {
        Json.encodeToString(it.value)
    }, restore = {
        mutableStateOf(Json.decodeFromString<T>(it))
    }), init = init)
}

@Composable
fun <E : Enum<E>> rememberMutableEnum(
    `class`: Class<E>, init: () -> MutableState<E>
): MutableState<E> {
    return rememberSaveable(saver = Saver(save = {
        it.value.name
    }, restore = { restoredName ->
        mutableStateOf(`class`.enumConstants!!.first {
            it.name == restoredName
        })
    }), init = init)
}