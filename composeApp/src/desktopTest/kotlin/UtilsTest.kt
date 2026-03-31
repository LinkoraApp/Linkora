import com.sakethh.linkora.data.local.repository.LocalLinksRepoImpl
import com.sakethh.linkora.domain.model.ScrapedLinkInfo
import com.sakethh.linkora.preferences.AppPreferences
import io.mockk.mockk
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class UtilsTest {

    @Test
    fun `html parsing should return valid meta info`() = runTest {
        val retrievalJobs = mutableListOf<Job>()
        listOf(
            "https://sakethpathike.github.io/blog/kmp-in-practice" to ScrapedLinkInfo(
                title = "Kotlin Multiplatform, in practice. • Saketh Pathike",
                imgUrl = "https://sakethpathike.github.io/images/ogImage-kmp-in-practice.png",
            ),
            "https://mockk.io/" to ScrapedLinkInfo(
                title = "MockK",
                imgUrl = "https://mockk.io/doc/stats-shared.png",
            ),
            "https://discuss.kotlinlang.org/t/how-to-best-use-mockito-in-kotlin/24675" to ScrapedLinkInfo(
                title = "How to best use Mockito in Kotlin?",
                imgUrl = "https://us1.discourse-cdn.com/flex019/uploads/kotlinlang/original/2X/2/224964e73572d20c3aa9d68b4c14ae5d11749202.png",
            ),
            "https://tidal.com/artist/4143898" to ScrapedLinkInfo(
                title = "Nujabes",
                imgUrl = "https://resources.tidal.com/images/f899da08/195b/432d/b17c/207ffb009380/750x750.jpg",
            ),
            "https://genius.com/albums/Nujabes/Modal-soul" to ScrapedLinkInfo(
                title = "Modal Soul by Nujabes",
                imgUrl = "https://images.genius.com/7f62b49d9becfdf686ce707a1e77a841.873x873x1.png",
            ),
        ).forEach { (linkUrl, expectedInfo) ->
            retrievalJobs.add(launch {
                val scrapedInfo = LocalLinksRepoImpl(
                    linksDao = mockk(),
                    primaryUserAgent = {
                        AppPreferences.primaryJsoupUserAgent.value
                    },
                    proxyUrl = {
                        AppPreferences.proxyUrl
                    },
                    standardClient = mockk(),
                    remoteLinksRepo = mockk(),
                    foldersDao = mockk(),
                    pendingSyncQueueRepo = mockk(),
                    preferencesRepository = mockk(),
                    tagsDao = mockk()
                ).scrapeLinkData(
                    linkUrl = linkUrl, userAgent = "Twitterbot/1.0"
                )

                assertContains(expectedInfo.title, scrapedInfo.title, ignoreCase = true)
                assertEquals(expectedInfo.imgUrl, scrapedInfo.imgUrl)
            })
        }
        retrievalJobs.joinAll()
    }
}