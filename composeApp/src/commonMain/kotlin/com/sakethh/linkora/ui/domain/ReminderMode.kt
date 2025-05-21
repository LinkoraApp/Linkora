package com.sakethh.linkora.ui.domain;

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoNotDisturbOnTotalSilence
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.ui.graphics.vector.ImageVector

enum class ReminderMode {
    SILENT {
        override val imgVector: ImageVector = Icons.Default.DoNotDisturbOnTotalSilence
    },
    VIBRATE {
        override val imgVector: ImageVector = Icons.Default.Vibration
    },
    CRUCIAL {
        override val imgVector: ImageVector = Icons.Default.VolumeUp
    }, ;

    abstract val imgVector: ImageVector
}