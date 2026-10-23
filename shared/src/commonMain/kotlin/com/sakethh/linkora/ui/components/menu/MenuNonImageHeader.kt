package com.sakethh.linkora.ui.components.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.ui.utils.pressScaleEffect

@ExperimentalFoundationApi
@Composable
fun MenuNonImageHeader(
    onClick: () -> Unit,
    leadingIcon: ImageVector,
    text: String,
) {
    Row(
        modifier =
            Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .pressScaleEffect()
                .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = leadingIcon,
            null,
            modifier = Modifier.padding(20.dp).size(28.dp),
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 20.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 20.sp,
        )
    }
}
