package com.sakethh.linkora.ui.domain

import androidx.compose.runtime.Stable
import com.sakethh.linkora.ui.LastSeenId
import com.sakethh.linkora.ui.LastSeenString

@Stable
data class PaginationState<T>(
    val isRetrieving: Boolean,
    val errorOccurred: Boolean,
    val errorMessage: String?,
    val pagesCompleted: Boolean,
    val data: T,
) {
    companion object {
        fun <T> retrieving(): PaginationState<Map<Pair<LastSeenId, LastSeenString>, T>> = PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = emptyMap(),
        )

        fun <T> retrievingOnEmpty(): PaginationState<Map<Pair<LastSeenId, LastSeenString>, T>> = PaginationState(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
            data = emptyMap(),
        )
    }
}
