package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.WebCaptureMetadata
import com.sakethh.linkora.WebCaptureMetadataDao
import com.sakethh.linkora.data.local.dao.CaptureTrackDao
import com.sakethh.linkora.domain.model.CaptureTrack
import com.sakethh.linkora.domain.repository.local.WebCaptureRepo

class WebCaptureRepoImpl(
    private val captureTrackDao: CaptureTrackDao,
    private val webCaptureMetadataDao: suspend () -> WebCaptureMetadataDao,
) : WebCaptureRepo {
    override suspend fun insertAProcessedId(captureTrack: CaptureTrack) = captureTrackDao.insertAProcessedId(captureTrack)

    override suspend fun getAllLinksCaptureProcessedLinkIds(): List<String> = captureTrackDao.getAllLinksCaptureProcessedLinkIds()

    override suspend fun deleteAllProcessedIds() {
        captureTrackDao.deleteAllBulkCaptureIds()
    }

    override suspend fun deleteByWorkerId(id: String) {
        captureTrackDao.deleteByWorkerId(id)
    }

    override suspend fun getAllWorkerIds(): List<String?> = captureTrackDao.getAllWorkerIds()

    override suspend fun insertMetadata(metadata: WebCaptureMetadata) {
        webCaptureMetadataDao().insert(metadata)
    }

    override suspend fun getFolderNameByLink(link: String): String? = webCaptureMetadataDao().getFolderNameByLink(link)

    override suspend fun getAllLinksFromMetadata(): List<String> = webCaptureMetadataDao().getAllLinks()
}
