package com.sakethh.linkora.common.utils

fun isAValidURL(webURL: String): Boolean {
    return if (webURL.isBlank() || webURL.isEmpty()) {
        false
    } else {
        try {
            webURL.split("/")[2]
            true
        } catch (_: Exception) {
            false
        }
    }
}