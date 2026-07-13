package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.model.PendingSyncQueue

interface PendingSyncQueueRepo {
    suspend fun addInQueue(pendingSyncQueue: PendingSyncQueue)

    suspend fun removeFromQueue(id: Long)

    suspend fun getAllItemsFromQueue(): List<PendingSyncQueue>
}
