package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.LocalizationRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.inDoubleQuotes
import com.sakethh.linkora.utils.replaceActual
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LanguageSettingsScreenVM(
    private val preferencesRepository: PreferencesRepository,
    private val localizationRepoRemote: LocalizationRepo.Remote,
    private val localizationRepoLocal: LocalizationRepo.Local,
    private val nativeUtils: NativeUtils,
) : ViewModel() {
    val preferencesAsFlow = preferencesRepository.preferencesAsFlow
    private val localizedStrings get() = localizationRepoLocal.localizedStrings.value
    private val _availableLanguages = MutableStateFlow(emptyList<LocalizedLanguage>())
    val availableLanguages = _availableLanguages.asStateFlow()

    fun loadLocalizedStrings(languageCode: String, languageName: String) {
        viewModelScope.launch {
            preferencesRepository.changePreferenceValue(
                preferenceKey = AppPreferences.APP_LANGUAGE_CODE,
                newValue = languageCode
            )
            preferencesRepository.changePreferenceValue(
                preferenceKey = AppPreferences.APP_LANGUAGE_NAME,
                newValue = languageName
            )
            localizationRepoLocal.loadLanguage(languageCode)
        }
    }

    fun doesLanguagePackExists(
        exists: MutableState<Boolean>,
        languageCode: String,
    ) = nativeUtils.platformRunBlocking {
        exists.value = localizationRepoLocal.doesStringsPackForThisLanguageExists(languageCode)
    }

    val languageSettingsState =
        mutableStateOf(
            LanguageSettingsState(
                fetchingStrings = false,
                fetchingLanguageInfo = false,
            ),
        )

    private fun resetState() {
        languageSettingsState.value =
            LanguageSettingsState(fetchingStrings = false, fetchingLanguageInfo = false)
    }

    // TODO: NESTED collection
    fun fetchRemoteLanguages() {
        viewModelScope.launch {
            localizationRepoRemote.getLanguagesFromServer().collect {
                it.onSuccess {
                    localizationRepoLocal
                        .addNewLanguages(
                            it.data.availableLanguages
                                .filter { it.languageCode != Constants.DEFAULT_APP_LANGUAGE_CODE }
                                .map {
                                    LocalizedLanguage(
                                        languageCode = it.languageCode,
                                        languageName = it.localizedName,
                                        localizedStringsCount = it.localizedStringsCount,
                                        contributionLink = "",
                                    )
                                },
                        )
                        .collectLatest {
                            it.onSuccess {
                                resetState()
                                pushUIEvent(
                                    UIEvent.Type.ShowSnackbar(
                                        localizedStrings.SavedAvailableLanguagesInfoLocally,
                                    ),
                                )
                            }
                            it.onFailure {
                                resetState()
                                pushUIEvent(UIEvent.Type.ShowSnackbar(it.message.toString()))
                            }
                        }
                }
                it.onLoading {
                    languageSettingsState.value =
                        LanguageSettingsState(fetchingStrings = false, fetchingLanguageInfo = true)
                }
                it.onFailure {
                    resetState()
                    pushUIEvent(UIEvent.Type.ShowSnackbar(it.message.toString()))
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            localizationRepoLocal.getAllLanguages().collectLatest {
                it.onSuccess {
                    _availableLanguages.emit(it.data)
                }
                it.onFailure {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(it.message.toString()))
                }
            }
        }
    }

    fun deleteALanguagePack(language: LocalizedLanguage) {
        viewModelScope.launch {
            localizationRepoLocal
                .deleteAllLocalizedStringsForThisLanguage(language.languageCode)
                .collectLatest {
                    it.onSuccess {
                        pushUIEvent(
                            UIEvent.Type.ShowSnackbar(
                                localizedStrings.DeletedTheStringsPack
                                    .replaceActual(
                                        language.languageName.inDoubleQuotes(),
                                    ),
                            ),
                        )
                    }
                    it.onFailure {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(it.message.toString()))
                    }
                }
        }
    }

    // TODO: NESTED collectLatest
    fun downloadALanguageStringsPack(language: LocalizedLanguage) {
        viewModelScope.launch {
            localizationRepoRemote.getLanguagePackFromServer(language.languageCode).collectLatest {
                it.onSuccess {
                    localizationRepoLocal.addLocalizedStrings(it.data).collectLatest {
                        it.onSuccess {
                            resetState()
                            pushUIEvent(
                                UIEvent.Type.ShowSnackbar(
                                    message =
                                        localizedStrings.DownloadedLanguageStrings
                                            .replaceActual(
                                                language.languageName.inDoubleQuotes(),
                                            ),
                                ),
                            )
                        }
                        it.onFailure {
                            resetState()
                            pushUIEvent(UIEvent.Type.ShowSnackbar(message = it.message.toString()))
                        }
                    }
                }
                it.onLoading {
                    languageSettingsState.value =
                        LanguageSettingsState(fetchingStrings = true, fetchingLanguageInfo = false)
                }
                it.onFailure {
                    resetState()
                    pushUIEvent(UIEvent.Type.ShowSnackbar(message = it.message.toString()))
                }
            }
        }
    }
}
