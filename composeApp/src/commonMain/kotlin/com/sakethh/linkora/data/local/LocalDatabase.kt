package com.sakethh.linkora.data.local

import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.TypeConverters
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.LocalDatabaseUtilsDao
import com.sakethh.linkora.data.local.dao.LocalizationDao
import com.sakethh.linkora.data.local.dao.PanelsDao
import com.sakethh.linkora.data.local.dao.PendingSyncQueueDao
import com.sakethh.linkora.data.local.dao.RefreshLinkDao
import com.sakethh.linkora.data.local.dao.SnapshotDao
import com.sakethh.linkora.data.local.dao.TagsDao
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.model.RefreshLink
import com.sakethh.linkora.domain.model.Snapshot
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.domain.model.localization.LocalizedString
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder
import com.sakethh.linkora.domain.model.tag.LinkTag
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.getSystemEpochSeconds

@Database(
    version = 14,
    exportSchema = true,
    entities = [Link::class, Folder::class,
        LocalizedString::class, LocalizedLanguage::class,
        Panel::class, PanelFolder::class, PendingSyncQueue::class,
        Snapshot::class, Tag::class, LinkTag::class, RefreshLink::class]
)
@TypeConverters(TypeConverter::class)
abstract class LocalDatabase : RoomDatabase() {

