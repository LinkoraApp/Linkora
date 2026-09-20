package com.sakethh.linkora.ui.components.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.sakethh.linkora.utils.highlightOnFocused

@Composable
fun SelectableFolderUIComponent(
    onClick: () -> Unit,
    folderName: String,
    imageVector: ImageVector,
    isComponentSelected: Boolean,
    forBtmSheetUI: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column {
        Row(
            modifier =
                Modifier
                    .highlightOnFocused()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                    .focusable(interactionSource = interactionSource)
                    .pointerHoverIcon(icon = PointerIcon.Hand)
                    .pressScaleEffect()
                    .fillMaxWidth()
                    .requiredHeight(75.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                tint =
                    if (isComponentSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor.current
                    },
                imageVector = imageVector,
                contentDescription = null,
                modifier =
                    Modifier
                        .padding(
                            end = 20.dp,
                            bottom = 20.dp,
                            top = if (forBtmSheetUI) 0.dp else 20.dp,
                        )
                        .size(28.dp),
            )

            Text(
                text = folderName,
                color =
                    if (isComponentSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        LocalContentColor.current
                    },
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                maxLines = if (forBtmSheetUI) 6 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .fillMaxWidth(if (isComponentSelected) 0.80f else 1f)
                        .padding(end = if (isComponentSelected) 10.dp else 0.dp),
            )

            if (isComponentSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(0.1f),
        )
    }
}
