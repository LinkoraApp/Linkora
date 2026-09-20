package com.sakethh.linkora.utils

import kotlinx.serialization.json.Json

object Utils {
    val json = Json {
        this.ignoreUnknownKeys = true
        this.encodeDefaults = true
        this.isLenient = true
        this.prettyPrint = true
    }
}
