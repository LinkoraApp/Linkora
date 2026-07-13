package com.sakethh.linkora.domain.model

import com.sakethh.linkora.domain.MediaType

data class ScrapedLinkInfo(
    val title: String,
    val imgUrl: String,
    val mediaType: MediaType = MediaType.IMAGE,
)
