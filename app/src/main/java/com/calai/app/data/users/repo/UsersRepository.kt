package com.calai.app.data.users.repo

import com.calai.app.data.users.api.MeDto
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
        // 401：未登入；其他碼你也可以依需求改成 throw
        if (e.code() == 401) null else null
    } catch (e: IOException) {
        null
    }
}
