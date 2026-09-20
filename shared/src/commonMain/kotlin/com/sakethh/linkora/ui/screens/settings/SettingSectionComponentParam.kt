package com.sakethh.linkora.ui.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Stable
data class SettingSectionComponentParam(
    val onClick: () -> Unit,
    val sectionTitle: String,
    val sectionIcon: ImageVector,
    val shouldArrowIconAppear: Boolean = true,
    val fontSize: TextUnit = 20.sp,
    val textStyle: @Composable () -> TextStyle = {
        MaterialTheme.typography.titleMedium
    },
    val bottomSpacing: Dp = 10.dp,
)
