package com.sakethh.linkora.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.retain.retain
import com.sakethh.linkora.domain.UnifiedLazyState
import com.sakethh.linkora.utils.Constants

@Composable
fun PerformAtTheEndOfTheList(
    unifiedLazyState: UnifiedLazyState,
    actionOnReachingEnd: () -> Unit,
) {
    val isAtTheEnd by retain {
        derivedStateOf {
            (unifiedLazyState.visibleItemsInfo.lastOrNull()?.index ?: 0) >=
                unifiedLazyState.totalItemsCount - Constants.TRIGGER_THRESHOLD_AT_THE_END
        }
    }

    LaunchedEffect(isAtTheEnd) {
        actionOnReachingEnd()
    }
}
