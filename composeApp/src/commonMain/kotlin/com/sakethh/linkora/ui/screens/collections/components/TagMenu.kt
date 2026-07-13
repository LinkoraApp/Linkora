package com.sakethh.linkora.ui.screens.collections.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.components.menu.IndividualMenuComponent
import com.sakethh.linkora.ui.components.menu.MenuNonImageHeader
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TagMenu(
    showMenu: Boolean,
    sheetState: SheetState,
    onHide: () -> Unit,
    tag: Tag,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val localClipBoardManager = LocalClipboardManager.current
    if (showMenu) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                coroutineScope
                    .launch {
                        sheetState.hide()
                    }
                    .invokeOnCompletion {
                        onHide()
                    }
            },
            dragHandle = null,
        ) {
            MenuNonImageHeader(
                onClick = {
                    localClipBoardManager.setText(AnnotatedString(tag.name))
                    coroutineScope
                        .launch {
                            sheetState.hide()
                            pushUIEvent(
                                UIEvent.Type.ShowSnackbar(
                                    Localization.Key.CopiedTitleToTheClipboard.getLocalizedString(),
                                ),
                            )
                        }
                        .invokeOnCompletion {
                            onHide()
                        }
                },
                text = tag.name,
                leadingIcon = Icons.Default.Tag,
            )
            ItemDivider(
                colorOpacity = 0.25f,
                paddingValues = PaddingValues(start = 25.dp, end = 25.dp),
            )
            Spacer(Modifier.height(5.dp))
            IndividualMenuComponent(
                onClick = onRename,
                elementName = Localization.Key.Rename.rememberLocalizedString(),
                elementImageVector = Icons.Default.DriveFileRenameOutline,
            )
            IndividualMenuComponent(
                onClick = onDelete,
                elementName = Localization.Key.Delete.rememberLocalizedString(),
                elementImageVector = Icons.Default.Delete,
            )
        }
    }
}
