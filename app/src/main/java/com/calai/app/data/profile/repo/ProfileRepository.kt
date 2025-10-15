package com.calai.app.data.profile.repo


import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.profile.api.UpsertProfileRequest
import com.calai.app.data.profile.api.UserProfileDto
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

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

    /** 上傳策略：一律送出 cm/kg；若使用者當下選英制，**同時**送 ft/in 或 lbs */
    suspend fun upsertFromLocal(): Result<UserProfileDto> = runCatching {
        val p = store.snapshot()

        // 取得 locale
        val localeTag = p.locale?.takeIf { it.isNotBlank() } ?: Locale.getDefault().toLanguageTag()

        // 身高：cm 一律送；英制在使用者選 FT_IN 且有值時才送
        val heightCm = p.heightCm?.toDouble()
        val (feet, inches) = when (p.heightUnit) {
            UserProfileStore.HeightUnit.FT_IN -> {
                // 優先用使用者實際輸入（若有），否則由 cm 推導
                val f = p.heightFeet ?: (heightCm?.let { cmToFeetInches(it.toInt()).first })
                val i = p.heightInches ?: (heightCm?.let { cmToFeetInches(it.toInt()).second })
                f to i
            }
            else -> null to null
        }

        // 體重：kg 一律送；英制在使用者選 LBS 且有值時才送
        val weightKg = p.weightKg?.toDouble()
        val weightLbs = when (p.weightUnit) {
            UserProfileStore.WeightUnit.LBS -> p.weightLbs ?: weightKg?.let { kgToLbsInt(it) }
            else -> null
        }

        // 目標體重：同上
        val targetWeightKg = p.targetWeightKg?.toDouble()
        val targetWeightLbs = when (p.targetWeightUnit) {
            UserProfileStore.WeightUnit.LBS -> p.targetWeightLbs ?: targetWeightKg?.let { kgToLbsInt(it) }
            else -> null
        }

        val req = UpsertProfileRequest(
            gender = p.gender,
            age = p.ageYears,
            heightCm = heightCm,
            heightFeet = feet,
            heightInches = inches,
            weightKg = weightKg,
            weightLbs = weightLbs,
            exerciseLevel = toExerciseLevel(p.exerciseFreqPerWeek),
            goal = p.goal,
            targetWeightKg = targetWeightKg,
            targetWeightLbs = targetWeightLbs,
            referralSource = p.referralSource,
            locale = localeTag
        )
        val resp = api.upsertMyProfile(req)
        runCatching { store.setHasServerProfile(true) }
        resp
    }

    suspend fun updateLocaleOnly(newLocale: String): Result<UserProfileDto> = runCatching {
        val cur = api.getMyProfile()
        val req = UpsertProfileRequest(
            gender = cur.gender,
            age = cur.age,
            heightCm = cur.heightCm,
            heightFeet = cur.heightFeet,
            heightInches = cur.heightInches,
            weightKg = cur.weightKg,
            weightLbs = cur.weightLbs,
            exerciseLevel = cur.exerciseLevel,
            goal = cur.goal,
            targetWeightKg = cur.targetWeightKg,
            targetWeightLbs = cur.targetWeightLbs,
            referralSource = cur.referralSource,
            locale = newLocale
        )
        api.upsertMyProfile(req)
    }
}