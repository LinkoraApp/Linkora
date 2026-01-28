package com.sakethh.linkora.ui.components.menu

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.ui.utils.pressScaleEffect

@Composable
fun RefreshMenuComponent(label: String, onClick: () -> Unit) {
    OutlinedButton(
        modifier = Modifier.pressScaleEffect()
            .pointerHoverIcon(icon = PointerIcon.Hand)
            .padding(start = 15.dp, end = 15.dp).fillMaxWidth(),
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Start,
            fontSize = 18.sp
        )
    }
}