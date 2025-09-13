package com.calai.app.data

import com.calai.app.core.net.ErrorMapper
import com.calai.app.core.net.NetworkResult
import com.calai.app.net.ApiService
import com.calai.app.net.InfoDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainRepository @Inject constructor(
    private val api: ApiService
) {
    suspend fun hello(): NetworkResult<String> = withContext(Dispatchers.IO) {
        try { NetworkResult.Success(api.hello()) }
        catch (t: Throwable) { ErrorMapper.map(t) }
    }

    suspend fun info(): NetworkResult<InfoDTO> = withContext(Dispatchers.IO) {
        try { NetworkResult.Success(api.info()) }
        catch (t: Throwable) { ErrorMapper.map(t) }
    }
}
