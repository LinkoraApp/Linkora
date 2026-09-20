package com.sakethh.linkora.ui.screens.settings.section.about

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LodingDialogBox(
    shouldDialogBoxAppear: MutableState<Boolean>,
    text: String,
) {
    if (shouldDialogBoxAppear.value) {
        BasicAlertDialog(
            onDismissRequest = {},
            modifier =
            Modifier.clip(RoundedCornerShape(20.dp))
                .background(AlertDialogDefaults.containerColor)
                .border(
                    1.dp,
                    AlertDialogDefaults.iconContentColor.copy(0.5f),
                    RoundedCornerShape(20.dp),
                ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Spacer(Modifier.width(20.dp))
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(5.dp))
                Text(
                    text = text,
                    color = AlertDialogDefaults.textContentColor,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 20.sp,
                    lineHeight = 28.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth().then(Modifier.padding(20.dp)).fillMaxWidth(),
                )
            }
        }
    }
}
