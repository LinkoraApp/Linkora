package com.sakethh.linkora.ui.utils

import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

/**
 * There's also `AndroidUIEvent`, which I initially created for handling Android-specific APIs and
 * behavior. But now, some of that functionality isn't strictly Android-specific and can be managed
 * in common code. So this setup feels fine for now. Even though there are two different event
 * objects—one for common and one for Android-specific cases— I could eventually merge them into
 * this single `UIEvent` object. For now, it's not a problem.
 */
object UIEvent {

    // MutableSharedFlow with proper capacity and replay will also work in this case
    private val _uiEvents = Channel<Type>(capacity = Channel.UNLIMITED)
    val uiEvents =
        _uiEvents
            .receiveAsFlow()
            .shareIn(
                scope = CoroutineScope(Dispatchers.Default),
                started = SharingStarted.WhileSubscribed(),
            )

    suspend fun pushUIEvent(type: Type) {
        _uiEvents.send(type)
    }

    fun CoroutineScope.pushUIEvent(type: Type) {
        this.launch {
            _uiEvents.send(type)
        }
    }

    suspend fun String.pushLocalizedSnackbar(append: String = "") {
        _uiEvents.send(Type.ShowSnackbar(this + append))
    }

    sealed interface Type {
        data class ShowSnackbar(
            val message: String,
        ) : Type

        data object ShowAddANewLinkDialogBox : Type

        data object ShowAddANewFolderDialogBox : Type

        data class ShowLinkMenuBtmSheet(
            val selectedLinkTagsPair: LinkTagsPair,
        ) : Type

        data class ShowFolderMenuBtmSheet(
            val selectedFolder: Folder,
        ) : Type

        data class ShowTagMenuBtmSheet(
            val selectedTag: Tag,
        ) : Type

        data object ShowCreateTagBtmSheet : Type

        data object ShowSortingBtmSheet : Type

        data object ShowDeleteLinkDialogBox : Type

        data object ShowDeleteAllSelectedDialogBox : Type

        data object ShowDeleteFolderDialogBox : Type

        data object ShowRenameLinkDialogBox : Type

        data object ShowRenameFolderDialogBox : Type

        data object MinimizeTheApp : Type
    }
}
