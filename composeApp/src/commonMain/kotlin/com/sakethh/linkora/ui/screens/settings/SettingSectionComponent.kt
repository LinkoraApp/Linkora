package com.sakethh.linkora.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.ui.utils.pressScaleEffect

@Composable
fun SettingSectionComponent(settingSectionComponentParam: SettingSectionComponentParam) {
    Column {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
            Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                .fillMaxWidth()
                .pressScaleEffect()
                .clickable(
                    interactionSource =
                    remember {
                        MutableInteractionSource()
                    },
                    onClick = {
                        settingSectionComponentParam.onClick()
                    },
                    indication = null,
                ),
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            FilledTonalIconButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                onClick = { settingSectionComponentParam.onClick() },
            ) {
                Icon(
                    imageVector = settingSectionComponentParam.sectionIcon,
                    contentDescription = null,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = settingSectionComponentParam.sectionTitle,
                style = settingSectionComponentParam.textStyle(),
                fontSize = settingSectionComponentParam.fontSize,
            )
            if (settingSectionComponentParam.shouldArrowIconAppear) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row {
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                            onClick = { settingSectionComponentParam.onClick() },
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                }
            }
        }
        if (settingSectionComponentParam.shouldArrowIconAppear) {
            Spacer(modifier = Modifier.height(settingSectionComponentParam.bottomSpacing))
        }
    }
}
