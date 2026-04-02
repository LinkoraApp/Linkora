package com.sakethh.linkora.ui.screens.collections.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.rememberLocalizedString

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RootCollectionSwitcher(
    preferences: AppPreferences,
    isRootContentSwitcherBtmSheetVisible: Boolean,
    rootContentSwitcherBtmSheetState: SheetState,
    onHide: () -> Unit,
    onSourceClick: (id: Int) -> Unit
) {
    if (isRootContentSwitcherBtmSheetVisible) {
        ModalBottomSheet(onDismissRequest = onHide, sheetState = rootContentSwitcherBtmSheetState) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Localization.Key.SelectACollectionSource.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(15.dp)
                )
                remember {
                    listOf(0 to Localization.Key.Folders.getLocalizedString(), 1 to Localization.Key.Tags.getLocalizedString())
                }.forEach { contentType ->
                    Row(modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth().clickable {
                        onSourceClick(contentType.first)
                    }.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                            selected = contentType.first == preferences.selectedCollectionSourceId,
                            onClick = {
                                onSourceClick(contentType.first)
                            })
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = contentType.second,
                            style = if (contentType.first == preferences.selectedCollectionSourceId) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                            color = if (contentType.first == preferences.selectedCollectionSourceId) LocalContentColor.current else LocalContentColor.current.copy(
                                0.85f
                            )
                        )
                    }
                }
            }
        }
    }
}