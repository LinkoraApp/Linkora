package com.sakethh.linkora.utils

import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.Quadruple
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.domain.model.ServerConnection
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun <T> wrappedResultFlow(init: suspend (SendChannel<Result<T>>) -> T): Flow<Result<T>> {
    return channelFlow {
        send(Result.Loading())
        init(this.channel).let {
            send(Result.Success(it))
        }
    }.catchAsExceptionAndEmitFailure()
}

fun defaultSavedLinksFolder(): Folder = Folder(
    name = Localization.Key.SavedLinks.getLocalizedString(),
    note = "",
    parentFolderId = null,
    localId = Constants.SAVED_LINKS_ID,
    remoteId = null,
    isArchived = false
)

fun defaultImpLinksFolder(): Folder = Folder(
    name = Localization.Key.ImportantLinks.getLocalizedString(),
    note = "",
    parentFolderId = null,
    localId = Constants.IMPORTANT_LINKS_ID,
    remoteId = null,
    isArchived = false
)

fun <T1, T2, T3, T4, T5, T6, T7, M> septetCombine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> M
): Flow<M> = combine(
    combine(flow, flow2, flow3, ::Triple), combine(flow4, flow5, flow6, flow7, ::Quadruple)
) { t1, t2 ->
    transform(
        t1.first, t1.second, t1.third, t2.first, t2.second, t2.third, t2.fourth
    )
}

fun <T1, T2, T3, T4, T5, T6, M> hexadCombine(
    flow: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> M
): Flow<M> = combine(
    combine(flow, flow2, flow3, ::Triple), combine(flow4, flow5, flow6, ::Triple)
) { t1, t2 ->
    transform(
        t1.first, t1.second, t1.third, t2.first, t2.second, t2.third
    )
}

inline fun <reified OutgoingBody, reified IncomingBody> postFlow(
    crossinline syncServerClient: () -> HttpClient,
    crossinline baseUrl: () -> String,
    crossinline authToken: () -> String,
    endPoint: String,
    outgoingBody: OutgoingBody,
    contentType: ContentType = ContentType.Application.Json
): Flow<Result<IncomingBody>> {
    return flow {
        emit(Result.Loading())
        syncServerClient().post(baseUrl() + endPoint) {
            bearerAuth(authToken())
            contentType(contentType)
            setBody(outgoingBody)
        }.handleResponseBody<IncomingBody>().run {
            emit(this)
        }
    }.catchAsExceptionAndEmitFailure()
}

fun <LocalType, RemoteType> performLocalOperationWithRemoteSyncFlow(
    performRemoteOperation: Boolean,
    remoteOperation: suspend () -> Flow<Result<RemoteType>> = { emptyFlow() },
    remoteOperationOnSuccess: suspend (RemoteType) -> Unit = {},
    onRemoteOperationFailure: suspend () -> Unit = {},
    localOperation: suspend () -> LocalType
): Flow<Result<LocalType>> {
    return flow {
        emit(Result.Loading())
        val localResult = localOperation()
        Result.Success(localResult).let { success ->
            if (performRemoteOperation && AppPreferences.canPushToServer()) {
                remoteOperation().collect { remoteResult ->
                    remoteResult.onFailure { failureMessage ->
                        success.isRemoteExecutionSuccessful = false
                        success.remoteFailureMessage = failureMessage
                        onRemoteOperationFailure()
                    }
                    remoteResult.onSuccess {
                        remoteOperationOnSuccess(it.data)
                    }
                }
            }
            emit(success)
        }
    }.catchAsThrowableAndEmitFailure(init = {
        if (performRemoteOperation && AppPreferences.canPushToServer()) {
            onRemoteOperationFailure()
        }
    })
}

fun defaultFolderIds(): List<Long> = listOf(
    Constants.SAVED_LINKS_ID,
    Constants.IMPORTANT_LINKS_ID,
    Constants.ARCHIVE_ID,
    Constants.ALL_LINKS_ID,
    Constants.HISTORY_ID,
    Constants.DEFAULT_PANELS_ID
)

fun ifServerConfigured(init: () -> Unit) {
    if (AppPreferences.isServerConfigured()) {
        init()
    }
}

fun getVideoPlatformBaseUrls(): List<String> = listOf("youtube.com", "youtu.be")

fun currentSavedServerConfig(): ServerConnection {
    return ServerConnection(
        serverUrl = AppPreferences.serverBaseUrl.value,
        authToken = AppPreferences.serverSecurityToken.value,
        syncType = AppPreferences.serverSyncType.value,
        webSocketScheme = AppPreferences.WEB_SOCKET_SCHEME
    )
}

@OptIn(ExperimentalTime::class)
fun getSystemEpochSeconds() = Clock.System.now().epochSeconds

@OptIn(ExperimentalTime::class)
fun epochToReadableDateTime(
    epochSeconds: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String? {
    return try {
        Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(timeZone)
            .run {
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
}