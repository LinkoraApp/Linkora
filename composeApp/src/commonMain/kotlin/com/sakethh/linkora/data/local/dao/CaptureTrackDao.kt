package com.sakethh.linkora.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.sakethh.linkora.domain.model.CaptureTrack

@Dao
interface CaptureTrackDao {
    @Insert suspend fun insertAProcessedId(captureTrack: CaptureTrack)

    @Query("SELECT capturedLinkId FROM CaptureTrack")
    suspend fun getProcessedLinkIds(): List<Long>

    @Query("DELETE FROM CaptureTrack")
    suspend fun deleteAllIds()

    @Query("DELETE FROM CaptureTrack WHERE capturedLinkId IN (:linkIds)")
    suspend fun deleteByLinkIds(linkIds: List<Long>)
}
