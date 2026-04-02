package com.sakethh.linkora.ui.components.menu

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.ui.components.CoilImage
import com.sakethh.linkora.ui.components.InfoCard
import com.sakethh.linkora.ui.components.link.TagsRow
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.utils.rememberLocalizedString

@Composable
fun NonMobileMenu(
    preferences: AppPreferences,
    menuBtmSheetParam: MenuBtmSheetParam,
    currentLinkTagsPair: LinkTagsPair,
    currentFolder: Folder?,
    commonMenuContent: ComposableContent
) {
    val menuBtmSheetFor = remember { 
        menuBtmSheetParam.menuBtmSheetFor
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (menuBtmSheetFor in menuBtmSheetLinkEntries()) {
            Column(
                modifier = Modifier.fillMaxWidth(0.5f).padding(start = 15.dp, bottom = 15.dp)
                    .wrapContentHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                CoilImage(
                    modifier = Modifier.animateContentSize().fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp)).height(200.dp),
                    imgURL = currentLinkTagsPair.link.imgURL,
                    preferences = preferences,
                    userAgent = currentLinkTagsPair.link.userAgent
                        ?: preferences.primaryJsoupUserAgent,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = currentLinkTagsPair.link.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp
                )
                if (currentLinkTagsPair.tags.isNotEmpty()) {
                    TagsRow(tags = currentLinkTagsPair.tags, onTagClick = {
                        menuBtmSheetParam.onTagClick(it)
                    })
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    text = currentLinkTagsPair.link.host,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(0.1f)).padding(5.dp),
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth()
                        .padding(end = 5.dp, top = 15.dp, bottom = 12.dp)
                )
                if (currentLinkTagsPair.link.note.isNotBlank()) {
                    Text(
                        text = Localization.Key.SavedNote.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = currentLinkTagsPair.link.note,
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    InfoCard(
                        info = Localization.Key.NoNoteAdded.rememberLocalizedString(),
                        paddingValues = PaddingValues(top = 2.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(0.5f).padding(start = 15.dp, bottom = 15.dp)
                    .wrapContentHeight()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(45.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = currentFolder!!.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 18.sp
                )
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth()
                        .padding(end = 5.dp, top = 15.dp, bottom = 12.dp)
                )
                if (currentFolder.note.isNotBlank()) {
                    Text(
                        text = Localization.Key.SavedNote.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = currentFolder.note,
                        style = MaterialTheme.typography.titleMedium,
                    )
                } else {
                    InfoCard(
                        info = Localization.Key.NoNoteAdded.rememberLocalizedString(),
                        paddingValues = PaddingValues(top = 2.dp)
                    )
                }
            }
        }
        commonMenuContent()
    }
}