package com.sakethh.linkora.ui

import com.sakethh.linkora.ui.domain.CurrentFABContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FabStateController {
    private val _fabState = MutableStateFlow(CurrentFABContext.ROOT)
    val fabState: StateFlow<CurrentFABContext> = _fabState.asStateFlow()

    fun updateState(newState: CurrentFABContext) {
        _fabState.value = newState
    }
}