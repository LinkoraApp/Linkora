import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.web_capture.WebCapture

class AndroidDesktopWebCapture {
    suspend fun init(): Result<Boolean> {
        return WebCapture.init().fold(
            onSuccess = {
                Result.Success(true)
            },
            onFailure = { Result.Failure(it.message.toString()) }
        )
    }

    suspend fun getHTMLPage(
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
    ): Result<ByteArray> {
        return when (val initResult = init()) {
            is Result.Failure -> Result.Failure(
                initResult.message
            )

            is Result.Loading -> Result.Loading(
                initResult.message
            )

            is Result.Success -> {
                try {
                    val htmlByteArray = WebCapture.getHTMLPage(
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
                    )
                    Result.Success(htmlByteArray)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Result.Failure(e.message.toString())
                }
            }
        }
    }
}