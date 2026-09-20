package com.sakethh.linkora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VerticalInfoCard(
    info: String,
    paddingValues: PaddingValues = PaddingValues(start = 20.dp, end = 20.dp, top = 15.dp),
) {
    Card(
        border =
        BorderStroke(
            1.dp,
            contentColorFor(MaterialTheme.colorScheme.surface),
        ),
        colors = CardDefaults.cardColors(containerColor = AlertDialogDefaults.containerColor),
        modifier = Modifier.fillMaxWidth().padding(paddingValues),
    ) {
        Column(
            modifier =
            Modifier.fillMaxWidth()
                .wrapContentHeight()
                .padding(
                    10.dp,
                ),
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier =
                Modifier.padding(
                    bottom = 10.dp,
                ),
            )
            Text(
                text = info,
                style = MaterialTheme.typography.titleSmall,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
    }
}
