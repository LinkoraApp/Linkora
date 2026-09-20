package com.sakethh.linkora.domain.repository.remote

import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.github.GitHubReleaseDTOItem
import kotlinx.coroutines.flow.Flow

interface GitHubReleasesRepo {
    suspend fun getLatestVersionData(): Flow<Result<GitHubReleaseDTOItem>>
}
