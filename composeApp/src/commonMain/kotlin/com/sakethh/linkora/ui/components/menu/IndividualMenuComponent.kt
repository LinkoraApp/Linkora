package com.sakethh.linkora.ui.components.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.utils.pressScaleEffect

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IndividualMenuComponent(
    onClick: () -> Unit,
    elementName: String,
    elementImageVector: ImageVector,
    inPanelsScreen: Boolean = false,
    isSelected: Boolean = false,
    onRenameClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .background(
                if (isSelected && platform() !is Platform.Android.Mobile) MaterialTheme.colorScheme.primary.copy(
                    0.1f
                ) else Color.Transparent
            )
            .pointerHoverIcon(icon = PointerIcon.Hand)
            .combinedClickable(
                interactionSource = null, indication = null,
                onClick = {
                    onClick()
                })
            .pressScaleEffect()
            .padding(end = 10.dp)
            .wrapContentHeight()
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(10.dp),
                onClick = { onClick() },
                colors = IconButtonDefaults.filledTonalIconButtonColors()
            ) {
                Icon(imageVector = elementImageVector, contentDescription = null)
            }
            Text(
                text = elementName,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(if (inPanelsScreen) 0.4f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (inPanelsScreen) {
            Row {
                IconButton(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                    onClick = onRenameClick
                ) {
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = null
                    )
                }

                IconButton(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                    onClick = onDeleteClick
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                }
            }
        }
    }
}