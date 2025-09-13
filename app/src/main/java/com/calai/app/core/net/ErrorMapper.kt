package com.calai.app.core.net

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

object ErrorMapper {
    fun <T> map(t: Throwable): NetworkResult<T> = when (t) {
        is HttpException -> NetworkResult.HttpError(t.code(), t.response()?.errorBody()?.string())
        is SocketTimeoutException -> NetworkResult.NetworkError("連線逾時")
        is IOException -> NetworkResult.NetworkError("網路連線失敗")
        else -> NetworkResult.Unexpected(t.message ?: t.toString())
    }
}
