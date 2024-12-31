package com.sakethh

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.Platform
import com.sakethh.linkora.data.local.LocalDatabase
import kotlinx.coroutines.Dispatchers
import java.io.File

actual val showFollowSystemThemeOption: Boolean = true
actual val BUILD_FLAVOUR: String = "desktop"
actual val platform: @Composable () -> Platform = {
    Platform.Desktop
}
actual val localDatabase: LocalDatabase? =
    File(System.getProperty("java.io.tmpdir"), "${LocalDatabase.NAME}.db").run {
        Room.databaseBuilder<LocalDatabase>(name = this.absolutePath).setDriver(BundledSQLiteDriver()).build()
    }

actual val poppinsFontFamily: FontFamily = com.sakethh.linkora.ui.theme.poppinsFontFamily
actual val showDynamicThemingOption: Boolean = false
