package com.calai.app.data.profile

import android.util.Log
import com.calai.app.data.auth.store.UserProfileStore
import javax.inject.Inject
import javax.inject.Singleton
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale

@Singleton
class ProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val store: UserProfileStore
) {

    /** 試取雲端 Profile；若能取到，順便把本機 hasServerProfile 標成 true */
    suspend fun existsOnServer(): Boolean = try {
        api.getMyProfile()
        runCatching { store.setHasServerProfile(true) }
        true
    } catch (e: HttpException) {
        when (e.code()) { 401, 404 -> false; else -> throw e }
    } catch (e: IOException) { throw e }

    /** 取伺服器 Profile；401/404 視為沒有，其他錯誤拋出或回 null（保守） */
    suspend fun getServerProfileOrNull(): UserProfileDto? = try {
        api.getMyProfile()
    } catch (e: HttpException) {
        when (e.code()) { 401, 404 -> null; else -> throw e }
    } catch (e: IOException) { null }

    /** 將伺服器 locale（若有且非空）同步到本機 DataStore。回傳是否有同步 */
    suspend fun syncLocaleFromServerToStore(): Boolean {
        val p = getServerProfileOrNull() ?: return false
        val tag = p.locale?.takeIf { it.isNotBlank() } ?: return false
        runCatching { store.setLocaleTag(tag) }
        return true
    }

    /** 同時支援 raw 次數(0..7+) 與 bucket(0/2/4/6/7) 的對映 */
    private fun toExerciseLevel(freqOrBucket: Int?): String? = when (freqOrBucket) {
        null -> null
        in Int.MIN_VALUE..0 -> "sedentary"   // 0 或更小
        in 1..3              -> "light"      // 1–3（含 bucket=2）
        in 4..5              -> "moderate"   // 4–5（含 bucket=4）
        6                    -> "active"     // 6（含 bucket=6）
        else                 -> "very_active"// 7 以上（含 bucket=7）
    }

    /** 新用戶：把本機 Onboarding 上傳；成功→標記 hasServerProfile=true */
    suspend fun upsertFromLocal(): Result<UserProfileDto> = runCatching {
        val p = store.snapshot()
        Log.d("ProfileRepo", "freq=${p.exerciseFreqPerWeek} -> level=${toExerciseLevel(p.exerciseFreqPerWeek)}")

        val localeTag = p.locale?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().toLanguageTag()

        val req = UpsertProfileRequest(
            gender = p.gender,
            age = p.ageYears,
            heightCm = p.heightCm?.toDouble(),
            weightKg = p.weightKg?.toDouble(),
            exerciseLevel = toExerciseLevel(p.exerciseFreqPerWeek),
            goal = p.goal,
            targetWeightKg = p.targetWeightKg?.toDouble(),
            referralSource = p.referralSource,
            locale = localeTag
        )
        val resp = api.upsertMyProfile(req)
        runCatching { store.setHasServerProfile(true) }
        resp
    }

    /** 回訪用戶：只更新語言（其餘欄位沿用 Server 值，避免被 null 蓋掉） */
    suspend fun updateLocaleOnly(newLocale: String): Result<UserProfileDto> = runCatching {
        val cur = api.getMyProfile() // 需已登入
        val req = UpsertProfileRequest(
            gender = cur.gender,
            age = cur.age,
            heightCm = cur.heightCm,
            weightKg = cur.weightKg,
            exerciseLevel = cur.exerciseLevel,
            goal = cur.goal,
            targetWeightKg = cur.targetWeightKg,
            referralSource = cur.referralSource,
            locale = newLocale
        )
        api.upsertMyProfile(req)
    }
}
