package com.sakethh.linkora.domain.model

import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sakethh.linkora.utils.getSystemEpochSeconds
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(tableName = "folders",
    indices = [
        Index(value = ["name"], name = "idx_folders_name")
    ])
@Serializable
data class Folder(
    val name: String,
    val note: String,
    val parentFolderId: Long?,
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val remoteId: Long? = null,
    val isArchived: Boolean = false,
    val lastModified: Long = getSystemEpochSeconds()
) {


    @Ignore
    @Transient
    var path: List<Folder>? = null

    class FolderAlreadyExists(message: String) : Throwable(message)
    class InvalidName(message: String) : Throwable(message)
}
