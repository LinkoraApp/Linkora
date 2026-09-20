package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.model.RefreshLink

interface RefreshLinksRepo {
    suspend fun insertAProcessedId(refreshLink: RefreshLink)

    suspend fun getProcessedLinkIds(): List<Long>

    suspend fun deleteAllIds()
}
