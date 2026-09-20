package com.sakethh.linkora.ui.components.sorting

import LocalizedStrings
import com.sakethh.linkora.data.local.WebCaptureDatabaseManager
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.repository.LocalizationRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.ui.domain.SortingType
import com.sakethh.linkora.ui.domain.model.SortingBtmSheet
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel

class SortingBtmSheetVM(
    preferencesRepository: PreferencesRepository,
    nativeUtils: NativeUtils,
    permissionManager: PermissionManager,
    webCapture: NativeUtils.WebCapture,
    webCaptureDatabaseManager: WebCaptureDatabaseManager,
    localizationRepo: LocalizationRepo.Local
) : SettingsScreenViewModel(
    preferencesRepository,
    nativeUtils,
    permissionManager,
    webCapture,
    webCaptureDatabaseManager,
    localizationRepo
) {
    fun sortingBtmSheetData(localizedStrings: LocalizedStrings): List<SortingBtmSheet> = listOf(
        SortingBtmSheet(
            sortingName = localizedStrings.NewestToOldest,
            onClick = {
                changeSettingPreferenceValue(
                    preferenceKey =
                        AppPreferences.SORTING_PREFERENCE,
                    newValue = SortingType.NEW_TO_OLD.name,
                )
            },
            sortingType = SortingType.NEW_TO_OLD,
        ),
        SortingBtmSheet(
            sortingName = localizedStrings.OldestToNewest,
            onClick = {
                changeSettingPreferenceValue(
                    preferenceKey =
                        AppPreferences.SORTING_PREFERENCE,
                    newValue = SortingType.OLD_TO_NEW.name,
                )
            },
            sortingType = SortingType.OLD_TO_NEW,
        ),
        SortingBtmSheet(
            sortingName = localizedStrings.AToZSequence,
            onClick = {
                changeSettingPreferenceValue(
                    preferenceKey =
                        AppPreferences.SORTING_PREFERENCE,
                    newValue = SortingType.A_TO_Z.name,
                )
            },
            sortingType = SortingType.A_TO_Z,
        ),
        SortingBtmSheet(
            sortingType = SortingType.Z_TO_A,
            sortingName = localizedStrings.ZToASequence,
            onClick = {
                changeSettingPreferenceValue(
                    preferenceKey =
                        AppPreferences.SORTING_PREFERENCE,
                    newValue = SortingType.Z_TO_A.name,
                )
            },
        ),
    )
}
