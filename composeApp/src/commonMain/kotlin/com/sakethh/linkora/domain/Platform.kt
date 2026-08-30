package com.sakethh.linkora.domain

import androidx.compose.runtime.Composable
import com.sakethh.linkora.ui.LocalPlatform

sealed interface Platform {

    sealed interface Android : Platform {

        companion object {
            @Composable
            fun onMobile(): Boolean = LocalPlatform.current !is TV && LocalPlatform.current is Mobile

            @Composable
            fun onTV(): Boolean = LocalPlatform.current is TV
        }

        data object TV : Android

        data object Mobile : Android

        data object Tablet : Android
    }

    data object Desktop : Platform

    data object Web : Platform
}
