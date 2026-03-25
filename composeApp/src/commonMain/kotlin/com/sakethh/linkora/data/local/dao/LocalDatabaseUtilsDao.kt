package com.sakethh.linkora.data.local.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.model.FlatChildFolderData
import com.sakethh.linkora.domain.model.FlatSearchResult
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.Sorting
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalDatabaseUtilsDao {

    @Query("SELECT COALESCE(MAX(localId), 0) FROM folders")
    suspend fun getFoldersRowCount(): Long


    @Query("""
    SELECT * FROM (
        SELECT *,
            CASE WHEN itemType = 'FOLDER' THEN 0 ELSE 1 END AS typeOrder,
            COALESCE(folderLocalId, linkLocalId) AS sortId
        FROM (
            SELECT 
                'FOLDER' AS itemType, folders.lastModified,
                folders.name AS folderName, folders.note AS folderNote, folders.parentFolderId AS folderParentId, folders.localId AS folderLocalId, folders.remoteId AS folderRemoteId, folders.isArchived AS folderIsArchived,
                NULL AS linkType, NULL AS linkLocalId, NULL AS linkRemoteId, NULL AS linkTitle, NULL AS linkUrl, NULL AS linkHost, NULL AS linkImgUrl, NULL AS linkNote, NULL AS linkIdOfLinkedFolder, NULL AS linkUserAgent, NULL AS linkMediaType, NULL AS linkTagsJson
            FROM folders WHERE folders.parentFolderID = :parentFolderId
            
            UNION ALL
            
            SELECT 
                'LINK' AS itemType, links.lastModified,
                NULL, NULL, NULL, NULL, NULL, NULL,
                links.linkType, links.localId, links.remoteId, links.title, links.url, links.baseURL, links.imgURL, links.note, links.idOfLinkedFolder, links.userAgent, links.mediaType,
                (SELECT json_group_array(json_object('localId', tags.localId, 'remoteId', tags.remoteId, 'lastModified', tags.lastModified, 'name', tags.name)) FROM tags INNER JOIN link_tags ON tags.localId = link_tags.tagId WHERE link_tags.linkId = links.localId) AS linkTagsJson
            FROM links WHERE links.linkType = :linkType AND links.idOfLinkedFolder = :parentFolderId
        )
    )
    WHERE 
        (:lastTypeOrder IS NULL)
        OR 
        (typeOrder > :lastTypeOrder) -- Moved from Folders (0) to Links (1)
        OR 
        (typeOrder = :lastTypeOrder AND (
             (:isAscending = 1 AND sortId > :lastSortId) -- Oldest First
             OR 
             (:isAscending = 0 AND sortId < :lastSortId) -- Newest First
        ))
    ORDER BY 
        typeOrder ASC,
        CASE WHEN :isAscending = 1 THEN sortId END ASC,
        CASE WHEN :isAscending = 0 THEN sortId END DESC
    LIMIT :pageSize
    """)
    fun getChildDataSortedById(
        parentFolderId: Long,
        linkType: LinkType,
        lastTypeOrder: Int?, // 0 for Folder, 1 for Link
        lastSortId: Long?,   // The ID of the last item
        isAscending: Boolean,
        pageSize: Int
    ): Flow<List<FlatChildFolderData>>


    @Query("""
    SELECT * FROM (
        SELECT *,
            CASE WHEN itemType = 'FOLDER' THEN 0 ELSE 1 END AS typeOrder,
            COALESCE(folderName, linkTitle) AS sortStr,
            COALESCE(folderLocalId, linkLocalId) AS uniqueId
        FROM (
            SELECT 
                'FOLDER' AS itemType, folders.lastModified,
                folders.name AS folderName, folders.note AS folderNote, folders.parentFolderId AS folderParentId, folders.localId AS folderLocalId, folders.remoteId AS folderRemoteId, folders.isArchived AS folderIsArchived,
                NULL AS linkType, NULL AS linkLocalId, NULL AS linkRemoteId, NULL AS linkTitle, NULL AS linkUrl, NULL AS linkHost, NULL AS linkImgUrl, NULL AS linkNote, NULL AS linkIdOfLinkedFolder, NULL AS linkUserAgent, NULL AS linkMediaType, NULL AS linkTagsJson
            FROM folders WHERE folders.parentFolderID = :parentFolderId
            
            UNION ALL
            
            SELECT 
                'LINK' AS itemType, links.lastModified,
                NULL, NULL, NULL, NULL, NULL, NULL,
                links.linkType, links.localId, links.remoteId, links.title, links.url, links.baseURL, links.imgURL, links.note, links.idOfLinkedFolder, links.userAgent, links.mediaType,
                (SELECT json_group_array(json_object('localId', tags.localId, 'remoteId', tags.remoteId, 'lastModified', tags.lastModified, 'name', tags.name)) FROM tags INNER JOIN link_tags ON tags.localId = link_tags.tagId WHERE link_tags.linkId = links.localId) AS linkTagsJson
            FROM links WHERE links.linkType = :linkType AND links.idOfLinkedFolder = :parentFolderId
        )
    )
    WHERE 
        (:lastTypeOrder IS NULL)
        OR 
        (typeOrder > :lastTypeOrder)
        OR 
        (typeOrder = :lastTypeOrder AND (
             (:isAscending = 1 AND (
                 sortStr COLLATE NOCASE > :lastSortStr 
                 OR (sortStr COLLATE NOCASE = :lastSortStr AND uniqueId > :lastUniqueId)
             ))
             OR 
             (:isAscending = 0 AND (
                 sortStr COLLATE NOCASE < :lastSortStr 
                 OR (sortStr COLLATE NOCASE = :lastSortStr AND uniqueId > :lastUniqueId)
             ))
        ))
    ORDER BY 
        typeOrder ASC,
        CASE WHEN :isAscending = 1 THEN sortStr END COLLATE NOCASE ASC,
        CASE WHEN :isAscending = 0 THEN sortStr END COLLATE NOCASE DESC,
        uniqueId ASC
    LIMIT :pageSize
    """)
    fun getChildDataSortedByName(
        parentFolderId: Long,
        linkType: LinkType,
        lastTypeOrder: Int?, // 0 for Folder, 1 for Link
        lastSortStr: String?, // The Name/Title of the last item
        lastUniqueId: Long?,  // The ID of the last item
        isAscending: Boolean,
        pageSize: Int
    ): Flow<List<FlatChildFolderData>> // UNION ALL in a typical RDBMS/SQL is a "god-sent" thing, i'm gonna tweet this

    @Query("""
    SELECT * FROM (
        SELECT *,
            CASE 
                WHEN itemType = '${Constants.TAG}' THEN 0 
                WHEN itemType = '${Constants.FOLDER}' THEN 1 
                ELSE 2 
            END as typeOrder,
            
            CASE 
                WHEN :sortOption IN ('${Sorting.A_TO_Z}', '${Sorting.Z_TO_A}') 
                THEN COALESCE(tagName, folderName, linkTitle) 
                ELSE '' 
            END as sortStr,
            
            CASE 
                WHEN :sortOption IN ('${Sorting.NEW_TO_OLD}', '${Sorting.OLD_TO_NEW}') 
                THEN COALESCE(tagLocalId, folderLocalId, linkLocalId) 
                ELSE 0 
            END as sortNum

        FROM (
            SELECT 
                '${Constants.TAG}' AS itemType,
                tags.localId AS tagLocalId, tags.remoteId AS tagRemoteId, tags.name AS tagName, tags.lastModified AS tagLastModified,
                NULL AS folderName, NULL AS folderNote, NULL AS folderParentId, NULL AS folderLocalId, NULL AS folderRemoteId, NULL AS folderIsArchived, NULL AS folderLastModified,
                NULL AS linkType, NULL AS linkLocalId, NULL AS linkRemoteId, NULL AS linkTitle, NULL AS linkUrl, NULL AS linkHost, NULL AS linkImgUrl, NULL AS linkNote, NULL AS linkIdOfLinkedFolder, NULL AS linkUserAgent, NULL AS linkMediaType, NULL AS linkLastModified,
                NULL AS linkTagsJson,
                tags.localId AS genericId 
            FROM tags
            WHERE (LOWER(tags.name) LIKE '%' || LOWER(:query) || '%')
            AND :shouldShowTags = 1
            
            UNION ALL
            
            SELECT 
                '${Constants.FOLDER}' AS itemType,
                NULL, NULL, NULL, NULL,
                folders.name, folders.note, folders.parentFolderId, folders.localId, folders.remoteId, folders.isArchived, folders.lastModified,
                NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
                NULL,
                folders.localId AS genericId
            FROM folders
            WHERE (LOWER(folders.name) LIKE '%' || LOWER(:query) || '%' OR LOWER(folders.note) LIKE '%' || LOWER(:query) || '%')
            AND :shouldShowFolders = 1
            AND (
                (folders.isArchived = 1 AND :includeArchivedFolders = 1)
                OR
                ((folders.isArchived = 0 OR folders.isArchived IS NULL) AND :includeRegularFolders = 1)
            )
            
            UNION ALL
            
            SELECT 
                '${Constants.LINK}' AS itemType,
                NULL, NULL, NULL, NULL,
                NULL, NULL, NULL, NULL, NULL, NULL, NULL,
                links.linkType, links.localId, links.remoteId, links.title, links.url, links.baseURL, links.imgURL, links.note, links.idOfLinkedFolder, links.userAgent, links.mediaType, links.lastModified,
                (SELECT json_group_array(json_object('localId', tags.localId, 'remoteId', tags.remoteId, 'lastModified', tags.lastModified, 'name', tags.name)) 
                 FROM tags INNER JOIN link_tags ON tags.localId = link_tags.tagId WHERE link_tags.linkId = links.localId) AS linkTagsJson,
                links.localId AS genericId
            FROM links
            WHERE (LOWER(links.title) LIKE '%' || LOWER(:query) || '%' OR LOWER(links.note) LIKE '%' || LOWER(:query) || '%' OR LOWER(links.url) LIKE '%' || LOWER(:query) || '%')
            AND :shouldShowLinks = 1
            AND (:isLinkTypeFilterActive = 0 OR links.linkType IN (:activeLinkTypeFilters))
        )
    )
    WHERE 
        (:lastTypeOrder = -1)
        OR
        (
            (typeOrder > :lastTypeOrder)
            OR
            (typeOrder = :lastTypeOrder AND (
                (:sortOption = '${Sorting.A_TO_Z}' AND sortStr > :lastSortStr) OR
                (:sortOption = '${Sorting.Z_TO_A}' AND sortStr < :lastSortStr) OR
                (:sortOption = '${Sorting.NEW_TO_OLD}' AND sortNum < :lastSortNum) OR
                (:sortOption = '${Sorting.OLD_TO_NEW}' AND sortNum > :lastSortNum)
            ))
            OR
            (typeOrder = :lastTypeOrder AND sortStr = :lastSortStr AND sortNum = :lastSortNum AND (
                 genericId > :lastId
            ))
        )

    ORDER BY 
        typeOrder ASC,
        CASE WHEN :sortOption = '${Sorting.A_TO_Z}' THEN sortStr END ASC,
        CASE WHEN :sortOption = '${Sorting.Z_TO_A}' THEN sortStr END DESC,
        CASE WHEN :sortOption = '${Sorting.NEW_TO_OLD}' THEN sortNum END DESC,
        CASE WHEN :sortOption = '${Sorting.OLD_TO_NEW}' THEN sortNum END ASC
    LIMIT :pageSize
""")
    fun search(
        query: String,
        sortOption: String,
        pageSize: Int,
        shouldShowTags: Boolean,
        shouldShowFolders: Boolean,
        includeArchivedFolders: Boolean,
        includeRegularFolders: Boolean,
        shouldShowLinks: Boolean,
        isLinkTypeFilterActive: Boolean,
        activeLinkTypeFilters: List<String>,
        lastTypeOrder: Int,
        lastSortStr: String,
        lastSortNum: Long,
        lastId: Long
    ): Flow<List<FlatSearchResult>>

}