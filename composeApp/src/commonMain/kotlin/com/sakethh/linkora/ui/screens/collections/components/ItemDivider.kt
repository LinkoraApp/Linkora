package com.sakethh.linkora.ui.screens.collections.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.platform.platform

@Composable
fun ItemDivider(
    colorOpacity: Float = 0.35f,
    thickness: Dp = 1.5.dp,
    paddingValues: PaddingValues = PaddingValues(
        top = 15.dp, start = 20.dp, end = if (Platform.Android.onMobile()) 20.dp else 5.dp
    ),
    color: Color = MaterialTheme.colorScheme.outline
) {
    HorizontalDivider(
        modifier = Modifier.padding(
            paddingValues
        ).clip(RoundedCornerShape(25.dp)), thickness = thickness, color = color.copy(colorOpacity)
    )
}