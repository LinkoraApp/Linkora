package com.sakethh.linkora.ui.domain

sealed interface AppAction {
    data class CopySelectedItems(
        val folderId: Long,
        val onStart: () -> Unit,
        val onCompletion: () -> Unit,
    ) : AppAction

    data class MoveSelectedItems(
        val folderId: Long,
        val onStart: () -> Unit,
        val onCompletion: () -> Unit,
    ) : AppAction

    data class MarkSelectedFoldersAsRoot(
        val onStart: () -> Unit,
        val onCompletion: () -> Unit,
    ) : AppAction

    data class MarkSelectedItemsAsRegular(
        val onStart: () -> Unit,
        val onCompletion: () -> Unit,
    ) : AppAction

    data class ArchiveSelectedItems(
        val onStart: () -> Unit,
        val onCompletion: () -> Unit,
    ) : AppAction

    data class ForceDataSync(val onStart: () -> Unit, val onCompletion: () -> Unit) : AppAction
}
