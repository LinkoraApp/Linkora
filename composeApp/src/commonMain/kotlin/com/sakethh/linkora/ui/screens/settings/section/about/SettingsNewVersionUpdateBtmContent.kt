package com.sakethh.linkora.ui.screens.settings.section.about

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.bottomNavPaddingAcrossPlatforms
import com.sakethh.linkora.utils.openUriOrNotify
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewVersionUpdateBtmContent(
    modalBtmSheetState: SheetState,
    shouldBtmModalSheetBeVisible: MutableState<Boolean>,
    latestVersion: String, urlOfLatestReleasePage: String, tagName: String
) {
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.fillMaxWidth().wrapContentHeight()
    ) {
        item {
            Text(
                text = Localization.Key.NewUpdateIsAvailable.rememberLocalizedString(),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp,
                textAlign = TextAlign.Start,
                lineHeight = 32.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp)
            )
        }
        item {
            VersionCardForBtmSheetContent(
                title = Localization.Key.CurrentVersion.rememberLocalizedString(),
                value = Constants.APP_VERSION_NAME
            )
        }
        item {
            VersionCardForBtmSheetContent(
                title = Localization.Key.LatestVersionAvailableDesc.rememberLocalizedString()
                    .replace(LinkoraPlaceHolder.First.value, tagName.substringBefore("-")),
                value = latestVersion
            )
        }
        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
        item {
            Button(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).padding(start = 20.dp, end = 20.dp).fillMaxWidth()
                    .pressScaleEffect(), onClick = {
                    coroutineScope.launch {
                        if (modalBtmSheetState.isVisible) {
                            modalBtmSheetState.hide()
                        }
                    }.invokeOnCompletion {
                        shouldBtmModalSheetBeVisible.value = false
                    }
                    coroutineScope.launch {
                        uriHandler.openUriOrNotify(urlOfLatestReleasePage)
                    }
                }) {
                Text(
                    text = Localization.Key.RedirectToLatestReleasePage.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Spacer(modifier = Modifier.bottomNavPaddingAcrossPlatforms())
        }
    }
}

@Composable
private fun VersionCardForBtmSheetContent(title: String, value: String) {
    Card(
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp).fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontSize = 16.sp,
            textAlign = TextAlign.Start,
            lineHeight = 18.sp,
            modifier = Modifier.padding(20.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = 20.dp, bottom = 20.dp, end = 20.dp)
        )
    }
}