package com.sakethh.linkora.ui.components.folder.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.HorizontalInfoCard

@Composable
fun FolderNonMobileMenu(
    preferences: AppPreferences,
    menuBtmSheetParam: FolderMenuBtmSheetParam,
    currentFolder: Folder,
    commonMenuContent: ComposableContent,
) {
    val localizedStrings = LocalizedStrings.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(0.5f)
                    .padding(start = 15.dp, bottom = 15.dp)
                    .wrapContentHeight(),
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier.size(45.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = currentFolder.name,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 18.sp,
            )
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(end = 5.dp, top = 15.dp, bottom = 12.dp),
            )
            if (currentFolder.note.isNotBlank()) {
                Text(
                    text = localizedStrings.SavedNote,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(5.dp))
                Text(text = currentFolder.note, style = MaterialTheme.typography.titleMedium)
            } else {
                HorizontalInfoCard(
                    info = localizedStrings.NoNoteAdded,
                    paddingValues = PaddingValues(top = 2.dp),
                )
            }
        }
        commonMenuContent()
    }
}
