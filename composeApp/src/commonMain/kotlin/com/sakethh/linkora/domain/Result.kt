package com.sakethh.linkora.domain

sealed interface Result<T> {
    data class Success<T>(
        val data: T,
        val remoteExecResult: RemoteExecResult? = null
    ) : Result<T> {
        data class RemoteExecResult(val isRemoteExecutionSuccessful: Boolean, val e: Exception?)
    }

    data class Loading<T>(
        val message: String = "",
    ) : Result<T>

    data class Failure<T>(
        val e: Exception,
    ) : Result<T>
}

suspend fun <T> Result<T>.onSuccess(init: suspend (Result.Success<T>) -> Unit): Result<T> {
    if (this is Result.Success) {
        try {
            init(this)
        } catch (e: Exception) {
            return Result.Failure(e)
        }
    }
    return this
}

suspend fun <T> Result<T>.onFailure(init: suspend (e: Exception) -> Unit): Result<T> {
    if (this is Result.Failure) {
        init(this.e)
    }
    return this
}

suspend fun <T> Result<T>.onLoading(init: suspend (loadingLog: String) -> Unit): Result<T> {
    if (this is Result.Loading) {
        init(this.message)
    }
    return this
}

typealias LinkoraResultFailure<T> = Result.Failure<T>
