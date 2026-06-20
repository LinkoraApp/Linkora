package com.sakethh.linkora.web_capture

import com.sakethh.linkora.JVMAndAndroidWebCapture
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebCaptureIntegrationTest {

    private lateinit var tempTestFile: File

    @BeforeTest
    fun setup() {
        tempTestFile = File.createTempFile("linkora_test_capture", ".html")
    }

    @AfterTest
    fun cleanup() {
        if (tempTestFile.exists()) {
            tempTestFile.delete()
        }
    }

    @Test
    fun `loads Native Library Successfully`() = runTest {
        val result = JVMAndAndroidWebCapture.init()

        assertTrue(
            result.isSuccess, "Failed to load the Rust native library. Check java.library.path."
        )
        assertEquals(result.getOrNull(), true)
    }

    @Test
    fun `writes valid html to disk`() = runTest {
        JVMAndAndroidWebCapture.init()

        val success = JVMAndAndroidWebCapture.saveHTMLPage(
            fileDescriptor = -1,
            filePath = tempTestFile.absolutePath,
            url = "https://sakethpathike.github.io/misc",
            userAgent = "LinkoraTest/1.0",
            timeout = 10000L,
            allowInsecureProtocol = false,
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

        assertTrue(success, "Rust function returned false, capture failed.")

        assertTrue(tempTestFile.exists(), "The test file was not created on the disk.")

        val fileContent = tempTestFile.readText()
        assertTrue(fileContent.isNotEmpty(), "The captured file is completely empty.")
        assertTrue(
            fileContent.contains("<html", ignoreCase = true),
            "The file does not look like an HTML document."
        )
        assertTrue(
            fileContent.contains("https://fonts.google.com/specimen/Schibsted+Grotesk") && fileContent.contains(
                "Schibsted Grotesk"
            ), "The file did not capture the correctly"
        )
    }
}