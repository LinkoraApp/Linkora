package com.sakethh.linkora.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.LocalDatabaseUtilsDao
import com.sakethh.linkora.data.local.dao.LocalizationDao
import com.sakethh.linkora.data.local.dao.PanelsDao
import com.sakethh.linkora.data.local.dao.PendingSyncQueueDao
import com.sakethh.linkora.data.local.dao.RefreshLinkDao
import com.sakethh.linkora.data.local.dao.SnapshotDao
import com.sakethh.linkora.data.local.dao.TagsDao
import kotlinx.coroutines.Dispatchers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class BaseDatabaseTest {

    lateinit var database: LocalDatabase
    lateinit var foldersDao: FoldersDao
    lateinit var tagsDao: TagsDao
    lateinit var snapshotDao: SnapshotDao
    lateinit var refreshLinkDao: RefreshLinkDao
    lateinit var pendingSyncQueueDao: PendingSyncQueueDao
    lateinit var panelsDao: PanelsDao
    lateinit var localizationDao: LocalizationDao
    lateinit var localDatabaseUtilsDao: LocalDatabaseUtilsDao
    lateinit var linksDao: LinksDao

    @BeforeTest
    fun createDb() {
        database = Room.inMemoryDatabaseBuilder<LocalDatabase>()
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Unconfined)
            .build()

        foldersDao = database.foldersDao
        tagsDao = database.tagsDao
        snapshotDao = database.snapshotDao
        refreshLinkDao = database.refreshDao
        pendingSyncQueueDao = database.pendingSyncQueueDao
        panelsDao = database.panelsDao
        localizationDao = database.localizationDao
        localDatabaseUtilsDao = database.localDatabaseUtilsDao
        linksDao = database.linksDao
    }

    @AfterTest
    fun closeDb() {
        database.close()
    }
}