import com.sakethh.linkora.KaptureOptions
import com.sakethh.linkora.WebCapture
import com.sakethh.linkora.domain.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidDesktopWebCapture {

    suspend fun init(options: KaptureOptions) = WebCapture.init(options)

    /**
     * Previously all rust operations used to be cancelled explicitly but since now
     * web-capture is based on kapture cancelling the coroutine will do the job
     */
    fun nuke() = Unit

    suspend fun saveHTMLPage(
        filePath: String,
        url: String,
    ) {
        withContext(Dispatchers.IO) {
            WebCapture.saveHTMLPage(
                filePath = filePath,
                url = url,
            ).run {
                Result.Success(true)
            }
        }
    }
}
