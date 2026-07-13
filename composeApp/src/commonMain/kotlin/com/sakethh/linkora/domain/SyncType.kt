package com.sakethh.linkora.domain

import com.sakethh.linkora.Localization

enum class SyncType {
    ClientToServer {
        override fun asUIString(): String = Localization.getLocalizedString(Localization.Key.ClientToServer)

        override fun description(): String = Localization.getLocalizedString(Localization.Key.ClientToServerDesc)
    },
    ServerToClient {
        override fun asUIString(): String = Localization.getLocalizedString(Localization.Key.ServerToClient)

        override fun description(): String = Localization.getLocalizedString(Localization.Key.ServerToClientDesc)
    },
    TwoWay {
        override fun asUIString(): String = Localization.getLocalizedString(Localization.Key.TwoWaySync)

        override fun description(): String = Localization.getLocalizedString(Localization.Key.TwoWaySyncDesc)
    },
    ;

    abstract fun asUIString(): String

    abstract fun description(): String
}
