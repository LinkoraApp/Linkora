package com.sakethh.linkora.ui.screens.settings.specific.advanced.site_specific_user_agent

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.sakethh.linkora.LocalizedStrings
import com.sakethh.linkora.ui.commonComposables.pulsateEffect
import com.sakethh.linkora.ui.screens.settings.composables.SpecificScreenScaffold
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteSpecificUserAgentSettingsScreen(navController: NavController) {
    val systemUiController = rememberSystemUiController()
    val bottomAppbarContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(
        BottomAppBarDefaults.ContainerElevation
    )
    val siteSpecificUserAgentScreenVM: SiteSpecificUserAgentScreenVM = hiltViewModel()
    val allSiteSpecificUserAgents =
        siteSpecificUserAgentScreenVM.allSiteSpecificUserAgents.collectAsStateWithLifecycle().value
    val sheetStateForAddingANewSiteSpecificUserAgent =
        rememberModalBottomSheetState(skipPartiallyExpanded = true)
    systemUiController.setNavigationBarColor(
        bottomAppbarContainerColor
    )
    val shouldBtmSheetForAddingANewRuleBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val coroutineScope = rememberCoroutineScope()
    SpecificScreenScaffold(topAppBarText = "Site-Specific User Agent Settings",
        navController = navController,
        bottomBar = {
            BottomAppBar {
                Button(modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp), onClick = {
                    shouldBtmSheetForAddingANewRuleBeVisible.value = true
                    coroutineScope.launch {
                        sheetStateForAddingANewSiteSpecificUserAgent.show()
                    }
                }) {
                    Text(
                        "Add a new Site-Specific User Agent",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
                .navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            item {
                Spacer(Modifier)
            }

            items(items = allSiteSpecificUserAgents, key = {
                it.id
            }) {
                SiteSpecificUserAgentItem(
                    domain = it.domain,
                    userAgent = it.userAgent,
                    onDeleteClick = {
                        siteSpecificUserAgentScreenVM.deleteASiteSpecificUserAgent(it.domain)
                    },
                    onSaveClick = { newUserAgent ->
                        siteSpecificUserAgentScreenVM.updateASpecificUserAgent(
                            newUserAgent = newUserAgent,
                            domain = it.domain
                        )
                    }
                )
            }
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
        AddANewRuleBottomSheet(
            sheetStateForAddingANewSiteSpecificUserAgent,
            shouldBtmSheetForAddingANewRuleBeVisible,
            siteSpecificUserAgentScreenVM
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddANewRuleBottomSheet(
    sheetState: SheetState,
    shouldBeVisible: MutableState<Boolean>,
    siteSpecificUserAgentScreenVM: SiteSpecificUserAgentScreenVM
) {
    val coroutineScope = rememberCoroutineScope()
    if (shouldBeVisible.value) {
        val domainValue = rememberSaveable {
            mutableStateOf("")
        }
        val userAgentValue = rememberSaveable {
            mutableStateOf("")
        }
        val context = LocalContext.current
        ModalBottomSheet(onDismissRequest = {
            coroutineScope.launch {
                sheetState.hide()
            }.invokeOnCompletion {
                shouldBeVisible.value = false
            }
        }, sheetState = sheetState) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Add a new Site-Specific User Agent",
                    color = AlertDialogDefaults.titleContentColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(
                        start = 20.dp, end = 20.dp
                    ),
                    lineHeight = 28.sp
                )
                OutlinedTextField(modifier = Modifier
                    .padding(
                        start = 20.dp, end = 20.dp, top = 15.dp
                    )
                    .fillMaxWidth(),
                    label = {
                        Text(
                            text = "Domain",
                            color = AlertDialogDefaults.textContentColor,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge,
                    singleLine = true,
                    value = domainValue.value,
                    onValueChange = {
                        domainValue.value = it.replace(" ", "")
                    },
                    supportingText = {
                        Text(
                            text = buildAnnotatedString {
                                append("Only the domain should be saved.\nFor example, save ")
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                    append("https://www.github.com/sakethpathike/Linkora")
                                }
                                append(" as ")
                                withStyle(
                                    SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    append("github.com")
                                }
                                append(".")
                            },
                            color = AlertDialogDefaults.textContentColor,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp,
                            lineHeight = 20.sp
                        )
                    })

                OutlinedTextField(modifier = Modifier
                    .padding(
                        start = 20.dp, end = 20.dp, top = 5.dp
                    )
                    .fillMaxWidth(),
                    label = {
                        Text(
                            text = "User Agent",
                            color = AlertDialogDefaults.textContentColor,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 12.sp
                        )
                    },
                    textStyle = MaterialTheme.typography.titleSmall,
                    singleLine = true,
                    value = userAgentValue.value,
                    onValueChange = {
                        userAgentValue.value = it
                    })
                OutlinedButton(colors = ButtonDefaults.outlinedButtonColors(),
                    border = BorderStroke(
                        width = 1.dp, color = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier
                        .padding(
                            end = 20.dp, top = 10.dp, start = 20.dp
                        )
                        .fillMaxWidth()
                        .pulsateEffect(),
                    onClick = {
                        coroutineScope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            shouldBeVisible.value = false
                        }
                    }) {
                    Text(
                        text = LocalizedStrings.cancel.value,
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }
                Button(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .padding(
                            end = 20.dp, top = 10.dp, start = 20.dp
                        )
                        .fillMaxWidth()
                        .pulsateEffect(),
                    onClick = {
                        if (Regex("""^(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z0-9][a-z0-9-]{0,61}[a-z0-9]$""").matches(
                                domainValue.value
                            )
                        ) {
                            siteSpecificUserAgentScreenVM.addANewSiteSpecificUserAgent(
                                domain = domainValue.value,
                                userAgent = userAgentValue.value
                            )
                        } else {
                            Toast.makeText(context, "invalid domain", Toast.LENGTH_SHORT).show()

                        }
                    }) {
                    Text(
                        text = LocalizedStrings.save.value,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.padding(bottom = 15.dp))
            }
        }
    }
}

@Composable
fun SiteSpecificUserAgentItem(
    domain: String,
    userAgent: String,
    onDeleteClick: () -> Unit,
    onSaveClick: (userAgent: String) -> Unit
) {
    val isUserAgentTextFieldReadOnly = rememberSaveable {
        mutableStateOf(true)
    }
    val userAgentValue = rememberSaveable(userAgent) {
        mutableStateOf(userAgent)
    }
    Card(
        border = BorderStroke(
            1.dp, contentColorFor(MaterialTheme.colorScheme.surface)
        ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 15.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier
                        .padding(
                            end = 15.dp
                        )
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(0.1f),
                            shape = RoundedCornerShape(5.dp)
                        )
                        .padding(5.dp)
                        .horizontalScroll(rememberScrollState()),
                    text = domain,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    textAlign = TextAlign.Start,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
            }
        }
        TextField(
            readOnly = isUserAgentTextFieldReadOnly.value,
            value = userAgentValue.value,
            onValueChange = {
                userAgentValue.value = it
            },
            textStyle = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .padding(start = 15.dp, end = 15.dp)
                .fillMaxWidth()
        )
        Spacer(Modifier.height(15.dp))
        if (isUserAgentTextFieldReadOnly.value) {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp),
                onClick = {
                    onSaveClick(userAgentValue.value)
                    isUserAgentTextFieldReadOnly.value = true
                },
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    disabledContentColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            ) {
                Text("Save", style = MaterialTheme.typography.titleSmall)
            }
        } else {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 15.dp, end = 15.dp),
                onClick = {
                    onDeleteClick()
                },
                colors = ButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    disabledContentColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                )
            ) {
                Text("Delete", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(5.dp))
                Button(
                    onClick = {
                        isUserAgentTextFieldReadOnly.value = false
                    }, modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 15.dp, end = 15.dp)
                ) {
                    Text("Edit", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        Spacer(Modifier.height(15.dp))
    }
}