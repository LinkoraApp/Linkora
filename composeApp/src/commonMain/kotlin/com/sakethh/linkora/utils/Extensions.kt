package com.sakethh.linkora.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.domain.model.FlatChildFolderData
import com.sakethh.linkora.domain.model.FlatSearchResult
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.PageKey
import com.sakethh.linkora.ui.domain.PaginationState
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.ui.utils.rememberDeserializableObject
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.util.TreeMap

fun String.addZeroAtPrefixOnInt() =
    try {
        if (toInt() > 9) this else "0$this"
    } catch (e: Exception) {
        e.printStackTrace()
        this
    }

fun String.initialCaps(): String {
    return when {
        length > 1 -> {
            get(0).uppercase() + substring(1).lowercase()
        }

        else -> this.uppercase()
    }
}

fun String.host(throwOnException: Boolean = true): String {
    return try {
        this.split("/")[2]
    } catch (e: Exception) {
        if (throwOnException) {
            throw e
        }
        this
    }
}

fun Modifier.fillMaxWidthWithPadding(
    paddingValues: PaddingValues = PaddingValues(
        start = 15.dp, end = 15.dp
    )
): Modifier {
    return this.fillMaxWidth().padding(paddingValues)
}

@Composable
fun Modifier.bottomNavPaddingAcrossPlatforms(): Modifier {
    return if (platform() is Platform.Android.Mobile) {
        this.navigationBarsPadding()
    } else {
        this.padding(bottom = 10.dp)
    }
}

// keeps the nav bar (what's it actually called?) transparent while still applying padding on top, end, bottom;
// kinda a hacky workaround, but there doesn't seem to be any clear documentation on how to handle this properly
fun Modifier.addEdgeToEdgeScaffoldPadding(paddingValues: PaddingValues) = this.padding(
    top = paddingValues.calculateTopPadding(), start = paddingValues.calculateStartPadding(
        LayoutDirection.Ltr
    ), end = paddingValues.calculateEndPadding(LayoutDirection.Rtl)
).consumeWindowInsets(paddingValues)

fun String?.isNotNullOrNotBlank(): Boolean {
    return !this.isNullOrBlank()
}