    companion object {
        const val NAME = "linkora_db"


        val MIGRATION_1_2 = object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {

                connection.execSQL("DROP TABLE IF EXISTS new_folders_table;")
                connection.execSQL("CREATE TABLE IF NOT EXISTS new_folders_table (folderName TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
                connection.execSQL("INSERT INTO new_folders_table (folderName, infoForSaving) SELECT folderName, infoForSaving FROM folders_table;")
                connection.execSQL("DROP TABLE IF EXISTS folders_table;")
                connection.execSQL("ALTER TABLE new_folders_table RENAME TO folders_table;")

                connection.execSQL("DROP TABLE IF EXISTS new_archived_links_table;")
                connection.execSQL("CREATE TABLE IF NOT EXISTS new_archived_links_table (title TEXT NOT NULL, webURL TEXT NOT NULL, baseURL TEXT NOT NULL, imgURL TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
                connection.execSQL("INSERT INTO new_archived_links_table (title, webURL, baseURL, imgURL, infoForSaving) SELECT title, webURL, baseURL, imgURL, infoForSaving FROM archived_links_table;")
                connection.execSQL("DROP TABLE IF EXISTS archived_links_table;")
                connection.execSQL("ALTER TABLE new_archived_links_table RENAME TO archived_links_table;")

                connection.execSQL("DROP TABLE IF EXISTS new_archived_folders_table;")
                connection.execSQL("CREATE TABLE IF NOT EXISTS new_archived_folders_table (archiveFolderName TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
                connection.execSQL("INSERT INTO new_archived_folders_table (archiveFolderName, infoForSaving) SELECT archiveFolderName, infoForSaving FROM archived_folders_table;")
                connection.execSQL("DROP TABLE IF EXISTS archived_folders_table;")
                connection.execSQL("ALTER TABLE new_archived_folders_table RENAME TO archived_folders_table;")

                connection.execSQL("DROP TABLE IF EXISTS new_important_links_table;")
                connection.execSQL("CREATE TABLE IF NOT EXISTS new_important_links_table (title TEXT NOT NULL, webURL TEXT NOT NULL, baseURL TEXT NOT NULL, imgURL TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
                connection.execSQL("INSERT INTO new_important_links_table (title, webURL, baseURL, imgURL, infoForSaving) SELECT title, webURL, baseURL, imgURL, infoForSaving FROM important_links_table;")
                connection.execSQL("DROP TABLE IF EXISTS important_links_table;")
                connection.execSQL("ALTER TABLE new_important_links_table RENAME TO important_links_table;")

                connection.execSQL("DROP TABLE IF EXISTS new_important_folders_table;")
                connection.execSQL("CREATE TABLE IF NOT EXISTS new_important_folders_table (impFolderName TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
                connection.execSQL("INSERT INTO new_important_folders_table (impFolderName, infoForSaving) SELECT impFolderName, infoForSaving FROM important_folders_table;")
                connection.execSQL("DROP TABLE IF EXISTS important_folders_table;")
                connection.execSQL("ALTER TABLE new_important_folders_table RENAME TO important_folders_table;")

            }

        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {

                connection.execSQL("DROP TABLE IF EXISTS folders_table_new")
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `folders_table_new` (`folderName` TEXT NOT NULL, `infoForSaving` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `parentFolderID` INTEGER DEFAULT NULL, `childFolderIDs` TEXT DEFAULT NULL, `isFolderArchived` INTEGER NOT NULL DEFAULT 0, `isMarkedAsImportant` INTEGER NOT NULL DEFAULT 0)"
                )
                connection.execSQL(
                    "INSERT INTO folders_table_new (folderName, infoForSaving, id) " + "SELECT folderName, infoForSaving, id FROM folders_table"
                )
                connection.execSQL("DROP TABLE folders_table")
                connection.execSQL("ALTER TABLE folders_table_new RENAME TO folders_table")


                connection.execSQL("DROP TABLE IF EXISTS links_table_new")
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS `links_table_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `webURL` TEXT NOT NULL, `baseURL` TEXT NOT NULL, `imgURL` TEXT NOT NULL, `infoForSaving` TEXT NOT NULL, `isLinkedWithSavedLinks` INTEGER NOT NULL, `isLinkedWithFolders` INTEGER NOT NULL, `keyOfLinkedFolderV10` INTEGER DEFAULT NULL, `keyOfLinkedFolder` TEXT, `isLinkedWithImpFolder` INTEGER NOT NULL, `keyOfImpLinkedFolder` TEXT NOT NULL, `keyOfImpLinkedFolderV10` INTEGER DEFAULT NULL, `isLinkedWithArchivedFolder` INTEGER NOT NULL, `keyOfArchiveLinkedFolderV10` INTEGER DEFAULT NULL, `keyOfArchiveLinkedFolder` TEXT)"
                )
                connection.execSQL(
                    "INSERT INTO links_table_new (id, title, webURL, baseURL, imgURL, infoForSaving, " + "isLinkedWithSavedLinks, isLinkedWithFolders, keyOfLinkedFolder, " + "isLinkedWithImpFolder, keyOfImpLinkedFolder, " + "isLinkedWithArchivedFolder, keyOfArchiveLinkedFolder) " + "SELECT id, title, webURL, baseURL, imgURL, infoForSaving, " + "isLinkedWithSavedLinks, isLinkedWithFolders, keyOfLinkedFolder, " + "isLinkedWithImpFolder, keyOfImpLinkedFolder," + "isLinkedWithArchivedFolder, keyOfArchiveLinkedFolder " + "FROM links_table"
                )
                connection.execSQL("DROP TABLE links_table")
                connection.execSQL("ALTER TABLE links_table_new RENAME TO links_table")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `home_screen_list_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `position` INTEGER NOT NULL, `folderName` TEXT NOT NULL, `shouldSavedLinksTabVisible` INTEGER NOT NULL, `shouldImpLinksTabVisible` INTEGER NOT NULL)")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE IF EXISTS home_screen_list_table")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `shelf` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `shelfName` TEXT NOT NULL, `shelfIconName` TEXT NOT NULL, `folderIds` TEXT NOT NULL)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `home_screen_list_table` (`primaryKey` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `folderName` TEXT NOT NULL, `parentShelfID` INTEGER NOT NULL)")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `language` (`languageCode` TEXT NOT NULL, `languageName` TEXT NOT NULL, `localizedStringsCount` INTEGER NOT NULL, `contributionLink` TEXT NOT NULL, PRIMARY KEY(`languageCode`))")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `translation` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `languageCode` TEXT NOT NULL, `stringName` TEXT NOT NULL, `stringValue` TEXT NOT NULL)")
            }
        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `site_specific_user_agent` (`domain` TEXT NOT NULL, `userAgent` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
                connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_site_specific_user_agent_domain` ON `site_specific_user_agent` (`domain`)")
                connection.execSQL("ALTER TABLE links_table ADD COLUMN userAgent TEXT DEFAULT NULL")
                connection.execSQL("ALTER TABLE archived_links_table ADD COLUMN userAgent TEXT DEFAULT NULL")
                connection.execSQL("ALTER TABLE important_links_table ADD COLUMN userAgent TEXT DEFAULT NULL")
                connection.execSQL("ALTER TABLE recently_visited_table ADD COLUMN userAgent TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override suspend fun migrate(connection: SQLiteConnection) {

                connection.execSQL("CREATE TABLE IF NOT EXISTS `panel` (`panelId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `panelName` TEXT NOT NULL)")

