package com.sakethh.linkora.data.local.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.sakethh.linkora.domain.model.Snapshot

@Dao
interface SnapshotDao {
    @Query("SELECT * FROM snapshot WHERE id = :id")
    suspend fun getASnapshot(id: Long): Snapshot

    @Insert
    suspend fun addASnapshot(snapshot: Snapshot): Long

    @Query("DELETE FROM snapshot WHERE id = :id")
    suspend fun deleteASnapshot(id: Long)
}