package com.sakethh.linkora.data.sync

import com.sakethh.linkora.data.remote.repository.sync.SyncSessionServiceImpl
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.SyncType
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.ServerConnection
import com.sakethh.linkora.domain.repository.NetworkRepo
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.domain.repository.remote.SyncSessionService
import com.sakethh.linkora.platform.Network
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class SyncSessionServiceTest {

    private lateinit var remoteSyncRepo: RemoteSyncRepo
    private lateinit var networkRepo: NetworkRepo
    private lateinit var network: Network

    @BeforeTest
    fun setup() {
        clearAllMocks()
        remoteSyncRepo = mockk(relaxed = true)
        networkRepo = mockk(relaxed = true)
        network = mockk(relaxed = true)
    }

    @AfterTest
    fun tearDown() {
        clearAllMocks()
    }

    private fun createService(dispatcher: CoroutineDispatcher): SyncSessionService {
        return SyncSessionServiceImpl(
            remoteSyncRepo = remoteSyncRepo,
            networkRepo = networkRepo,
            network = network,
            syncDispatcher = dispatcher,
        )
    }

    private fun syncTest(
        block: suspend TestScope.(SyncSessionService, CoroutineDispatcher) -> Unit,
    ) =
        runTest(timeout = 5.seconds) {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val service = createService(dispatcher)

            try {
                block(service, dispatcher)
            } finally {
                service.stop()
                advanceUntilIdle()
            }
        }

    private fun TestScope.startCollector(
        flow: Flow<SyncSessionService.SyncState>,
        dispatcher: CoroutineDispatcher,
    ): Pair<MutableList<SyncSessionService.SyncState>, Job> {
        val states = mutableListOf<SyncSessionService.SyncState>()
        val job =
            launch(dispatcher) {
                flow.collect {
                    states.add(it)
                }
            }
        return states to job
    }

    private fun serverConnection(
        url: String = "https://valid.url",
        type: SyncType = SyncType.TwoWay,
        token: String = "token",
    ): ServerConnection {
        return mockk<ServerConnection>(relaxed = true) {
            every { serverUrl } returns url
            every { syncType } returns type
            every { authToken } returns token
        }
    }

    private fun mockSuccessfulSetup() {
        coEvery { network.configureSyncServerClient(any()) } returns Unit
        coEvery { networkRepo.isSyncServerReachable(any(), any()) } returns true
    }

    private fun stubPush(flow: Flow<Result<Unit>>) {
        coEvery {
            with(remoteSyncRepo) {
                any<SendChannel<Result<Unit>>>().pushPendingSyncQueueToServer<Unit>()
            }
        } returns flow
    }

    private fun stubPushMany(vararg flows: Flow<Result<Unit>>) {
        coEvery {
            with(remoteSyncRepo) {
                any<SendChannel<Result<Unit>>>().pushPendingSyncQueueToServer()
            }
        } returnsMany flows.toList()
    }

    private fun verifyPush(exactly: Int) {
        coVerify(exactly = exactly) {
            with(remoteSyncRepo) {
                any<SendChannel<Result<Unit>>>().pushPendingSyncQueueToServer<Unit>()
            }
        }
    }

    private fun stubTombstones(flow: Flow<Result<Unit>>) {
        coEvery {
            remoteSyncRepo.applyUpdatesBasedOnRemoteTombstones(any())
        } returns flow
    }

    private fun stubRemoteUpdates(flow: Flow<Result<Unit>>) {
        coEvery {
            remoteSyncRepo.applyUpdatesFromRemote(any())
        } returns flow
    }

    private fun stubSocketReturns() {
        coEvery {
            remoteSyncRepo.readSocketEvents(any())
        } returns Unit
    }

    private fun stubSocketThrows(error: Throwable) {
        coEvery {
            remoteSyncRepo.readSocketEvents(any())
        } throws error
    }

    private fun stubAllSuccess() {
        stubPush(flowOf(Result.Success(Unit)))
        stubTombstones(flowOf(Result.Success(Unit)))
        stubRemoteUpdates(flowOf(Result.Success(Unit)))
        stubSocketReturns()
    }

    @Test
    fun `start with blank server url emits InvalidServerConfiguration`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()
            stubAllSuccess()

            val connection = serverConnection(url = "")
            val correlation = Correlation("id", "name")

            val (states, collector) =
                startCollector(
                    service.start(
                        lastSynced = 0L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            assertTrue(states.any { it is SyncSessionService.SyncState.InvalidServerConfiguration })
            assertFalse(service.isBatchSyncRunning.value)
            verifyPush(exactly = 0)
        }

    @Test
    fun `start when network configuration fails emits InvalidSyncServerClientConfiguration`() =
        syncTest { service, dispatcher ->
            coEvery { network.configureSyncServerClient(any()) } throws
                RuntimeException("Config failed")
            coEvery { networkRepo.isSyncServerReachable(any(), any()) } returns true
            stubAllSuccess()

            val connection = serverConnection()
            val correlation = Correlation("id", "name")

            val (states, collector) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            assertTrue(
                states.any {
                    it is SyncSessionService.SyncState.InvalidSyncServerClientConfiguration
                },
            )
            assertFalse(service.isBatchSyncRunning.value)
            verifyPush(exactly = 0)
        }

    @Test
    fun `start when server is unreachable emits ConnectionFailed`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()
            coEvery { networkRepo.isSyncServerReachable(any(), any()) } returns false
            stubAllSuccess()

            val connection = serverConnection()
            val correlation = Correlation("id", "name")

            val (states, collector) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            assertTrue(states.any { it is SyncSessionService.SyncState.ConnectionFailed })
            assertFalse(service.isBatchSyncRunning.value)
            verifyPush(exactly = 0)
        }

    @Test
    fun `start with TwoWay sync triggers push tombstone remote and socket operations`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()
            stubAllSuccess()

            val connection = serverConnection(type = SyncType.TwoWay)
            val correlation = Correlation("id", "name")

            val (_, collector) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            verifyPush(exactly = 1)
            coVerify(exactly = 1) { remoteSyncRepo.applyUpdatesBasedOnRemoteTombstones(1000L) }
            coVerify(exactly = 1) { remoteSyncRepo.applyUpdatesFromRemote(1000L) }
            coVerify(exactly = 1) { remoteSyncRepo.readSocketEvents(correlation) }
            assertFalse(service.isBatchSyncRunning.value)
        }

    @Test
    fun `start with ClientToServer sync triggers only push operations`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()
            stubAllSuccess()

            val connection = serverConnection(type = SyncType.ClientToServer)
            val correlation = Correlation("id", "name")

            val (_, collector) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            verifyPush(exactly = 1)
            coVerify(exactly = 0) { remoteSyncRepo.applyUpdatesBasedOnRemoteTombstones(any()) }
            coVerify(exactly = 0) { remoteSyncRepo.applyUpdatesFromRemote(any()) }
            coVerify(exactly = 0) { remoteSyncRepo.readSocketEvents(any()) }
            assertFalse(service.isBatchSyncRunning.value)
        }

    @Test
    fun `start with ServerToClient sync triggers only pull and socket operations`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()
            stubAllSuccess()

            val connection = serverConnection(type = SyncType.ServerToClient)
            val correlation = Correlation("id", "name")

            val (_, collector) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            verifyPush(exactly = 0)
            coVerify(exactly = 1) { remoteSyncRepo.applyUpdatesBasedOnRemoteTombstones(1000L) }
            coVerify(exactly = 1) { remoteSyncRepo.applyUpdatesFromRemote(1000L) }
            coVerify(exactly = 1) { remoteSyncRepo.readSocketEvents(correlation) }
            assertFalse(service.isBatchSyncRunning.value)
        }

    @Test
    fun `start with lastSynced 0 passes 0 to pull operations for initial sync`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()
            stubAllSuccess()

            val connection = serverConnection(type = SyncType.TwoWay)
            val correlation = Correlation("id", "name")

            val (_, collector) =
                startCollector(
                    service.start(
                        lastSynced = 0L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            coVerify(exactly = 1) { remoteSyncRepo.applyUpdatesBasedOnRemoteTombstones(0L) }
            coVerify(exactly = 1) { remoteSyncRepo.applyUpdatesFromRemote(0L) }
        }

    @Test
    fun `second start cancels previous session and starts a new active session`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()

            var firstPushStarted = false
            var firstPushCancelled = false
            var secondPushStarted = false
            var secondPushCancelled = false

            stubPushMany(
                flow {
                    firstPushStarted = true
                    try {
                        awaitCancellation()
                    } finally {
                        firstPushCancelled = true
                    }
                },
                flow {
                    secondPushStarted = true
                    try {
                        awaitCancellation()
                    } finally {
                        secondPushCancelled = true
                    }
                },
            )

            stubTombstones(flowOf(Result.Success(Unit)))
            stubRemoteUpdates(flowOf(Result.Success(Unit)))
            stubSocketReturns()

            val connection = serverConnection(type = SyncType.TwoWay)
            val correlation = Correlation("id", "name")

            val (_, collector1) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()

            assertTrue(firstPushStarted)
            assertFalse(firstPushCancelled)
            assertTrue(service.isBatchSyncRunning.value)

            val (_, collector2) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()

            assertTrue(firstPushCancelled)
            assertTrue(secondPushStarted)
            assertFalse(secondPushCancelled)
            assertTrue(service.isBatchSyncRunning.value)
            verifyPush(exactly = 2)

            service.stop()
            advanceUntilIdle()

            assertTrue(secondPushCancelled)
            assertFalse(service.isBatchSyncRunning.value)

            collector1.cancelAndJoin()
            collector2.cancelAndJoin()
        }

    @Test
    fun `second start cancels previous socket and starts a new socket`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()

            var firstSocketStarted = false
            var firstSocketCancelled = false
            var secondSocketStarted = false
            var secondSocketCancelled = false

            stubPush(flowOf(Result.Success(Unit)))
            stubTombstones(flowOf(Result.Success(Unit)))
            stubRemoteUpdates(flowOf(Result.Success(Unit)))

            var socketCallCount = 0

            coEvery {
                remoteSyncRepo.readSocketEvents(any())
            } coAnswers
                {
                    socketCallCount++

                    when (socketCallCount) {
                        1 -> {
                            firstSocketStarted = true
                            try {
                                awaitCancellation()
                            } finally {
                                firstSocketCancelled = true
                            }
                        }

                        else -> {
                            secondSocketStarted = true
                            try {
                                awaitCancellation()
                            } finally {
                                secondSocketCancelled = true
                            }
                        }
                    }
                }

            val connection = serverConnection(type = SyncType.TwoWay)
            val correlation = Correlation("id", "name")

            val (_, collector1) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()

            assertTrue(firstSocketStarted)
            assertFalse(firstSocketCancelled)

            val (_, collector2) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()

            assertTrue(firstSocketCancelled)
            assertTrue(secondSocketStarted)
            assertFalse(secondSocketCancelled)

            service.stop()
            advanceUntilIdle()

            assertTrue(secondSocketCancelled)

            collector1.cancelAndJoin()
            collector2.cancelAndJoin()
        }

    @Test
    fun `stop cancels active batch sync and sets isSyncing to false`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()

            var pushCancelled = false

            stubPush(
                flow {
                    try {
                        awaitCancellation()
                    } finally {
                        pushCancelled = true
                    }
                },
            )

            stubTombstones(flowOf(Result.Success(Unit)))
            stubRemoteUpdates(flowOf(Result.Success(Unit)))
            stubSocketReturns()

            val connection = serverConnection(type = SyncType.TwoWay)
            val correlation = Correlation("id", "name")

            val (_, collector) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            assertTrue(service.isBatchSyncRunning.value)

            service.stop()
            advanceUntilIdle()

            assertTrue(pushCancelled)
            assertFalse(service.isBatchSyncRunning.value)

            collector.cancelAndJoin()
        }

    @Test
    fun `calling stop multiple times is safe`() = syncTest { service, _ ->
        service.stop()
        service.stop()
        service.stop()

        assertFalse(service.isBatchSyncRunning.value)
    }

    @Test
    fun `push failure emits SyncOpFailed but does not cancel pull operations`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()

            val pushException = RuntimeException("Push failed")

            stubPush(flowOf<Result<Unit>>(Result.Failure(pushException)))
            stubTombstones(flowOf(Result.Success(Unit)))
            stubRemoteUpdates(flowOf(Result.Success(Unit)))
            stubSocketReturns()

            val connection = serverConnection(type = SyncType.TwoWay)
            val correlation = Correlation("id", "name")

            val (states, collector) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            assertTrue(
                states.any {
                    it is SyncSessionService.SyncState.SyncOpFailed && it.throwable == pushException
                },
            )

            assertTrue(states.any { it is SyncSessionService.SyncState.SyncBatchHadFailure })
            assertFalse(states.any { it is SyncSessionService.SyncState.SyncBatchSuccessful })

            coVerify(exactly = 1) { remoteSyncRepo.applyUpdatesBasedOnRemoteTombstones(1000L) }
            coVerify(exactly = 1) { remoteSyncRepo.applyUpdatesFromRemote(1000L) }
            coVerify(exactly = 1) { remoteSyncRepo.readSocketEvents(correlation) }

            assertFalse(service.isBatchSyncRunning.value)
        }

    @Test
    fun `socket flow throwing emits SocketFailure`() = syncTest { service, dispatcher ->
        mockSuccessfulSetup()

        val socketException = RuntimeException("Socket died")

        stubPush(flowOf(Result.Success(Unit)))
        stubTombstones(flowOf(Result.Success(Unit)))
        stubRemoteUpdates(flowOf(Result.Success(Unit)))
        stubSocketThrows(socketException)

        val connection = serverConnection(type = SyncType.ServerToClient)
        val correlation = Correlation("id", "name")

        val (states, collector) =
            startCollector(
                service.start(
                    lastSynced = 1000L,
                    correlation = correlation,
                    serverConnection = connection,
                    bypassCertCheck = false,
                ),
                dispatcher,
            )

        advanceUntilIdle()
        collector.cancelAndJoin()

        assertTrue(
            states.any {
                it is SyncSessionService.SyncState.SocketFailure && it.throwable == socketException
            },
        )

        assertTrue(states.any { it is SyncSessionService.SyncState.SyncBatchSuccessful })
        assertFalse(states.any { it is SyncSessionService.SyncState.SyncBatchHadFailure })
    }

    @Test
    fun `socket returning normally does not emit SocketFailure`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()

            stubPush(flowOf(Result.Success(Unit)))
            stubTombstones(flowOf(Result.Success(Unit)))
            stubRemoteUpdates(flowOf(Result.Success(Unit)))
            stubSocketReturns()

            val connection = serverConnection(type = SyncType.ServerToClient)
            val correlation = Correlation("id", "name")

            val (states, collector) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            assertFalse(states.any { it is SyncSessionService.SyncState.SocketFailure })
            assertTrue(states.any { it is SyncSessionService.SyncState.SyncBatchSuccessful })
        }

    @Test
    fun `loading result from push emits SyncLog`() = syncTest { service, dispatcher ->
        mockSuccessfulSetup()

        stubPush(
            flowOf(
                Result.Loading("Pushing items..."),
                Result.Success(Unit),
            ),
        )

        stubTombstones(flowOf(Result.Success(Unit)))
        stubRemoteUpdates(flowOf(Result.Success(Unit)))
        stubSocketReturns()

        val connection = serverConnection(type = SyncType.ClientToServer)
        val correlation = Correlation("id", "name")

        val (states, collector) =
            startCollector(
                service.start(
                    lastSynced = 1000L,
                    correlation = correlation,
                    serverConnection = connection,
                    bypassCertCheck = false,
                ),
                dispatcher,
            )

        advanceUntilIdle()
        collector.cancelAndJoin()

        assertTrue(
            states.any {
                it is SyncSessionService.SyncState.SyncLog && it.str == "Pushing items..."
            },
        )

        assertTrue(states.any { it is SyncSessionService.SyncState.SyncBatchSuccessful })
        assertFalse(states.any { it is SyncSessionService.SyncState.SyncBatchHadFailure })
    }

    @Test
    fun `isSyncing stays true while batch sync is active`() = syncTest { service, dispatcher ->
        mockSuccessfulSetup()

        var pushStarted = false
        var pushCancelled = false

        stubPush(
            flow {
                pushStarted = true
                try {
                    awaitCancellation()
                } finally {
                    pushCancelled = true
                }
            },
        )

        stubTombstones(flowOf(Result.Success(Unit)))
        stubRemoteUpdates(flowOf(Result.Success(Unit)))
        stubSocketReturns()

        val connection = serverConnection(type = SyncType.TwoWay)
        val correlation = Correlation("id", "name")

        val (_, collector) =
            startCollector(
                service.start(
                    lastSynced = 1000L,
                    correlation = correlation,
                    serverConnection = connection,
                    bypassCertCheck = false,
                ),
                dispatcher,
            )

        advanceUntilIdle()

        assertTrue(pushStarted)
        assertFalse(pushCancelled)
        assertTrue(service.isBatchSyncRunning.value)

        service.stop()
        advanceUntilIdle()

        assertTrue(pushCancelled)
        assertFalse(service.isBatchSyncRunning.value)

        collector.cancelAndJoin()
    }

    @Test
    fun `socket stays alive after batch sync completes`() = syncTest { service, dispatcher ->
        mockSuccessfulSetup()

        var socketStarted = false
        var socketCancelled = false

        stubPush(flowOf(Result.Success(Unit)))
        stubTombstones(flowOf(Result.Success(Unit)))
        stubRemoteUpdates(flowOf(Result.Success(Unit)))

        coEvery {
            remoteSyncRepo.readSocketEvents(any())
        } coAnswers
            {
                socketStarted = true
                try {
                    awaitCancellation()
                } finally {
                    socketCancelled = true
                }
            }

        val connection = serverConnection(type = SyncType.TwoWay)
        val correlation = Correlation("id", "name")

        val (states, collector) =
            startCollector(
                service.start(
                    lastSynced = 1000L,
                    correlation = correlation,
                    serverConnection = connection,
                    bypassCertCheck = false,
                ),
                dispatcher,
            )

        advanceUntilIdle()

        assertTrue(socketStarted)
        assertFalse(socketCancelled)
        assertFalse(service.isBatchSyncRunning.value)
        assertTrue(states.any { it is SyncSessionService.SyncState.SyncBatchSuccessful })

        service.stop()
        advanceUntilIdle()

        assertTrue(socketCancelled)

        collector.cancelAndJoin()
    }

    @Test
    fun `bypassCertCheck is passed to network configuration`() = syncTest { service, dispatcher ->
        mockSuccessfulSetup()
        stubAllSuccess()

        val connection = serverConnection(type = SyncType.ClientToServer)
        val correlation = Correlation("id", "name")

        val (_, collector) =
            startCollector(
                service.start(
                    lastSynced = 1000L,
                    correlation = correlation,
                    serverConnection = connection,
                    bypassCertCheck = true,
                ),
                dispatcher,
            )

        advanceUntilIdle()
        collector.cancelAndJoin()

        coVerify(exactly = 1) {
            network.configureSyncServerClient(true)
        }
    }

    @Test
    fun `reachability check uses server url plus test bearer route and auth token`() =
        syncTest { service, dispatcher ->
            mockSuccessfulSetup()
            stubAllSuccess()

            val connection =
                serverConnection(
                    url = "https://valid.url",
                    type = SyncType.ClientToServer,
                    token = "secret-token",
                )
            val correlation = Correlation("id", "name")

            val (_, collector) =
                startCollector(
                    service.start(
                        lastSynced = 1000L,
                        correlation = correlation,
                        serverConnection = connection,
                        bypassCertCheck = false,
                    ),
                    dispatcher,
                )

            advanceUntilIdle()
            collector.cancelAndJoin()

            coVerify(exactly = 1) {
                networkRepo.isSyncServerReachable(
                    serverUrl = "https://valid.url" + SyncServerRoute.TEST_BEARER.name,
                    token = "secret-token",
                )
            }
        }
}
