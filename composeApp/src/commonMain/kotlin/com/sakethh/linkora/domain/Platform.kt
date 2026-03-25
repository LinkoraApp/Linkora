package com.sakethh.linkora.domain

import androidx.compose.runtime.Composable
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.utils.supportsWideDisplay

sealed interface Platform {
    data object Android : Platform {
        @Composable
        fun onMobile(): Boolean = platform == Android || !supportsWideDisplay()
    }

    data object Desktop : Platform

    data object Web : Platform
}
