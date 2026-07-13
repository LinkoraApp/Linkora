package com.sakethh.linkora

sealed interface Action {
    data object Minimize : Action

    data object LaunchWriteExternalStoragePermission : Action

    data class LaunchFileImport(
        val fileType: String,
    ) : Action

    data object ShowNotificationPermissionDialog : Action

    data object LaunchDirectoryPicker : Action
}
