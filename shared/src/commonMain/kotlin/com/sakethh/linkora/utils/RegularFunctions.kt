package com.sakethh.linkora.utils

import LocalizedStrings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.Quadruple
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.ui.utils.UIEvent
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun <T> wrappedResultFlow(init: suspend (SendChannel<Result<T>>) -> T): Flow<Result<T>> = channelFlow {
        send(Result.Loading())
        init(this.channel).let {
            send(Result.Success(it))
        }
    }
        .catchAsExceptionAndEmitFailure()

fun defaultSavedLinksFolder(localizedStrings: LocalizedStrings): Folder = Folder(
    name = localizedStrings.SavedLinks,
    note = "",
    parentFolderId = null,
    localId = Constants.SAVED_LINKS_ID,
    remoteId = null,
    isArchived = false,
)

fun defaultImpLinksFolder(localizedStrings: LocalizedStrings): Folder = Folder(
    name = localizedStrings.ImportantLinks,
    note = "",
    parentFolderId = null,
    localId = Constants.IMPORTANT_LINKS_ID,
    remoteId = null,
    isArchived = false,
)

fun <T1, T2, T3, T4, T5, T6, T7, M> septetCombine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> M,
): Flow<M> = combine(
    combine(flow, flow2, flow3, ::Triple),
    combine(flow4, flow5, flow6, flow7, ::Quadruple),
) { t1, t2 ->
    transform(
        t1.first,
        t1.second,
        t1.third,
        t2.first,
        t2.second,
        t2.third,
        t2.fourth,
    )
}

fun <T1, T2, T3, T4, T5, T6, M> hexadCombine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> M,
): Flow<M> = combine(
    combine(flow, flow2, flow3, ::Triple),
    combine(flow4, flow5, flow6, ::Triple),
) { t1, t2 ->
    transform(
        t1.first,
        t1.second,
        t1.third,
        t2.first,
        t2.second,
        t2.third,
    )
}

inline fun <reified OutgoingBody, reified IncomingBody> postFlow(
    crossinline syncServerClient: () -> HttpClient,
    crossinline baseUrl: suspend () -> String,
    crossinline authToken: suspend () -> String,
    endPoint: String,
    outgoingBody: OutgoingBody,
    contentType: ContentType = ContentType.Application.Json,
): Flow<Result<IncomingBody>> = flow {
    emit(Result.Loading())
    syncServerClient()
        .post(baseUrl() + endPoint) {
            bearerAuth(authToken())
            contentType(contentType)
            setBody(outgoingBody)
        }
        .handleResponseBody<IncomingBody>()
        .run {
            emit(this)
        }
}
    .catchAsExceptionAndEmitFailure()

inline fun <reified IncomingBody> getFlow(
    crossinline syncServerClient: () -> HttpClient,
    crossinline baseUrl: suspend () -> String,
    crossinline authToken: suspend () -> String,
    endPoint: String,
    contentType: ContentType = ContentType.Application.Json,
): Flow<Result<IncomingBody>> = flow {
    emit(Result.Loading())
    syncServerClient()
        .get(baseUrl() + endPoint) {
            bearerAuth(authToken())
            contentType(contentType)
        }
        .handleResponseBody<IncomingBody>()
        .run {
            emit(this)
        }
}
    .catchAsExceptionAndEmitFailure()

fun <LocalType, RemoteType> performLocalOperationWithRemoteSyncFlow(
    performRemoteOperation: Boolean,
    canPushToServer: suspend () -> Boolean,
    remoteOperation: suspend () -> Flow<Result<RemoteType>> = { emptyFlow() },
    remoteOperationOnSuccess: suspend (RemoteType) -> Unit = {},
    onRemoteOperationFailure: suspend () -> Unit = {},
    localOperation: suspend () -> LocalType,
): Flow<Result<LocalType>> = flow {
    emit(Result.Loading())
    val localResult = localOperation()
    var remoteExecResult: Result.Success.RemoteExecResult? = null
    if (performRemoteOperation && canPushToServer()) {
        remoteOperation().collect { remoteResult ->
            remoteResult.onFailure { exception ->
                remoteExecResult = Result.Success.RemoteExecResult(
                    isRemoteExecutionSuccessful = false,
                    e = exception
                )
                onRemoteOperationFailure()
            }
            remoteResult.onSuccess {
                remoteOperationOnSuccess(it.data)
                remoteExecResult = Result.Success.RemoteExecResult(
                    isRemoteExecutionSuccessful = true,
                    e = null
                )
            }
        }
    }
    emit(Result.Success(data = localResult, remoteExecResult = remoteExecResult))
}.catch {
    it.printStackTrace()
    emit(Result.Failure(e = it))
}

fun defaultFolderIds(): List<Long> = listOf(
    Constants.SAVED_LINKS_ID,
    Constants.IMPORTANT_LINKS_ID,
    Constants.ARCHIVE_ID,
    Constants.ALL_LINKS_ID,
    Constants.HISTORY_ID,
    Constants.DEFAULT_PANELS_ID,
)

fun getVideoPlatformBaseUrls(): List<String> = listOf("youtube.com", "youtu.be")

@OptIn(ExperimentalTime::class)
fun getSystemEpochSeconds() = Clock.System.now().epochSeconds

@OptIn(ExperimentalTime::class)
fun epochToReadableDateTime(
    epochSeconds: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String? = try {
    Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(timeZone).run {
        "${"${this.date.day}".addZeroAtPrefixOnInt()} ${
            month.name.initialCaps()
        } ${this.year}, ${
            "${
                (if (this.time.hour > 12) time.hour - 12 else time.hour)
            }".addZeroAtPrefixOnInt()
        }:${"${this.time.minute}".addZeroAtPrefixOnInt()}:${"${this.time.second}".addZeroAtPrefixOnInt()} ${if (this.time.hour > 11) "PM" else "AM"}"
    }
} catch (e: Exception) {
    e.printStackTrace()
    null
}

fun longPreferencesKey(key: String) = PreferenceKey.LongPreferencesKey(key)

fun intPreferencesKey(key: String) = PreferenceKey.IntPreferencesKey(key)

fun stringPreferencesKey(key: String) = PreferenceKey.StringPreferencesKey(key)

fun booleanPreferencesKey(key: String) = PreferenceKey.BooleanPreferencesKey(key)

@Composable
fun supportsWideDisplay(): Boolean = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.width.toDp() >= 840.dp
}

suspend fun UriHandler.openUriOrNotify(uri: String, localizedStrings: LocalizedStrings) {
    try {
        openUri(uri)
    } catch (e: Exception) {
        e.printStackTrace()
        UIEvent.pushUIEvent(
            UIEvent.Type.ShowSnackbar(localizedStrings.CouldntOpenLinkInBrowser),
        )
    }
}
