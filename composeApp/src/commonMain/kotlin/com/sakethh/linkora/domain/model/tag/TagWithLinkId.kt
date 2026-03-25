package com.sakethh.linkora.domain.model.tag

import androidx.room3.Embedded

data class TagWithLinkId(
    val linkId: Long,
    @Embedded
    val tag: Tag
)