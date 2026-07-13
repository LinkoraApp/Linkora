package com.sakethh.linkora

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase

@Entity(tableName = "metadata")
data class MetaDataEntity(
    @PrimaryKey val link: String,
    val uuid: String,
)

@Dao
interface MetaDataDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(metaData: MetaDataEntity)

    @Query("SELECT uuid FROM metadata WHERE link = :link")
    suspend fun getFolderNameByLink(link: String): String?

    @Query("SELECT link FROM metadata")
    suspend fun getAllCapturedLinks(): List<String>
}

@Database(entities = [MetaDataEntity::class], version = 1)
@ConstructedBy(WebCaptureDatabaseConstructor::class)
abstract class WebCaptureDatabase : RoomDatabase() {
    companion object {
        const val NAME = "WebCapture"
    }

    abstract val metaDataDao: MetaDataDao
}
