package com.calai.app.data.users.repo

import com.calai.app.data.users.api.MeDto
import com.calai.app.data.users.api.UpdateNameRequest
import com.calai.app.data.users.api.UsersApi
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersRepository @Inject constructor(
    private val api: UsersApi
) {
    suspend fun meOrNull(): MeDto? = try {
        api.me()
    } catch (e: HttpException) {
        if (e.code() == 401) null else null
    } catch (e: IOException) {
        throw e
    }

    suspend fun updateName(newName: String): MeDto {
        // ✅ 統一在 repo 先 trim，避免 UI 忘記 trim
        val trimmed = newName.trim()
        return api.updateMe(UpdateNameRequest(name = trimmed))
    }
}
