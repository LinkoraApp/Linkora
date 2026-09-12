package com.sakethh.linkora

import com.sakethh.linkora.data.local.repository.LocalLinksRepoImpl
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.model.ScrapedLinkInfo
import com.sakethh.linkora.utils.replaceActual
import io.ktor.http.ContentType
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import java.net.ServerSocket
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UtilsTest {

    private var masterServer: ApplicationEngine? = null
    private var masterPort: Int = 0

    @BeforeTest
    fun setup() {
        if (masterServer == null) {
            // Find a random available port
            masterPort = ServerSocket(0).use { it.localPort }

            masterServer = embeddedServer(CIO, port = masterPort) {
                routing {
                    get("/mockk") {
                        call.respondText(
                            """
                            <html>
                                <head>
                                    <meta property="og:title" content="MockK">
                                    <meta property="og:image" content="https://mockk.io/doc/stats-shared.png">
                                </head>
                                <body></body>
                            </html>
                            """.trimIndent(),
                            ContentType.Text.Html,
                        )
                    }
                    get("/kotlinlang") {
                        call.respondText(
                            """
                            <html>
                                <head>
                                    <meta property="og:title" content="How to best use Mockito in Kotlin?">
                                    <meta property="og:image" content="https://us1.discourse-cdn.com/flex019/uploads/kotlinlang/original/2X/2/224964e73572d20c3aa9d68b4c14ae5d11749202.png">
                                </head>
                                <body></body>
                            </html>
                            """.trimIndent(),
                            ContentType.Text.Html,
                        )
                    }
                    get("/tidal") {
                        call.respondText(
                            """
                            <html>
                                <head>
                                    <meta property="og:title" content="Nujabes">
                                    <meta property="og:image" content="https://resources.tidal.com/images/f899da08/195b/432d/b17c/207ffb009380/750x750.jpg">
                                </head>
                                <body></body>
                            </html>
                            """.trimIndent(),
                            ContentType.Text.Html,
                        )
                    }
                    get("/genius") {
                        call.respondText(
                            """
                            <html>
                                <head>
                                    <meta property="og:title" content="Modal Soul by Nujabes">
                                    <meta property="og:image" content="https://images.genius.com/7f62b49d9becfdf686ce707a1e77a841.873x873x1.png">
                                </head>
                                <body></body>
                            </html>
                            """.trimIndent(),
                            ContentType.Text.Html,
                        )
                    }
                }
            }.start(wait = false).engine
        }
    }

    @Test
    fun `html parsing should return valid meta info`() = runTest {
        val retrievalJobs = mutableListOf<Job>()
        val appPreferences = AppPreferences()

        listOf(
            "http://127.0.0.1:$masterPort/mockk" to ScrapedLinkInfo(
                title = "MockK",
                imgUrl = "https://mockk.io/doc/stats-shared.png",
            ),
            "http://127.0.0.1:$masterPort/kotlinlang" to ScrapedLinkInfo(
                title = "How to best use Mockito in Kotlin?",
                imgUrl = "https://us1.discourse-cdn.com/flex019/uploads/kotlinlang/original/2X/2/224964e73572d20c3aa9d68b4c14ae5d11749202.png",
            ),
            "http://127.0.0.1:$masterPort/tidal" to ScrapedLinkInfo(
                title = "Nujabes",
                imgUrl = "https://resources.tidal.com/images/f899da08/195b/432d/b17c/207ffb009380/750x750.jpg",
            ),
            "http://127.0.0.1:$masterPort/genius" to ScrapedLinkInfo(
                title = "Modal Soul by Nujabes",
                imgUrl = "https://images.genius.com/7f62b49d9becfdf686ce707a1e77a841.873x873x1.png",
            ),
        ).forEach { (linkUrl, expectedInfo) ->
            retrievalJobs.add(
                launch {
                    val scrapedInfo = LocalLinksRepoImpl(
                        linksDao = mockk(),
                        primaryUserAgent = { appPreferences.primaryJsoupUserAgent },
                        proxyUrl = { appPreferences.proxyUrl },
                        standardClient = mockk(),
                        remoteLinksRepo = mockk(),
                        foldersDao = mockk(),
                        pendingSyncQueueRepo = mockk(),
                        preferencesRepository = mockk(),
                        tagsDao = mockk(),
                        webCapture = mockk(),
                    ).scrapeLinkData(
                        linkUrl = linkUrl,
                        userAgent = "Twitterbot/1.0",
                    )

                    assertTrue(
                        scrapedInfo.title.contains(expectedInfo.title, ignoreCase = true),
                        "Expected title to contain '${expectedInfo.title}', but got '${scrapedInfo.title}'",
                    )
                    assertEquals(expectedInfo.imgUrl, scrapedInfo.imgUrl)
                },
            )
        }

        retrievalJobs.joinAll()
    }

    data class LocalizedTestItem(
        val rawStr: String,
        val actuals: Array<String>,
        val expected: String,
    )

    @Test
    fun `replaceActual substitutes placeholders in order`() {
        listOf(
            LocalizedTestItem(
                rawStr = "Selected >{} links",
                actuals = arrayOf("9"),
                expected = "Selected 9 links"
            ),
            LocalizedTestItem(
                rawStr = "Create A New Folder In >{}",
                actuals = arrayOf("ParentFolder"),
                expected = "Create A New Folder In ParentFolder"
            ),
            LocalizedTestItem(
                rawStr = "The folder >{} has been successfully created.",
                actuals = arrayOf("ChildFolder"),
                expected = "The folder ChildFolder has been successfully created."
            ),
            LocalizedTestItem(
                rawStr = "Downloaded Language Strings for the >{}.",
                actuals = arrayOf("LanguageTemp"),
                expected = "Downloaded Language Strings for the LanguageTemp."
            ),
            LocalizedTestItem(
                rawStr = ">{} of >{} links refreshed.",
                actuals = arrayOf("94", "100"),
                expected = "94 of 100 links refreshed."
            ),
            LocalizedTestItem(
                rawStr = ">{} files are not supported for importing, pick valid >{} file.",
                actuals = arrayOf("JSON", "HTML"),
                expected = "JSON files are not supported for importing, pick valid HTML file."
            ),
            LocalizedTestItem(
                rawStr = ">{}/>{} strings localized",
                actuals = arrayOf("20", "25"),
                expected = "20/25 strings localized"
            ),
            LocalizedTestItem(
                rawStr = ">{}/>{} strings localized",
                actuals = arrayOf("20", "25"),
                expected = "20/25 strings localized"
            ),
            LocalizedTestItem(
                rawStr = ">{}>{}",
                actuals = arrayOf("a", "b"),
                expected = "ab"
            ),
            LocalizedTestItem(
                rawStr = "No placeholders here",
                actuals = arrayOf("ignored"),
                expected = "No placeholders here"
            ),
            LocalizedTestItem(
                rawStr = ">{} and >{}",
                actuals = arrayOf("first"),
                expected = "first and >{}"
            ),
            LocalizedTestItem(
                rawStr = ">{} and >{} and >{}",
                actuals = arrayOf("first", "second"),
                expected = "first and second and >{}"
            ),
            LocalizedTestItem(
                rawStr = "",
                actuals = arrayOf("x"),
                expected = ""
            ),
            LocalizedTestItem(
                rawStr = ">{}",
                actuals = arrayOf(),
                expected = ">{}"
            )
        ).forEach { (rawStr, actuals, expected) ->
            assertEquals(
                rawStr.replaceActual(*actuals),
                expected
            )
        }
    }
}
