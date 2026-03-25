package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.LocalizationRepo
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.inDoubleQuotes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LanguageSettingsScreenVM(
    private val localizationRepoRemote: LocalizationRepo.Remote,
    private val localizationRepoLocal: LocalizationRepo.Local,
    private val nativeUtils: NativeUtils
) : ViewModel() {
    private val _availableLanguages = MutableStateFlow(
        emptyList<LocalizedLanguage>()
    )
    val availableLanguages = _availableLanguages.asStateFlow()

    fun doesLanguagePackExists(exists: MutableState<Boolean>, languageCode: String) =
        nativeUtils.platformRunBlocking {
            exists.value = localizationRepoLocal.doesStringsPackForThisLanguageExists(languageCode)
        }

    val languageSettingsState = mutableStateOf(
        LanguageSettingsState(
            fetchingStrings = false,
            fetchingLanguageInfo = false
        )
    )

    private fun resetState() {
        languageSettingsState.value =
            LanguageSettingsState(fetchingStrings = false, fetchingLanguageInfo = false)
    }

    fun fetchRemoteLanguages() {
        viewModelScope.launch {
            localizationRepoRemote.getLanguagesFromServer().collect {
                it.onSuccess {
                    localizationRepoLocal.addNewLanguages(it.data.availableLanguages.filter { it.languageCode != "en" }
                        .map {
                            LocalizedLanguage(
                                languageCode = it.languageCode,
                                languageName = it.localizedName,
                                localizedStringsCount = it.localizedStringsCount,
                                contributionLink = ""
                            )
                        }).collectLatest {
                        it.onSuccess {
                            resetState()
                            pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.SavedAvailableLanguagesInfoLocally.getLocalizedString()))
                        }
                        it.onFailure {
                            resetState()
                            pushUIEvent(UIEvent.Type.ShowSnackbar(it))
                        }
                    }
                }
                it.onLoading {
                    languageSettingsState.value =
                        LanguageSettingsState(fetchingStrings = false, fetchingLanguageInfo = true)
                }
                it.onFailure {
                    resetState()
                    pushUIEvent(UIEvent.Type.ShowSnackbar(it))
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
                    pushUIEvent(UIEvent.Type.ShowSnackbar(it))
                }
            }
        }
    }

    fun deleteALanguagePack(language: LocalizedLanguage) {
        viewModelScope.launch {
            localizationRepoLocal.deleteAllLocalizedStringsForThisLanguage(language.languageCode)
                .collectLatest {
                    it.onSuccess {
                        pushUIEvent(
                            UIEvent.Type.ShowSnackbar(
                                Localization.Key.DeletedTheStringsPack.getLocalizedString().replace(
                                    LinkoraPlaceHolder.First.value,
                                    language.languageName.inDoubleQuotes()
                                )
                            )
                        )
                    }
                    it.onFailure {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(it))
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
                                    message = Localization.Key.DownloadedLanguageStrings.getLocalizedString()
                                        .replace(
                                            LinkoraPlaceHolder.First.value,
                                            language.languageName.inDoubleQuotes()
                                        )
                                )
                            )
                        }
                        it.onFailure {
                            resetState()
                            pushUIEvent(UIEvent.Type.ShowSnackbar(message = it))
                        }
                    }
                }
                it.onLoading {
                    languageSettingsState.value =
                        LanguageSettingsState(fetchingStrings = true, fetchingLanguageInfo = false)
                }
                it.onFailure {
                    resetState()
                    pushUIEvent(UIEvent.Type.ShowSnackbar(message = it))
                }
            }
        }
    }
}