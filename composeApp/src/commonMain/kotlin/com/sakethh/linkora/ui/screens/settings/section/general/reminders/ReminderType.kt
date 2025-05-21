package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.ui.graphics.vector.ImageVector

enum class ReminderType {
    ONCE {
        override val imgVector: ImageVector = Icons.Default.LooksOne
    },
    PERIODIC {
        override val imgVector: ImageVector = Icons.Default.Repeat
    },
    STICKY {
        override val imgVector: ImageVector = Icons.Default.Pin
    }, ;

    abstract val imgVector: ImageVector
}