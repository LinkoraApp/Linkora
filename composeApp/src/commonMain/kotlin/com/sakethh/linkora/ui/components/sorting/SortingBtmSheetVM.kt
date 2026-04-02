package com.sakethh.linkora.ui.components.sorting

import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.ui.domain.SortingType
import com.sakethh.linkora.ui.domain.model.SortingBtmSheet
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.stringPreferencesKey


class SortingBtmSheetVM(
    preferencesRepository: PreferencesRepository,
    nativeUtils: NativeUtils,
    permissionManager: PermissionManager
) :
    SettingsScreenViewModel(preferencesRepository, nativeUtils, permissionManager) {

    fun sortingBtmSheetData(): List<SortingBtmSheet> {
        return listOf(
            SortingBtmSheet(
                sortingName = Localization.Key.NewestToOldest.getLocalizedString(), onClick = {
                    changeSettingPreferenceValue(
                        preferenceKey = stringPreferencesKey(
                            AppPreferences.SORTING_PREFERENCE.key
                        ), newValue = SortingType.NEW_TO_OLD.name
                    )
                }, sortingType = SortingType.NEW_TO_OLD
            ),
            SortingBtmSheet(
                sortingName = Localization.Key.OldestToNewest.getLocalizedString(), onClick = {
                    changeSettingPreferenceValue(
                        preferenceKey = stringPreferencesKey(
                            AppPreferences.SORTING_PREFERENCE.key
                        ), newValue = SortingType.OLD_TO_NEW.name
                    )
                }, sortingType = SortingType.OLD_TO_NEW
            ),
            SortingBtmSheet(
                sortingName = Localization.Key.AToZSequence.getLocalizedString(), onClick = {
                    changeSettingPreferenceValue(
                        preferenceKey = stringPreferencesKey(
                            AppPreferences.SORTING_PREFERENCE.key
                        ), newValue = SortingType.A_TO_Z.name
                    )
                }, sortingType = SortingType.A_TO_Z
            ),
            SortingBtmSheet(
                sortingType = SortingType.Z_TO_A,
                sortingName = Localization.Key.ZToASequence.getLocalizedString(),
                onClick = {
                    changeSettingPreferenceValue(
                        preferenceKey = stringPreferencesKey(
                            AppPreferences.SORTING_PREFERENCE.key
                        ), newValue = SortingType.Z_TO_A.name
                    )
                }),
        )
    }
}