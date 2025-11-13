package com.sakethh.linkora.ui.domain.model


data class LinkPref(
    val title: String, val onClick: () -> Unit, val isSwitchChecked: () -> Boolean
)