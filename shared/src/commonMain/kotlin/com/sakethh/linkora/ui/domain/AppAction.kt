package com.sakethh.linkora.ui.domain

import com.sakethh.linkora.ui.domain.model.ServerConnection

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

    data class SaveServerConnectionAndSync(
        val serverConnection: ServerConnection,
        val timeStampAfter: suspend () -> Long,
        val onSyncStart: () -> Unit,
        val onCompletion: () -> Unit,
    ) : AppAction
}
