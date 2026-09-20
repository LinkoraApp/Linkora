package com.sakethh.linkora.ui.utils

import com.sakethh.linkora.platform.platformSpecificLogging

fun linkoraLog(value: Any?) {
    platformSpecificLogging(value.toString())
}
