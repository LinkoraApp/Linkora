package com.sakethh.linkora.ui.screens.settings.section.general.reminders

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.screens.settings.section.data.components.ToggleButton
import com.sakethh.linkora.ui.utils.linkoraLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersSettingsScreen() {
    val navController = LocalNavController.current
    val links = listOf(
        Link(
            linkType = LinkType.HISTORY_LINK,
            title = "Nas - Life is Like a Dice Game",
            url = "https://hiphophero.com/nas-life-is-like-a-dice-game",
            imgURL = "https://hiphophero.com/static/uploads/5/2021/07/Nas-finally-releases-completed-version-of-Life-Is-Like-a-Dice-Game.jpg",
            note = "Classic unreleased track officially dropped.",
            idOfLinkedFolder = null
        ), Link(
            linkType = LinkType.HISTORY_LINK,
            title = "2Pac Interview 1996",
            url = "https://raparchives.com/2pac-1996-interview",
            imgURL = "https://cdn-images.dzcdn.net/images/artist/7be1be44b68b21641c2511e1034bc4c9/1900x1900-000000-80-0-0.jpg",
            note = "Rare insights into Pac's thoughts before his final album.",
            idOfLinkedFolder = null
        ), Link(
            linkType = LinkType.HISTORY_LINK,
            title = "Biggie’s Last Freestyle",
            url = "https://classicrap.com/biggie-last-freestyle",
            imgURL = "https://hips.hearstapps.com/hmg-prod/images/biggie_smalls_photo_by_clarence_davis_new_york_daily_news_archive_getty_97348258.jpg?crop=1xw:1.0xh;center,top&resize=640:*",
            note = "Raw freestyle from a studio session in 1997.",
            idOfLinkedFolder = null
        ), Link(
            linkType = LinkType.HISTORY_LINK,
            title = "Wu-Tang Clan - 36 Chambers Behind the Scenes",
            url = "https://rapdocs.net/wu-tang-36chambers",
            imgURL = "https://upload.wikimedia.org/wikipedia/en/5/53/Wu-TangClanEntertheWu-Tangalbumcover.jpg",
            note = "Documentary on the making of a legendary debut.",
            idOfLinkedFolder = null
        ), Link(
            linkType = LinkType.HISTORY_LINK,
            title = "Kendrick Lamar - Section.80 Retrospective",
            url = "https://modernhiphop.com/kdot-section80",
            imgURL = "https://wp.theringer.com/wp-content/uploads/2022/08/lamar_section80.jpeg",
            note = "Exploring themes and influence of Kendrick's early work.",
            idOfLinkedFolder = null
        )
    )
    val reminders = listOf(
        Reminder(
            id = 1L,
            linkId = 101L,
            title = "Watch Nas Dice Game Release",
            description = "Listen to Nas’s completed track and read the article.",
            scheduleInfo = "2025-05-22T18:00:00+05:30"
        ), Reminder(
            id = 2L,
            linkId = 102L,
            title = "Read 2Pac's 1996 Interview",
            description = "Important points for the rap culture timeline project.",
            scheduleInfo = "2025-05-23T09:30:00+05:30"
        ), Reminder(
            id = 3L,
            linkId = 103L,
            title = "Play Biggie’s Last Freestyle",
            description = "Use in music analysis presentation.",
            scheduleInfo = "2025-05-24T21:00:00+05:30"
        ), Reminder(
            id = 4L,
            linkId = 104L,
            title = "Watch Wu-Tang Documentary",
            description = "Behind the scenes footage of Enter the Wu-Tang.",
            scheduleInfo = "2025-05-25T16:45:00+05:30"
        ), Reminder(
            id = 5L,
            linkId = 105L,
            title = "Analyze Section.80 Themes",
            description = "Prepare points about social commentary for essay.",
            scheduleInfo = "2025-05-26T11:15:00+05:30"
        )
    )

    SettingsSectionScaffold(
        topAppBarText = "Reminders",
        navController = navController,
        actions = {},
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection).fillMaxSize()
        ) {
            item {
                Text(
                    text = "Upcoming",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(
                        top = 15.dp, start = 15.dp, end = 15.dp, bottom = 7.5.dp
                    ),
                )
            }
            items(5) {
                ReminderComponent(
                    link = links[it],
                    reminder = reminders[it],
                    onEditClick = { },
                    onDeleteClick = { },
                    onUrlClick = {})
            }
        }
    }
}