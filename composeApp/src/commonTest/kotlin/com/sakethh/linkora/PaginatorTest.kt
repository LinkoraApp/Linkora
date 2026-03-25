package com.sakethh.linkora

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.ui.LastSeenId
import com.sakethh.linkora.ui.Paginator
import com.sakethh.linkora.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PaginatorTest {

    private val activeConnections = LinkedHashSet(mutableSetOf<Long>())

    private suspend fun onRetrieve(
        lastSeenId: LastSeenId?,
        lastSeenString: String?
    ): Flow<Result<List<String>>> {
        val idValue = lastSeenId ?: Constants.EMPTY_LAST_SEEN_ID
        return flow {
            activeConnections.add(idValue)
            emit(Result.Success(List(Constants.PAGE_SIZE) { "item $it" }))
            try {
                awaitCancellation()
            } finally {
                activeConnections.remove(idValue)
            }
        }
    }

    private fun createPaginator(scope: CoroutineScope): Paginator<String> {
        return Paginator(
            coroutineScope = scope,
            onRetrieve = ::onRetrieve,
            onRetrieved = { (currentId, _), _ ->
                val nextId =
                    if (currentId == Constants.EMPTY_LAST_SEEN_ID) 19L else currentId + Constants.PAGE_SIZE
                nextId to ""
            },
            onError = {},
            onRetrieving = {},
            onPagesFinished = {}
        )
    }

    private suspend fun loadPages(paginator: Paginator<String>, count: Int) {
        paginator.retrieveNextBatch()
        paginator.updateFirstVisibleItemIndex(0)

        repeat(count - 1) { paginator.retrieveNextBatch() }
    }

    @Test
    fun `window never exceeds 3 active connections across every batch of 15 pages`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val paginator = createPaginator(this.backgroundScope)
        val totalPages = 15
        loadPages(paginator, totalPages)

        for (batch in 0 until totalPages) {
            paginator.updateFirstVisibleItemIndex((batch * Constants.PAGE_SIZE + 1).toLong())

            val expectedMin = (batch - 1).coerceAtLeast(0)
            val expectedMax = (batch + 1).coerceAtMost(totalPages - 1)
            val expectedSize = expectedMax - expectedMin + 1

            assertEquals(
                expectedSize, activeConnections.size,
                "Batch $batch: expected $expectedSize active (window [$expectedMin..$expectedMax]), got: $activeConnections"
            )
        }
    }

    @Test
    fun `scrolling backward reactivates previously cancelled connections`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val paginator = createPaginator(this.backgroundScope)
        loadPages(paginator, 5)

        paginator.updateFirstVisibleItemIndex(81)

        assertFalse("Key -1 should be cancelled") { activeConnections.contains(-1L) }

        paginator.updateFirstVisibleItemIndex(21)

        assertTrue("Key -1 should be reactivated") { activeConnections.contains(-1L) }
        assertTrue("Key 19 should be active") { activeConnections.contains(19L) }
        assertTrue("Key 39 should be active") { activeConnections.contains(39L) }
        assertFalse("Key 79 should be cancelled") { activeConnections.contains(79L) }
        assertEquals(3, activeConnections.size)
    }

    @Test
    fun `zigzag scrolling never breaks the 3-connection limit`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val paginator = createPaginator(this.backgroundScope)
        loadPages(paginator, 10)

        val zigzag = listOf(0, 9, 1, 8, 2, 7, 3, 6, 4, 5)
        for (batch in zigzag) {
            paginator.updateFirstVisibleItemIndex((batch * Constants.PAGE_SIZE + 1).toLong())

            assertTrue("Batch $batch: got ${activeConnections.size}") {
                activeConnections.size <= 3
            }
        }
    }

    @Test
    fun `single page stays active`() = runTest(UnconfinedTestDispatcher()) {
        val paginator = createPaginator(this.backgroundScope)
        loadPages(paginator, 1)

        assertEquals(1, activeConnections.size)
        assertTrue { activeConnections.contains(-1L) }
    }

    @Test
    fun `idempotent updates`() = runTest(UnconfinedTestDispatcher()) {
        val paginator = createPaginator(this.backgroundScope)
        loadPages(paginator, 5)

        paginator.updateFirstVisibleItemIndex(41)

        val snapshot = activeConnections.toSet()

        repeat(3) {
            paginator.updateFirstVisibleItemIndex(41)
            assertEquals(snapshot, activeConnections.toSet())
        }
    }

    @Test
    fun `cancelAndReset clears everything and allows fresh reload`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val paginator = createPaginator(this.backgroundScope)
        loadPages(paginator, 5)

        paginator.updateFirstVisibleItemIndex(61)
        assertTrue { activeConnections.isNotEmpty() }

        paginator.cancelAndReset()

        assertTrue("Connections cleared") { activeConnections.isEmpty() }
        assertFalse(paginator.isRetrieving)
        assertFalse(paginator.isPagesFinished)
        assertFalse(paginator.errorOccurred)

        loadPages(paginator, 3)
        paginator.updateFirstVisibleItemIndex(21)

        assertTrue { activeConnections.contains(19L) }
        assertTrue { activeConnections.size <= 3 }
    }

    @Test
    fun `isPagesFinished stops further fetches`() = runTest(UnconfinedTestDispatcher()) {
        var callCount = 0
        val paginator = Paginator<String>(
            coroutineScope = this.backgroundScope,
            onRetrieve = { _, _ ->
                callCount++
                flow {
                    emit(
                        if (callCount == 1) Result.Success(List(Constants.PAGE_SIZE) { "item $it" })
                        else Result.Success(emptyList())
                    )
                }
            },
            onRetrieved = { (currentId, _), _ ->
                val nextId =
                    if (currentId == Constants.EMPTY_LAST_SEEN_ID) 19L else currentId + Constants.PAGE_SIZE
                nextId to ""
            },
            onError = {},
            onRetrieving = {},
            onPagesFinished = {}
        )

        paginator.retrieveNextBatch()
        paginator.updateFirstVisibleItemIndex(0)

        assertFalse(paginator.isPagesFinished)

        paginator.retrieveNextBatch()

        assertTrue(paginator.isPagesFinished)

        val count = callCount
        paginator.retrieveNextBatch()

        assertEquals(count, callCount, "No fetches after pages finished")
    }

    @Test
    fun `errorOccurred stops further fetches`() = runTest(UnconfinedTestDispatcher()) {
        var callCount = 0
        val paginator = Paginator<String>(
            coroutineScope = this.backgroundScope,
            onRetrieve = { _, _ ->
                callCount++
                flow {
                    emit(
                        if (callCount == 1) Result.Success(List(Constants.PAGE_SIZE) { "item $it" })
                        else Result.Failure("Network error")
                    )
                }
            },
            onRetrieved = { (currentId, _), _ ->
                val nextId =
                    if (currentId == Constants.EMPTY_LAST_SEEN_ID) 19L else currentId + Constants.PAGE_SIZE
                nextId to ""
            },
            onError = {},
            onRetrieving = {},
            onPagesFinished = {}
        )

        paginator.retrieveNextBatch()
        paginator.updateFirstVisibleItemIndex(0)

        paginator.retrieveNextBatch()

        assertTrue(paginator.errorOccurred)

        val count = callCount
        paginator.retrieveNextBatch()

        assertEquals(count, callCount, "No fetches after error")
    }
}