package com.sakethh.linkora.web_capture

import com.sakethh.linkora.JVMAndAndroidWebCapture
import io.ktor.http.ContentType
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.net.ServerSocket
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class WebCaptureIntegrationTest {

    private val tempFiles = mutableListOf<File>()
    private var masterServer: ApplicationEngine? = null
    private var masterPort: Int = 0

    @BeforeTest
    fun setup() {
        tempFiles.clear()

        if (masterServer == null) {
            masterPort = ServerSocket(0).use { it.localPort }
            masterServer = embeddedServer(CIO, port = masterPort) {
                routing {
                    get("/") {
                        val requestedDelay =
                            call.request.queryParameters["delay"]?.toLongOrNull() ?: 0L
                        val targetId = call.request.queryParameters["id"] ?: "Unknown"

                        if (requestedDelay > 0) {
                            delay(requestedDelay.milliseconds)
                        }
                        call.respondText(
                            "<html><body>Target $targetId</body></html>",
                            ContentType.Text.Html
                        )
                    }
                    get("/trap") {
                        val trapHtml =
                            "<html><body><img src=\"http://127.0.0.1:$masterPort/?delay=3000&id=trap_image\"></body></html>"
                        call.respondText(trapHtml, ContentType.Text.Html)
                    }
                }
            }.start(wait = false).engine
        }
    }

    @AfterTest
    fun cleanup() {
        tempFiles.forEach { file ->
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun createTempTestFile(): File {
        val file = File.createTempFile("linkora_test_capture_", ".html")
        tempFiles.add(file)
        return file
    }

    @Test
    fun loadsNativeLibrarySuccessfully() = runBlocking {
        val result = JVMAndAndroidWebCapture.init()
        assertTrue(result.isSuccess)
        assertEquals(result.getOrNull(), true)
    }

    @Test
    fun nukeAndCallFailsSafely(): Unit = runBlocking {
        JVMAndAndroidWebCapture.init()
        JVMAndAndroidWebCapture.nuke()

        val tempFile = createTempTestFile()

        assertFailsWith<IllegalStateException> {
            JVMAndAndroidWebCapture.saveHTMLPage(
                fileDescriptor = -1,
                filePath = tempFile.absolutePath,
                url = "http://localhost",
                userAgent = "LinkoraTest/1.0",
                timeout = 10000L,
                allowInsecureProtocol = true,
                ignoreDocErrors = true,
                useCss = false,
                embedFonts = false,
                embedImages = false,
                restrictJs = false,
                includeAudioElements = false,
                includeVideoElements = false,
                includeMetadata = false,
                logStuff = true
            )
        }
    }

    @Test
    fun writesValidHtmlToDisk() = runBlocking {
        JVMAndAndroidWebCapture.init()
        val tempFile = createTempTestFile()

        val result = JVMAndAndroidWebCapture.saveHTMLPage(
            fileDescriptor = -1,
            filePath = tempFile.absolutePath,
            url = "http://127.0.0.1:$masterPort/?id=FastResponse",
            userAgent = "LinkoraTest/1.0",
            timeout = 10000L,
            allowInsecureProtocol = true,
            ignoreDocErrors = true,
            useCss = false,
            embedFonts = false,
            embedImages = false,
            restrictJs = false,
            includeAudioElements = false,
            includeVideoElements = false,
            includeMetadata = false,
            logStuff = true
        )

        assertTrue(result)
        assertTrue(tempFile.exists())

        val fileContent = tempFile.readText()
        assertTrue(fileContent.contains("Target FastResponse"))
    }

    @Test
    fun cleanlyCancelsSlowRequest() = runBlocking {
        JVMAndAndroidWebCapture.init()
        val tempFile = createTempTestFile()

        val captureJob = launch {
            try {
                JVMAndAndroidWebCapture.saveHTMLPage(
                    fileDescriptor = -1,
                    filePath = tempFile.absolutePath,
                    url = "http://127.0.0.1:$masterPort/?delay=3000&id=SlowRequest",
                    userAgent = "LinkoraTest/1.0",
                    timeout = 10000L,
                    allowInsecureProtocol = true,
                    ignoreDocErrors = true,
                    useCss = false,
                    embedFonts = false,
                    embedImages = false,
                    restrictJs = false,
                    includeAudioElements = false,
                    includeVideoElements = false,
                    includeMetadata = false,
                    logStuff = true
                )
            } catch (e: Exception) {
                assertTrue(e is CancellationException)
            }
        }

        delay(100.milliseconds)
        captureJob.cancelAndJoin()

        val fileContent = tempFile.readText()
        assertTrue(fileContent.isEmpty())
    }

    @Test
    fun cleanlyCancelsDuringDomProcessing() = runBlocking {
        JVMAndAndroidWebCapture.init()
        val tempFile = createTempTestFile()

        val captureJob = launch {
            try {
                JVMAndAndroidWebCapture.saveHTMLPage(
                    fileDescriptor = -1,
                    filePath = tempFile.absolutePath,
                    url = "http://127.0.0.1:$masterPort/trap",
                    userAgent = "LinkoraTest/1.0",
                    timeout = 10000L,
                    allowInsecureProtocol = true,
                    ignoreDocErrors = true,
                    useCss = false,
                    embedFonts = false,
                    embedImages = true,
                    restrictJs = false,
                    includeAudioElements = false,
                    includeVideoElements = false,
                    includeMetadata = false,
                    logStuff = true
                )
            } catch (e: Exception) {
                assertTrue(e is CancellationException)
            }
        }

        delay(100.milliseconds)
        captureJob.cancelAndJoin()

        val fileContent = tempFile.readText()
        assertTrue(fileContent.isEmpty())
    }

    @Test
    fun chaoticConcurrentLoadTest(): Unit = runBlocking {
        JVMAndAndroidWebCapture.init()

        val deferredResults = (0 until 20).map { i ->
            async {
                val serverDelayMs = (50..200).random().toLong()
                val tempFile = createTempTestFile()

                val shouldCancel = (0..1).random() == 0
                var captureSucceeded = false

                val captureJob = launch {
                    try {
                        captureSucceeded = JVMAndAndroidWebCapture.saveHTMLPage(
                            fileDescriptor = -1,
                            filePath = tempFile.absolutePath,
                            url = "http://127.0.0.1:$masterPort/?delay=$serverDelayMs&id=$i",
                            userAgent = "LinkoraTest/1.0",
                            timeout = 10000L,
                            allowInsecureProtocol = true,
                            ignoreDocErrors = true,
                            useCss = false,
                            embedFonts = false,
                            embedImages = false,
                            restrictJs = false,
                            includeAudioElements = false,
                            includeVideoElements = false,
                            includeMetadata = false,
                            logStuff = true
                        )
                    } catch (e: Exception) {
                        assertTrue(e is CancellationException)
                    }
                }

                if (shouldCancel) {
                    delay((10..50).random().toLong().milliseconds)
                    captureJob.cancelAndJoin()
                } else {
                    captureJob.join()
                }

                if (captureSucceeded) {
                    val content = tempFile.readText()
                    assertTrue(
                        content.contains("Target $i")
                    )
                } else {
                    val content = tempFile.readText()
                    assertTrue(content.isEmpty())
                }
            }
        }

        deferredResults.awaitAll()
    }
}