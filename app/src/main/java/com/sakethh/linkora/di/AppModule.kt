package com.sakethh.linkora.di

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.data.local.dataImport.ImportImpl
import com.sakethh.linkora.data.local.dataImport.ImportRepo
import com.sakethh.linkora.data.local.folders.FoldersImpl
import com.sakethh.linkora.data.local.folders.FoldersRepo
import com.sakethh.linkora.data.local.links.LinksImpl
import com.sakethh.linkora.data.local.links.LinksRepo
import com.sakethh.linkora.data.local.search.SearchImpl
import com.sakethh.linkora.data.local.search.SearchRepo
import com.sakethh.linkora.data.local.sorting.folders.archive.ParentArchivedFoldersSortingImpl
import com.sakethh.linkora.data.local.sorting.folders.archive.ParentArchivedFoldersSortingRepo
import com.sakethh.linkora.data.local.sorting.folders.regular.ParentRegularFoldersSortingImpl
import com.sakethh.linkora.data.local.sorting.folders.regular.ParentRegularFoldersSortingRepo
import com.sakethh.linkora.data.local.sorting.folders.subfolders.SubFoldersSortingImpl
import com.sakethh.linkora.data.local.sorting.folders.subfolders.SubFoldersSortingRepo
import com.sakethh.linkora.data.local.sorting.links.archive.ArchivedLinksSortingImpl
import com.sakethh.linkora.data.local.sorting.links.archive.ArchivedLinksSortingRepo
import com.sakethh.linkora.data.local.sorting.links.folder.archive.ArchivedFolderLinksSortingImpl
import com.sakethh.linkora.data.local.sorting.links.folder.archive.ArchivedFolderLinksSortingRepo
import com.sakethh.linkora.data.local.sorting.links.folder.regular.RegularFolderLinksSortingImpl
import com.sakethh.linkora.data.local.sorting.links.folder.regular.RegularFolderLinksSortingRepo
import com.sakethh.linkora.data.local.sorting.links.history.HistoryLinksSortingImpl
import com.sakethh.linkora.data.local.sorting.links.history.HistoryLinksSortingRepo
import com.sakethh.linkora.data.local.sorting.links.important.ImportantLinksSortingImpl
import com.sakethh.linkora.data.local.sorting.links.important.ImportantLinksSortingRepo
import com.sakethh.linkora.data.local.sorting.links.saved.SavedLinksSortingImpl
import com.sakethh.linkora.data.local.sorting.links.saved.SavedLinksSortingRepo
import com.sakethh.linkora.data.remote.scrape.LinkMetaDataScrapperImpl
import com.sakethh.linkora.data.remote.scrape.LinkMetaDataScrapperService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL("DROP TABLE IF EXISTS new_folders_table;")
            db.execSQL("CREATE TABLE IF NOT EXISTS new_folders_table (folderName TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
            db.execSQL("INSERT INTO new_folders_table (folderName, infoForSaving) SELECT folderName, infoForSaving FROM folders_table;")
            db.execSQL("DROP TABLE IF EXISTS folders_table;")
            db.execSQL("ALTER TABLE new_folders_table RENAME TO folders_table;")

            db.execSQL("DROP TABLE IF EXISTS new_archived_links_table;")
            db.execSQL("CREATE TABLE IF NOT EXISTS new_archived_links_table (title TEXT NOT NULL, webURL TEXT NOT NULL, baseURL TEXT NOT NULL, imgURL TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
            db.execSQL("INSERT INTO new_archived_links_table (title, webURL, baseURL, imgURL, infoForSaving) SELECT title, webURL, baseURL, imgURL, infoForSaving FROM archived_links_table;")
            db.execSQL("DROP TABLE IF EXISTS archived_links_table;")
            db.execSQL("ALTER TABLE new_archived_links_table RENAME TO archived_links_table;")

            db.execSQL("DROP TABLE IF EXISTS new_archived_folders_table;")
            db.execSQL("CREATE TABLE IF NOT EXISTS new_archived_folders_table (archiveFolderName TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
            db.execSQL("INSERT INTO new_archived_folders_table (archiveFolderName, infoForSaving) SELECT archiveFolderName, infoForSaving FROM archived_folders_table;")
            db.execSQL("DROP TABLE IF EXISTS archived_folders_table;")
            db.execSQL("ALTER TABLE new_archived_folders_table RENAME TO archived_folders_table;")

            db.execSQL("DROP TABLE IF EXISTS new_important_links_table;")
            db.execSQL("CREATE TABLE IF NOT EXISTS new_important_links_table (title TEXT NOT NULL, webURL TEXT NOT NULL, baseURL TEXT NOT NULL, imgURL TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
            db.execSQL("INSERT INTO new_important_links_table (title, webURL, baseURL, imgURL, infoForSaving) SELECT title, webURL, baseURL, imgURL, infoForSaving FROM important_links_table;")
            db.execSQL("DROP TABLE IF EXISTS important_links_table;")
            db.execSQL("ALTER TABLE new_important_links_table RENAME TO important_links_table;")

            db.execSQL("DROP TABLE IF EXISTS new_important_folders_table;")
            db.execSQL("CREATE TABLE IF NOT EXISTS new_important_folders_table (impFolderName TEXT NOT NULL, infoForSaving TEXT NOT NULL, id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL);")
            db.execSQL("INSERT INTO new_important_folders_table (impFolderName, infoForSaving) SELECT impFolderName, infoForSaving FROM important_folders_table;")
            db.execSQL("DROP TABLE IF EXISTS important_folders_table;")
            db.execSQL("ALTER TABLE new_important_folders_table RENAME TO important_folders_table;")

        }

    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL("DROP TABLE IF EXISTS folders_table_new")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `folders_table_new` (`folderName` TEXT NOT NULL, `infoForSaving` TEXT NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `parentFolderID` INTEGER DEFAULT NULL, `childFolderIDs` TEXT DEFAULT NULL, `isFolderArchived` INTEGER NOT NULL DEFAULT 0, `isMarkedAsImportant` INTEGER NOT NULL DEFAULT 0)"
            )
            db.execSQL(
                "INSERT INTO folders_table_new (folderName, infoForSaving, id) " + "SELECT folderName, infoForSaving, id FROM folders_table"
            )
            db.execSQL("DROP TABLE folders_table")
            db.execSQL("ALTER TABLE folders_table_new RENAME TO folders_table")


            db.execSQL("DROP TABLE IF EXISTS links_table_new")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `links_table_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `webURL` TEXT NOT NULL, `baseURL` TEXT NOT NULL, `imgURL` TEXT NOT NULL, `infoForSaving` TEXT NOT NULL, `isLinkedWithSavedLinks` INTEGER NOT NULL, `isLinkedWithFolders` INTEGER NOT NULL, `keyOfLinkedFolderV10` INTEGER DEFAULT NULL, `keyOfLinkedFolder` TEXT, `isLinkedWithImpFolder` INTEGER NOT NULL, `keyOfImpLinkedFolder` TEXT NOT NULL, `keyOfImpLinkedFolderV10` INTEGER DEFAULT NULL, `isLinkedWithArchivedFolder` INTEGER NOT NULL, `keyOfArchiveLinkedFolderV10` INTEGER DEFAULT NULL, `keyOfArchiveLinkedFolder` TEXT)"
            )
            db.execSQL(
                "INSERT INTO links_table_new (id, title, webURL, baseURL, imgURL, infoForSaving, " + "isLinkedWithSavedLinks, isLinkedWithFolders, keyOfLinkedFolder, " + "isLinkedWithImpFolder, keyOfImpLinkedFolder, " + "isLinkedWithArchivedFolder, keyOfArchiveLinkedFolder) " + "SELECT id, title, webURL, baseURL, imgURL, infoForSaving, " + "isLinkedWithSavedLinks, isLinkedWithFolders, keyOfLinkedFolder, " + "isLinkedWithImpFolder, keyOfImpLinkedFolder," + "isLinkedWithArchivedFolder, keyOfArchiveLinkedFolder " + "FROM links_table"
            )
            db.execSQL("DROP TABLE links_table")
            db.execSQL("ALTER TABLE links_table_new RENAME TO links_table")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `home_screen_list_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `position` INTEGER NOT NULL, `folderName` TEXT NOT NULL, `shouldSavedLinksTabVisible` INTEGER NOT NULL, `shouldImpLinksTabVisible` INTEGER NOT NULL)")
        }
    }
    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS home_screen_list_table")
            db.execSQL("CREATE TABLE IF NOT EXISTS `shelf` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `shelfName` TEXT NOT NULL, `shelfIconName` TEXT NOT NULL, `folderIds` TEXT NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `home_screen_list_table` (`primaryKey` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `id` INTEGER NOT NULL, `position` INTEGER NOT NULL, `folderName` TEXT NOT NULL, `parentShelfID` INTEGER NOT NULL)")
        }
    }

    @Provides
    @Singleton
    fun provideLocalDatabase(app: Application): LocalDatabase {
        return Room.databaseBuilder(
            app, LocalDatabase::class.java, "linkora_db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }

    @Provides
    @Singleton
    fun provideLinksRepo(localDatabase: LocalDatabase): LinksRepo {
        return LinksImpl(
            localDatabase,
            provideLinksRepo(localDatabase),
            provideFoldersRepo(localDatabase),
            provideLinkMetaDataScrapperService()
        )
    }

    @Provides
    @Singleton
    fun provideFoldersRepo(localDatabase: LocalDatabase): FoldersRepo {
        return FoldersImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun archiveLinksSorting(localDatabase: LocalDatabase): ArchivedLinksSortingRepo {
        return ArchivedLinksSortingImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun parentFoldersSorting(localDatabase: LocalDatabase): ParentRegularFoldersSortingRepo {
        return ParentRegularFoldersSortingImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun subFoldersSorting(localDatabase: LocalDatabase): SubFoldersSortingRepo {
        return SubFoldersSortingImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun savedLinksSorting(localDatabase: LocalDatabase): SavedLinksSortingRepo {
        return SavedLinksSortingImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun importantLinksSorting(localDatabase: LocalDatabase): ImportantLinksSortingRepo {
        return ImportantLinksSortingImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun folderLinksSorting(localDatabase: LocalDatabase): RegularFolderLinksSortingRepo {
        return RegularFolderLinksSortingImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun archiveFolderLinksSorting(localDatabase: LocalDatabase): ArchivedFolderLinksSortingRepo {
        return ArchivedFolderLinksSortingImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun archiveFoldersSorting(localDatabase: LocalDatabase): ParentArchivedFoldersSortingRepo {
        return ParentArchivedFoldersSortingImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun provideSearchRepo(localDatabase: LocalDatabase): SearchRepo {
        return SearchImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun provideHistoryLinksSortingRepo(localDatabase: LocalDatabase): HistoryLinksSortingRepo {
        return HistoryLinksSortingImpl(localDatabase)
    }

    @Provides
    @Singleton
    fun provideLinkMetaDataScrapperService(): LinkMetaDataScrapperService {
        return LinkMetaDataScrapperImpl()
    }

    @Provides
    @Singleton
    fun provideImportRepo(
        localDatabase: LocalDatabase,
        foldersRepo: FoldersRepo,
        linksRepo: LinksRepo
    ): ImportRepo {
        return ImportImpl(localDatabase, foldersRepo, linksRepo)
    }
}