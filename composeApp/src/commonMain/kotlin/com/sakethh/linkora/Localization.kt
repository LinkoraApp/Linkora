package com.sakethh.linkora

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.utils.Constants
import kotlinx.coroutines.runBlocking

typealias LocalizedStringKey = String

object Localization {
    private val localizedStrings = mutableStateMapOf<LocalizedStringKey, String>()

    fun loadLocalizedStrings(
        languageCode: String, forceLoadDefaultValues: Boolean = false
    ) = runBlocking {
        if (languageCode == Constants.DEFAULT_APP_LANGUAGE_CODE && forceLoadDefaultValues.not()) return@runBlocking
        if (AppPreferences.preferredAppLanguageCode.value != languageCode) {
            AppPreferences.preferredAppLanguageName.value =
                if (languageCode == Constants.DEFAULT_APP_LANGUAGE_CODE) {
                    Constants.DEFAULT_APP_LANGUAGE_NAME
                } else {
                    DependencyContainer.localizationRepo.getLanguageNameForTheCode(
                        languageCode
                    )
                }
            AppPreferences.preferredAppLanguageCode.value = languageCode
            DependencyContainer.preferencesRepo.changePreferenceValue(
                stringPreferencesKey(AppPreferenceType.APP_LANGUAGE_NAME.name),
                AppPreferences.preferredAppLanguageName.value
            )
            DependencyContainer.preferencesRepo.changePreferenceValue(
                stringPreferencesKey(AppPreferenceType.APP_LANGUAGE_CODE.name),
                AppPreferences.preferredAppLanguageCode.value
            )
        }
        Key.entries.forEach { key ->
            localizedStrings[key.name] =
                if (languageCode == Constants.DEFAULT_APP_LANGUAGE_CODE || forceLoadDefaultValues) {
                    key.defaultValue
                } else {
                    DependencyContainer.localizationRepo.getLocalizedStringValueFor(
                        key.name, languageCode
                    ).run {
                        if (this == null || this.isBlank()) {
                            key.defaultValue
                        } else {
                            this
                        }
                    }
                }
        }
    }

    @Composable
    fun rememberLocalizedString(key: Key): String {
        return remember {
            derivedStateOf {
                localizedStrings[key.name] ?: key.defaultValue
            }
        }.value
    }

    fun getLocalizedString(key: Key): String {
        return localizedStrings[key.name] ?: key.defaultValue
    }

    // very bad way but fine for now, will be replaced with build-time generation
    /* fun generateKeysAndDefaultValues() {
         File("/home/saketh/StudioProjects/Linkora/locales/default_en.json").writeText(
             Key.entries.joinToString(
                 separator = ",\n", prefix = "{", postfix = "}"
             ) {
                 """"${it.name}":"${it.defaultValue.replace("\n", "\\n").replace("\"", "\\\"")}""""
             })
     }*/

