package com.sakethh.linkora.domain

sealed interface PermissionStatus {
    object Granted : PermissionStatus

    object NeedsRequest : PermissionStatus
}
