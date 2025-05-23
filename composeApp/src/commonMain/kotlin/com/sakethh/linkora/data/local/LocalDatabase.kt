package com.sakethh.linkora.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sakethh.linkora.data.local.dao.FoldersDao
import com.sakethh.linkora.data.local.dao.LinksDao
import com.sakethh.linkora.data.local.dao.LocalizationDao
import com.sakethh.linkora.data.local.dao.PanelsDao
import com.sakethh.linkora.data.local.dao.PendingSyncQueueDao
import com.sakethh.linkora.data.local.dao.ReminderDao
import com.sakethh.linkora.data.local.dao.SnapshotDao
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.PendingSyncQueue
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.model.Snapshot
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.domain.model.localization.LocalizedString
import com.sakethh.linkora.domain.model.panel.Panel
import com.sakethh.linkora.domain.model.panel.PanelFolder

@Database(
    version = 11,
    exportSchema = true,
    entities = [Link::class, Folder::class, LocalizedString::class, LocalizedLanguage::class, Panel::class, PanelFolder::class, PendingSyncQueue::class, Snapshot::class, Reminder::class]
)
@TypeConverters(TypeConverter::class)
abstract class LocalDatabase : RoomDatabase() {

    companion object {
        const val NAME = "linkora_db"
    }

    abstract val linksDao: LinksDao
    abstract val foldersDao: FoldersDao
    abstract val localizationDao: LocalizationDao
    abstract val panelsDao: PanelsDao
    abstract val pendingSyncQueueDao: PendingSyncQueueDao
    abstract val snapshotDao: SnapshotDao
    abstract val reminderDao: ReminderDao
}