    enum class Key(val defaultValue: String) {
        SkipSavingExistingLinksLabel(defaultValue = "Skip saving existing links"),
        SkipSavingExistingLinksDesc(defaultValue = "If enabled, a link won't be saved if it already exists in the destination. An error will be thrown instead."),
        Copy(defaultValue = "Copy"),
        Open(defaultValue = "Open"),
        AttachTags(defaultValue = "Attach Tags"),
        CreateANewTag(defaultValue = "Create A New Tag"),
        TagDeletionConfirmation(defaultValue = "Are you sure you want to delete this tag?"),
        SearchFoldersToAdd(defaultValue = "Search folders to add"),
        CouldNotImportCert(defaultValue = "Could not import the certificate file."),
        TagName(defaultValue = "Tag Name"),
        SelectAnAppIcon(defaultValue = "Select an App Icon"),
        ChangeAppIcon(defaultValue = "Change App Icon"),
        ChangeAppIconDesc(defaultValue = "After confirming changes, the app will close automatically to apply them. Notifications will continue to display the default Linkora icon and name."),
        ChangeInAppNameDesc(defaultValue = "The app name will be displayed as \"Weather\" instead of \"Linkora\" in your app drawer."),
        AppIconCurrentlyInUse(defaultValue = "App Icon Currently in Use"),
        NoTagsFound(defaultValue = "No tags found"),
        Tags(defaultValue = "Tags"),
        AddedCopyToImpLinks(defaultValue = "Added Copy to Important Links"),
        NoAttachmentsToTags(defaultValue = "No links were attached to this tag."),
        AboutLibraries(defaultValue = "About Libraries"),
        Libraries(defaultValue = "Libraries"),
        Settings(defaultValue = "Settings"),
        Theme(defaultValue = "Theme"),
        General(defaultValue = "General"),
        Advanced(
            defaultValue = "Advanced"
        ),
        Layout(defaultValue = "Layout"),
        Language(defaultValue = "Language"),
        Data(defaultValue = "Data"),
        Privacy(
            defaultValue = "Privacy"
        ),
        About(defaultValue = "About"),
        Acknowledgments(defaultValue = "Acknowledgments"),
        UseDarkMode(
            defaultValue = "Use Dark Theme"
        ),
        AppLanguage(defaultValue = "App Language"),
        DisplayingRemoteStrings(defaultValue = "Displaying Remote Strings"),
        ResetAppLanguage(
            defaultValue = "Reset App Language"
        ),
        AvailableLanguages(defaultValue = "Available Languages"),
        LoadServerStrings(defaultValue = "Load Server Strings"),
        LoadCompiledStrings(
            defaultValue = "Load Compiled Strings"
        ),
        UpdateLanguageStrings(defaultValue = "Update Language Strings"),
        DownloadLanguageStrings(
            defaultValue = "Download Language Strings"
        ),
        RemoveLanguageStrings(defaultValue = "Remove Language Strings"),
        DisplayingCompiledStrings(
            defaultValue = "Displaying Compiled Strings"
        ),
        Home(defaultValue = "Home"),
        Search(defaultValue = "Search"),
        History(defaultValue = "History"),
        HistoryLinks(
            defaultValue = "History Links"
        ),
        FolderLinks(defaultValue = "Folder Links"),
        Collections(defaultValue = "Collections"),
        LinkoraServerSetup(
            defaultValue = "Linkora Server Setup"
        ),
        ShowOnboardingSlides("Show Onboarding Slides"),
        CollectionDetailPane(defaultValue = "Collection Detail Pane"),
        CreateANewFolder(
            defaultValue = "Create A New Folder"
        ),
        AppIntroSlide3PanelName("Brainstorm Panel"),
        AppIntroSlide3Folder2Name("Reference Materials"),
        AppIntroSlide3Folder2_1Name("Cool Animations"),
        AppIntroSlide3Folder2_1Note("snappy transitions and smooth stuff"),
        AppIntroSlide3Folder3_1Name("Code Snippets"),
        AppIntroSlide3Folder3_1Note("reusable bits and tricks"),
        AppIntroSlide3MainLabel("Introducing Panels."),
        AppIntroSlide3MainLabelDesc("Add any folder to a Panel for quick access from the Home screen. Panels work like tags but for folders. You can group different folders under each one and access them instantly.\nOh, and yep — Linkora supports subfolders too."),
        AppIntroSlide4Label1("Wait,\nThere's More."),
        AppIntroSlide4Label1Desc1("Search, sort, auto title and image detection (when available)."),
        AppIntroSlide4Label1Desc2("Export or import as JSON or HTML, with auto-backup support."),
        AppIntroSlide4Label1Desc3("Sync with your own server if you want."),
        AppIntroSlide4Label1Desc4("Opened links are saved in history — even if the original link is deleted."),
        AppIntroSlide4Label1Desc5("Supports different layout settings."),
        AppIntroSlide4Label1Desc6("Dynamic Material theming (if supported by your device)."),
        AppIntroSlide4Label1Desc7("OLED theme included for Android devices."),
        AppIntroSlide4Label1Desc8("Localization with a central server — language updates without app updates."),
        AppIntroSlide4Label1Desc9("No ads, no paywalls, free as in freedom."),
        AppIntroSlide4Label1Desc10("Just simple, solid bookmarking."),
        DeletingDuplicatesLabel("Deleting Duplicates..."),
        SelectedLinksCount("Selected ${LinkoraPlaceHolder.First.value} links"),
        SelectedFoldersCount("Selected ${LinkoraPlaceHolder.First.value} folders"),
        MultiActionsLabel("Actions"),
        MarkSelectedFoldersAsRoot("Mark selected folders as root"),
        NavigateToCollectionsScreen("Navigate to Collections Screen"),
        AppIntroSlide4Label2("Open. Local First. Yours."),
        AppIntroSlide4Label2Desc("Linkora and its related projects, including the sync-server, are released as free and open-source software under their respective licenses."),
        PreviousPage("Previous page"),
        NextPage("Next page"),
        Done("Done"),
        AppIntroSlide2Folder1Name(
            "Inspiration & Ideas"
        ),
        AppIntroSlide2Folder2Name("Explainers"),
        AppIntroSlide2Folder2Note("in-depth articles or breakdowns"),
        AppIntroSlide2MainLabelDesc(
            "Store links in folders, tag them, mark as Important or Archived, or keep them in \"Saved Links\" to make it your own."
        ),
        AppIntroSlide2MainLabel("Folders, Links &\nTags."),
        AppIntroSlide2Folder1Note(
            "cool stuff i might use later"
        ),
        AppIntroSlide1SwipeLabel("Swipe through or hit Next to discover Linkora's features."),
        AppIntroSlide1Label(
            "Linkora keeps your links private.\nSync and organize—nothing leaves your device unless you set up your own server.\nNo tracking, no cloud."
        ),
        CreateANewFolderIn(
            defaultValue = "Create A New Folder In ${LinkoraPlaceHolder.First.value}"
        ),
        Create(defaultValue = "Create"),
        AddANewLink(defaultValue = "Add A New Link"),
        SavedLinks(
            defaultValue = "Saved Links"
        ),
        AddANewLinkInImportantLinks(defaultValue = "Add a new link in Important Links"),
        AddANewLinkInSavedLinks(
            defaultValue = "Add a new link in Saved Links"
        ),
        AddANewLinkIn(defaultValue = "Add a new link in ${LinkoraPlaceHolder.First.value}"),
        LinkAddress(
            defaultValue = "Link Address"
        ),
        TitleForTheLink(defaultValue = "Title for the link"),
        NoteForSavingTheLink(defaultValue = "Note for saving the link"),
        AutoDetectTitleIsEnabled(
            defaultValue = "Auto Detect Title is currently active."
        ),
        DataRetrievalDisabled(defaultValue = "Data retrieval is blocked as the 'Force Save Links without retrieval' feature is currently active."),
        ForceAutoDetectTitle(
            defaultValue = "Force Auto-Detect Title"
        ),
        RetryingWithSecondaryUserAgent(defaultValue = "Retrying metadata retrieval using a secondary user agent."),
        ForceSaveWithoutRetrievingMetadata(
            defaultValue = "Force Save Without Retrieving Metadata"
        ),
        AddIn(defaultValue = "Add in"),
        InitialRequestFailed(defaultValue = "he initial request failed."),
        ImportantLinks(
            defaultValue = "Important Links"
        ),
        Save(defaultValue = "Save"),
        Update(defaultValue = "Update"),
        SelectACollectionSource(defaultValue = "Select a collection source"),
        SaveInThisFolder(defaultValue = "Save in this folder"),
        Cancel(
            defaultValue = "Cancel"
        ),
        FolderName(defaultValue = "Folder name"),
        SearchForFolders(defaultValue = "Search for Folders..."),
        NoteForCreatingTheFolder(defaultValue = "Note For Creating The Folder"),
        AllLinks(
            defaultValue = "All Links"
        ),
        Links(defaultValue = "Links"),
        Archive(defaultValue = "Archive"),
        ArchiveLinks(defaultValue = "Archive Links"),
        Folders(
            defaultValue = "Folders"
        ),
        SuccessfullySavedConnectionDetails(defaultValue = "Successfully saved connection details."),
        DeletedTheServerConnectionSuccessfully(
            defaultValue = "Deleted the server connection successfully."
        ),
        UseInAppBrowser(defaultValue = "Use In-App Browser"),
        UseCustomAppVersionLabel(defaultValue = "Use Custom App Version Label"),
        UseCustomAppVersionDesc(defaultValue = "Enables a custom font-based version label throughout the app."),
        UseInAppBrowserDesc(defaultValue = "Enable this to open links within the app; otherwise, your default browser will open when clicking on links."),
        EnableHomeScreen(
            defaultValue = "Enable Home Screen"
        ),
        EnableHomeScreenDesc(defaultValue = "When disabled, Collections opens on launch if Home is set as the initial route."),
        AutoDetectTitle(
            defaultValue = "Auto-Detect Title"
        ),
        AutoDetectTitleDesc(defaultValue = "Note: This may not detect every website."),
        ForceSaveWithoutRetrievingMetadataDesc(
            defaultValue = "Link will be saved as you save it, nothing gets fetched. Note that this will impact on refreshing links from link menu, link will NOT be refreshed if this is enabled."
        ),
        ShowAssociatedImageInLinkMenu(defaultValue = "Show associated image in link menu"),
        ShowAssociatedImageInLinkMenuDesc(
            defaultValue = "Enables the display of an associated image within the link menu."
        ),
        AutoCheckForUpdates(defaultValue = "Enables the display of an associated image within the link menu."),
        AutoCheckForUpdatesDesc(
            defaultValue = "Enable to auto-check for updates on app launch. Disable for manual checks."
        ),
        ShowDescriptionForSettings(defaultValue = "Show description for Settings"),
        ShowDescriptionForSettingsDesc(
            defaultValue = "Enable to show detailed descriptions for settings. Disable to show only titles."
        ),
        ManageConnectedServer(defaultValue = "Manage Connected Server"),
        ManageConnectedServerDesc(
            defaultValue = "Your data is synced with the Linkora server. Tap to manage or disconnect."
        ),
        CurrentlyConnectedTo(defaultValue = "Currently Connected To"),
        SyncType(defaultValue = "Sync Type"),
        EditServerConfiguration(
            defaultValue = "Edit server configuration"
        ),
        DeleteTheServerConnection(defaultValue = "Delete the connection"),
        Configuration(
            defaultValue = "Configuration"
        ),
        ServerURL(defaultValue = "Server URL"),
        ServerSetupInstruction(defaultValue = "Ensure the server is running. The URL pattern should be: https://<IPv4>:<HTTPS_PORT>/"),
        SecurityToken(
            defaultValue = "Security Token"
        ),
        ServerIsReachable(defaultValue = "Server Exists and Is Reachable!"),
        TestServerAvailability(
            defaultValue = "Test Server Availability"
        ),
        UseThisConnection(defaultValue = "Use This Connection"),
        ClientToServer(defaultValue = "Client To Server"),
        ClientToServerDesc(
            defaultValue = "Client changes are sent to the server, but client is not updated with server changes."
        ),
        ServerToClient(defaultValue = "Server To Client"),
        ServerToClientDesc(defaultValue = "Server changes are sent to the client, but server is not updated with client changes."),
        TwoWaySync(
            defaultValue = "Two-Way Sync"
        ),
        TwoWaySyncDesc(defaultValue = "Changes are sent both ways: client updates the server, and server updates the client."),
        ImportLabel(
            defaultValue = "Import"
        ),
        ImportLabelDesc(
            defaultValue = "You are connected to a sync server. To load existing data from the remote database, use server-sync instead of manual import. Importing externally will duplicate data, even if it already exists remotely."
        ),
        ExportLabel(defaultValue = "Export"),
        ExportLabelDesc(defaultValue = "Exporting will remove all data linked to the remote database, making the exported data portable across different clients."),
        ImportUsingJsonFile(
            defaultValue = "Import using JSON file"
        ),
        ImportUsingJsonFileDesc(
            defaultValue = "Use a Linkora-compatible JSON file to import your data"
        ),
        ImportDataFromHtmlFile(defaultValue = "Import data from HTML file"),
        ImportDataFromHtmlFileDesc(
            defaultValue = "Load data from a bookmarks-compatible HTML file"
        ),
        ExportDataAsJson(defaultValue = "Export Data as JSON"),
        ExportDataAsJsonDesc(defaultValue = "Exports all data in Linkora-compatible JSON format"),
        ExportDataAsHtml(
            defaultValue = "Export Data as HTML"
        ),
        ExportDataAsHtmlDesc(defaultValue = "Export all your data as an HTML file (excluding Panels and Tags). Compatible with browser-based import tools."),
        Sync(
            defaultValue = "Sync"
        ),
        ConnectToALinkoraServer(defaultValue = "Connect to a Linkora Server"),
        ConnectToALinkoraServerDesc(
            defaultValue = "By connecting to a Linkora server, you can sync your data and access it on any device using the Linkora app."
        ),
        DeleteEntireDataPermanently(defaultValue = "Delete entire data permanently"),
        DeleteEntireDataPermanentlyDesc(
            defaultValue = "Permanently delete all links, folders, panels, and localized strings."
        ),
        DeleteDuplicateLinksFromAllCollections(defaultValue = "Delete Duplicate Links"),
        DeleteDuplicateLinksFromAllCollectionsDesc(
            defaultValue = "Removes all duplicate links from your local storage and the database connected via Linkora’s sync server."
        ),
        DeletedDuplicatedLinksSuccessfully(defaultValue = "Deleted Duplicate Links Successfully."),
        DeletedEntireDataPermanently(
            defaultValue = "Deleted entire data permanently."
        ),
        ClearImageCache(
            defaultValue = "Clear Image Cache"
        ),
        ClearImageCacheDesc(defaultValue = "Images are cached by default. Changing the user agent might affect what you see. Clear the cache to resolve it."),
        RefreshAllLinksTitlesAndImages(
            defaultValue = "Refresh Titles and Images of all links"
        ),
        RefreshAllLinksTitlesAndImagesDesc(defaultValue = "Manually entered titles will be replaced with detected titles."),
        RefreshingLinks(
            defaultValue = "Refreshing links…"
        ),
        RefreshingLinksAndroidDesc(defaultValue = "Closing Linkora won\'t interrupt link refreshing, but newly added links might not be processed."),
        RefreshingLinksDesktopDesc(
            defaultValue = "Closing Linkora will cancel link refreshing."
        ),
        InitialScreenOnLaunch(defaultValue = "Initial Screen on Launch"),
        InitialScreenOnLaunchDesc(
            defaultValue = "Changes made with this option will reflect in the navigation of the initial screen that will open when you launch Linkora."
        ),
        Confirm(defaultValue = "Confirm"),
        SelectTheInitialScreen(defaultValue = "Choose the screen to launch the app with"),
        ShowBorderAroundLinks(
            defaultValue = "Show Border Around Links"
        ),
        ShowTitle(defaultValue = "Show Title"),
        ShowHostAddress(defaultValue = "Show Host Address"),
        ShowBottomFadedEdge(
            defaultValue = "Show Bottom Faded Edge"
        ),
        ShowNote(
            defaultValue = "Show Note"
        ),
        ShowVideoTagOnUIIfApplicable(defaultValue = "Show Video Tag On UI If Applicable"),
        LinkLayoutSettings(
            defaultValue = "Link Layout Settings"
        ),
        ChooseTheLayoutYouLikeBest(defaultValue = "Select your preferred layout"),
        FeedPreview(
            defaultValue = "Feed Preview"
        ),
        ChangeInitialRoute("Change Initial Route"),
        ChangeInitialRouteDesc("Changing the initial route lets you set which screen opens first when the app launches."),
        RegularListView(defaultValue = "Regular List View"),
        TitleOnlyListView(defaultValue = "Title Only List View"),
        GridView(
            defaultValue = "Grid View"
        ),
        StaggeredView(defaultValue = "Staggered View"),
        FollowSystemTheme(defaultValue = "Follow System Theme"),
        UseDynamicTheming(
            defaultValue = "Use dynamic theming"
        ),
        UseDynamicThemingDesc(defaultValue = "Automatically sync the app's color theme with your wallpaper."),
        UseAmoledTheme(
            defaultValue = "Use Amoled Theme"
        ),
        RetrieveLanguageInfoFromServer(defaultValue = "Retrieve Language Info from Server"),
        SelectACollection(
            defaultValue = "Select a Collection"
        ),
        SelectAPanel(defaultValue = "Select a Panel"),
        FolderHasBeenCreatedSuccessful(defaultValue = "The folder ${LinkoraPlaceHolder.First.value} has been successfully created."),
        RemoteExecutionFailed(
            defaultValue = "Remote execution failed :"
        ),
        SavedAvailableLanguagesInfoLocally(defaultValue = "Saved Available Languages Info locally."),
        DeletedTheStringsPack(
            defaultValue = "Deleted the ${LinkoraPlaceHolder.First.value} strings pack."
        ),
        DownloadedLanguageStrings(defaultValue = "Downloaded Language Strings for the ${LinkoraPlaceHolder.First.value}."),
        Linkora(
            defaultValue = "Linkora"
        ),
        LinkoraIsConnectedToAServer(defaultValue = "Linkora is connected to the server; syncing is based on ${LinkoraPlaceHolder.First.value}."),
        CopiedLinkToClipboard(
            defaultValue = "Copied Link to Clipboard"
        ),
        FetchingAvailableLanguages(defaultValue = "Fetching Available Languages"),
        DownloadingStrings(
            defaultValue = "Downloading Strings for ${LinkoraPlaceHolder.First.value}"
        ),
        CopiedTitleToTheClipboard(defaultValue = "Copied Title to The Clipboard"),
        CopiedNoteToTheClipboard(
            defaultValue = "Copied Note to The Clipboard"
        ),
        ViewNote(defaultValue = "View Note"),
        Rename(defaultValue = "Rename"),
        RefreshImageAndTitle(
            defaultValue = "Refresh Image And Title"
        ),
        Refresh(defaultValue = "Refresh"),
        UnArchive(defaultValue = "Unarchive"),
        UnArchived(
            defaultValue = "Unarchived and saved to the default \"Saved Links\" collection."
        ),
        DeleteTheNote(defaultValue = "Delete The Note"),
        MoveToRootFolders(defaultValue = "Move To Root Folders"),
        DeleteTheLink(
            defaultValue = "Delete the Link"
        ),
        DeletedTheLink(defaultValue = "Deleted the Link"),
        CopyFolder(defaultValue = "Copy Folder"),
        MoveToOtherFolder(
            defaultValue = "Move To Other Folder"
        ),
        CopyLink(defaultValue = "Copy Link"),
        MoveLink(defaultValue = "Move Link"),
        DeleteTheFolder(
            defaultValue = "Delete The Folder"
        ),
        DeletedTheFolder(defaultValue = "Folder ${LinkoraPlaceHolder.First.value}, all internal folders, and associated links have been successfully deleted."),
        DeletedTheNoteOfAFolder(
            defaultValue = "Successfully deleted the note of the ${LinkoraPlaceHolder.First.value}."
        ),
        DeletedTheNoteOfALink(defaultValue = "Successfully deleted the note."),
        FolderDeletionLabel(
            defaultValue = "Deleting this folder will also remove all its subfolders"
        ),
        AreYouSureDeleteSelectedLinks(defaultValue = "Are you sure you want to delete all selected links?"),
        AreYouSureDeleteLink(
            defaultValue = "Are you sure you want to delete the link?"
        ),
        AreYouSureDeleteSelectedFolders(defaultValue = "Are you sure you want to delete all selected folders?"),
        AreYouSureDeleteFolder(
            defaultValue = "Are you sure you want to delete the folder?"
        ),
        AreYouSureDeleteSelectedItems(defaultValue = "Are you sure you want to delete all selected items?"),
        AreYouSureDeleteEverything(
            defaultValue = "Are you sure you want to delete all folders, links, panels, tags, and localized strings?"
        ),
        SavedNote(defaultValue = "Saved Note"),
        NoNoteAdded(defaultValue = "You haven't added a note for this yet."),
        Delete(
            defaultValue = "Delete"
        ),
        ArchivedTheFolder(defaultValue = "The folder ${LinkoraPlaceHolder.First.value} has been archived."),
        UnArchivedTheFolder(
            defaultValue = "The folder ${LinkoraPlaceHolder.First.value} has been unarchived."
        ),
        ArchivedTheLink(defaultValue = "Link has been archived."),
        ChangeBothNameAndNote(
            defaultValue = "Change Name and Note"
        ),
        ChangeNoteOnly(defaultValue = "Change Note"),
        RenameFolder(defaultValue = "Rename Folder ${LinkoraPlaceHolder.First.value}"),
        ChangeLinkData(
            defaultValue = "Change Link data"
        ),
        NewName(defaultValue = "New Name"),
        NewTitle(defaultValue = "New Title"),
        NewNote(
            defaultValue = "New Note"
        ),
        UpdatedTheNote(defaultValue = "Updated the note successfully."),
        UpdatedTheName(defaultValue = "Updated the name successfully."),
        UpdatedTheTitle(
            defaultValue = "Updated the title successfully."
        ),
        UpdatedTheFolderData(defaultValue = "Folder data has been updated successfully."),
        InvalidLink(
            defaultValue = "The link provided is invalid. Please check and try again."
        ),
        SavedTheLink(defaultValue = "Saved the link successfully."),
        DeletionInProgress(defaultValue = "Deletion In Progress.."),
        RemoveALinkFromImpLink(
            defaultValue = "Remove from Important Links"
        ),
        MarkALinkAsImpLink(defaultValue = "Add to Important Links"),
        NewestToOldest(defaultValue = "Newest to Oldest"),
        OldestToNewest(
            defaultValue = "Oldest to Newest"
        ),
        AToZSequence(defaultValue = "A to Z Sequence"),
        ZToASequence(defaultValue = "Z to A Sequence"),
        SortFoldersBy(
            defaultValue = "Sort folders by"
        ),
        SortHistoryLinksBy(defaultValue = "Sort History Links by"),
        SortBy(defaultValue = "Sort by"),
        SortSavedLinksBy(
            defaultValue = "Sort Saved Links by"
        ),
        SortImportantLinksBy(defaultValue = "Sort Important Links by"),
        SortBasedOn(defaultValue = "Sort Based on"),
        SearchTitlesToFindLinksAndFolders(
            defaultValue = "Search titles or notes to find links and folders"
        ),
        RegularFolder(defaultValue = "Regular Folder"),
        ArchiveFolder(defaultValue = "Archive Folder"),
        GoodMorning(
            defaultValue = "Good Morning"
        ),
        GoodAfternoon(defaultValue = "Good Afternoon"),
        GoodEvening(defaultValue = "Good Evening"),
        HeyHi(
            defaultValue = "Hey, hi👋"
        ),
        Default(defaultValue = "Default"),
        SelectedPanel(defaultValue = "Selected Panel"),
        AddANewPanel(
            defaultValue = "Add A New Panel"
        ),
        Panels(defaultValue = "Panels"),
        Snapshots(defaultValue = "Snapshots"),
        RenameTagName(defaultValue = "Rename tag Name"),
        NewTagName(defaultValue = "New tag name"),
        FoldersInThisPanel(defaultValue = "Folders in This Panel"),
        FoldersThatCanBeAddedToThisPanel(
            defaultValue = "Folders that can be added to this panel"
        ),
        PanelName(defaultValue = "Panel Name"),
        PermanentlyDeleteThePanel(defaultValue = "Permanently Delete Panel"),
        OnceDeletedThisPanelCannotBeRestored(
            defaultValue = "Once deleted, this Panel cannot be restored."
        ),
        AreYouSureWantToDeleteThePanel(defaultValue = "Are you sure want to delete the panel named ${LinkoraPlaceHolder.First.value}?"),
        NewNameForPanel(
            defaultValue = "New Name for Panel"
        ),
        ChangePanelName(defaultValue = "Change Panel Name"),
        EditPanelName(defaultValue = "Edit ${LinkoraPlaceHolder.First.value} Panel Name"),
        RedirectToLatestReleasePage(
            defaultValue = "Redirect to latest release page"
        ),
        NewUpdateIsAvailable(defaultValue = "Linkora just got better, new update is available."),
        CurrentVersion(
            defaultValue = "version you're using"
        ),
        LatestVersionAvailableDesc(defaultValue = "a new ${LinkoraPlaceHolder.First.value} build has been released"),
        TrackRecentChangesAndUpdatesToLinkora(
            defaultValue = "Track recent changes and updates to Linkora."
        ),
        Changelog(defaultValue = "Changelog"),
        OpenAGithubIssue(defaultValue = "Open a GitHub Issue"),
        HaveASuggestionCreateAnIssueOnGithubToImproveLinkora(
            defaultValue = "Have a suggestion? Create an issue on GitHub to improve Linkora."
        ),
        GithubDesc(defaultValue = "The Linkora app, sync server, and localization server are public and open-source—feel free to explore the code."),
        Github(
            defaultValue = "Github"
        ),
        Discord(defaultValue = "Discord"),
        Twitter(defaultValue = "Twitter"),
        Development(
            defaultValue = "Development"
        ),
        Socials(defaultValue = "Socials"),
        YouAreUsingLatestVersionOfLinkora(defaultValue = "You are using latest version of Linkora."),
        CheckForLatestVersion(
            defaultValue = "Check for latest version"
        ),
        RetrievingLatestInformation(defaultValue = "Retrieving latest information, this may take sometime."),
        LinkoraOpenSourceAcknowledgement(
            defaultValue = "Linkora wouldn't be possible without the following open-source software, libraries."
        ),
        UserAgent(defaultValue = "User Agent"),
        UserAgentDesc(defaultValue = "Helps detect images and titles from webpage meta tags. Results may vary based on the agent string."),
        LocalizationServerDesc(
            defaultValue = "Linkora’s localization server lets you update strings without updating the app. By default, it uses Linkora’s server.\n\nYou can switch to your own server if needed. Changes will reflect in Linkora’s network requests for language or string updates.\n\nOnly change this if you’re sure about what you’re doing."
        ),
        LocalizationServer(defaultValue = "Localization Server"),
        PreparingToExportYourData(
            defaultValue = "Preparing to export your data..."
        ),
        CollectingLinksForExport(defaultValue = "Collecting links for export..."),
        CollectingFoldersForExport(
            defaultValue = "Collecting folders for export..."
        ),
        ForceShuffleLinks("Force Shuffle Links"),
        ForceShuffleLinksDesc("Forces a randomized link order, overriding the sorting type above. This applies only to links, not folders."),
        CollectingPanelsForExport(defaultValue = "Collecting panels for export..."),
        CollectingPanelFoldersForExport(
            defaultValue = "Collecting panel-folders for export..."
        ),
        SerializingCollectedDataForExport(defaultValue = "Serializing the collected data for export..."),
        ImportExportScreenTopAppBarDesc(
            defaultValue = "Stay on this page! DO NOT PANIC IF IT LOOKS STUCK."
        ),
        ExportedSuccessfully(defaultValue = "Exported Successfully"),
        StoragePermissionIsRequired(
            defaultValue = "Storage permission is required to store or export the file. Please grant the permission to proceed."
        ),
        NotificationPermissionIsRequired(defaultValue = "Enable notification permission to view the progress of link refreshes."),
        PermissionGranted(
            defaultValue = "Permission granted. Please retry the action to continue with your import/export."
        ),
        LinkRefreshedSuccessfully(defaultValue = "The link data has been successfully refreshed."),
        Share(
            defaultValue = "Share"
        ), Edit(
            defaultValue = "Edit"
        ),AssociatedTags(
            defaultValue = "Associated Tags"
        ),
        ForceOpenInABrowser(defaultValue = "Open In A Browser"),
        WorkManagerDesc(defaultValue = "Work Manager is scheduling the links refreshing task. It will continue shortly.\nYou can close the app; this task will continue in the background."),
        NoOfLinksRefreshed(
            defaultValue = "${LinkoraPlaceHolder.First.value} of ${LinkoraPlaceHolder.Second.value} links refreshed."
        ),
        TopDecoratorSetting(defaultValue = "Use Linkora's Top Decorator"),
        TopDecoratorSettingDesc(
            defaultValue = "When disabled, the default decorator will be used. Changes will apply on the next launch."
        ),
        ThisFolderHasNoSubfolders(defaultValue = "This folder does not contain any subfolders."),
        PanelCreatedSuccessfully(
            defaultValue = "Panel created successfully with the name: ${LinkoraPlaceHolder.First.value}"
        ),
        DeletedPanelSuccessfully(defaultValue = "Panel deleted successfully."),
        UpdatedThePanelNameSuccessfully(
            defaultValue = "Panel name updated to: ${LinkoraPlaceHolder.First.value}."
        ),
        ImportingDataFromTheSever(defaultValue = "Importing Data from the Remote Server"),
        ImportingDataFromTheSeverDesc(
            defaultValue = "If you cancel the import, some data may still be saved, and the server connection will be removed."
        ),
        SuccessfullyConnectedToTheServer(defaultValue = "Server connection established successfully."),
        ConnectionToServerFailed(
            defaultValue = "Could not connect to the server. Ensure the server is reachable."
        ),
        UpdatingChangesOnRemoteServer(defaultValue = "Updating Changes on Remote Server"),
        LOLCATplAck(
            defaultValue = "LOLCATpl made this logo. Across the app on all platforms, Linkora's improved UI components are inspired and based on the designs made by LOLCATpl."
        ),
        MondsternAck(
            defaultValue = "mondstern painted this logo, which was based on the legacy logo."
        ),
        AckEndingText(defaultValue = "And, of course, the underlying libraries used by these also impact Linkora's behavior."),
        LOLCATplOnDiscord(
            defaultValue = "LOLCATpl on Discord"
        ),
        MondsternOnDiscord(defaultValue = "mondstern on Pixelfed"),
        NoFoldersOrLinksFound(
            defaultValue = "Nothing here yet. Add folders or links to begin."
        ),
        FoldersExistsButNotLinks(
            defaultValue = "Folders are in. Now add links to organize your content."
        ),
        NoFoldersFound(defaultValue = "No folders found. Add folders to get started."),
        ExportingDataToJSON(
            defaultValue = "Exporting Data to JSON..."
        ),
        NoFoldersFoundInArchive(defaultValue = "No folders found."),
        ExportingDataToHTML(defaultValue = "Exporting Data to HTML..."),
        ReadingFile(defaultValue = "Reading file..."),
        SuccessfullyImportedTheData(
            defaultValue = "Successfully imported the data."
        ),
        SelectAValidFile(defaultValue = "Select a valid ${LinkoraPlaceHolder.First.value} File"),
        FileTypeNotSupportedOnDesktopImport(
            defaultValue = "${LinkoraPlaceHolder.First.value} files are not supported for importing, pick valid ${LinkoraPlaceHolder.Second.value} file."
        ),
        NoFoldersInThePanel(defaultValue = "No folders in this panel. Add folders in this panel to get started."),
        NoLinksFound(
            defaultValue = "No links available. Start by adding a few."
        ),
        NoArchiveLinksFound(
            defaultValue = "No links found."
        ),
        NoRemoteLangPacks(defaultValue = "No remote language packs found.\nLoad from the server to start using Linkora in your language."),
        StringsLocalizedStatus(
            defaultValue = "${LinkoraPlaceHolder.First.value}/${LinkoraPlaceHolder.Second.value} strings localized"
        ),
        EnableNotifications(defaultValue = "Enable Notifications"),
        NotificationPermissionRequired(
            defaultValue = "Notification Permission Required"
        ),
        NotificationPermissionDesc(defaultValue = "Linkora needs notification permission to display sync progress, link refreshes, and auto-save status."),
        NoPanelsFound(
            defaultValue = "No panels yet.\nCreate panels and link folders to organize your work by topic or purpose."
        ),
        SearchInLinkora(defaultValue = "Search Linkora: Browse through all your saved links and folders."),
        NoSearchResults(
            defaultValue = "No results found."
        ),
        NoHistoryFound(defaultValue = "No history found. Your history is clean!"),
        StartingImportingProcess(
            defaultValue = "Starting data import from JSON file: ${LinkoraPlaceHolder.First.value}"
        ),
        DataSynchronizationCompletedSuccessfully(defaultValue = "Data synchronization completed successfully."),
        InitiateManualSync(
            defaultValue = "Initiate Manual Sync"
        ),
        InitiateManualSyncDesc(defaultValue = "Pending queue items will be force-pushed, while non-synced server items will be pulled."),
        InitiateManualSyncDescAlt(
            defaultValue = "Pending items will be pushed, and unsynced data from the server will be pulled."
        ),
        SyncingDataLabel(defaultValue = "Syncing Data..."),
        RemoteDataDeletionFailure(defaultValue = "The data from the remote database could not be deleted."),
        DeleteEverythingFromRemoteDatabaseLabel(
            defaultValue = "Delete all data from the remote database as well"
        ),
        ProvideAValidFileLocation(defaultValue = "Provide a valid file location"),
        ImportMethodLabel(
            defaultValue = "Import Method : "
        ),
        FileLocationLabel(defaultValue = "File Location"),
        FilePickerLabel(defaultValue = "File Picker"),
        NavigateAndCopyDesc(defaultValue = "Navigate to the folder where you want to copy the selected items and hit the paste button to copy them there."),
        NavigateAndMoveDesc(defaultValue = "Navigate to the folder where you want to move the selected items and hit the paste button to move them there."),
        Moving(defaultValue = "Moving..."),
        Copying(defaultValue = "Copying..."),
        LinkoraOnTwitter(defaultValue = "Linkora on Twitter"),
        LinkoraOnDiscord(defaultValue = "Linkora on Discord"),
        LinkoraOnGithub(defaultValue = "Linkora on Github"),
        LinkoraIssues(defaultValue = "Issues · LinkoraApp/Linkora"),
        LinokraReleases(defaultValue = "Releases · LinkoraApp/Linkora"),
        LinokraReleasesOnGitHub(defaultValue = "Releases · LinkoraApp/Linkora on Github"),
        LinkoraIssuesOnGithub(defaultValue = "Issues · LinkoraApp/Linkora on Github"),
        CurrentExportLocation(defaultValue = "Current export location"),
        CurrentExportLocationSupportingText(defaultValue = "If the selected directory is deleted or moved, export will silently fail. Make sure the selected directory always exists."),
        ServerCertificateAlreadyImported(defaultValue = "A certificate has already been imported. If you're unsure whether it matches the server-generated certificate, you can import the updated certificate to replace the existing one.\n\n${LinkoraPlaceHolder.First.value}"),
        ImportServerCertificateDescription(defaultValue = "To connect securely, please import the .cer certificate automatically generated by your server."),
        ImportedServerCertificate(defaultValue = "Imported: ${LinkoraPlaceHolder.First.value}"),
        ProcessingCertificate(defaultValue = "Processing the certificate..."),
        ImportServerCertificate(defaultValue = "Import Server Certificate"),
        ForceBypassCertificateChecking(defaultValue = "Force bypass certificate checking"),
        ForceBypassCertificateCheckingDescription(defaultValue = "Bypasses certificate validation. This is not recommended. If you have a valid .cer certificate, keep this disabled and import the certificate instead."),
        Translate(defaultValue = "Translate"),
        BuyMeACoffee(defaultValue = "Buy me a Coffee"),
        UseSnapshots(defaultValue = "Use snapshots"),
        UseSnapshotsDescription(defaultValue = "Links, folders, panels, and panel folders will be auto-exported in your chosen format."),
        SnapshotsBackupLocationWarning(defaultValue = "If the selected directory is moved or deleted, backup will silently fail. Make sure the selected directory always exists."),
        SnapshotsBackupLocation(defaultValue = "Current backup location"),
        EnableAutoDeleteSnapshots(defaultValue = "Enable Auto-Deletion of Old Snapshots"),
        EnableAutoDeleteSnapshotsDescription(defaultValue = "When enabled, the app will automatically delete the oldest snapshots once they exceed the configured limit."),
        SnapshotsFileLimitWarning(defaultValue = "File limit is exclusive - checked before creating new backups, not after."),
        SnapshotsFileLimit(defaultValue = "Auto-delete if snapshots count exceeds limit:"),
        ExportAs(defaultValue = "Export As"),
        SnapshotsExportDescriptionDesktop(defaultValue = "Each create, update, or delete action on a link, folder, panel, or panel folder triggers an export. Progress is shown in the side navigation rail."),
        SnapshotsExportDescriptionAndroid(defaultValue = "Any time you add, edit, or delete a link, folder, panel, or panel folder, Linkora auto-exports in the background."),
        BackupsWorkOnlyWithDirectory("Backups will only work if you pick a directory first."),
        ExportRequiresDirectory("You need to pick a directory before exporting."),
        ArchivedSuccessfully("Archived successfully."),
        DeletedSuccessfully("Deleted successfully."),
        ServerCertificateSavedSuccessfully("Server certificate saved successfully."),
        Both("Both")
    }
}