package com.sakethh.linkora.domain.repository.remote

interface RemoteSyncRepo {
    suspend fun readSocketEvents()
    suspend fun updateDataBasedOnRemoteTombstones(timeStampAfter: Long)
}