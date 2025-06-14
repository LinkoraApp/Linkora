package com.sakethh.linkora.ui.components.folder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Checkbox
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
import com.sakethh.linkora.ui.screens.collections.ItemDivider
import com.sakethh.linkora.ui.utils.pulsateEffect
import com.sakethh.platform

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderComponent(folderComponentParam: FolderComponentParam) {
    Column(
        modifier = Modifier.fillMaxWidth().then(
            if (folderComponentParam.isSelectedForSelection.value) Modifier.background(
                MaterialTheme.colorScheme.primary.copy(0.25f)
            ) else Modifier
        )
            .then(
                if (platform() is Platform.Android.Mobile) Modifier else Modifier.background(
                    if (folderComponentParam.isCurrentlyInDetailsView.value) MaterialTheme.colorScheme.primary.copy(
                        0.25f
                    ) else Color.Transparent
                )
            ).animateContentSize()
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
            if (folderComponentParam.showCheckBox.value) {
                Checkbox(
                    checked = folderComponentParam.isSelectedForSelection.value, onCheckedChange = {
                        folderComponentParam.onCheckBoxChanged(it)
                    }, modifier = Modifier.padding(20.dp).size(28.dp)
                )
            } else {
                Icon(
                    imageVector = folderComponentParam.mainIcon,
                    contentDescription = null,
                    modifier = Modifier.padding(20.dp).size(28.dp)
                )
            }
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
                if (folderComponentParam.showMoreIcon.value && folderComponentParam.showCheckBox.value.not()) {
                    IconButton(onClick = { folderComponentParam.onMoreIconClick() }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = null
                        )
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = folderComponentParam.showCheckBox.value.not(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            ItemDivider(
                colorOpacity = 0.25f,
                thickness = 1.25.dp,
                paddingValues = PaddingValues(start = 25.dp, end = 25.dp)
            )
        }
    }
}