package com.sakethh.linkora.ui

import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

typealias LastSeenId = Long
typealias LastSeenString = String
typealias UpdatedLastSeenId = Long
typealias UpdatedLastSeenString = String

@OptIn(ExperimentalAtomicApi::class)
class Paginator<T>(
    private val coroutineScope: CoroutineScope,
    private val onRetrieve: suspend (lastSeenId: LastSeenId?, lastSeenString: String?) -> Flow<Result<List<T>>>,
    private val onRetrieved: suspend (currentKey: Pair<LastSeenId, LastSeenString>, data: List<T>) -> Pair<UpdatedLastSeenId, UpdatedLastSeenString>,
    private val onError: suspend (String) -> Unit,
    private val onRetrieving: suspend () -> Unit,
    private val onPagesFinished: suspend () -> Unit
) {

    var isRetrieving = false
        private set

    var isPagesFinished = false
        private set

    var errorOccurred = false
        private set

    private val lastSeenId = AtomicLong(Constants.EMPTY_LAST_SEEN_ID)
    private val lastSeenString = AtomicReference("")

    private val orderedPageKeys = ConcurrentMutableList<Pair<LastSeenId, LastSeenString>>()

    private val seenPageKeys = mutableSetOf<Pair<LastSeenId, LastSeenString>>()

    private val firstVisibleItemIndex: MutableStateFlow<Long> = MutableStateFlow(0)
    suspend fun updateFirstVisibleItemIndex(newIndex: Long) = firstVisibleItemIndex.emit(newIndex)

    private val collectionJobs = ConcurrentMutableMap<Pair<LastSeenId, LastSeenString>, Job>()

    init {
        coroutineScope.launch {
            firstVisibleItemIndex.collect { firstVisibleIndex ->

                val visibleBatchIndex = (firstVisibleIndex / Constants.PAGE_SIZE).toInt()

                val minBatchIndex =
                    (visibleBatchIndex - Constants.ACTIVE_PAGE_COLLECTION_SIZE).coerceAtLeast(0)
                val maxHistoryIndex = orderedPageKeys.lastIndex
                val maxBatchIndex =
                    (visibleBatchIndex + Constants.ACTIVE_PAGE_COLLECTION_SIZE).coerceAtMost(
                        maxHistoryIndex
                    )

                val qualifiedForRestarting =
                    if (orderedPageKeys.isNotEmpty() && minBatchIndex <= maxBatchIndex) {
                        (minBatchIndex..maxBatchIndex).map { index ->
                            orderedPageKeys[index]
                        }.toSet()
                    } else {
                        emptySet()
                    }

                linkoraLog("qualifiedForRestarting size = ${qualifiedForRestarting.size}:\n$qualifiedForRestarting")

                collectionJobs.toList().forEach { (pageKey, job) ->
                    if (!job.isCancelled && pageKey !in qualifiedForRestarting) {
                        linkoraLog("cancelling the $pageKey job")
                        job.cancel()
                    }
                }

                linkoraLog("After cancellation::\n")
                logCoroutineScopeChildrenCount()

                qualifiedForRestarting.forEach { pageKey ->
                    val existingJob = collectionJobs[pageKey]

                    if ((existingJob == null || existingJob.isCancelled) && seenPageKeys.contains(
                            pageKey
                        )
                    ) {
                        linkoraLog("restarting the $pageKey job")

                        val restartId =
                            if (pageKey.first == Constants.EMPTY_LAST_SEEN_ID) null else pageKey.first

                        var newJob: Job? = null

                        newJob = coroutineScope.launch {
                            try {
                                onRetrieve(restartId, pageKey.second)
                                    .restartCollectData(pageKey)
                            } finally {
                                newJob?.let { collectionJobs.remove(pageKey) }
                            }
                        }
                        collectionJobs[pageKey] = newJob
                    }
                }

                linkoraLog("After restarting::\n")
                logCoroutineScopeChildrenCount()
            }
        }
    }

    private fun logCoroutineScopeChildrenCount() {
        linkoraLog(
            "active children count:${
                try {
                    coroutineScope.coroutineContext.job.children.count { it.isActive } - 1
                } catch (_: Exception) {
                    null
                }
            }"
        )
        linkoraLog(
            "cancelled children count:${
                try {
                    coroutineScope.coroutineContext.job.children.count { it.isCancelled }
                } catch (_: Exception) {
                    null
                }
            }"
        )
    }

    private val seenPagesMutex = Mutex()

    suspend fun retrieveNextBatch() {
        if ((isRetrieving || isPagesFinished || errorOccurred).also {
                if (it) linkoraLog("isRetrieving=$isRetrieving, isPagesFinished=$isPagesFinished, errorOccurred=$errorOccurred")
            }) return

        isRetrieving = true

        val currentId = lastSeenId.load()
        val currentString = lastSeenString.load()
        val pageKeyPair = currentId to currentString

        seenPagesMutex.withLock {
            if (!seenPageKeys.contains(pageKeyPair)) {
                orderedPageKeys.add(pageKeyPair)
                seenPageKeys.add(pageKeyPair)
            }
        }

        val retrieveId = if (currentId == Constants.EMPTY_LAST_SEEN_ID) null else currentId
        val dataFlow = onRetrieve(retrieveId, currentString)

        var didLaunch = false

        collectionJobs.computeIfAbsent(pageKeyPair) {
            didLaunch = true

            coroutineScope.launch {
                try {
                    dataFlow.collectData(pageKeyPair)
                } finally {
                    collectionJobs.remove(pageKeyPair)
                    isRetrieving = false
                }
            }.also {
                linkoraLog("Seen pages:${seenPageKeys.count()}")
            }
        }

        if (!didLaunch) {
            isRetrieving = false
            linkoraLog("Collections for the key $pageKeyPair is already happening")
        }
    }

    private suspend fun Flow<Result<List<T>>>.collectData(
        pageKey: Pair<LastSeenId, LastSeenString>
    ) {
        cancellable().distinctUntilChanged().collect { result ->
            result.onSuccess {
                val (updatedLastSeenId, updatedLastSeenString) = onRetrieved(pageKey, it.data)

                this@Paginator.lastSeenId.store(updatedLastSeenId)
                this@Paginator.lastSeenString.store(updatedLastSeenString)

                linkoraLog("Retrieved from page key: $pageKey of size = ${it.data.size}")

                isRetrieving = false
                isPagesFinished = it.data.isEmpty() || it.data.size < Constants.PAGE_SIZE

                if (isPagesFinished) {
                    onPagesFinished()
                }
            }.onFailure {
                linkoraLog(it)
                isRetrieving = false
                errorOccurred = true
                onError(it)
            }.onLoading {
                onRetrieving()
            }
        }
    }

    private suspend fun Flow<Result<List<T>>>.restartCollectData(
        pageKey: Pair<LastSeenId, LastSeenString>
    ) {
        cancellable().distinctUntilChanged().collect { result ->
            result.onSuccess {
                onRetrieved(pageKey, it.data)
            }.onFailure {
                linkoraLog("Silent failure for page $pageKey: $it")
            }
        }
    }

    suspend fun cancelAndReset() {
        collectionJobs.values.toList().forEach { it.cancel() }
        collectionJobs.clear()
        orderedPageKeys.clear()
        seenPageKeys.clear()

        isRetrieving = false
        isPagesFinished = false
        errorOccurred = false

        lastSeenId.store(Constants.EMPTY_LAST_SEEN_ID)
        lastSeenString.store("")

        updateFirstVisibleItemIndex(0)
    }
}