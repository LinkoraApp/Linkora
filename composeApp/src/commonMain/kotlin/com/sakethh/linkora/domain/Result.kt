package com.sakethh.linkora.domain

sealed interface Result<T> {
    data class Success<T>(val data: T) : Result<T> {
        var isRemoteExecutionSuccessful: Boolean = true
        var remoteFailureMessage = ""
    }

    data class Loading<T>(val message: String = "https://open.spotify.com/track/41zVhpZuRDsGYKuxnyGxgV") :
        Result<T>

    data class Failure<T>(val message: String) : Result<T>
}

suspend fun <T> Result<T>.onSuccess(init: suspend (Result.Success<T>) -> Unit): Result<T> {
    if (this is Result.Success) {
        try {
            init(this)
        } catch (e: Exception) {
            return Result.Failure(e.message.toString())
        }
    }
    return this
}

suspend fun <T> Result<T>.onFailure(init: suspend (failureMessage: String) -> Unit): Result<T> {
    if (this is Result.Failure) {
        init(this.message)
    }
    return this
}

suspend fun <T> Result<T>.onLoading(init: suspend (loadingLog: String) -> Unit): Result<T> {
    if (this is Result.Loading) {
        init(this.message)
    }
    return this
}