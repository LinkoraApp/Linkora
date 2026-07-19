import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.platform.PlatformIODispatcher
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.screens.settings.section.data.RefreshLinksState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch

object RefreshAllLinksService {
    private var linksRefreshJob: Job? = null

    fun cancel() {
        linksRefreshJob?.cancel()
        DataSettingsScreenVM.refreshLinksState.value =
            RefreshLinksState(
                isInRefreshingState = false,
                currentIteration = 0,
                total = 0,
            )
    }

    fun invoke(localLinksRepo: LocalLinksRepo) {
        val preferences = DependencyContainer.preferencesRepo.getPreferences()

        linksRefreshJob =
            CoroutineScope(PlatformIODispatcher).launch {
                localLinksRepo.getAllLinks().let { allLinks ->
                    DataSettingsScreenVM.refreshLinksState.value =
                        DataSettingsScreenVM.refreshLinksState.value.copy(
                            isInRefreshingState = true,
                            currentIteration = 0,
                            total = allLinks.size,
                        )

                    var processedCount = 0

                    allLinks
                        .asFlow()
                        .flatMapMerge(concurrency = preferences.maxConcurrentRefreshCount) { link ->
                            localLinksRepo.refreshLinkMetadata(
                                link,
                                preferences.selectedLinkRefreshType,
                                preferences.captureWhenRefreshAllLink,
                            )
                        }
                        .catch { it.printStackTrace() }
                        .collect { result ->
                            result.onSuccess {
                                processedCount++
                                DataSettingsScreenVM.refreshLinksState.value =
                                    DataSettingsScreenVM.refreshLinksState.value.copy(
                                        currentIteration = processedCount,
                                    )
                            }
                            result.onFailure {
                                processedCount++
                            }
                        }
                }
            }

        linksRefreshJob?.invokeOnCompletion {
            DataSettingsScreenVM.refreshLinksState.value =
                DataSettingsScreenVM.refreshLinksState.value.copy(
                    isInRefreshingState = false,
                    currentIteration = 0,
                )
        }
    }
}
