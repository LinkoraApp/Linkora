package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import linkora.composeapp.generated.resources.Res

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutLibrariesScreen() {
    val localizedStrings = LocalizedStrings.current
    val libraries by produceLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }
    SettingsSectionScaffold(
        topAppBarText = localizedStrings.Libraries,
    ) { paddingValues, topAppBarScrollBehaviour ->
        LibrariesContainer(
            libraries,
            modifier =
                Modifier.fillMaxSize()
                    .addEdgeToEdgeScaffoldPadding(paddingValues)
                    .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
        )
    }
}
