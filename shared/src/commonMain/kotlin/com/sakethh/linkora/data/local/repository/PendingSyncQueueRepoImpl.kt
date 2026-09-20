package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.data.local.dao.PendingSyncQueueDao
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.repository.local.PendingSyncQueueRepo

class PendingSyncQueueRepoImpl(
    private val pendingSyncQueueDao: PendingSyncQueueDao,
) : PendingSyncQueueRepo {
    override suspend fun addInQueue(pendingSyncQueue: PendingSyncQueue) {
        pendingSyncQueueDao.addInQueue(pendingSyncQueue)
    }

    override suspend fun removeFromQueue(id: Long) {
        pendingSyncQueueDao.deleteFromQueue(id)
    }

    override suspend fun getAllItemsFromQueue(): List<PendingSyncQueue> = pendingSyncQueueDao.getAllItemsFromQueue()
}
