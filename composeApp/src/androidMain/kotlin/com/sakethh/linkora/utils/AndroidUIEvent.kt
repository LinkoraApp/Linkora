package com.sakethh.linkora.utils

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object AndroidUIEvent {
    private val _androidUIEventChannel = MutableSharedFlow<Type>()
    val androidUIEventChannel = _androidUIEventChannel.asSharedFlow()

    fun CoroutineScope.pushUIEvent(type: Type) {
        this.launch {
            _androidUIEventChannel.emit(type)
        }
    }

    suspend fun pushUIEvent(type: Type) {
        _androidUIEventChannel.emit(type)
    }

    sealed interface Type {
        data object ShowRuntimePermissionForStorage : Type

        data object ShowRuntimePermissionForNotifications : Type

        data class StoragePermissionGrantedForAndBelowQ(
            val isGranted: Boolean,
        ) : Type

        data class NotificationPermissionState(
            val isGranted: Boolean,
        ) : Type

        data class UriOfTheFileForImporting(
            val uri: Uri?,
        ) : Type

        data class ImportAFile(
            val fileType: String,
        ) : Type

        data object PickADirectory : Type

        data class PickedDirectory(
            val uri: Uri?,
        ) : Type
    }
}
