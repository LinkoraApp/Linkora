package com.sakethh.linkora.ui.screens.settings.section.about

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.ui.utils.pressScaleEffect

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsAppInfoComponent(
    hasDescription: Boolean = true,
    description: String,
    icon: ImageVector?,
    title: String,
    onClick: () -> Unit,
    paddingValues: PaddingValues? = null,
) {
    if (hasDescription) {
        Text(
            text = description,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 16.sp,
            textAlign = TextAlign.Start,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 15.dp, end = 15.dp),
        )
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        modifier =
        Modifier.padding(
            top = paddingValues?.calculateTopPadding() ?: 20.dp,
            end = paddingValues?.calculateEndPadding(LayoutDirection.Ltr) ?: 20.dp,
            start = paddingValues?.calculateStartPadding(LayoutDirection.Ltr) ?: 20.dp,
        )
            .wrapContentHeight()
            .fillMaxWidth()
            .pointerHoverIcon(icon = PointerIcon.Hand)
            .combinedClickable(
                interactionSource =
                remember {
                    MutableInteractionSource()
                },
                indication = null,
                onClick = {
                    onClick()
                },
                onLongClick = {},
            )
            .pressScaleEffect(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    modifier = Modifier.padding(20.dp),
                    imageVector = icon,
                    contentDescription = null,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 16.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(end = 15.dp),
            )
        }
    }
}
