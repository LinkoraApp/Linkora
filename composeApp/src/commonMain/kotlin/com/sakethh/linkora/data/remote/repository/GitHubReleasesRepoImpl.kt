package com.sakethh.linkora.data.remote.repository

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.github.GitHubReleaseDTOItem
import com.sakethh.linkora.domain.repository.remote.GitHubReleasesRepo
import com.sakethh.linkora.utils.wrappedResultFlow
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow

class GitHubReleasesRepoImpl(
    private val standardClient: HttpClient,
) : GitHubReleasesRepo {
    override suspend fun getLatestVersionData(): Flow<Result<GitHubReleaseDTOItem>> = wrappedResultFlow {
        standardClient
            .get("https://api.github.com/repos/sakethpathike/Linkora/releases")
            .body<List<GitHubReleaseDTOItem>>()
            .first()
            .run {
                copy(
                    releaseName =
                    if (releaseName.startsWith("v").not()) {
                        "v$releaseName"
                    } else {
                        releaseName
                    },
                )
            }
    }
}
