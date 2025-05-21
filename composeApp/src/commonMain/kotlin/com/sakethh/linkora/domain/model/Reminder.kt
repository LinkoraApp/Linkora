package com.sakethh.linkora.domain.model


data class Reminder(
    val id: Long = 0,
    val linkId: Long,
    val title: String,
    val description: String,
    val scheduleInfo: String
)
