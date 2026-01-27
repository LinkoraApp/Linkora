package com.sakethh.linkora.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PublicOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Home
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.UriHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.PermissionStatus
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.domain.AppIconCode
import com.sakethh.linkora.ui.domain.model.LinkComponentParam
import com.sakethh.linkora.ui.domain.model.LinkPref
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.onboarding.OnboardingSlide
import com.sakethh.linkora.ui.screens.onboarding.Slide1
import com.sakethh.linkora.ui.screens.onboarding.Slide2
import com.sakethh.linkora.ui.screens.onboarding.Slide3
import com.sakethh.linkora.ui.screens.onboarding.Slide4
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.utils.getLocalizedString
import kotlinx.coroutines.launch

open class SettingsScreenViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val nativeUtils: NativeUtils,
    private val permissionManager: PermissionManager
) : ViewModel() {

    fun generalSection(platform: Platform): List<SettingComponentParam> {
        return buildList {
            this.addAll(
                listOf(
                    SettingComponentParam(
                        title = Localization.getLocalizedString(Localization.Key.AutoDetectTitle),
                        doesDescriptionExists = true,
                        description = Localization.getLocalizedString(Localization.Key.AutoDetectTitleDesc),
                        isSwitchNeeded = true,
                        isSwitchEnabled = AppPreferences.isAutoDetectTitleForLinksEnabled,
                        isIconNeeded = mutableStateOf(true),
                        icon = Icons.Default.Search,
                        onSwitchStateChange = {
                            viewModelScope.launch {
                                changeSettingPreferenceValue(
                                    preferenceKey = booleanPreferencesKey(
                                        AppPreferenceType.AUTO_DETECT_TITLE_FOR_LINK.name
                                    ), newValue = it
                                )
                                AppPreferences.isAutoDetectTitleForLinksEnabled.value = it

                                if (it) {
                                    changeSettingPreferenceValue(
                                        preferenceKey = booleanPreferencesKey(
                                            AppPreferenceType.FORCE_SAVE_WITHOUT_FETCHING_META_DATA.name
                                        ), newValue = false
                                    )
                                    AppPreferences.forceSaveWithoutFetchingAnyMetaData.value = false
                                }
                            }
                        }), SettingComponentParam(
                        title = Localization.getLocalizedString(Localization.Key.ForceSaveWithoutRetrievingMetadata),
                        doesDescriptionExists = true,
                        description = Localization.getLocalizedString(Localization.Key.ForceSaveWithoutRetrievingMetadataDesc),
                        isSwitchNeeded = true,
                        isSwitchEnabled = AppPreferences.forceSaveWithoutFetchingAnyMetaData,
                        isIconNeeded = mutableStateOf(true),
                        icon = Icons.Default.PublicOff,
                        onSwitchStateChange = {
                            viewModelScope.launch {
                                changeSettingPreferenceValue(
                                    preferenceKey = booleanPreferencesKey(
                                        AppPreferenceType.FORCE_SAVE_WITHOUT_FETCHING_META_DATA.name
                                    ), newValue = it
                                )
                                AppPreferences.forceSaveWithoutFetchingAnyMetaData.value = it

                                if (it) {
                                    changeSettingPreferenceValue(
                                        preferenceKey = booleanPreferencesKey(
                                            AppPreferenceType.AUTO_DETECT_TITLE_FOR_LINK.name
                                        ), newValue = false
                                    )
                                    AppPreferences.isAutoDetectTitleForLinksEnabled.value = false
                                }
                            }
                        },
                    ), SettingComponentParam(
                        title = Localization.Key.SkipSavingExistingLinksLabel.getLocalizedString(),
                        doesDescriptionExists = true,
                        description = Localization.Key.SkipSavingExistingLinksDesc.getLocalizedString(),
                        isSwitchNeeded = true,
                        isSwitchEnabled = AppPreferences.skipSavingExistingLink,
                        isIconNeeded = mutableStateOf(true),
                        icon = Icons.Default.Block,
                        onSwitchStateChange = {
                            viewModelScope.launch {
                                changeSettingPreferenceValue(
                                    preferenceKey = booleanPreferencesKey(
                                        AppPreferenceType.SKIP_SAVING_EXISTING_LINK.name
                                    ), newValue = it
                                )
                                AppPreferences.skipSavingExistingLink.value = it
                            }
                        },
                    )
                )
            )

            if (platform == Platform.Android.Mobile) {
                add(
                    SettingComponentParam(
                        title = Localization.getLocalizedString(Localization.Key.ShowAssociatedImageInLinkMenu),
                        doesDescriptionExists = true,
                        description = Localization.getLocalizedString(Localization.Key.ShowAssociatedImageInLinkMenuDesc),
                        isSwitchNeeded = true,
                        isSwitchEnabled = AppPreferences.showAssociatedImageInLinkMenu,
                        isIconNeeded = mutableStateOf(true),
                        icon = Icons.Default.Image,
                        onSwitchStateChange = {
                            viewModelScope.launch {
                                changeSettingPreferenceValue(
                                    preferenceKey = booleanPreferencesKey(
                                        AppPreferenceType.ASSOCIATED_IMAGES_IN_LINK_MENU_VISIBILITY.name
                                    ), newValue = it
                                )
                                AppPreferences.showAssociatedImageInLinkMenu.value = it
                            }
                        })
                )

                add(
                    SettingComponentParam(
                        title = "Auto-save links when shared from other apps",
                        doesDescriptionExists = true,
                        description = "Auto-saved links will be added to \"Saved Links\" without tags, and their title and image will be fetched automatically.",
                        isSwitchNeeded = true,
                        isSwitchEnabled = AppPreferences.autoSaveOnShareIntent,
                        onSwitchStateChange = {
                            viewModelScope.launch {
                                if (permissionManager.permittedToShowNotification() == PermissionStatus.Granted) {
                                    changeSettingPreferenceValue(
                                        preferenceKey = booleanPreferencesKey(
                                            AppPreferenceType.AUTO_SAVE_ON_SHARE_INTENT.name
                                        ), newValue = it
                                    )
                                    AppPreferences.autoSaveOnShareIntent.value = it
                                } else {
                                    UIEvent.pushUIEvent(UIEvent.Type.ShowSnackbar(message = "Auto-saving requires notification permission."))
                                }
                            }
                        },
                        isIconNeeded = mutableStateOf(true),
                        icon = Icons.Default.AutoMode
                    )
                )
            }
            add(
                SettingComponentParam(
                    title = Localization.Key.EnableHomeScreen.getLocalizedString(),
                    doesDescriptionExists = true,
                    description = Localization.Key.EnableHomeScreenDesc.getLocalizedString(),
                    isSwitchNeeded = true,
                    isSwitchEnabled = AppPreferences.isHomeScreenEnabled,
                    onSwitchStateChange = {
                        AppPreferences.isHomeScreenEnabled.value = it
                        changeSettingPreferenceValue(
                            preferenceKey = booleanPreferencesKey(
                                AppPreferenceType.HOME_SCREEN_VISIBILITY.name
                            ), newValue = it
                        )
                    },
                    isIconNeeded = mutableStateOf(true),
                    icon = Icons.Rounded.Home
                )
            )
        }
    }

    fun <T> changeSettingPreferenceValue(
        preferenceKey: Preferences.Key<T>, newValue: T, onCompletion: () -> Unit = {}
    ) {
        viewModelScope.launch {
            preferencesRepository.changePreferenceValue(preferenceKey, newValue)
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    fun currInitialRoute(init: (String) -> Unit) {
        viewModelScope.launch {
            (preferencesRepository.readPreferenceValue(stringPreferencesKey(AppPreferenceType.INITIAL_ROUTE.name))
                ?: Navigation.Root.HomeScreen.toString()).let {
                init(it)
            }
        }
    }

    fun sampleLinks(localUriHandler: UriHandler): List<LinkComponentParam> {
        return listOf(
            LinkComponentParam(
                link = Link(
                    title = "This Could Be A Dream - YouTube Music",
                    host = "music.youtube.com",
                    imgURL = "https://lh3.googleusercontent.com/KMdNxgppeQ_CEAv3mcwYde9s6ehw-r9MWnE4wC2T0Yhax1aOwYvfRLfHCbLBbW-UVQQEdYniiXThgso",
                    url = "https://music.youtube.com/watch?v=DbiB1AtCA9k",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null
                ),
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://music.youtube.com/watch?v=DbiB1AtCA9k")
                },
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                tags = listOf(Tag(name = "TGWCT")),
                onTagClick = {}, showPath = false, onFolderClick = {}),
            LinkComponentParam(
                link = Link(
                    title = "Red Dead Redemption 2 - Rockstar Games",
                    host = "rockstargames.com",
                    imgURL = "https://media-rockstargames-com.akamaized.net/rockstargames-newsite/img/global/games/fob/640/reddeadredemption2.jpg",
                    url = "https://www.rockstargames.com/reddeadredemption2",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "RDR2 is the epic tale of outlaw Arthur Morgan and the infamous Van der Linde gang, on the run across America at the dawn of the modern age.",
                    idOfLinkedFolder = null
                ),
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://www.rockstargames.com/reddeadredemption2")
                },
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                tags = listOf(Tag(name = "oh arthur")),
                onTagClick = {}, showPath = false, onFolderClick = {}),
            LinkComponentParam(
                link = Link(
                    title = "A Plague Tale: Requiem | Download and Buy Today - Epic Games Store",
                    host = "store.epicgames.com",
                    imgURL = listOf(
                        "https://pbs.twimg.com/media/FUPM2TrWYAAQsXm?format=jpg",
                        "https://pbs.twimg.com/media/FLJx9epWYAADM0O?format=jpg",
                        "https://pbs.twimg.com/media/FAdLIY8WUAEgLRM?format=jpg",
                        "https://pbs.twimg.com/media/ETUI-RDWsAE2UYR?format=jpg",
                        "https://pbs.twimg.com/media/ET9J7vTWsAYVtvG?format=jpg",
                        "https://pbs.twimg.com/media/GRo2CKkWUAEsdEl?format=jpg",
                        "https://pbs.twimg.com/media/FezZxQYWQAQ4K3f?format=jpg",
                        "https://pbs.twimg.com/media/FezaHWkX0AIWvvU?format=jpg",
                        "https://i.redd.it/qoa6gk4ii8571.jpg",
                        "https://i.redd.it/8psapajhi8571.jpg"
                    ).random(),
                    url = "https://store.epicgames.com/en-US/p/a-plague-tale-requiem",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "The plague ravages the Kingdom of France. Amicia and her younger brother Hugo are pursued by the Inquisition through villages devastated by the disease.",
                    idOfLinkedFolder = null
                ),
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://store.epicgames.com/en-US/p/a-plague-tale-requiem")
                },
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                tags = null,
                onTagClick = {}, showPath = false, onFolderClick = {}),
            LinkComponentParam(
                link = Link(
                    title = "Shadow of the Tomb Raider",
                    imgURL = "https://images.ctfassets.net/x77ixfmkpoiv/4UnPNfdN8Yq2aZvOhIdBx9/1b641d296ebb37bfa3eca8873c25a321/SOTTR_Product_Image.jpg",
                    url = "https://www.tombraider.com/products/games/shadow-of-the-tomb-raider",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "As Lara Croft races to save the world from a Maya apocalypse, she must become the Tomb Raider she is destined to be.",
                    idOfLinkedFolder = null
                ),
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://www.tombraider.com/products/games/shadow-of-the-tomb-raider")
                },
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                tags = null,
                onTagClick = {}, showPath = false, onFolderClick = {}),
            LinkComponentParam(
                link = Link(
                    title = "Nas | Spotify",
                    host = "open.spotify.com",
                    imgURL = "https://cdn.prod.website-files.com/673de86e5b9b97bfffe3e0e4/67563ebf6f96ce8ad4d79ae4_Nas-Website.png",
                    url = "https://open.spotify.com/artist/20qISvAhX20dpIbOOzGK3q",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "he's da man",
                    idOfLinkedFolder = null
                ),
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://open.spotify.com/artist/20qISvAhX20dpIbOOzGK3q")
                },
                tags = listOf(Tag(name = "half man, half amazing.")),
                onTagClick = {}, showPath = false, onFolderClick = {}),
            LinkComponentParam(
                link = Link(
                    title = "Listen Gentle - YouTube Music",
                    host = "music.youtube.com",
                    imgURL = "https://lh3.googleusercontent.com/hloaKrX1jN1EfSLOUA11tgHZ3faSc5QFHNbMuB9bO-QTAdQRl-1oMZEXNQxOlk-p_sWBlf9Dd-4cal14",
                    url = "https://music.youtube.com/watch?v=Q5jl_fmMd8M",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "listen to this RN!!! If you like this (you will), you'll love the album, also check out McKinley's previous work such as Beautiful Paradise Jazz.",
                    idOfLinkedFolder = null
                ),
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://music.youtube.com/watch?v=Q5jl_fmMd8M")
                },
                tags = null,
                onTagClick = {}, showPath = false, onFolderClick = {}),
            LinkComponentParam(
                link = Link(
                    title = "Hacker (small type)",
                    host = "twitter.com",
                    imgURL = "https://pbs.twimg.com/media/GT7RIrWWwAAjZzg.jpg",
                    url = "https://twitter.com/CatWorkers/status/1819121250226127061",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null
                ),
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://twitter.com/CatWorkers/status/1819121250226127061")
                },
                tags = listOf(Tag(name = "\uD83D\uDE97")),
                onTagClick = {}, showPath = false, onFolderClick = {}),
            LinkComponentParam(
                link = Link(
                    title = "Nas - You're da Man (from Made You Look: God's Son Live)",
                    host = "youtube.com",
                    imgURL = "https://i.ytimg.com/vi/3vlqI5TPVjQ/maxresdefault.jpg",
                    url = "https://www.youtube.com/watch?v=3vlqI5TPVjQ",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null,
                    mediaType = MediaType.VIDEO
                ),
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://www.youtube.com/watch?v=3vlqI5TPVjQ")
                },
                tags = null,
                onTagClick = {}, showPath = false, onFolderClick = {}),
            LinkComponentParam(
                link = Link(
                    title = "Clipse, Nas, Pusha T, Malice - Let God Sort Em Out/Chandeliers - YouTube Music",
                    host = "music.youtube.com",
                    imgURL = "https://i.ytimg.com/vi/qQH24C1Jrx0/maxresdefault.jpg",
                    url = "https://music.youtube.com/watch?v=78YNulckDng",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null,
                    mediaType = MediaType.IMAGE
                ),
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://music.youtube.com/watch?v=78YNulckDng")
                },
                tags = null,
                onTagClick = {}, showPath = false, onFolderClick = {}),
            LinkComponentParam(
                link = Link(
                    title = "Nas - Rare (Official Video)",
                    host = "youtube.com",
                    imgURL = "https://i.ytimg.com/vi/66OFYWBrg3o/maxresdefault.jpg",
                    url = "https://www.youtube.com/watch?v=66OFYWBrg3o",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null,
                    mediaType = MediaType.VIDEO
                ),
                onForceOpenInExternalBrowserClicked = { },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { },
                onMoreIconClick = { },
                onLinkClick = {
                    localUriHandler.openUri("https://www.youtube.com/watch?v=66OFYWBrg3o")
                },
                tags = listOf(Tag(name = "KD"), Tag(name = "Kings Disease")),
                onTagClick = {}, showPath = false, onFolderClick = {}),
        ).sortedBy {
            it.link.title
        }
    }

    val gridViewPref = listOf(
        LinkPref(
            onClick = {
                AppPreferences.showTitleInLinkGridView.value =
                    !AppPreferences.showTitleInLinkGridView.value
                changeSettingPreferenceValue(
                    preferenceKey = booleanPreferencesKey(AppPreferenceType.TITLE_VISIBILITY_FOR_NON_LIST_VIEWS.name),
                    newValue = AppPreferences.showTitleInLinkGridView.value
                )
            },
            title = Localization.Key.ShowTitle.getLocalizedString(),
            isSwitchChecked = { AppPreferences.showTitleInLinkGridView.value }),
        LinkPref(
            onClick = {
                AppPreferences.showNoteInLinkView.value =
                    !AppPreferences.showNoteInLinkView.value
                changeSettingPreferenceValue(
                    preferenceKey = booleanPreferencesKey(AppPreferenceType.NOTE_VISIBILITY_IN_LIST_VIEWS.name),
                    newValue = AppPreferences.showNoteInLinkView.value
                )
            },
            title = Localization.Key.ShowNote.getLocalizedString(),
            isSwitchChecked = { AppPreferences.showNoteInLinkView.value }),
        LinkPref(
            onClick = {
                AppPreferences.showHostInLinkListView.value =
                    !AppPreferences.showHostInLinkListView.value
                changeSettingPreferenceValue(
                    preferenceKey = booleanPreferencesKey(AppPreferenceType.BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS.name),
                    newValue = AppPreferences.showHostInLinkListView.value
                )
            },
            title = Localization.Key.ShowHostAddress.getLocalizedString(),
            isSwitchChecked = { AppPreferences.showHostInLinkListView.value }),
        LinkPref(
            onClick = {
                AppPreferences.showTagsInLinkView =
                    !AppPreferences.showTagsInLinkView
                changeSettingPreferenceValue(
                    preferenceKey = booleanPreferencesKey(AppPreferenceType.SHOW_TAGS_IN_LINK_VIEW.name),
                    newValue = AppPreferences.showTagsInLinkView
                )
            },
            title = "Show Tags",
            isSwitchChecked = { AppPreferences.showTagsInLinkView }),
        LinkPref(
            onClick = {
                AppPreferences.showDateInLinkView =
                    !AppPreferences.showDateInLinkView
                changeSettingPreferenceValue(
                    preferenceKey = booleanPreferencesKey(AppPreferenceType.SHOW_DATE_IN_LINK_VIEW.name),
                    newValue = AppPreferences.showDateInLinkView
                )
            },
            title = "Show Date",
            isSwitchChecked = { AppPreferences.showDateInLinkView }),
        LinkPref(
            onClick = {
                AppPreferences.enableFadedEdgeForNonListViews.value =
                    !AppPreferences.enableFadedEdgeForNonListViews.value
                changeSettingPreferenceValue(
                    preferenceKey = booleanPreferencesKey(AppPreferenceType.FADED_EDGE_VISIBILITY_FOR_NON_LIST_VIEWS.name),
                    newValue = AppPreferences.enableFadedEdgeForNonListViews.value
                )
            },
            title = Localization.Key.ShowBottomFadedEdge.getLocalizedString(),
            isSwitchChecked = { AppPreferences.enableFadedEdgeForNonListViews.value }),
        LinkPref(
            onClick = {
                AppPreferences.showMenuOnGridLinkClick = !AppPreferences.showMenuOnGridLinkClick
                changeSettingPreferenceValue(
                    preferenceKey = booleanPreferencesKey(AppPreferenceType.SHOW_MENU_ON_GRID_LINK_CLICK.name),
                    newValue = AppPreferences.showMenuOnGridLinkClick
                )
            },
            title = "Click to Open Menu",
            isSwitchChecked = { AppPreferences.showMenuOnGridLinkClick }),
    )

    private val allIconCodes = AppIconCode.entries.map { it.name }

    fun onIconChange(newIconCode: String, onCompletion: () -> Unit) {
        AppPreferences.selectedAppIcon = newIconCode
        nativeUtils.onIconChange(
            allIconCodes = allIconCodes, newIconCode = newIconCode, onCompletion = {
                viewModelScope.launch {
                    preferencesRepository.changePreferenceValue(
                        preferenceKey = stringPreferencesKey(
                            AppPreferenceType.SELECTED_APP_ICON.name
                        ), newValue = newIconCode
                    )
                }.invokeOnCompletion {
                    onCompletion()
                }
            })
    }

    val onboardingSlides: List<OnboardingSlide> = listOf(OnboardingSlide {
        Slide1()
    }, OnboardingSlide {
        Slide2()
    }, OnboardingSlide {
        Slide3()
    }, OnboardingSlide {
        Slide4()
    })

}