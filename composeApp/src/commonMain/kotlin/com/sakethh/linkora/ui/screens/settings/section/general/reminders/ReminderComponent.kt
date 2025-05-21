package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EditNotifications
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.components.CoilImage

@Composable
fun ReminderComponent(
    link: Link,
    reminder: Reminder,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onUrlClick: (String) -> Unit
) {
    Card(modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 7.5.dp, bottom = 7.5.dp)) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(if (link.imgURL.isNotBlank()) 0.65f else 1f)
                ) {
                    Text(
                        modifier = Modifier.background(
                            color = MaterialTheme.colorScheme.primary.copy(0.1f),
                            shape = RoundedCornerShape(5.dp)
                        ).clip(RoundedCornerShape(5.dp)).clickable {
                            onUrlClick(link.url)
                        }.padding(5.dp),
                        text = link.url,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        textAlign = TextAlign.Start,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp
                    )
                }
                CoilImage(
                    modifier = Modifier.width(95.dp).height(60.dp).clip(RoundedCornerShape(15.dp)),
                    imgURL = link.imgURL,
                    userAgent = link.userAgent ?: AppPreferences.primaryJsoupUserAgent.value,
                    alignment = Alignment.Center
                )
            }
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = reminder.title, style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = reminder.description, style = MaterialTheme.typography.titleSmall
            )
            HorizontalDivider(modifier = Modifier.padding(top = 15.dp, bottom = 10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "reminder.scheduleInfo", style = MaterialTheme.typography.titleSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Default.EditNotifications, contentDescription = null
                        )
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null)
                    }
                }
            }
        }
    }
}