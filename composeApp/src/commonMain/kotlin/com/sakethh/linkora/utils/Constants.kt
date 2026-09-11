package com.sakethh.linkora.utils

object Constants {
    const val APP_VERSION_NAME = "v0.21.0"
    const val LOCALIZATION_SERVER_URL = "https://linkoralocalizationserver.onrender.com/"
    const val PROXY_SERVER_URL = "https://linkora-proxy.onrender.com"

    const val ALL_LINKS_ID: Long = -1
    const val SAVED_LINKS_ID: Long = -2
    const val IMPORTANT_LINKS_ID: Long = -3
    const val ARCHIVE_ID: Long = -4
    const val HISTORY_ID: Long = -5
    const val DEFAULT_PANELS_ID: Long = -6

    const val DEFAULT_APP_LANGUAGE_CODE = "en"
    const val DEFAULT_APP_LANGUAGE_NAME = "English"
    const val DEFAULT_USER_AGENT = "Twitterbot/1.0"

    const val DATA_STORE_NAME = "linkoraDataStore.preferences_pb"

    const val COLLECTION_INFO_SAVED_STATE_HANDLE_KEY = "parentFolderDetail"

    const val PAGE_SIZE = 20
    const val ACTIVE_PAGE_COLLECTION_SIZE = 1

    const val DOUBLE_TAP_DELAY = 500L

    const val FOLDER = "FOLDER"
    const val LINK = "LINK"
    const val TAG = "TAG"

    const val TRIGGER_THRESHOLD_AT_THE_END = 5

    const val MAX_INSERTION_IN_DB_SINGLE_SHOT = 999

    const val EMPTY_LAST_SEEN_ID: Long = -1

    /*
     * this is a hack for something that shouldn't have been in the codebase earlier.
     * i did allow two possibilities to determine the type of link, one is the linkType and the other is its parent id.
     * now this in itself isn't an issue if insertions and updates were happening strictly on one of them,
     * but i think that wasn't the case.
     *
     * so we now need this just to make sure we are retrieving data that somehow isn't properly mapped
     * due to the stupid decision i took earlier in the project.
     */
    const val LINK_TYPE_PARAM_TO_FOLDER_ID_CASE =
    """
    idOfLinkedFolder = CASE
        WHEN :linkType = '${LinkType.SAVED_LINK}' THEN $SAVED_LINKS_ID
        WHEN :linkType = '${LinkType.IMPORTANT_LINK}' THEN $IMPORTANT_LINKS_ID
        WHEN :linkType = '${LinkType.HISTORY_LINK}' THEN $HISTORY_ID
        WHEN :linkType = '${LinkType.ARCHIVE_LINK}' THEN $ARCHIVE_ID
    END
    """

    const val LINK_TYPE_COLUMN_TO_FOLDER_ID_CASE =
    """
    idOfLinkedFolder = CASE
        WHEN linkType = '${LinkType.SAVED_LINK}' THEN $SAVED_LINKS_ID
        WHEN linkType = '${LinkType.IMPORTANT_LINK}' THEN $IMPORTANT_LINKS_ID
        WHEN linkType = '${LinkType.HISTORY_LINK}' THEN $HISTORY_ID
        WHEN linkType = '${LinkType.ARCHIVE_LINK}' THEN $ARCHIVE_ID
    END
    """

    const val SNAPSHOT_HTML_FORMAT_ID = 0
    const val SNAPSHOT_JSON_FORMAT_ID = 1
    const val SNAPSHOT_BOTH_FORMAT_ID = 2

    const val SNAPSHOT_HTML_FORMAT = "HTML"
    const val SNAPSHOT_JSON_FORMAT = "JSON"
}

object LinkType {
    const val SAVED_LINK = "SAVED_LINK"
    const val FOLDER_LINK = "FOLDER_LINK"
    const val HISTORY_LINK = "HISTORY_LINK"
    const val IMPORTANT_LINK = "IMPORTANT_LINK"
    const val ARCHIVE_LINK = "ARCHIVE_LINK"
}

object Sorting {
    const val A_TO_Z = "A_TO_Z"
    const val Z_TO_A = "Z_TO_A"
    const val NEW_TO_OLD = "NEW_TO_OLD"
    const val OLD_TO_NEW = "OLD_TO_NEW"
}
