package com.sakethh.linkora.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.pressScaleEffect

@Composable
fun SortingIconButton() {
    val coroutineScope = rememberCoroutineScope()
    IconButton(
        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).pressScaleEffect(),
        onClick = {
            coroutineScope.pushUIEvent(UIEvent.Type.ShowSortingBtmSheet)
        },
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Sort,
            contentDescription = null,
        )
    }
}
