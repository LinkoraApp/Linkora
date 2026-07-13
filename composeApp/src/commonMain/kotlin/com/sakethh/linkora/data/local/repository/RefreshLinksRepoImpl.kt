package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.data.local.dao.RefreshLinkDao
import com.sakethh.linkora.domain.model.RefreshLink
import com.sakethh.linkora.domain.repository.local.RefreshLinksRepo

class RefreshLinksRepoImpl(
    private val refreshLinkDao: RefreshLinkDao,
) : RefreshLinksRepo {
    override suspend fun insertAProcessedId(refreshLink: RefreshLink) = refreshLinkDao.insertAProcessedId(refreshLink)

    override suspend fun getProcessedLinkIds(): List<Long> = refreshLinkDao.getProcessedLinkIds()

    override suspend fun deleteAllIds() {
        refreshLinkDao.deleteAllIds()
    }
}
