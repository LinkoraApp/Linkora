package com.sakethh.linkora.domain.model.link

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.utils.epochToReadableDateTime
import com.sakethh.linkora.utils.getSystemEpochSeconds
import com.sakethh.linkora.utils.host
import com.sakethh.linkora.utils.isATwitterUrl
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Entity(tableName = "links",
    indices = [
        Index(value = ["title"], name = "idx_links_title")
    ])
@Serializable
data class Link(
    val linkType: LinkType,
    @PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val remoteId: Long? = null,
    val title: String,
    val url: String,
    @SerialName("baseURL") // called baseUrl, but in some non-ui cases it kinda acts like host. back then I showed baseUrl instead of just host, so stuck with it
    @ColumnInfo(name = "baseURL")
    val host: String = if (url.isATwitterUrl()) "twitter.com" else url.host(throwOnException = false),
    val imgURL: String,
    val note: String,
    val idOfLinkedFolder: Long?,
    val userAgent: String?,
    val mediaType: MediaType = MediaType.IMAGE,
    val lastModified: Long = getSystemEpochSeconds()
) {


    @Ignore
    @Transient
    val date: String? = epochToReadableDateTime(lastModified)

    @Ignore
    @Transient
    var path: List<Folder>? = null

    class Invalid(message: String = Localization.getLocalizedString(Localization.Key.InvalidLink)) :
        Throwable(message)
}