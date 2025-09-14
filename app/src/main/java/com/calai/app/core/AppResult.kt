package com.calai.app.core

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : AppResult<Nothing>()
}

suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): AppResult<T> =
    try { AppResult.Success(block()) }
    catch (t: Throwable) {
        val msg = when (t) {
            is java.net.SocketTimeoutException -> "請求逾時，請稍後再試"
            is java.net.UnknownHostException   -> "無法連線，請確認網路/伺服器"
            else -> t.message ?: "發生未知錯誤"
        }
        AppResult.Error(msg, t)
    }
