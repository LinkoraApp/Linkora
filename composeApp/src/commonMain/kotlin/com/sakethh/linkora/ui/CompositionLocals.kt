package com.sakethh.linkora.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import com.sakethh.linkora.domain.Platform

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("LocalNavController isn't provided")
}

val LocalFabController = staticCompositionLocalOf<FabStateController> {
    error("LocalFabController isn't provided")
}

val LocalPlatform = compositionLocalOf<Platform> {
    error("LocalPlatform isn't provided")
}