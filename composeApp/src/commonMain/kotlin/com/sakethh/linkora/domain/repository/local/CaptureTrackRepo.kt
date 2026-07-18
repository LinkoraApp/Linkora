package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.domain.model.CaptureTrack

interface CaptureTrackRepo {
    suspend fun insertAProcessedId(captureTrack: CaptureTrack)

    suspend fun getProcessedLinkIds(): List<Long>

    suspend fun deleteAllIds()

    suspend fun deleteByLinkIds(linkIds: List<Long>)
}
