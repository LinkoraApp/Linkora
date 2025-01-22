package com.sakethh.linkora.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sakethh.linkora.common.utils.LinkType
import com.sakethh.linkora.common.utils.Sorting
import com.sakethh.linkora.domain.model.link.Link
import kotlinx.coroutines.flow.Flow

@Dao
interface LinksDao {
    @Insert
    suspend fun addANewLink(link: Link): Long

    @Insert
    suspend fun addMultipleLinks(links: List<Link>)

    @Query("DELETE FROM links WHERE idOfLinkedFolder = :folderId")
    suspend fun deleteLinksOfFolder(folderId: Long)

    @Query("UPDATE links SET note = '' WHERE localId=:linkId")
    suspend fun deleteALinkNote(linkId: Long)

    @Query("UPDATE links SET linkType = '${LinkType.ARCHIVE_LINK}' WHERE localId=:linkId")
    suspend fun archiveALink(linkId: Long)

    @Query("DELETE FROM links WHERE localId = :linkId")
    suspend fun deleteALink(linkId: Long)

    @Query("UPDATE links SET note = :newNote WHERE localId=:linkId")
    suspend fun updateLinkNote(linkId: Long, newNote: String)

    @Query("UPDATE links SET title = :newTitle WHERE localId=:linkId")
    suspend fun updateLinkTitle(linkId: Long, newTitle: String)

    @Query("SELECT linkType = '${LinkType.IMPORTANT_LINK}' OR markedAsImportant FROM links WHERE url=:url")
    suspend fun markedAsImportant(url: String): Boolean

    @Query("SELECT linkType = '${LinkType.ARCHIVE_LINK}' FROM links WHERE url=:url")
    suspend fun isInArchive(url: String): Boolean

    @Query(
        "SELECT * FROM links \n" +
                "    WHERE (LOWER(title) LIKE '%' || LOWER(:query) || '%' \n" +
                "           OR LOWER(note) LIKE '%' || LOWER(:query) || '%') \n" +
                "    ORDER BY \n" +
                "        CASE WHEN :sortOption = '${Sorting.A_TO_Z}' THEN title COLLATE NOCASE END ASC,\n" +
                "        CASE WHEN :sortOption = '${Sorting.Z_TO_A}' THEN title COLLATE NOCASE END DESC,\n" +
                "        CASE WHEN :sortOption = '${Sorting.NEW_TO_OLD}' THEN localId END DESC,\n" +
                "        CASE WHEN :sortOption = '${Sorting.OLD_TO_NEW}' THEN localId END ASC"
    )
    fun search(query: String, sortOption: String): Flow<List<Link>>

    @Query(
        """
    SELECT * FROM links 
    WHERE 
        (:linkType = '${LinkType.IMPORTANT_LINK}' AND (linkType = '${LinkType.IMPORTANT_LINK}' OR markedAsImportant = 1))
        OR (:linkType != '${LinkType.IMPORTANT_LINK}' AND linkType = :linkType)
    ORDER BY 
        CASE WHEN :sortOption = '${Sorting.A_TO_Z}' THEN title COLLATE NOCASE END ASC,
        CASE WHEN :sortOption = '${Sorting.Z_TO_A}' THEN title COLLATE NOCASE END DESC,
        CASE WHEN :sortOption = '${Sorting.NEW_TO_OLD}' THEN localId END DESC,
        CASE WHEN :sortOption = '${Sorting.OLD_TO_NEW}' THEN localId END ASC
    """
    )
    fun sortLinks(
        linkType: com.sakethh.linkora.domain.LinkType, sortOption: String
    ): Flow<List<Link>>

    @Query(
        """
    SELECT * FROM links 
    ORDER BY 
        CASE WHEN :sortOption = '${Sorting.A_TO_Z}' THEN title COLLATE NOCASE END ASC,
        CASE WHEN :sortOption = '${Sorting.Z_TO_A}' THEN title COLLATE NOCASE END DESC,
        CASE WHEN :sortOption = '${Sorting.NEW_TO_OLD}' THEN localId END DESC,
        CASE WHEN :sortOption = '${Sorting.OLD_TO_NEW}' THEN localId END ASC
    """
    )
    fun sortAllLinks(
        sortOption: String
    ): Flow<List<Link>>


    @Query(
        """
    SELECT * FROM links 
    WHERE linkType = :linkType AND idOfLinkedFolder = :parentFolderId 
    ORDER BY 
        CASE WHEN :sortOption = '${Sorting.A_TO_Z}' THEN title COLLATE NOCASE END ASC,
        CASE WHEN :sortOption = '${Sorting.Z_TO_A}' THEN title COLLATE NOCASE END DESC,
        CASE WHEN :sortOption = '${Sorting.NEW_TO_OLD}' THEN localId END DESC,
        CASE WHEN :sortOption = '${Sorting.OLD_TO_NEW}' THEN localId END ASC
    """
    )
    fun sortLinks(
        linkType: com.sakethh.linkora.domain.LinkType, parentFolderId: Long, sortOption: String
    ): Flow<List<Link>>

    @Query("SELECT * FROM links")
    suspend fun getAllLinks(): List<Link>

    @Query("SELECT * FROM links WHERE idOfLinkedFolder=:folderId")
    suspend fun getLinksOfThisFolderAsList(folderId: Long): List<Link>

    @Query("DELETE FROM links")
    suspend fun deleteAllLinks()


    @Update
    suspend fun updateALink(link: Link)

    @Query("SELECT remoteId FROM links WHERE localId = :localId LIMIT 1")
    suspend fun getRemoteIdOfLocalLink(localId: Long): Long?

    @Query("SELECT localId FROM links WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getLocalIdOfALink(remoteId: Long): Long?

    @Query("SELECT MAX(localId) FROM links")
    suspend fun getLatestId(): Long

    @Query("SELECT * FROM links WHERE localId = :localId LIMIT 1")
    suspend fun getLink(localId: Long): Link
}