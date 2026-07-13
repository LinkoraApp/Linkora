package com.sakethh.linkora.ui.screens.settings.common.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PreferenceTextField(
    textFieldDescText: String,
    textFieldLabel: String,
    textFieldValue: String,
    onResetButtonClick: () -> Unit,
    onTextFieldValueChange: (String) -> Unit,
    onConfirmButtonClick: () -> Unit,
    focusRequester: FocusRequester,
    readonly: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 25.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.titleSmall) {
            OutlinedTextField(
                supportingText = {
                    Text(
                        text = textFieldDescText,
                        style = MaterialTheme.typography.titleSmall,
                        lineHeight = 18.sp,
                        modifier =
                        Modifier.padding(
                            top = 5.dp,
                            bottom = 5.dp,
                        ),
                    )
                },
                value = textFieldValue,
                onValueChange = {
                    onTextFieldValueChange(it)
                },
                readOnly = readonly,
                modifier = Modifier.fillMaxWidth(0.8f).focusRequester(focusRequester),
                label = {
                    Text(
                        text = textFieldLabel,
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FilledTonalIconToggleButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                checked = !readonly,
                onCheckedChange = {
                    onConfirmButtonClick()
                },
            ) {
                Icon(
                    imageVector = if (readonly) Icons.Default.Edit else Icons.Default.Check,
                    contentDescription = null,
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
            FilledTonalIconButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                onClick = onResetButtonClick,
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                )
            }
        }
    }
}
