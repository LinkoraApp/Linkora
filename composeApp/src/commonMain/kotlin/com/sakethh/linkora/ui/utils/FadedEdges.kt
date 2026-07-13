package com.sakethh.linkora.ui.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

enum class EdgeType {
    BOTTOM,
    TOP,
}

fun Modifier.fadedEdges(
    colorScheme: ColorScheme,
    edgeType: EdgeType = EdgeType.BOTTOM,
): Modifier = this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
    drawContent()
    drawRect(
        Brush.verticalGradient(
            if (edgeType == EdgeType.BOTTOM) {
                listOf(
                    colorScheme.surface,
                    Color.Transparent,
                )
            } else {
                listOf(
                    Color.Transparent,
                    colorScheme.surface,
                )
            },
        ),
        blendMode = BlendMode.DstIn,
    )
}
