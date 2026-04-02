package com.sakethh.linkora.ui.screens.settings.common.composables

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.ui.utils.pressScaleEffect

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingComponent(
    settingComponentParam: SettingComponentParam
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
            .combinedClickable(
                interactionSource = remember {
                    MutableInteractionSource()
                }, indication = null,
                onClick = {
                    settingComponentParam.onSwitchStateChange(!settingComponentParam.isSwitchEnabled)
                    settingComponentParam.onAcknowledgmentClick(uriHandler)
                })
            .pressScaleEffect()
            .fillMaxWidth()
            .animateContentSize(), verticalAlignment = Alignment.CenterVertically
    ) {
        if (settingComponentParam.isIconNeeded && settingComponentParam.icon != null) {
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                colors = if (settingComponentParam.shouldFilledIconBeUsed) IconButtonDefaults.filledTonalIconButtonColors() else IconButtonDefaults.iconButtonColors(),
                onClick = { settingComponentParam.onSwitchStateChange(!settingComponentParam.isSwitchEnabled) }) {
                Icon(
                    imageVector = settingComponentParam.icon,
                    contentDescription = null,
                    modifier = if (settingComponentParam.title == Localization.Key.TopDecoratorSetting.getLocalizedString()) Modifier.rotate(
                        180f
                    ) else Modifier
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column {
            Text(
                text = rememberSaveable(settingComponentParam.title) {
                    settingComponentParam.title
                },
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth(if (settingComponentParam.shouldArrowIconBeAppear || settingComponentParam.isSwitchNeeded) 0.75f else 1f)
                    .padding(
                        start = if (settingComponentParam.isIconNeeded) 0.dp else 15.dp,
                        end = if (!settingComponentParam.isSwitchNeeded) 25.dp else 0.dp
                    ),
                lineHeight = 20.sp
            )
            if (settingComponentParam.doesDescriptionExists) {
                Text(
                    text = rememberSaveable(settingComponentParam.description) {
                        settingComponentParam.description ?: ""
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth(if (settingComponentParam.shouldArrowIconBeAppear || settingComponentParam.isSwitchNeeded) 0.75f else 1f)
                        .padding(
                            start = if (settingComponentParam.isIconNeeded) 0.dp else 15.dp,
                            top = 10.dp,
                            end = if (!settingComponentParam.isSwitchNeeded) 25.dp else 15.dp
                        )
                )
            }
        }
        if (settingComponentParam.isSwitchNeeded) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Switch(
                    modifier = Modifier
                        .padding(end = 15.dp),
                    checked = settingComponentParam.isSwitchEnabled,
                    onCheckedChange = {
                        settingComponentParam.onSwitchStateChange(it)
                    })
            }
        }
        if (settingComponentParam.shouldArrowIconBeAppear) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                IconButton(modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand), onClick = {
                    settingComponentParam.onAcknowledgmentClick(
                        uriHandler
                    )
                }) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null
                    )
                }
            }
        }
    }
}