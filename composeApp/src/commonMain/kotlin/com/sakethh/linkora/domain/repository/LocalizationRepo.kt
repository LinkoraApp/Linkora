package com.sakethh.linkora.domain.repository

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.localization.LocalizationInfoDTO
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.domain.model.localization.LocalizedString
import kotlinx.coroutines.flow.Flow

interface LocalizationRepo {
    interface Remote {
        fun getLanguagesFromServer(): Flow<Result<LocalizationInfoDTO>>

        suspend fun getLanguagePackFromServer(languageCode: String): Flow<Result<List<LocalizedString>>>
    }

    interface Local {
        suspend fun addLocalizedStrings(localizedStrings: List<LocalizedString>): Flow<Result<Unit>>

        suspend fun doesStringsPackForThisLanguageExists(languageCode: String): Boolean

        suspend fun deleteAllLocalizedStringsForThisLanguage(languageCode: String): Flow<Result<Unit>>

        suspend fun getLocalizedStringValueFor(
            stringName: String,
            languageCode: String,
        ): String?

        suspend fun addANewLanguage(localizedLanguage: LocalizedLanguage): Flow<Result<Unit>>

        suspend fun addNewLanguages(languages: List<LocalizedLanguage>): Flow<Result<Unit>>

        suspend fun deleteALanguage(localizedLanguage: LocalizedLanguage): Flow<Result<Unit>>

        suspend fun deleteALanguage(languageName: String): Flow<Result<Unit>>

        suspend fun deleteALanguageBasedOnLanguageCode(languageCode: String): Flow<Result<Unit>>

        suspend fun getLanguageNameForTheCode(languageCode: String): String

        suspend fun getLanguageCodeForTheLanguageNamed(languageName: String): String

        fun getAllLanguages(): Flow<Result<List<LocalizedLanguage>>>
    }
}
