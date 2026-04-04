package com.sakethh.linkora.di

import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.Network
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.platform.PlatformPreference

class LinkoraSDK(
    val nativeUtils: NativeUtils,
    val fileManager: FileManager,
    val permissionManager: PermissionManager,
    val localDatabase: LocalDatabase,
    val platformPreference: PlatformPreference,
    val network: Network,
    val dataSyncingNotificationService: NativeUtils.DataSyncingNotificationService
) {
    companion object {
        private lateinit var shared: LinkoraSDK
        private var assigned = false

        fun getInstance(): LinkoraSDK {
            require(assigned) {
                "LinkoraSDK has not been set. Call LinkoraSDK.set() first."
            }
            return shared
        }

        fun set(linkoraSdk: LinkoraSDK) {
            require(!assigned) {
                "LinkoraSDK has already been set and can only be set once."
            }

            shared = linkoraSdk
            assigned = true
        }
    }
}