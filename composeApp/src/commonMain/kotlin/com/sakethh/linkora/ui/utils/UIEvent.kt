package com.sakethh.linkora.ui.utils

import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.utils.getLocalizedString
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
There's also `AndroidUIEvent`, meant for stuff between `actual` Android implementations and related components, like MainActivity in the android module, to handle Android-specific stuff.
This object has nothing to do with that. This one is for common use cases.
 **/
object UIEvent {
    private val _uiEvents =
        MutableSharedFlow<Type>() // `StateFlow` won't emit the same value again, so to make sure we're playing it safe, `SharedFlow` is the way 🤪
    val uiEvents = _uiEvents.asSharedFlow()

    suspend fun pushUIEvent(type: Type) {
        _uiEvents.emit(type)
    }

    fun CoroutineScope.pushUIEvent(type: Type) {
        this.launch {
            _uiEvents.emit(type)
        }
    }

    suspend fun Localization.Key.pushLocalizedSnackbar(append: String = "") {
        _uiEvents.emit(Type.ShowSnackbar(this.getLocalizedString() + append))
    }

    sealed interface Type {
        data class ShowSnackbar(val message: String) : Type

        data object ShowAddANewLinkDialogBox : Type

        data object ShowAddANewFolderDialogBox : Type

        data class ShowMenuBtmSheetUI(
            val menuBtmSheetFor: MenuBtmSheetType,
            val selectedLinkForMenuBtmSheet: Link?,
            val selectedFolderForMenuBtmSheet: Folder?
        ) : Type

        data object ShowSortingBtmSheetUI : Type

        data object ShowDeleteDialogBox : Type

        data object ShowRenameDialogBox : Type

        data object MinimizeTheApp : Type

        data object Nothing : Type
    }

}