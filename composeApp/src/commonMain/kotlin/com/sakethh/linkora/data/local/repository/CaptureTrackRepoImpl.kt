package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.data.local.dao.CaptureTrackDao
import com.sakethh.linkora.domain.model.CaptureTrack
import com.sakethh.linkora.domain.repository.local.CaptureTrackRepo

class CaptureTrackRepoImpl(
    private val captureTrackDao: CaptureTrackDao,
) : CaptureTrackRepo {
    override suspend fun insertAProcessedId(captureTrack: CaptureTrack) = captureTrackDao.insertAProcessedId(captureTrack)

    override suspend fun getProcessedLinkIds(): List<Long> = captureTrackDao.getProcessedLinkIds()

    override suspend fun deleteAllIds() {
        captureTrackDao.deleteAllIds()
    }

    override suspend fun deleteByLinkIds(linkIds: List<Long>) {
        captureTrackDao.deleteByLinkIds(linkIds)
    }
}
