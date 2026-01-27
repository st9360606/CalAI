// app/src/main/java/com/calai/app/data/account/api/AccountApi.kt
package com.calai.bitecal.data.account.api

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.POST

interface AccountApi {

    @POST("/api/v1/account/deletion-request")
    suspend fun requestDeletion(): AccountDeletionResponse

    @GET("/api/v1/account/deletion-request")
    suspend fun deletionStatus(): AccountDeletionStatusResponse
}

@Serializable
data class AccountDeletionResponse(
    val ok: Boolean
)

@Serializable
data class AccountDeletionStatusResponse(
    val status: String,
    val requestedAtUtc: String? = null
)
