package com.sakethh.linkora.domain.repository

import LocalizedStrings
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.localization.LocalizationInfoDTO
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.domain.model.localization.LocalizedString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface LocalizationRepo {
    interface Remote {
        fun getLanguagesFromServer(): Flow<Result<LocalizationInfoDTO>>

        suspend fun getLanguagePackFromServer(languageCode: String): Flow<Result<List<LocalizedString>>>
    }

    interface Local {
        suspend fun addLocalizedStrings(localizedStrings: List<LocalizedString>): Flow<Result<Unit>>

        suspend fun doesStringsPackForThisLanguageExists(languageCode: String): Boolean

        suspend fun deleteAllLocalizedStringsForThisLanguage(languageCode: String): Flow<Result<Unit>>

        suspend fun addNewLanguages(languages: List<LocalizedLanguage>): Flow<Result<Unit>>

        fun getAllLanguages(): Flow<Result<List<LocalizedLanguage>>>

        val localizedStrings: StateFlow<LocalizedStrings>

        suspend fun loadLanguage(languageCode: String)
    }
}
