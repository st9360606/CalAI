package com.calai.app.data.auth.repo


import com.calai.app.core.device.DeviceIdProvider
import com.calai.app.data.auth.api.AuthApi
import com.calai.app.data.auth.api.model.StartEmailReq
import com.calai.app.data.auth.api.model.VerifyEmailReq
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EmailAuthRepository @Inject constructor(
    @Named("authApi") private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val deviceId: DeviceIdProvider
) {
    suspend fun start(email: String): Boolean {
        val res = api.startEmail(StartEmailReq(email.trim().lowercase()))
        return res.sent
    }

    suspend fun verify(email: String, code: String) {
        val dto = api.verifyEmail(
            VerifyEmailReq(email.trim().lowercase(), code.trim()),
            deviceId = deviceId.get()
        )
        tokenStore.save(
            access = dto.accessToken,
            refresh = dto.refreshToken,
            accessExpiresInSec = dto.accessExpiresInSec,
            serverTimeEpochSec = dto.serverTimeEpochSec
        )
    }

}
