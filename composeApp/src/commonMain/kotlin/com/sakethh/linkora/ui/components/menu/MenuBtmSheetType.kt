package com.sakethh.linkora.ui.components.menu

import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType.Link.ArchiveLink
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType.Link.FolderLink
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType.Link.HistoryLink
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType.Link.ImportantLink
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType.Link.SavedLink
import kotlinx.serialization.Serializable

@Serializable
sealed interface MenuBtmSheetType {
    @Serializable
    sealed interface Folder : MenuBtmSheetType {
        @Serializable data object RegularFolder : Folder

        @Serializable data object ArchiveFolder : Folder
    }

    @Serializable
    sealed interface Link : MenuBtmSheetType {
        @Serializable data object SavedLink : Link

        @Serializable data object FolderLink : Link

        @Serializable data object HistoryLink : Link

        @Serializable data object ImportantLink : Link

        @Serializable data object ArchiveLink : Link
    }
}

fun menuBtmSheetLinkEntries(): List<MenuBtmSheetType.Link> = listOf(SavedLink, FolderLink, HistoryLink, ImportantLink, ArchiveLink)

fun menuBtmSheetFolderEntries(): List<MenuBtmSheetType.Folder> = listOf(MenuBtmSheetType.Folder.RegularFolder, MenuBtmSheetType.Folder.ArchiveFolder)
