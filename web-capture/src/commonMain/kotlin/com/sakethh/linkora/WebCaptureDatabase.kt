package com.sakethh.linkora

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Upsert

@Entity(tableName = "metadata")
data class WebCaptureMetadata(
    @PrimaryKey val link: String,
    val uuid: String,
)

@Dao
interface WebCaptureMetadataDao {
    @Upsert
    suspend fun insert(metaData: WebCaptureMetadata)

    @Query("SELECT uuid FROM metadata WHERE link = :link")
    suspend fun getFolderNameByLink(link: String): String?

    @Query("SELECT link FROM metadata")
    suspend fun getAllLinks(): List<String>
}

@Database(entities = [WebCaptureMetadata::class], version = 1)
@ConstructedBy(WebCaptureDatabaseConstructor::class)
abstract class WebCaptureDatabase : RoomDatabase() {
    companion object {
        const val NAME = "WebCaptureMetadata"
    }

    abstract val webCaptureMetadataDao: WebCaptureMetadataDao
}
