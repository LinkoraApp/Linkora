package com.sakethh.linkora.domain.model

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity
data class CaptureTrack(
    @PrimaryKey val capturedLinkId: String,
    val captureWorkerId: String? = null,
) {
    companion object {
        fun getCaptureLinkId(
            inAllLinksWorker: Boolean,
            linkId: Long?,
            link: String? = null,
        ): String = if (inAllLinksWorker) {
            "all_"
        } else {
            "individual_"
        } + (linkId ?: link)
    }
}
