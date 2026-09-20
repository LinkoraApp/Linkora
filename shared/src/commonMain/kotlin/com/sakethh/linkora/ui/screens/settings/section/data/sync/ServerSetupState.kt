package com.sakethh.linkora.ui.screens.settings.section.data.sync

data class ServerSetupState(
    val isConnecting: Boolean,
    val isConnectedSuccessfully: Boolean,
    val isError: Boolean,
)