fun String.isAValidLink(): Boolean {
    return try {
        this.host()
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun Boolean.ifNot(init: () -> Unit): Boolean {
    if (!this) {
        init()
    }
    return this
}

fun Boolean.ifTrue(init: () -> Unit): Boolean {
    if (this) {
        init()
    }
    return this
}

fun Localization.Key.getLocalizedString(): String {
    return Localization.getLocalizedString(this)
}

@Composable
fun Localization.Key.rememberLocalizedString(): String {
    return Localization.rememberLocalizedString(this)
}

suspend fun <T> Result<T>.pushSnackbarOnFailure() {
    if (this is Result.Failure) {
        pushUIEvent(UIEvent.Type.ShowSnackbar(this.message))
    }
}

fun <T> Result.Success<T>.getRemoteOnlyFailureMsg(): String {
    return if (this.isRemoteExecutionSuccessful.not()) "\n\n${Localization.Key.RemoteExecutionFailed.getLocalizedString()}\n" + this.remoteFailureMessage else ""
}

fun Exception?.pushSnackbar(coroutineScope: CoroutineScope) {
    if (this != null) {
        coroutineScope.pushUIEvent(UIEvent.Type.ShowSnackbar(this.message.toString()))
    }
}

suspend fun Exception?.pushSnackbar() {
    if (this != null) {
        pushUIEvent(UIEvent.Type.ShowSnackbar(this?.message.toString()))
    }
}

fun Throwable?.pushSnackbar(coroutineScope: CoroutineScope) {
    if (this != null) {
        coroutineScope.pushUIEvent(UIEvent.Type.ShowSnackbar(this.message.toString()))
    }
}

fun <T> Flow<Result<T>>.catchAsThrowableAndEmitFailure(init: suspend () -> Unit = {}): Flow<Result<T>> {
    return this.catch {
        init()
        it.printStackTrace()
        emit(Result.Failure(message = it.message.toString()))
    }
}

fun <T> Flow<Result<T>>.catchAsExceptionAndEmitFailure(): Flow<Result<T>> {
    return this.catch {
        try {
            it as Exception
            it.printStackTrace()
            emit(Result.Failure(message = it.message.toString()))
        } catch (e: Exception) {
            e.printStackTrace()
            it.printStackTrace()
            emit(Result.Failure(message = it.message.toString()))
        }
    }
}

fun String.replaceFirstPlaceHolderWith(string: String): String {
    return this.replace(LinkoraPlaceHolder.First.value, string.inDoubleQuotes())
}

fun String.isATwitterUrl(): Boolean {
    return this.trim().startsWith("http://twitter.com/") or this.trim()
        .startsWith("https://twitter.com/") or this.trim().startsWith(
        "http://x.com/"
    ) or this.trim().startsWith("https://x.com/")
}

suspend fun <T : Any> T.then(init: suspend () -> Unit): T {
    init()
    return this
}


@Composable
fun NavHostController.inRootScreen(includeSettingsScreen: Boolean): Boolean? {
    val rootRoutesList = rememberDeserializableObject {
        listOf(
            Navigation.Root.HomeScreen,
            Navigation.Root.SearchScreen,
            Navigation.Root.CollectionsScreen,
            Navigation.Root.SettingsScreen,
        )
    }
    return this.currentBackStackEntryAsState().value?.destination?.let { destination ->
        rootRoutesList.filter {
            includeSettingsScreen || it != Navigation.Root.SettingsScreen
        }.any {
            destination.hasRoute(it::class)
        }
    }
}

fun String.inDoubleQuotes(): String = "\"$this\""


suspend inline fun <reified IncomingBody> HttpResponse.handleResponseBody(): Result<IncomingBody> {
    return if (this.status.isSuccess().not()) {
        Result.Failure(this.status.value.toString() + " " + this.status.description)
    } else {
        Result.Success(this.body<IncomingBody>())
    }
}


fun Correlation.isSameAsCurrentClient(): Boolean = this.id == AppPreferences.getCorrelation().id

suspend fun PreferencesRepository.updateLastSyncedWithServerTimeStamp(newValue: Long) {
    this.changePreferenceValue(
        preferenceKey = longPreferencesKey(AppPreferenceType.LAST_TIME_SYNCED_WITH_SERVER.name),
        newValue = newValue
    )
}

suspend fun File.duplicate(): File? = withContext(Dispatchers.IO) {
    try {
        val tempFile = File.createTempFile("temp_${nameWithoutExtension}", ".${extension}")
        this@duplicate.inputStream().use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


fun <ItemType> MutableStateFlow<PaginationState<Map<PageKey, List<ItemType>>>>.onRetrieved(
    pageKey: PageKey,
    retrievedData: List<Pair<PageKey, ItemType>>
) {
    update { currentState ->
        val updatedData = TreeMap(currentState.data)
        updatedData[pageKey] = retrievedData.map {
            it.second
        }

        currentState.copy(
            data = updatedData,
            isRetrieving = false,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
        )
    }
}

fun <T> MutableStateFlow<PaginationState<T>>.onError(errorMsg: String) {
    update { currentState ->
        currentState.copy(
            isRetrieving = false,
            errorOccurred = true,
            errorMessage = errorMsg,
            pagesCompleted = false,
        )
    }
}

fun <T> MutableStateFlow<PaginationState<T>>.onRetrieving() {
    update { currentState ->
        currentState.copy(
            isRetrieving = true,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = false,
        )
    }
}

fun <T> MutableStateFlow<PaginationState<T>>.onPagesFinished() {
    update { currentState ->
        currentState.copy(
            isRetrieving = false,
            errorOccurred = false,
            errorMessage = null,
            pagesCompleted = true,
        )
    }
}

context(viewModel: ViewModel)
fun <T> Flow<T>.asStateInWhileSubscribed(
    initialValue: T,
    stopTimeoutMillis: Long = 5000
): StateFlow<T> {
    return stateIn(
        scope = viewModel.viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = stopTimeoutMillis),
        initialValue = initialValue
    )
}

@JvmName("shuffleLinksFlatChildFolderData")
fun Flow<Result<List<FlatChildFolderData>>>.shuffleLinks(): Flow<Result<List<FlatChildFolderData>>> {
    return transform {
        when (it) {
            is Result.Success -> emit(it.copy(data = it.data.filter {
                it.itemType != Constants.LINK
            } + it.data.filter {
                it.itemType == Constants.LINK
            }.shuffled()))

            else -> emit(it)
        }
    }
}

fun Flow<Result<List<Link>>>.shuffleLinks(): Flow<Result<List<Link>>> {
    return transform {
        when (it) {
            is Result.Success -> emit(it.copy(data = it.data.shuffled()))
            else -> emit(it)
        }
    }
}

@JvmName("shuffleLinksFlatSearchResult")
fun Flow<Result<List<FlatSearchResult>>>.shuffleLinks(): Flow<Result<List<FlatSearchResult>>> {
    return transform {
        when (it) {
            is Result.Success -> emit(it.copy(data = it.data.filter {
                it.itemType != Constants.LINK
            } + it.data.filter {
                it.itemType == Constants.LINK
            }.shuffled()))

            else -> emit(it)
        }
    }
}