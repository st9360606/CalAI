package com.calai.app.core.net

sealed interface NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>
    data class HttpError(val code: Int, val body: String?) : NetworkResult<Nothing>
    data class NetworkError(val message: String) : NetworkResult<Nothing>   // 連線/逾時
    data class Unexpected(val message: String) : NetworkResult<Nothing>     // 其他未預期
}