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
                rawStr = "Selected >{0} links",
                actuals = arrayOf("9"),
                expected = "Selected 9 links"
            ),
            LocalizedTestItem(
                rawStr = "Create A New Folder In >{0}",
                actuals = arrayOf("ParentFolder"),
                expected = "Create A New Folder In ParentFolder"
            ),
            LocalizedTestItem(
                rawStr = "The folder >{0} has been successfully created.",
                actuals = arrayOf("ChildFolder"),
                expected = "The folder ChildFolder has been successfully created."
            ),
            LocalizedTestItem(
                rawStr = "Downloaded Language Strings for the >{0}.",
                actuals = arrayOf("LanguageTemp"),
                expected = "Downloaded Language Strings for the LanguageTemp."
            ),
            LocalizedTestItem(
                rawStr = ">{0} of >{1} links refreshed.",
                actuals = arrayOf("94", "100"),
                expected = "94 of 100 links refreshed."
            ),
            LocalizedTestItem(
                rawStr = ">{0} files are not supported for importing, pick valid >{1} file.",
                actuals = arrayOf("JSON", "HTML"),
                expected = "JSON files are not supported for importing, pick valid HTML file."
            ),
            LocalizedTestItem(
                rawStr = ">{0}/>{1} strings localized",
                actuals = arrayOf("20", "25"),
                expected = "20/25 strings localized"
            ),
            LocalizedTestItem(
                rawStr = ">{0}>{1}",
                actuals = arrayOf("a", "b"),
                expected = "ab"
            ),
            LocalizedTestItem(
                rawStr = "No placeholders here",
                actuals = arrayOf("ignored"),
                expected = "No placeholders here"
            ),
            LocalizedTestItem(
                rawStr = ">{0} and >{1}",
                actuals = arrayOf("first"),
                expected = "first and >{1}"
            ),
            LocalizedTestItem(
                rawStr = ">{0} and >{1} and >{2}",
                actuals = arrayOf("first", "second"),
                expected = "first and second and >{2}"
            ),
            LocalizedTestItem(
                rawStr = "",
                actuals = arrayOf("x"),
                expected = ""
            ),
            LocalizedTestItem(
                rawStr = ">{0}",
                actuals = arrayOf(),
                expected = ">{0}"
            ),
            LocalizedTestItem(
                rawStr = ">{1} and >{0}",
                actuals = arrayOf("a", "b"),
                expected = "b and a"
            ),
            LocalizedTestItem(
                rawStr = ">{1} of >{0} links refreshed.",
                actuals = arrayOf("94", "100"),
                expected = "100 of 94 links refreshed."
            ),
            LocalizedTestItem(
                rawStr = ">{0} and >{0}",
                actuals = arrayOf("x", "y"),
                expected = "x and x"
            ),
            LocalizedTestItem(
                rawStr = ">{0} and >{0}",
                actuals = arrayOf("x"),
                expected = "x and x"
            ),
            LocalizedTestItem(
                rawStr = ">{2} >{0} >{1}",
                actuals = arrayOf("a", "b", "c"),
                expected = "c a b"
            ),
            LocalizedTestItem(
                rawStr = ">{10}",
                actuals = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "ten"),
                expected = "ten"
            ),
            LocalizedTestItem(
                rawStr = ">{10}  >{11}",
                actuals = arrayOf("1"),
                expected = ">{10}  >{11}"
            ),
            LocalizedTestItem(
                rawStr = ">{0",
                actuals = arrayOf("a"),
                expected = ">{0"
            ),
            LocalizedTestItem(
                rawStr = ">",
                actuals = arrayOf("a"),
                expected = ">"
            ),
            LocalizedTestItem(
                rawStr = ">{}",
                actuals = arrayOf("a"),
                expected = ">{}"
            ),
            LocalizedTestItem(
                rawStr = ">{a}",
                actuals = arrayOf("a"),
                expected = ">{a}"
            ),
            LocalizedTestItem(
                rawStr = ">{-1}",
                actuals = arrayOf("a"),
                expected = ">{-1}"
            ),
            LocalizedTestItem(
                rawStr = ">{ 1 }",
                actuals = arrayOf("a", "b"),
                expected = ">{ 1 }"
            ),
            LocalizedTestItem(
                rawStr = ">{0}}>{1}",
                actuals = arrayOf("a", "b"),
                expected = "a}b"
            ),
            LocalizedTestItem(
                rawStr = "Result: >{0}",
                actuals = arrayOf(">{0}"),
                expected = "Result: >{0}"
            ),
            LocalizedTestItem(
                rawStr = ">{01}",
                actuals = arrayOf("a", "b"),
                expected = "b"
            ),
            LocalizedTestItem(
                rawStr = ">{+1}",
                actuals = arrayOf("a", "b"),
                expected = "b"
            ),
            LocalizedTestItem(
                rawStr = ">{1} 件のリンクのうち >{0} 件を更新しました。",
                actuals = arrayOf("94", "100"),
                expected = "100 件のリンクのうち 94 件を更新しました。"
            ),
        ).forEach { (rawStr, actuals, expected) ->
            assertEquals(
                expected,
                rawStr.replaceActual(*actuals)
            )
        }
    }
}
