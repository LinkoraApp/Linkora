package com.sakethh.linkora.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WebSocketEvent(
    val operation: String,
    val payload: JsonElement,
)
