package com.sakethh.linkora.ui.components.folder

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.ui.domain.model.FolderComponentParam
import com.sakethh.linkora.ui.utils.pulsateEffect
import com.sakethh.platform

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderComponent(folderComponentParam: FolderComponentParam) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (platform() is Platform.Android.Mobile) Modifier else Modifier.background(
                    if (folderComponentParam.isCurrentlyInDetailsView.value) MaterialTheme.colorScheme.primary.copy(
                        0.25f
                    ) else Color.Transparent
                )
            )
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    interactionSource = remember {
                        MutableInteractionSource()
                    }, indication = null,
                    onClick = {
                        folderComponentParam.onClick()
                    },
                    onLongClick = {
                        folderComponentParam.onLongClick()
                    })
                .pulsateEffect()
                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier
                    .padding(20.dp)
                    .size(28.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth(if (folderComponentParam.showMoreIcon.value) 0.80f else 1f),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = folderComponentParam.folder.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(
                        end = if (folderComponentParam.showMoreIcon.value) 0.dp else 20.dp
                    ),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (!folderComponentParam.showMoreIcon.value) 20.sp else TextUnit.Unspecified
                )
                if (folderComponentParam.folder.note.isNotEmpty()) {
                    Text(
                        text = folderComponentParam.folder.note,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (platform() == Platform.Android.Mobile) 15.dp else 0.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (folderComponentParam.showMoreIcon.value) {
                    IconButton(onClick = { folderComponentParam.onMoreIconClick() }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 25.dp, end = 25.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(0.25f)
        )
    }
}