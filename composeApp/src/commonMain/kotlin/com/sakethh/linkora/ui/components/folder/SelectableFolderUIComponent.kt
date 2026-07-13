package com.sakethh.linkora.ui.components.folder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.ui.utils.pressScaleEffect

@Composable
fun SelectableFolderUIComponent(
    onClick: () -> Unit,
    folderName: String,
    imageVector: ImageVector,
    isComponentSelected: Boolean,
    forBtmSheetUI: Boolean = false,
) {
    val componentSelectedState =
        rememberSaveable(inputs = arrayOf(isComponentSelected)) {
            mutableStateOf(isComponentSelected)
        }
    val forBtmSheetUIState =
        rememberSaveable(inputs = arrayOf(forBtmSheetUI)) {
            mutableStateOf(forBtmSheetUI)
        }
    Column {
        Row(
            modifier =
            Modifier.pressScaleEffect()
                .clickable(onClick = onClick, indication = null, interactionSource = null)
                .pointerHoverIcon(icon = PointerIcon.Hand)
                .fillMaxWidth()
                .requiredHeight(75.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                tint =
                if (componentSelectedState.value) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
                imageVector = imageVector,
                contentDescription = null,
                modifier =
                Modifier.padding(
                    end = 20.dp,
                    bottom = 20.dp,
                    top = if (forBtmSheetUIState.value) 0.dp else 20.dp,
                )
                    .size(28.dp),
            )
            Text(
                text = folderName,
                color =
                if (componentSelectedState.value) {
                    MaterialTheme.colorScheme.primary
                } else {
                    LocalContentColor.current
                },
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                maxLines = if (forBtmSheetUIState.value) 6 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.80f),
            )
            if (componentSelectedState.value) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                        tint =
                        if (componentSelectedState.value) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            LocalContentColor.current
                        },
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(0.1f),
        )
    }
}
