package com.sakethh.linkora.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DataEmptyScreen(
    text: String,
    paddingValues: PaddingValues = PaddingValues(top = 75.dp, start = 15.dp),
) {
    Box(
        modifier = Modifier.padding(paddingValues).fillMaxSize(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 32.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxSize().padding(end = 50.dp),
            )
        }
    }
}