                connection.execSQL(
                    """
            INSERT INTO panel (panelId, panelName)
            SELECT id, shelfName FROM shelf
        """.trimIndent()
                )

                connection.execSQL("DROP TABLE shelf")

                connection.execSQL("CREATE TABLE IF NOT EXISTS `panel_folder` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `folderId` INTEGER NOT NULL, `panelPosition` INTEGER NOT NULL, `folderName` TEXT NOT NULL, `connectedPanelId` INTEGER NOT NULL)")


                connection.execSQL(
                    """
            INSERT INTO panel_folder (folderId, panelPosition, folderName, connectedPanelId)
            SELECT id, position, folderName, parentShelfID FROM home_screen_list_table
            """.trimIndent()
                )

                connection.execSQL("DROP TABLE home_screen_list_table")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override suspend fun migrate(connection: SQLiteConnection) {
                val timestamp = getSystemEpochSeconds()

                connection.execSQL("CREATE TABLE IF NOT EXISTS `pending_sync_queue` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `operation` TEXT NOT NULL, `payload` TEXT NOT NULL)")

                connection.execSQL("CREATE TABLE IF NOT EXISTS `links` (`linkType` TEXT NOT NULL, `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `baseURL` TEXT NOT NULL, `imgURL` TEXT NOT NULL, `note` TEXT NOT NULL, `idOfLinkedFolder` INTEGER, `userAgent` TEXT, `markedAsImportant` INTEGER NOT NULL, `mediaType` TEXT NOT NULL, `lastModified` INTEGER NOT NULL DEFAULT $timestamp)")
                connection.execSQL(
                    """
                    INSERT INTO links (
                        remoteId,
                        title,
                        url,
                        baseURL,
                        imgURL,
                        note,
                        idOfLinkedFolder,
                        userAgent,
                        markedAsImportant,
                        mediaType,
                        linkType
                    )
                    SELECT
                        NULL AS remoteId,
                        title,
                        webURL AS url,
                        baseURL,
                        imgURL,
                        infoForSaving AS note,
                        keyOfLinkedFolderV10 AS idOfLinkedFolder,
                        userAgent,
                        0 AS markedAsImportant,
                        'IMAGE' AS mediaType,
                        CASE
                            WHEN keyOfLinkedFolderV10 IS NULL THEN 'SAVED_LINK'
                            ELSE 'FOLDER_LINK'
                        END AS linkType
                    FROM links_table;
                """.trimIndent()
                )
                connection.execSQL(
                    """
                    INSERT INTO links (
                        remoteId,
                        title,
                        url,
                        baseURL,
                        imgURL,
                        note,
                        idOfLinkedFolder,
                        userAgent,
                        markedAsImportant,
                        mediaType,
                        linkType
                    )
                    SELECT
                        NULL AS remoteId,
                        title,
                        webURL AS url,
                        baseURL,
                        imgURL,
                        infoForSaving AS note,
                        NULL AS idOfLinkedFolder,
                        userAgent,
                        0 AS markedAsImportant,
                        'IMAGE' AS mediaType,
                        'ARCHIVE_LINK' AS linkType
                    FROM archived_links_table;
                """.trimIndent()
                )
                connection.execSQL(
                    """
                   INSERT INTO links (
                       remoteId,
                       title,
                       url,
                       baseURL,
                       imgURL,
                       note,
                       idOfLinkedFolder,
                       userAgent,
                       markedAsImportant,
                       mediaType,
                       linkType
                   )
                   SELECT
                       NULL AS remoteId,
                       title,
                       webURL AS url,
                       baseURL,
                       imgURL,
                       infoForSaving AS note,
                       NULL AS idOfLinkedFolder,
                       userAgent,
                       1 AS markedAsImportant,
                       'IMAGE' AS mediaType,
                       'IMPORTANT_LINK' AS linkType
                   FROM important_links_table;
                """.trimIndent()
                )

                connection.execSQL(
                    """
                    INSERT INTO links (
                        remoteId,
                        title,
                        url,
                        baseURL,
                        imgURL,
                        note,
                        idOfLinkedFolder,
                        userAgent,
                        markedAsImportant,
                        mediaType,
                        linkType
                    )
                    SELECT
                        NULL AS remoteId,
                        title,
                        webURL AS url,
                        baseURL,
                        imgURL,
                        infoForSaving AS note,
                        NULL AS idOfLinkedFolder,
                        userAgent,
                        0 AS markedAsImportant,
                        'IMAGE' AS mediaType,
                        'HISTORY_LINK' AS linkType
                    FROM recently_visited_table;
                """.trimIndent()
                )

                connection.execSQL("CREATE TABLE IF NOT EXISTS `folders` (`name` TEXT NOT NULL, `note` TEXT NOT NULL, `parentFolderId` INTEGER, `localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER, `isArchived` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL DEFAULT $timestamp)")
                connection.execSQL(
                    """
                    INSERT INTO folders (
                        name,
                        note,
                        parentFolderId,
                        localId,
                        remoteId,
                        isArchived
                    )
                    SELECT
                        folderName AS name,
                        infoForSaving AS note,
                        parentFolderID AS parentFolderId,
                        id AS localId,
                        NULL AS remoteId,
                        isFolderArchived AS isArchived
                    FROM folders_table;
                """.trimIndent()
                )

                connection.execSQL("CREATE TABLE IF NOT EXISTS `panel_new` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `panelName` TEXT NOT NULL, `remoteId` INTEGER, `lastModified` INTEGER NOT NULL DEFAULT $timestamp)")
                connection.execSQL("INSERT INTO panel_new (localId, panelName) SELECT panelId, panelName FROM panel;")
                connection.execSQL("DROP TABLE panel;")
                connection.execSQL("ALTER TABLE panel_new RENAME TO panel;")

                connection.execSQL("CREATE TABLE IF NOT EXISTS `panel_folder_new` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER, `folderId` INTEGER NOT NULL, `panelPosition` INTEGER NOT NULL, `folderName` TEXT NOT NULL, `connectedPanelId` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL DEFAULT $timestamp)")
                connection.execSQL("INSERT INTO panel_folder_new (localId, remoteId, folderId, panelPosition, folderName, connectedPanelId) SELECT id, NULL, folderId, panelPosition, folderName, connectedPanelId FROM panel_folder;")
                connection.execSQL("DROP TABLE panel_folder;")
                connection.execSQL("ALTER TABLE panel_folder_new RENAME TO panel_folder;")

                connection.execSQL("CREATE TABLE IF NOT EXISTS `localized_languages` (`languageCode` TEXT NOT NULL, `languageName` TEXT NOT NULL, `localizedStringsCount` INTEGER NOT NULL, `contributionLink` TEXT NOT NULL, PRIMARY KEY(`languageCode`))")
                connection.execSQL("INSERT INTO localized_languages (languageCode, languageName, localizedStringsCount, contributionLink) SELECT languageCode, languageName, localizedStringsCount, contributionLink FROM language;")
                connection.execSQL("DROP TABLE language;")

                connection.execSQL("CREATE TABLE IF NOT EXISTS `localized_strings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `languageCode` TEXT NOT NULL, `stringName` TEXT NOT NULL, `stringValue` TEXT NOT NULL)")
                connection.execSQL("INSERT INTO localized_strings (id, languageCode, stringName, stringValue) SELECT id, languageCode, stringName, stringValue FROM translation;")
                connection.execSQL("DROP TABLE translation;")

                connection.execSQL("DROP TABLE links_table;")
                connection.execSQL("DROP TABLE folders_table;")
                connection.execSQL("DROP TABLE archived_links_table;")
                connection.execSQL("DROP TABLE archived_folders_table;")
                connection.execSQL("DROP TABLE important_links_table;")
                connection.execSQL("DROP TABLE important_folders_table;")
                connection.execSQL("DROP TABLE recently_visited_table;")
                linkoraLog("hell yeah $timestamp")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `snapshot` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL)")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override suspend fun migrate(connection: SQLiteConnection) {

