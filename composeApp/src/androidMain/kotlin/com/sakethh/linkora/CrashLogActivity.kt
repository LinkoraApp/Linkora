package com.sakethh.linkora

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.sakethh.linkora.ui.domain.Font
import com.sakethh.linkora.ui.theme.DarkColors
import com.sakethh.linkora.ui.theme.LightColors
import com.sakethh.linkora.ui.theme.LinkoraTheme
import com.sakethh.linkora.ui.theme.googleSansFlexFontFamily
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.AndroidConstants
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding

class CrashLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val localClipboardManager = LocalClipboardManager.current
            val crashLogs = retain {
                intent?.getStringExtra(AndroidConstants.CRASH_LOG_KEY)
                    ?: "Nothing found"
            }
            val localContext = LocalContext.current
            LinkoraTheme(
                colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
                preferredFont = Font.GOOGLE_SANS_FLEX
            ) {
                Surface {
                    Scaffold(bottomBar = {
                        Column(
                            modifier = Modifier
                                .background(BottomAppBarDefaults.containerColor)
                                .navigationBarsPadding()
                                .fillMaxWidth()
                                .padding(15.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    localClipboardManager.setText(AnnotatedString(crashLogs))
                                    finishAndRemoveTask()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScaleEffect()
                            ) {
                                Text(
                                    fontFamily = googleSansFlexFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    text = "Copy Crash Log"
                                )
                            }
                            Button(
                                onClick = {
                                    val subject =
                                        Uri.encode("Linkora ${Constants.APP_VERSION_NAME} Crashed")
                                    val body = Uri.encode(crashLogs)

                                    val emailUri =
                                        "mailto:${AndroidConstants.LINKORA_MAIL_ADDR}?subject=$subject&body=$body".toUri()
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = emailUri
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf(AndroidConstants.LINKORA_MAIL_ADDR))
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }

                                    try {
                                        localContext.startActivity(emailIntent)
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(
                                            localContext,
                                            "No email app found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScaleEffect()
                            ) {
                                Text(
                                    fontFamily = googleSansFlexFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    text = "Report Crash"
                                )
                            }
                        }
                    }) { paddingValues ->
                        Column(
                            modifier = Modifier
                                .addEdgeToEdgeScaffoldPadding(paddingValues)
                                .fillMaxSize()
                                .padding(15.dp)
                                .verticalScroll(rememberScrollState())
                                .statusBarsPadding()
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                fontSize = 24.sp,
                                fontFamily = googleSansFlexFontFamily,
                                fontWeight = FontWeight.Bold,
                                text = "Linkora Crashed",
                                modifier = Modifier
                                    .padding(top = 25.dp)
                            )
                            Text(
                                fontSize = 16.sp,
                                fontFamily = googleSansFlexFontFamily,
                                fontWeight = FontWeight.Medium,
                                text = crashLogs,
                                softWrap = false,
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(bottom = 25.dp, top = 25.dp)
                                    .fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
