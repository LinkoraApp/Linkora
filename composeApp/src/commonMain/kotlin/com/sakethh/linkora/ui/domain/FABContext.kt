package com.sakethh.linkora.ui.domain

import com.sakethh.linkora.domain.model.Folder

enum class FABContext {
    ADD_LINK_ONLY,
    REGULAR,
    HIDE
}

data class CurrentFABContext(
    val fabContext: FABContext, val currentFolder: Folder? = null
) {
    companion object {
        val ROOT = CurrentFABContext(fabContext = FABContext.REGULAR, currentFolder = null)
    }
}