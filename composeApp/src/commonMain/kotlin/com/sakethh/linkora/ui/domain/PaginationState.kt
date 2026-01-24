package com.sakethh.linkora.ui.domain

import androidx.compose.runtime.Stable
import com.sakethh.linkora.ui.PageKey

@Stable
data class PaginationState<T>(
    val isRetrieving: Boolean,
    val errorOccurred: Boolean,
    val errorMessage: String?,
    val pagesCompleted: Boolean,
    val data: T,
) {
    companion object {
        fun <T> retrieving(): PaginationState<Map<PageKey, T>> {
            return PaginationState(
                isRetrieving = true,
                errorOccurred = false,
                errorMessage = null,
                pagesCompleted = false,
                data = emptyMap()
            )
        }
        fun <T> retrievingOnEmpty(): PaginationState<Map<PageKey, T>> {
            return PaginationState(
                isRetrieving = true,
                errorOccurred = false,
                errorMessage = null,
                pagesCompleted = false,
                data = emptyMap()
            )
        }
    }
}