                connection.execSQL(
                    """
            INSERT INTO links (
                linkType, remoteId, title, url, baseURL, imgURL, note,
                idOfLinkedFolder, userAgent, mediaType, lastModified, markedAsImportant
            )
            SELECT
                'IMPORTANT_LINK' AS linkType,
                remoteId, title, url, baseURL, imgURL, note,
                idOfLinkedFolder, userAgent, mediaType, lastModified, 1 AS markedAsImportant
            FROM links
            WHERE markedAsImportant = 1
        """
                )

                connection.execSQL(
                    """
            CREATE TABLE new_links (
                linkType TEXT NOT NULL,
                localId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                remoteId INTEGER,
                title TEXT NOT NULL,
                url TEXT NOT NULL,
                baseURL TEXT NOT NULL,
                imgURL TEXT NOT NULL,
                note TEXT NOT NULL,
                idOfLinkedFolder INTEGER,
                userAgent TEXT,
                mediaType TEXT NOT NULL,
                lastModified INTEGER NOT NULL
            )
        """
                )

                connection.execSQL(
                    """
            INSERT INTO new_links (
                linkType, localId, remoteId, title, url, baseURL, imgURL, note,
                idOfLinkedFolder, userAgent, mediaType, lastModified
            )
            SELECT
                linkType, localId, remoteId, title, url, baseURL, imgURL, note,
                idOfLinkedFolder, userAgent, mediaType, lastModified
            FROM links
        """
                )

