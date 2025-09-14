package com.calai.app.data

import com.calai.app.core.AppResult
import com.calai.app.core.safeApiCall
import com.calai.app.net.ApiService
import com.calai.app.net.InfoDTO
import javax.inject.Inject

class MainRepository @Inject constructor(private val api: ApiService) {
    suspend fun hello(): AppResult<String> = safeApiCall { api.hello() }
    suspend fun info():  AppResult<InfoDTO> = safeApiCall { api.info() }
}
