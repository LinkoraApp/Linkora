package com.sakethh.linkora.ui.screens.settings.section.data.sync

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.NetworkRepo
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ServerSetupScreenViewModel(
    private val networkRepo: NetworkRepo
) : ViewModel() {
    val serverSetupState = mutableStateOf(
        ServerSetupState(
            isConnecting = false,
            isConnectedSuccessfully = false,
            isError = false
        )
    )


    fun resetState() {
        serverSetupState.value = ServerSetupState(
            isConnecting = false,
            isConnectedSuccessfully = false,
            isError = false
        )
    }

    fun testServerConnection(serverUrl: String, token: String) {
        viewModelScope.launch {
            networkRepo.testServerConnection(serverUrl, token).collectLatest {
                it.onSuccess {
                    serverSetupState.value = ServerSetupState(
                        isConnecting = false,
                        isConnectedSuccessfully = true,
                        isError = false
                    )
                }.onFailure { failureMessage ->
                    serverSetupState.value = ServerSetupState(
                        isConnecting = false,
                        isConnectedSuccessfully = false,
                        isError = true
                    )
                    pushUIEvent(UIEvent.Type.ShowSnackbar(failureMessage))
                }.onLoading {
                    serverSetupState.value = ServerSetupState(
                        isConnecting = true,
                        isConnectedSuccessfully = false,
                        isError = false
                    )
                }
            }
        }
    }
}