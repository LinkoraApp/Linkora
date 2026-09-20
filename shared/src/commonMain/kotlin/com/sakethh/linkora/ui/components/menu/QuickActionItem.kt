package com.sakethh.linkora.ui.components.menu

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.ui.LocalPlatform

@Composable
fun QuickActionItem(
    shape: Shape,
    modifier: Modifier,
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
) {
    var hasFocus by rememberSaveable {
        mutableStateOf(false)
    }
    val onAndroidTV = LocalPlatform.current is Platform.Android.TV
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        onClick = onClick,
        modifier =
            modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(start = 2.5.dp, end = 2.5.dp)
                .onFocusChanged { focusState ->
                    hasFocus = focusState.hasFocus
                }
                .then(
                    if (hasFocus && onAndroidTV) {
                        Modifier.border(
                            width = 4.5.dp,
                            color = MaterialTheme.colorScheme.inversePrimary,
                            shape
                        )
                    } else {
                        Modifier
                    }
                ),
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                tint = MaterialTheme.colorScheme.onPrimary,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
