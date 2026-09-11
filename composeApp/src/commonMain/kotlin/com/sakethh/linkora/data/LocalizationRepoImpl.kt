package com.sakethh.linkora.data

import LocalizedStrings
import com.sakethh.linkora.data.local.dao.LocalizationDao
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.localization.LocalizationInfoDTO
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.domain.model.localization.LocalizedString
import com.sakethh.linkora.domain.repository.LocalizationRepo
import com.sakethh.linkora.utils.catchAsExceptionAndEmitFailure
import com.sakethh.linkora.utils.wrappedResultFlow
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.json.Json

class LocalizationRepoImpl(
    private val standardClient: HttpClient,
    private val localizationServerURL: suspend () -> String,
    private val localizationDao: LocalizationDao,
) : LocalizationRepo.Remote,
    LocalizationRepo.Local {
    override fun getLanguagesFromServer(): Flow<Result<LocalizationInfoDTO>> = flow {
        emit(Result.Loading())
        standardClient.get(localizationServerURL() + "info").bodyAsText().let {
            emit(Result.Success(Json.decodeFromString<LocalizationInfoDTO>(it)))
        }
    }
        .catchAsExceptionAndEmitFailure()

    override suspend fun getLanguagePackFromServer(
        languageCode: String,
    ): Flow<Result<List<LocalizedString>>> = flow {
        emit(Result.Loading())
        standardClient
            .get(localizationServerURL() + languageCode)
            .bodyAsText()
            .let {
                Json.decodeFromString<Map<String, String>>(it)
            }
            .map {
                LocalizedString(
                    languageCode = languageCode,
                    stringName = it.key,
                    stringValue = it.value,
                )
            }
            .let {
                emit(Result.Success(it))
            }
    }
        .catchAsExceptionAndEmitFailure()

    override suspend fun addLocalizedStrings(
        localizedStrings: List<LocalizedString>,
    ): Flow<Result<Unit>> = wrappedResultFlow {
        localizationDao.addLocalizedStrings(localizedStrings)
    }

    override suspend fun doesStringsPackForThisLanguageExists(languageCode: String): Boolean = localizationDao.doesStringsPackForThisLanguageExists(languageCode)

    override suspend fun deleteAllLocalizedStringsForThisLanguage(
        languageCode: String,
    ): Flow<Result<Unit>> = wrappedResultFlow {
        localizationDao.deleteAllLocalizedStringsForThisLanguage(languageCode)
    }

    override suspend fun addNewLanguages(languages: List<LocalizedLanguage>): Flow<Result<Unit>> = wrappedResultFlow {
            localizationDao.addNewLanguages(languages)
        }

    override fun getAllLanguages(): Flow<Result<List<LocalizedLanguage>>> = localizationDao
        .getAllLanguages()
        .map {
            Result.Success(it)
        }
        .onStart {
            Result.Loading<List<LocalizedLanguage>>()
        }
        .catchAsExceptionAndEmitFailure()

    private val _localizedStrings: MutableStateFlow<LocalizedStrings> =
        MutableStateFlow(
            LocalizedStrings(
                values = mapOf()
            )
        )

    override val localizedStrings: StateFlow<LocalizedStrings> = _localizedStrings.asStateFlow()

    override suspend fun loadLanguage(languageCode: String) {
        // if strings are missing, LocalizedStrings falls back to default values, so this shouldn't be an issue
        val localizedPairs = localizationDao.getLocalizedPairs(
            languageCode = languageCode
        )

        _localizedStrings.emit(
            LocalizedStrings(
                values = localizedPairs
            )
        )
    }
}
