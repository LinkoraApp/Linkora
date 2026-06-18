package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.SettingSectionComponent
import com.sakethh.linkora.ui.screens.settings.SettingSectionComponentParam
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.openUriOrNotify
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.launch
import linkora.composeapp.generated.resources.LOLCATpl_logo
import linkora.composeapp.generated.resources.Res
import linkora.composeapp.generated.resources.mondstern_logo
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcknowledgementScreen() {
    val navController = LocalNavController.current
    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.AcknowledgementScreen.toString(),
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().addEdgeToEdgeScaffoldPadding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(15.dp))
                AcknowledgeComponent(
                    btnText = Localization.Key.MondsternOnDiscord.rememberLocalizedString(),
                    image = Res.drawable.mondstern_logo,
                    btnRedirectUrl = "https://pixelfed.social/mondstern",
                    text = Localization.Key.MondsternAck.rememberLocalizedString(),
                )
                Spacer(modifier = Modifier.height(15.dp))
                AcknowledgeComponent(
                    btnText = Localization.Key.LOLCATplOnDiscord.rememberLocalizedString(),
                    image = Res.drawable.LOLCATpl_logo,
                    btnRedirectUrl = "https://discord.com/users/494115165927637007",
                    text = Localization.Key.LOLCATplAck.rememberLocalizedString()
                )
            }
            item {
                SettingSectionComponent(
                    SettingSectionComponentParam(
                        onClick = {
                            navController.navigate(Navigation.Settings.AboutLibraries)
                        },
                        sectionIcon = Icons.Default.Info,
                        sectionTitle = Localization.Key.AboutLibraries.rememberLocalizedString()
                    )
                )
            }
            item {
                Spacer(modifier = Modifier)
            }
        }
    }
}

@Composable
private fun AcknowledgeComponent(
    image: DrawableResource,
    text: String,
    btnRedirectUrl: String,
    btnText: String,
) {
    val localUriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp)
            .clip(RoundedCornerShape(15.dp)).border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(0.5f),
                shape = RoundedCornerShape(15.dp)
            )
    ) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.padding(start = 15.dp, top = 15.dp, bottom = 10.dp)
                .clip(RoundedCornerShape(15.dp)).size(65.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = 15.dp, end = 15.dp),
            softWrap = true
        )
        FilledTonalButton(
            onClick = {
                coroutineScope.launch {
                    localUriHandler.openUriOrNotify(btnRedirectUrl)
                }
            },
            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                .padding(start = 15.dp, top = 10.dp, end = 15.dp)
                .pressScaleEffect()
        ) {
            Text(
                text = btnText,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
    }
}