                connection.execSQL("DROP TABLE links")

                connection.execSQL("ALTER TABLE new_links RENAME TO links")

                linkoraLog("Applied MIGRATION_10_11")
            }
        }
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `tags` (`localId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `remoteId` INTEGER, `lastModified` INTEGER NOT NULL, `name` TEXT NOT NULL)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS `link_tags` (`remoteId` INTEGER, `linkId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, `lastModified` INTEGER NOT NULL, PRIMARY KEY(`linkId`, `tagId`), FOREIGN KEY(`linkId`) REFERENCES `links`(`localId`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`tagId`) REFERENCES `tags`(`localId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `RefreshLink` (`refreshedLinkId` INTEGER NOT NULL, PRIMARY KEY(`refreshedLinkId`))")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE INDEX IF NOT EXISTS idx_folders_name ON folders(name)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS idx_links_title ON links(title)")
                connection.execSQL("CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name)")
            }
        }
    }

    abstract val linksDao: LinksDao
    abstract val foldersDao: FoldersDao
    abstract val localizationDao: LocalizationDao
    abstract val panelsDao: PanelsDao
    abstract val pendingSyncQueueDao: PendingSyncQueueDao
    abstract val snapshotDao: SnapshotDao
    abstract val tagsDao: TagsDao
    abstract val refreshDao: RefreshLinkDao
    abstract val localDatabaseUtilsDao: LocalDatabaseUtilsDao
}