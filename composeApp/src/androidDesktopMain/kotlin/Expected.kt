import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.web_capture.WebCapture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDesktopWebCapture {
    suspend fun init(): Result<Boolean> {
        return WebCapture.init().fold(onSuccess = {
            Result.Success(true)
        }, onFailure = { Result.Failure(it.message.toString()) })
    }

    suspend fun saveHTMLPage(
        fileDescriptor: Int,
        filePath: String,
        url: String,
        userAgent: String,
        timeout: Long,
        allowInsecureProtocol: Boolean,
        ignoreDocErrors: Boolean,
        useCss: Boolean,
        embedFonts: Boolean,
        embedImages: Boolean,
        restrictJs: Boolean,
        includeAudioElements: Boolean,
        includeVideoElements: Boolean,
        includeMetadata: Boolean,
        logStuff: Boolean
    ): Result<Boolean> {
        return when (val initResult = init()) {
            is Result.Failure -> Result.Failure(
                initResult.message
            )

            is Result.Loading -> Result.Loading(
                initResult.message
            )

            is Result.Success -> {
                try {
                    withContext(Dispatchers.IO) {
                        WebCapture.saveHTMLPage(
                            fileDescriptor = fileDescriptor,
                            filePath = filePath,
                            url = url,
                            userAgent = userAgent,
                            timeout = timeout,
                            allowInsecureProtocol = allowInsecureProtocol,
                            ignoreDocErrors = ignoreDocErrors,
                            useCss = useCss,
                            embedFonts = embedFonts,
                            embedImages = embedImages,
                            restrictJs = restrictJs,
                            logStuff = logStuff,
                            includeAudioElements = includeAudioElements,
                            includeVideoElements = includeVideoElements,
                            includeMetadata = includeMetadata
                        ).run {
                            Result.Success(this)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Result.Failure(e.message.toString())
                }
            }
        }
    }
}