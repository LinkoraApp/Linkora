package com.sakethh.linkora.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.sakethh.linkora.domain.model.CaptureTrack

@Dao
interface CaptureTrackDao {
    @Insert
    suspend fun insertAProcessedId(captureTrack: CaptureTrack)

    @Query("SELECT capturedLinkId FROM CaptureTrack WHERE captureWorkerId IS NULL")
    suspend fun getAllLinksCaptureProcessedLinkIds(): List<String>

    @Query("DELETE FROM CaptureTrack WHERE captureWorkerId IS NULL")
    suspend fun deleteAllBulkCaptureIds()

    @Query("DELETE FROM CaptureTrack WHERE captureWorkerId = :id")
    suspend fun deleteByWorkerId(id: String)

    @Query("SELECT captureWorkerId FROM CaptureTrack WHERE captureWorkerId IS NOT NULL")
    suspend fun getAllWorkerIds(): List<String>

    @Query("DELETE FROM CaptureTrack WHERE capturedLinkId IN (:linkIds)")
    suspend fun deleteByLinkIds(linkIds: List<Long>)
}
