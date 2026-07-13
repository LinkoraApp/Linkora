package com.sakethh.linkora.ui.screens.settings.section.data.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.domain.ComposableContent

@Composable
fun ToggleButton(
    shape: Shape,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: ComposableContent,
) {
    Box(
        modifier =
        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
            .clip(shape)
            .clickable { onCheckedChange(!checked) }
            .background(MaterialTheme.colorScheme.primary.copy(if (checked) 1f else 0.125f))
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
