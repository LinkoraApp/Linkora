package com.sakethh.linkora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakethh.linkora.ui.domain.Font
import com.sakethh.linkora.ui.theme.DarkColors
import com.sakethh.linkora.ui.theme.LightColors
import com.sakethh.linkora.ui.theme.LinkoraTheme
import com.sakethh.linkora.ui.theme.googleSansFlexFontFamily
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.AndroidConstants
import com.sakethh.linkora.utils.Constants

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
            LinkoraTheme(
                colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
                preferredFont = Font.GOOGLE_SANS_FLEX
            ) {
                Surface {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(15.dp)
                            .verticalScroll(rememberScrollState())
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            text = crashLogs,
                            modifier = Modifier
                                .padding(bottom = 25.dp, top = 25.dp)
                                .fillMaxSize()
                        )
                        Button(onClick = {
                            localClipboardManager.setText(AnnotatedString(crashLogs))
                            finishAndRemoveTask()
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .pressScaleEffect()) {
                            Text(
                                fontFamily = googleSansFlexFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                text = "Copy Crash Log"
                            )
                        }
                    }
                }
            }
        }
    }
}