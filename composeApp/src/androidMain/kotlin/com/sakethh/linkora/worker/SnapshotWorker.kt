package com.sakethh.linkora.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.stringPreferencesKey

class SnapshotWorker(appContext: Context, workerParameters: WorkerParameters) :
    CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        return try {
            val rawExportStringID = inputData.getLong(key = "rawExportStringID", defaultValue = 0)
            val rawExportString =
                DependencyContainer.snapshotRepo.getASnapshot(rawExportStringID)
            val fileType = inputData.getString(key = "fileType")!!
            val exportLocation = DependencyContainer.preferencesRepo.readPreferenceValue(
                stringPreferencesKey(AppPreferences.BACKUP_LOCATION.key)
            ) ?: return Result.failure().also {
                val failureMsg = "Couldn't save snapshot as save location can't be retrieved"
                linkoraLog(failureMsg)
                UIEvent.pushUIEvent(UIEvent.Type.ShowSnackbar(failureMsg))
            }
            LinkoraSDK.getInstance().fileManager.writeRawExportStringToFile(
                exportLocation = exportLocation,
                exportFileType = ExportFileType.valueOf(fileType),
                rawExportString = rawExportString.content,
                onCompletion = {
                    try {
                        DependencyContainer.snapshotRepo.deleteASnapshot(rawExportStringID)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    linkoraLog("Snapshot saved as: $it")
                },
                exportLocationType = ExportLocationType.SNAPSHOT
            )
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}