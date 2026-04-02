package com.sakethh.linkora.ui.domain.model

import com.sakethh.linkora.domain.SyncType

data class ServerConnection(
    val serverUrl: String,
    val webSocketScheme: String = "wss",
    val authToken: String,
    val syncType: SyncType
)
