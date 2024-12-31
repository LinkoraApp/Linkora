package com.sakethh.linkora.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import com.sakethh.linkora.common.utils.Constants
import kotlinx.coroutines.runBlocking

typealias LocalizedStringKey = String

object Localization {
    private val localizedStrings = mutableStateMapOf<LocalizedStringKey, String>()

    fun loadLocalizedStrings(languageCode: String) = runBlocking {
        if (languageCode == "en") return@runBlocking
        Key.entries.forEach {
            localizedStrings[it.toString()] = it.defaultValue
        }
    }

    @Composable
    fun rememberLocalizedString(key: Key): String {
        val localizedString by remember {
            derivedStateOf { localizedStrings[key.toString()] ?: key.defaultValue }
        }
        return localizedString
    }

    fun getLocalizedString(key: Key): String {
        return localizedStrings[key.toString()] ?: key.defaultValue
    }

    enum class Key(val defaultValue: String) {
        Settings(defaultValue = "Settings") {
            override fun toString(): String {
                return "settings"
            }
        },
        Theme(defaultValue = "Theme") {
            override fun toString(): String {
                return "theme"
            }
        },
        General(defaultValue = "General") {
            override fun toString(): String {
                return "general"
            }
        },
        Advanced(defaultValue = "Advanced") {
            override fun toString(): String {
                return "advanced"
            }
        },
        Layout(defaultValue = "Layout") {
            override fun toString(): String {
                return "layout"
            }
        },
        Language(defaultValue = "Language") {
            override fun toString(): String {
                return "language"
            }
        },
        Data(defaultValue = "Data") {
            override fun toString(): String {
                return "data"
            }
        },
        Privacy(defaultValue = "Privacy") {
            override fun toString(): String {
                return "privacy"
            }
        },
        About(defaultValue = "About") {
            override fun toString(): String {
                return "about"
            }
        },
        Acknowledgments(defaultValue = "Acknowledgments") {
            override fun toString(): String {
                return "acknowledgments"
            }
        },
        UseDarkMode(defaultValue = "Use Dark Theme") {
            override fun toString(): String {
                return "use_dark_theme"
            }
        },
        AppLanguage(defaultValue = "App Language") {
            override fun toString(): String {
                return "app_language"
            }
        },
        DisplayingRemoteStrings(defaultValue = "Displaying Remote Strings") {
            override fun toString(): String {
                return "displaying_remote_strings"
            }
        },
        ResetAppLanguage(defaultValue = "Reset App Language") {
            override fun toString(): String {
                return "reset_app_language"
            }
        },
        AvailableLanguages(defaultValue = "Available Languages") {
            override fun toString(): String {
                return "available_languages"
            }
        },
        LoadServerStrings(defaultValue = "Load Server Strings") {
            override fun toString(): String {
                return "load_server_strings"
            }
        },
        LoadCompiledStrings(defaultValue = "Load Compiled Strings") {
            override fun toString(): String {
                return "load_compiled_strings"
            }
        },
        UpdateRemoteLanguageStrings(defaultValue = "Update Remote Language Strings") {
            override fun toString(): String {
                return "update_remote_language_strings"
            }
        },
        RemoveRemoteLanguageStrings(defaultValue = "Remove Remote Language Strings") {
            override fun toString(): String {
                return "remove_remote_language_strings"
            }
        },
        DisplayingCompiledStrings(defaultValue = "Displaying Compiled Strings") {
            override fun toString(): String {
                return "displaying_compiled_strings"
            }
        },
        Home(defaultValue = "Home") {
            override fun toString(): String {
                return "home"
            }
        },

        Search(defaultValue = "Search") {
            override fun toString(): String {
                return "search"
            }
        },

        Collections(defaultValue = "Collections") {
            override fun toString(): String {
                return "collections"
            }
        },

        LinkoraServerSetup(defaultValue = "Linkora Server Setup") {
            override fun toString(): String {
                return "linkora_server_setup"
            }
        },

