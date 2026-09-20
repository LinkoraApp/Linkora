package com.sakethh.linkora.domain

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Stable

/*
If there was a common interface for `LazyGridLayoutInfo`, `LazyListItemInfo`, and `LazyStaggeredGridItemInfo`
that provided the `visibleItemsInfo` & `totalItemsCount`, I wouldn't have to do this
but simply use a helper-like function.
* */

@Stable
interface UnifiedLazyState {
    val visibleItemsInfo: List<UnifiedLazyItem>
    val totalItemsCount: Int
}

@Stable
data class UnifiedLazyItem(
    val index: Int,
)

fun LazyListState.asUnifiedLazyState(): UnifiedLazyState = object : UnifiedLazyState {
    override val visibleItemsInfo: List<UnifiedLazyItem>
        get() =
            this@asUnifiedLazyState.layoutInfo.visibleItemsInfo.map {
                UnifiedLazyItem(it.index)
            }

    override val totalItemsCount: Int
        get() = this@asUnifiedLazyState.layoutInfo.totalItemsCount
}

fun LazyGridState.asUnifiedLazyState(): UnifiedLazyState = object : UnifiedLazyState {
    override val visibleItemsInfo: List<UnifiedLazyItem>
        get() =
            this@asUnifiedLazyState.layoutInfo.visibleItemsInfo.map {
                UnifiedLazyItem(it.index)
            }

    override val totalItemsCount: Int
        get() = this@asUnifiedLazyState.layoutInfo.totalItemsCount
}

fun LazyStaggeredGridState.asUnifiedLazyState(): UnifiedLazyState = object : UnifiedLazyState {
    override val visibleItemsInfo: List<UnifiedLazyItem>
        get() =
            this@asUnifiedLazyState.layoutInfo.visibleItemsInfo.map {
                UnifiedLazyItem(it.index)
            }

    override val totalItemsCount: Int
        get() = this@asUnifiedLazyState.layoutInfo.totalItemsCount
}
