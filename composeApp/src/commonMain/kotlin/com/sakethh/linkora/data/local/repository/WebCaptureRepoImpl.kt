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

    override suspend fun getProcessedLinkIds(): List<Long> = captureTrackDao.getProcessedLinkIds()

    override suspend fun deleteAllProcessedIds() {
        captureTrackDao.deleteAllIds()
    }

    override suspend fun insertMetadata(metadata: WebCaptureMetadata) {
        webCaptureMetadataDao().insert(metadata)
    }

    override suspend fun getFolderNameByLink(link: String): String? = webCaptureMetadataDao().getFolderNameByLink(link)

    override suspend fun getAllLinksFromMetadata(): List<String> = webCaptureMetadataDao().getAllLinks()
}
