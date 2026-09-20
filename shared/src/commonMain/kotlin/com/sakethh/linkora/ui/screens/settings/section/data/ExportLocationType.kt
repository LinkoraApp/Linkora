package com.sakethh.linkora.ui.screens.settings.section.data

enum class ExportLocationType {
    EXPORT {
        override val dirRef: String = "Exports"
    },
    SNAPSHOT {
        override val dirRef: String = "Snapshots"
    },
    WEB_CAPTURE {
        override val dirRef: String = "Web-Captures"
    };

    abstract val dirRef: String
}
