package com.sakethh.linkora.domain.model.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.UriHandler

@Stable

data class SettingComponentParam(
    val title: String,
    val doesDescriptionExists: Boolean,
    val description: String?,
    val isSwitchNeeded: Boolean,
    val isSwitchEnabled: Boolean,
    val onSwitchStateChange: (newValue: Boolean) -> Unit,
    val onAcknowledgmentClick: (uriHandler: UriHandler) -> Unit = { },
    val icon: ImageVector? = null,
    val isIconNeeded: Boolean,
    val shouldFilledIconBeUsed: Boolean = false,
    val shouldArrowIconBeAppear: Boolean = false
)
