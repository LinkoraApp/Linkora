package com.sakethh.linkora.domain.repository.local

import com.sakethh.linkora.WebCaptureMetadata
import com.sakethh.linkora.domain.model.CaptureTrack

interface WebCaptureRepo {
    suspend fun insertAProcessedId(captureTrack: CaptureTrack)

    suspend fun getProcessedLinkIds(): List<Long>

    suspend fun deleteAllProcessedIds()

    suspend fun insertMetadata(metadata: WebCaptureMetadata)

    suspend fun getFolderNameByLink(link: String): String?

    suspend fun getAllLinksFromMetadata(): List<String>
}
