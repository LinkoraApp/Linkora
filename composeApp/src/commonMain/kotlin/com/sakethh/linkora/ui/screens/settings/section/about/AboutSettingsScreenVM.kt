package com.sakethh.linkora.ui.screens.settings.section.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.dto.github.GitHubReleaseDTOItem
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.remote.GitHubReleasesRepo
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AboutSettingsScreenVM(
    private val localLinksRepo: LocalLinksRepo, private val gitHubReleasesRepo: GitHubReleasesRepo
) : ViewModel() {


    fun addANewLinkToHistory(
        link: Link,
        tagIds: List<Long>?,
        useProxy: Boolean,
        skipSavingIfExists: Boolean,
        forceSaveIfRetrievalFails: Boolean
    ) {
        viewModelScope.launch {
            localLinksRepo.addANewLink(
                link = link.copy(
                    linkType = LinkType.HISTORY_LINK,
                    idOfLinkedFolder = null,
                ),
                linkSaveConfig = LinkSaveConfig(
                    forceAutoDetectTitle = false, forceSaveWithoutRetrievingData = true,
                    useProxy = useProxy,
                    skipSavingIfExists = skipSavingIfExists,
                    forceSaveIfRetrievalFails = forceSaveIfRetrievalFails
                ),
                selectedTagIds = tagIds
            ).collectLatest {
                it.onSuccess {
                    if (it.isRemoteExecutionSuccessful.not()) {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(it.getRemoteOnlyFailureMsg()))
                    }
                }
                it.pushSnackbarOnFailure()
            }
        }
    }

    fun retrieveLatestVersionData(
        onLoading: () -> Unit, onCompletion: (gitHubReleaseDTOItem: GitHubReleaseDTOItem?) -> Unit
    ) {
        viewModelScope.launch {
            gitHubReleasesRepo.getLatestVersionData().collectLatest {
                it.onLoading {
                    onLoading()
                }.onSuccess { success ->
                    onCompletion(success.data)
                }.onFailure { failureMsg ->
                    onCompletion(null)
                    this.pushUIEvent(UIEvent.Type.ShowSnackbar(failureMsg))
                }
            }
        }
    }
}