package com.sakethh.linkora.ui.components.sorting

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.ComposableContent
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.domain.SortingBtmSheetType
import com.sakethh.linkora.ui.domain.SortingType
import com.sakethh.linkora.ui.screens.collections.components.ItemDivider
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SortingBottomSheet(
    sortingBottomSheetParam: SortingBottomSheetParam
) {
    val coroutineScope = rememberCoroutineScope()
    val sortingBtmSheetVM: SortingBtmSheetVM = linkoraViewModel()
    val linksSortingSelectedState = rememberSaveable {
        mutableStateOf(sortingBottomSheetParam.showLinksSelection.value)
    }
    val foldersSortingSelectedState = rememberSaveable {
        mutableStateOf(sortingBottomSheetParam.showFoldersSelection.value)
    }
    val didAnyCheckBoxStateChanged = rememberSaveable {
        mutableStateOf(false)
    }
    val hideBtmSheet: () -> Unit = {
        coroutineScope.launch {
            sortingBottomSheetParam.bottomModalSheetState.hide()
        }.invokeOnCompletion {
            sortingBottomSheetParam.onDismiss()
        }
    }
    val sortByContent: ComposableContent = {
        sortingBtmSheetVM.sortingBtmSheetData().forEach {
            Column(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).combinedClickable(interactionSource = remember {
                    MutableInteractionSource()
                }, indication = null, onClick = {
                    sortingBottomSheetParam.onSelected(
                        it.sortingType,
                        linksSortingSelectedState.value,
                        foldersSortingSelectedState.value
                    )
                    it.onClick()
                    hideBtmSheet()
                }, onLongClick = {}).pressScaleEffect().fillMaxWidth().wrapContentHeight()
            ) {
                Row(
                    modifier = Modifier.padding(start = 15.dp).fillMaxWidth().wrapContentHeight(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = it.sortingName,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (it.sortingType == SortingType.valueOf(
                                AppPreferences.selectedSortingType.value
                            ) && !didAnyCheckBoxStateChanged.value
                        ) MaterialTheme.colorScheme.primary else LocalTextStyle.current.color
                    )
                    RadioButton(
                        selected = it.sortingType.name == AppPreferences.selectedSortingType.value && !didAnyCheckBoxStateChanged.value,
                        onClick = {
                            sortingBottomSheetParam.onSelected(
                                it.sortingType,
                                linksSortingSelectedState.value,
                                foldersSortingSelectedState.value
                            )
                            it.onClick()
                            hideBtmSheet()
                        },
                        modifier = Modifier.padding(end = 5.dp).pointerHoverIcon(icon = PointerIcon.Hand)
                    )
                }
            }
        }
    }
    ModalBottomSheet(
        sheetState = sortingBottomSheetParam.bottomModalSheetState, onDismissRequest = hideBtmSheet
    ) {
        Column(
            modifier = Modifier.animateContentSize()
        ) {
            Text(
                text = Localization.Key.SortBy.rememberLocalizedString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 15.dp)
            )
            if (sortingBottomSheetParam.sortingBtmSheetType == SortingBtmSheetType.REGULAR_FOLDER_SCREEN) {
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (((sortingBottomSheetParam.sortingBtmSheetType == SortingBtmSheetType.REGULAR_FOLDER_SCREEN && foldersSortingSelectedState.value) || (sortingBottomSheetParam.sortingBtmSheetType == SortingBtmSheetType.REGULAR_FOLDER_SCREEN && linksSortingSelectedState.value)) || ((sortingBottomSheetParam.sortingBtmSheetType == SortingBtmSheetType.ARCHIVE_FOLDER_SCREEN && foldersSortingSelectedState.value) || sortingBottomSheetParam.sortingBtmSheetType == SortingBtmSheetType.ARCHIVE_FOLDER_SCREEN && linksSortingSelectedState.value)) {
                Text(
                    text = Localization.Key.SortBy.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 15.dp, top = 20.dp)
                )
                sortByContent()
            }
            if (sortingBottomSheetParam.sortingBtmSheetType != SortingBtmSheetType.REGULAR_FOLDER_SCREEN && sortingBottomSheetParam.sortingBtmSheetType != SortingBtmSheetType.ARCHIVE_FOLDER_SCREEN) {
                sortByContent()
            }
            ItemDivider(
                paddingValues = PaddingValues(
                    top = 10.dp, start = 15.dp, end = 15.dp, bottom = 18.dp
                )
            )
            SettingComponent(
                SettingComponentParam(
                    title = Localization.Key.ForceShuffleLinks.rememberLocalizedString(),
                    doesDescriptionExists = true,
                    description = Localization.Key.ForceShuffleLinksDesc.rememberLocalizedString(),
                    isSwitchNeeded = true,
                    isSwitchEnabled = AppPreferences.forceShuffleLinks,
                    onSwitchStateChange = {
                        AppPreferences.forceShuffleLinks.value = it
                        sortingBtmSheetVM.changeSettingPreferenceValue(
                            preferenceKey = booleanPreferencesKey(
                                AppPreferenceType.FORCE_SHUFFLE_LINKS.name
                            ),
                            newValue = AppPreferences.forceShuffleLinks.value,
                            onCompletion = hideBtmSheet
                        )
                    },
                    isIconNeeded = rememberSaveable {
                        mutableStateOf(false)
                    })
            )
            Spacer(
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}