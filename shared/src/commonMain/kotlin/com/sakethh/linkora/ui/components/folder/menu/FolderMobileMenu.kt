package com.sakethh.linkora.ui.components.folder.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.menu.MenuNonImageHeader
import com.sakethh.linkora.ui.screens.collections.components.ItemDivider
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FolderMobileMenu(
    preferences: AppPreferences,
    menuBtmSheetParam: FolderMenuBtmSheetParam,
    currentFolder: Folder,
    showNote: Boolean,
    commonMenuContent: ComposableContent,
) {
    val localizedStrings = LocalizedStrings.current
    val localClipBoardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.navigationBarsPadding().verticalScroll(rememberScrollState())) {
        MenuNonImageHeader(
            onClick = {
                localClipBoardManager.setText(AnnotatedString(currentFolder.name))
                coroutineScope.pushUIEvent(UIEvent.Type.ShowSnackbar(localizedStrings.CopiedTitleToTheClipboard))
            },
            leadingIcon = Icons.Outlined.Folder,
            text = currentFolder.name,
        )
        ItemDivider(colorOpacity = 0.25f, paddingValues = PaddingValues(start = 25.dp, end = 25.dp))
        Spacer(Modifier.height(5.dp))

        if (!showNote) {
            commonMenuContent()
        } else {
            val note = currentFolder.note
            if (note.isNotEmpty()) {
                Text(
                    text = localizedStrings.SavedNote,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(20.dp),
                )
                Text(
                    text = note,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 20.sp,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .pointerHoverIcon(icon = PointerIcon.Hand)
                            .combinedClickable(onClick = {}, onLongClick = {
                                localClipBoardManager.setText(AnnotatedString(note))
                                coroutineScope.pushUIEvent(
                                    UIEvent.Type.ShowSnackbar(
                                        localizedStrings.CopiedNoteToTheClipboard
                                    )
                                )
                            })
                            .padding(start = 20.dp, end = 25.dp),
                    textAlign = TextAlign.Start,
                    lineHeight = 24.sp,
                )
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = localizedStrings.NoNoteAdded,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 24.sp,
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