        CreateANewFolder(defaultValue = "Create A New Folder") {
            override fun toString(): String {
                return "create_a_new_folder"
            }
        },
        CreateANewFolderIn(defaultValue = "Create A New Folder In ${Constants.VALUE_PLACE_HOLDER_1}") {
            override fun toString(): String {
                return "create_a_new_folder_in"
            }
        },

        Create(defaultValue = "Create") {
            override fun toString(): String {
                return "create"
            }
        },

        AddANewLink(defaultValue = "Add A New Link") {
            override fun toString(): String {
                return "add_a_new_link"
            }
        },

        SavedLinks(defaultValue = "Saved Links") {
            override fun toString(): String {
                return "saved_links"
            }
        },
        AddANewLinkInImportantLinks(defaultValue = "Add a new link in Important Links") {
            override fun toString(): String {
                return "add_a_new_link_in_important_links"
            }
        },
        AddANewLinkInSavedLinks(defaultValue = "Add a new link in Saved Links") {
            override fun toString(): String {
                return "add_a_new_link_in_saved_links"
            }
        },
        AddANewLinkIn(defaultValue = "Add a new link in") {
            override fun toString(): String {
                return "add_a_new_link_in"
            }
        },
        LinkAddress(defaultValue = "Link Address") {
            override fun toString(): String {
                return "link_address"
            }
        },
        TitleForTheLink(defaultValue = "Title for the link") {
            override fun toString(): String {
                return "title_for_the_link"
            }
        },
        NoteForSavingTheLink(defaultValue = "Note for saving the link") {
            override fun toString(): String {
                return "note_for_saving_the_link"
            }
        },
        AutoDetectTitleIsEnabled(defaultValue = "Auto Detect Title is currently active.") {
            override fun toString(): String {
                return "auto_detect_title_is_enabled"
            }
        },
        DataRetrievalDisabled(defaultValue = "Data retrieval is blocked as the 'Force Save Links without retrieval' feature is currently active.") {
            override fun toString(): String {
                return "data_retrieval_disabled"
            }
        },
        ForceAutoDetectTitle(defaultValue = "Force Auto-Detect Title") {
            override fun toString(): String {
                return "force_auto_detect_title"
            }
        },
        RetryingWithSecondaryUserAgent(defaultValue = "Retrying metadata retrieval using a secondary user agent.") {
            override fun toString(): String {
                return "retrying_with_secondary_user_agent"
            }
        },
        ForceSaveWithoutRetrievingMetadata(defaultValue = "Force Save Without Retrieving Metadata") {
            override fun toString(): String {
                return "force_save_without_retrieving_metadata"
            }
        },
        AddIn(defaultValue = "Add in") {
            override fun toString(): String {
                return "add_in"
            }
        },
        InitialRequestFailed(defaultValue = "he initial request failed.") {
            override fun toString(): String {
                return "initial_request_failed"
            }
        },
        ImportantLinks(defaultValue = "Important Links") {
            override fun toString(): String {
                return "important_links"
            }
        },
        Save(defaultValue = "Save") {
            override fun toString(): String {
                return "save"
            }
        },
        Cancel(defaultValue = "Cancel") {
            override fun toString(): String {
                return "cancel"
            }
        },
        FolderName(defaultValue = "Folder Name") {
            override fun toString(): String {
                return "folder_name"
            }
        },
        NoteForCreatingTheFolder(defaultValue = "Note For Creating The Folder") {
            override fun toString(): String {
                return "note_for_creating_the_folder"
            }
        },
        AllLinks(defaultValue = "All Links") {
            override fun toString(): String {
                return "all_links"
            }
        },
        Archive(defaultValue = "Archive") {
            override fun toString(): String {
                return "archive"
            }
        },
        Folders(defaultValue = "Folders") {
            override fun toString(): String {
                return "folders"
            }
        },
        SuccessfullySavedConnectionDetails(defaultValue = "Successfully saved connection details.") {
            override fun toString(): String {
                return "successfully_saved_connection_details"
            }
        },
        DeletedTheServerConnectionSuccessfully(defaultValue = "Deleted the server connection successfully.") {
            override fun toString(): String {
                return "deleted_the_server_connection_successfully"
            }
        },
        UseInAppBrowser(defaultValue = "Use In-App Browser") {
            override fun toString(): String {
                return "use_in_app_browser"
            }
        },
        UseInAppBrowserDesc(defaultValue = "Enable this to open links within the app; otherwise, your default browser will open when clicking on links.") {
            override fun toString(): String {
                return "use_in_app_browser_desc"
            }
        },
        EnableHomeScreen(defaultValue = "Enable Home Screen") {
            override fun toString(): String {
                return "enable_home_screen"
            }
        },
        EnableHomeScreenDesc(defaultValue = "If this is enabled, Home Screen option will be shown in Bottom Navigation Bar; if this setting is not enabled, Home screen option will NOT be shown.") {
            override fun toString(): String {
                return "enable_home_screen_desc"
            }
        },
        AutoDetectTitle(defaultValue = "Auto-Detect Title") {
            override fun toString(): String {
                return "auto_detect_title"
            }
        },
        AutoDetectTitleDesc(defaultValue = "Note: This may not detect every website.") {
            override fun toString(): String {
                return "auto_detect_title_desc"
            }
        },
        ForceSaveWithoutRetrievingMetadataDesc(defaultValue = "Link will be saved as you save it, nothing gets fetched. Note that this will impact on refreshing links from link menu, link will NOT be refreshed if this is enabled.") {
            override fun toString(): String {
                return "force_save_without_retrieving_metadata_desc"
            }
        },
        ShowAssociatedImageInLinkMenu(defaultValue = "Show associated image in link menu") {
            override fun toString(): String {
                return "show_associated_image_in_link_menu"
            }
        },
        ShowAssociatedImageInLinkMenuDesc(defaultValue = "Enables the display of an associated image within the link menu.") {
            override fun toString(): String {
                return "show_associated_image_in_link_menu_desc"
            }
        },
        AutoCheckForUpdates(defaultValue = "Enables the display of an associated image within the link menu.") {
            override fun toString(): String {
                return "auto_check_for_updates"
            }
        },
        AutoCheckForUpdatesDesc(defaultValue = "Enable to auto-check for updates on app launch. Disable for manual checks.") {
            override fun toString(): String {
                return "auto_check_for_updates_desc"
            }
        },
        ShowDescriptionForSettings(defaultValue = "Show description for Settings") {
            override fun toString(): String {
                return "show_description_for_settings"
            }
        },
        ShowDescriptionForSettingsDesc(defaultValue = "Enable to show detailed descriptions for settings. Disable to show only titles.") {
            override fun toString(): String {
                return "show_description_for_settings_desc"
            }
        },
        ManageConnectedServer(defaultValue = "Manage Connected Server") {
            override fun toString(): String {
                return "manage_connected_server"
            }
        },
        ManageConnectedServerDesc(defaultValue = "Your data is synced with the Linkora server. Tap to manage or disconnect.") {
            override fun toString(): String {
                return "manage_connected_server_desc"
            }
        },
        CurrentlyConnectedTo(defaultValue = "Currently Connected To") {
            override fun toString(): String {
                return "currently_connected_to"
            }
        },
        SyncType(defaultValue = "Sync Type") {
            override fun toString(): String {
                return "sync_type"
            }
        },
        EditServerConfiguration(defaultValue = "Edit server configuration") {
            override fun toString(): String {
                return "edit_server_configuration"
            }
        },
        DeleteTheServerConnection(defaultValue = "Delete the connection") {
            override fun toString(): String {
                return "delete_the_server_connection"
            }
        },
        Configuration(defaultValue = "Configuration") {
            override fun toString(): String {
                return "configuration"
            }
        },
        ServerURL(defaultValue = "Server URL") {
            override fun toString(): String {
                return "server_url"
            }
        },
        ServerSetupInstruction(defaultValue = "Ensure the server is running. If hosted locally, the server URL should include the correct port number. No port is needed if the server is not hosted locally.") {
            override fun toString(): String {
                return "server_setup_instruction"
            }
        },
        SecurityToken(defaultValue = "Security Token") {
            override fun toString(): String {
                return "security_token"
            }
        },
        ServerIsReachable(defaultValue = "Server Exists and Is Reachable!") {
            override fun toString(): String {
                return "server_is_reachable"
            }
        },
        TestServerAvailability(defaultValue = "Test Server Availability") {
            override fun toString(): String {
                return "test_server_availability"
            }
        },
        UseThisConnection(defaultValue = "Use This Connection") {
            override fun toString(): String {
                return "use_this_connection"
            }
        },
        ClientToServer(defaultValue = "Client To Server") {
            override fun toString(): String {
                return "client_to_server"
            }
        },
        ClientToServerDesc(defaultValue = "Client changes are sent to the server, but client is not updated with server changes.") {
            override fun toString(): String {
                return "client_to_server_desc"
            }
        },
        ServerToClient(defaultValue = "Server To Client") {
            override fun toString(): String {
                return "server_to_client"
            }
        },
        ServerToClientDesc(defaultValue = "Server changes are sent to the client, but server is not updated with client changes.") {
            override fun toString(): String {
                return "server_to_client_desc"
            }
        },
        TwoWaySync(defaultValue = "Two-Way Sync") {
            override fun toString(): String {
                return "two_way_sync"
            }
        },
        TwoWaySyncDesc(defaultValue = "Changes are sent both ways: client updates the server, and server updates the client.") {
            override fun toString(): String {
                return "two_way_sync_desc"
            }
        },
        ImportLabel(defaultValue = "Import") {
            override fun toString(): String {
                return "import"
            }
        },
        ExportLabel(defaultValue = "Export") {
            override fun toString(): String {
                return "export"
            }
        },
        ImportUsingJsonFile(defaultValue = "Import using JSON file") {
            override fun toString(): String {
                return "import_using_json_file"
            }
        },
        ImportUsingJsonFileDesc(defaultValue = "Import data from external JSON file based on Linkora Schema.") {
            override fun toString(): String {
                return "import_using_json_file_desc"
            }
        },
        ImportDataFromHtmlFile(defaultValue = "Import data from HTML file") {
            override fun toString(): String {
                return "import_data_from_html_file"
            }
        },
        ImportDataFromHtmlFileDesc(defaultValue = "Import data from an external HTML file that follows the standard bookmarks import/export format.") {
            override fun toString(): String {
                return "import_data_from_html_file_desc"
            }
        },
        ExportDataAsJson(defaultValue = "Export Data as JSON") {
            override fun toString(): String {
                return "export_data_as_json"
            }
        },
        ExportDataAsJsonDesc(defaultValue = "Export All Data to a JSON File") {
            override fun toString(): String {
                return "export_data_as_json_desc"
            }
        },
        ExportDataAsHtml(defaultValue = "Export Data as HTML") {
            override fun toString(): String {
                return "export_data_as_html"
            }
        },
        ExportDataAsHtmlDesc(defaultValue = "Export All Your Data (Excluding Panels) as HTML File") {
            override fun toString(): String {
                return "export_data_as_html_desc"
            }
        },
        Sync(defaultValue = "Sync") {
            override fun toString(): String {
                return "sync"
            }
        },
        ConnectToALinkoraServer(defaultValue = "Connect to a Linkora Server") {
            override fun toString(): String {
                return "connect_to_a_linkora_server"
            }
        },
        ConnectToALinkoraServerDesc(defaultValue = "By connecting to a Linkora server, you can sync your data and access it on any device using the Linkora app.") {
            override fun toString(): String {
                return "connect_to_a_linkora_server_desc"
            }
        },
        DeleteEntireDataPermanently(defaultValue = "Delete entire data permanently") {
            override fun toString(): String {
                return "delete_entire_data_permanently"
            }
        },
        DeleteEntireDataPermanentlyDesc(defaultValue = "Delete all links and folders permanently including archives.") {
            override fun toString(): String {
                return "delete_entire_data_permanently_desc"
            }
        },
        ClearImageCache(defaultValue = "Clear Image Cache") {
            override fun toString(): String {
                return "clear_image_cache"
            }
        },
        ClearImageCacheDesc(defaultValue = "Images are cached by default. Changing the user agent might affect what you see. Clear the cache to resolve it.") {
            override fun toString(): String {
                return "clear_image_cache_desc"
            }
        },
        RefreshAllLinksTitlesAndImages(defaultValue = "Refresh All Links\\' Titles and Images") {
            override fun toString(): String {
                return "refresh_all_links_titles_and_images"
            }
        },
        RefreshAllLinksTitlesAndImagesDesc(defaultValue = "Manually entered titles will be replaced with detected titles.") {
            override fun toString(): String {
                return "refresh_all_links_titles_and_images_desc"
            }
        },
        RefreshingLinks(defaultValue = "Refreshing links…") {
            override fun toString(): String {
                return "refreshing_links"
            }
        },
        RefreshingLinksDesc(defaultValue = "Closing Linkora won\\'t interrupt link refreshing, but newly added links might not be processed.") {
            override fun toString(): String {
                return "refreshing_links_Desc"
            }
        },
        InitialScreenOnLaunch(defaultValue = "Initial Screen on Launch") {
            override fun toString(): String {
                return "initial_screen_on_launch"
            }
        },
        InitialScreenOnLaunchDesc(defaultValue = "Changes made with this option will reflect in the navigation of the initial screen that will open when you launch Linkora.") {
            override fun toString(): String {
                return "initial_screen_on_launch_Desc"
            }
        },
        Confirm(defaultValue = "Confirm") {
            override fun toString(): String {
                return "confirm"
            }
        },
        SelectTheInitialScreen(defaultValue = "Select the initial screen on launch") {
            override fun toString(): String {
                return "select_the_initial_screen_on_launch"
            }
        },
        ShowBorderAroundLinks(defaultValue = "Show Border Around Links") {
            override fun toString(): String {
                return "show_border_around_links"
            }
        },
        ShowTitle(defaultValue = "Show Title") {
            override fun toString(): String {
                return "show_title"
            }
        },
        ShowBaseURL(defaultValue = "Show Base URL") {
            override fun toString(): String {
                return "show_base_url"
            }
        },
        ShowBottomFadedEdge(defaultValue = "Show Bottom Faded Edge") {
            override fun toString(): String {
                return "show_bottom_faded_edge"
            }
        },
        LinkLayoutSettings(defaultValue = "Link Layout Settings") {
            override fun toString(): String {
                return "link_layout_settings"
            }
        },
        ChooseTheLayoutYouLikeBest(defaultValue = "Choose the layout you like best") {
            override fun toString(): String {
                return "choose_the_layout_you_like_best"
            }
        },
        FeedPreview(defaultValue = "Feed Preview") {
            override fun toString(): String {
                return "feed_preview"
            }
        },
        RegularListView(defaultValue = "Regular List View") {
            override fun toString(): String {
                return "regular_list_view"
            }
        },
        TitleOnlyListView(defaultValue = "Title Only List View") {
            override fun toString(): String {
                return "title_only_list_view"
            }
        },
        GridView(defaultValue = "Grid View") {
            override fun toString(): String {
                return "grid_view"
            }
        },
        StaggeredView(defaultValue = "Staggered View") {
            override fun toString(): String {
                return "staggered_view"
            }
        },
        FollowSystemTheme(defaultValue = "Follow System Theme") {
            override fun toString(): String {
                return "follow_system_theme"
            }
        },
        UseDynamicTheming(defaultValue = "Use dynamic theming") {
            override fun toString(): String {
                return "use_dynamic_theming"
            }
        },
        UseDynamicThemingDesc(defaultValue = "Change colour themes within the app based on your wallpaper.") {
            override fun toString(): String {
                return "use_dynamic_theming_desc"
            }
        },
        UseAmoledTheme(defaultValue = "Use Amoled Theme") {
            override fun toString(): String {
                return "use_amoled_theme"
            }
        },
        RetrieveLanguageInfoFromServer(defaultValue = "Retrieve Language Info from Server") {
            override fun toString(): String {
                return "retrieve_language_info_from_server"
            }
        }
    }
}

