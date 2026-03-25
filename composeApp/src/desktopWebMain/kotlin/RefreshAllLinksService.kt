import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.platform.PlatformIODispatcher
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.RefreshLinksState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object RefreshAllLinksService {

    private var linksRefreshJob: Job? = null

    fun cancel() {
        linksRefreshJob?.cancel()
        DataSettingsScreenVM.refreshLinksState.value = RefreshLinksState(
            isInRefreshingState = false, currentIteration = 0
        )
    }

    fun invoke(localLinksRepo: LocalLinksRepo) {
        linksRefreshJob = CoroutineScope(PlatformIODispatcher).launch {
            localLinksRepo.getAllLinks().let { allLinks ->
                DataSettingsScreenVM.totalLinksForRefresh.value = allLinks.size
                DataSettingsScreenVM.refreshLinksState.value =
                    DataSettingsScreenVM.refreshLinksState.value.copy(
                        isInRefreshingState = true, currentIteration = 0
                    )

                allLinks.forEach { link ->
                    launch {
                        localLinksRepo.refreshLinkMetadata(
                            link,
                            AppPreferences.selectedLinkRefreshType
                        ).collectLatest {
                            it.onSuccess {
                                DataSettingsScreenVM.refreshLinksState.value =
                                    DataSettingsScreenVM.refreshLinksState.value.let { currentLinkRefreshState ->
                                        currentLinkRefreshState.copy(currentIteration = currentLinkRefreshState.currentIteration + 1)
                                    }
                            }
                        }
                    }
                }
            }
        }
        linksRefreshJob?.invokeOnCompletion {
            DataSettingsScreenVM.refreshLinksState.value =
                DataSettingsScreenVM.refreshLinksState.value.copy(
                    isInRefreshingState = false, currentIteration = 0
                )
        }
    }
}