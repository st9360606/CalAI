package com.calai.app.data.profile.repo

import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.profile.api.UpdateGoalWeightRequest
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
        when (e.code()) {
            401, 404 -> false
            else     -> throw e
        }
    } catch (e: IOException) {
        throw e
    }

    /** 取伺服器 Profile；401/404 視為沒有，其他錯誤拋出或回 null（保守） */
    suspend fun getServerProfileOrNull(): UserProfileDto? = try {
        api.getMyProfile()
    } catch (e: HttpException) {
        when (e.code()) {
            401, 404 -> null
            else     -> throw e
        }
    } catch (e: IOException) {
        null
    }

    /** 將伺服器 locale（若有且非空）同步到本機 DataStore。回傳是否有同步 */
    suspend fun syncLocaleFromServerToStore(): Boolean {
        val p = getServerProfileOrNull() ?: return false
        val tag = p.locale?.takeIf { it.isNotBlank() } ?: return false
        runCatching { store.setLocaleTag(tag) }
        return true
    }

    /** 同時支援 raw 次數(0..7+) 與 bucket(0/2/4/6/7) 的對映 */
    private fun toExerciseLevel(freqOrBucket: Int?): String? = when (freqOrBucket) {
        null                  -> null
        in Int.MIN_VALUE..0   -> "sedentary"    // 0 或更小
        in 1..3               -> "light"        // 1–3（含 bucket=2）
        in 4..5               -> "moderate"     // 4–5（含 bucket=4）
        6                     -> "active"       // 6（含 bucket=6）
        else                  -> "very_active"  // 7 以上（含 bucket=7）
    }

    /**
     * 上傳策略：
     * - 身高：一律送 cm；若使用者是 ft/in，就另外送 feet/inches。
     * - 體重：只送「使用者選的主單位」，另一個單位交給 Server 自己換算。
     */
    suspend fun upsertFromLocal(): Result<UserProfileDto> = runCatching {
        val p = store.snapshot()

        val localeTag = p.locale?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().toLanguageTag()

        // ★ 身高 cm：保持 0.1 精度（無條件捨去）
        val heightCmToSend: Double? = when (p.heightUnit) {
            UserProfileStore.HeightUnit.FT_IN -> null // ✅ ft/in 模式不送 cm
            else -> p.heightCm?.toDouble()?.let { round1Floor(it) }
        }

        val (feet, inches) = when (p.heightUnit) {
            UserProfileStore.HeightUnit.FT_IN ->
                p.heightFeet to p.heightInches
            else ->
                null to null
        }

        // 原始 current / goal 體重
        val rawWeightKg:   Double? = p.weightKg?.toDouble()
        val rawWeightLbs:  Double? = p.weightLbs?.toDouble()
        val rawGoalKg:   Double? = p.goalWeightKg?.toDouble()
        val rawGoalLbs:  Double? = p.goalWeightLbs?.toDouble()

        // 使用者偏好的主單位
        val weightUnit = p.weightUnit ?: UserProfileStore.WeightUnit.KG
        val goalWeightUnit = p.goalWeightUnit ?: weightUnit

        // --- current：只送主單位 ---
        val (weightKgToSend, weightLbsToSend) = when (weightUnit) {
            UserProfileStore.WeightUnit.KG  -> rawWeightKg  to null
            UserProfileStore.WeightUnit.LBS -> null         to rawWeightLbs
        }

        // --- goal：只送主單位 ---
        val (goalKgToSend, goalLbsToSend) = when (goalWeightUnit) {
            UserProfileStore.WeightUnit.KG  -> rawGoalKg  to null
            UserProfileStore.WeightUnit.LBS -> null         to rawGoalLbs
        }

        val req = UpsertProfileRequest(
            gender = p.gender,
            age = p.ageYears,
            heightCm = heightCmToSend,
            heightFeet = feet,
            heightInches = inches,
            weightKg = weightKgToSend,
            weightLbs = weightLbsToSend,
            exerciseLevel = toExerciseLevel(p.exerciseFreqPerWeek),
            goal = p.goal,
            goalWeightKg = goalKgToSend,
            goalWeightLbs = goalLbsToSend,
            dailyStepGoal = p.dailyStepGoal,
            referralSource = p.referralSource,
            locale = localeTag
        )

        val resp = api.upsertMyProfile(req)
        runCatching { store.setHasServerProfile(true) }
        resp
    }

    /** 只更新 locale（語系切換時使用） */
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
            goalWeightKg = cur.goalWeightKg,
            goalWeightLbs = cur.goalWeightLbs,
            dailyStepGoal = cur.dailyStepGoal,
            referralSource = cur.referralSource,
            locale = newLocale
        )
        api.upsertMyProfile(req)
    }

    /**
     * 更新目標體重：
     * - unit 由前端決定（KG / LBS）
     * - value 先在 client 做一次「無條件捨去到小數第 1 位」，再給後端
     *   （後端仍會再 clamp + floor，一致更安全）
     */
    suspend fun updateGoalWeight(
        value: Double,
        unit: UserProfileStore.WeightUnit
    ): Result<UserProfileDto> = runCatching {
        val trimmed = round1Floor(value)   // e.g. 73.04 → 73.0, 152.09 → 152.0
        val body = UpdateGoalWeightRequest(
            value = trimmed,
            unit = unit.name               // "KG" or "LBS"
        )
        val resp = api.updateGoalWeight(body)

        // 同步回本機 DataStore（快照用）
        runCatching {
            resp.goalWeightKg?.let { store.setGoalWeightKg(it.toFloat()) }
            resp.goalWeightLbs?.let { store.setGoalWeightLbs(it.toFloat()) }
        }
        resp
    }

    suspend fun syncServerProfileToStore(): Boolean {
        val p: UserProfileDto = getServerProfileOrNull() ?: return false

        runCatching {
            p.gender?.let { store.setGender(it) }
            p.age?.let { store.setAge(it) }
            p.locale?.let { store.setLocaleTag(it) }
            p.referralSource?.let { store.setReferralSource(it) }
            p.goal?.let { store.setGoal(it) }
            p.dailyStepGoal?.let { store.setDailyStepGoal(it) } // ✅ NEW（你需在 store 加 setter）
            // height：有 feet/inches 就視為英制，否則用 cm
            if (p.heightFeet != null && p.heightInches != null) {
                store.setHeightUnit(UserProfileStore.HeightUnit.FT_IN)
                store.setHeightImperial(p.heightFeet, p.heightInches)
                p.heightCm?.let { store.setHeightCm(roundCm1(it)) }
            } else {
                p.heightCm?.let {
                    store.setHeightUnit(UserProfileStore.HeightUnit.CM)
                    store.setHeightCm(roundCm1(it))
                    store.clearHeightImperial()
                }
            }

            // weight：兩制都寫入數值，但不要動 weightUnit/goalWeightUnit（偏好留在本機）
            p.weightKg?.let { store.setWeightKg(roundKg1(it)) }
            p.weightLbs?.let { store.setWeightLbs(roundLbs1(it)) }

            p.goalWeightKg?.let { store.setGoalWeightKg(roundKg1(it)) }
            p.goalWeightLbs?.let { store.setGoalWeightLbs(roundLbs1(it)) }
        }

        return true
    }

    suspend fun updateGenderOnly(newGender: String): Result<UserProfileDto> = runCatching {
        val normalized = newGender.trim()
        val req = UpsertProfileRequest(
            gender = normalized,
            age = null,
            heightCm = null,
            heightFeet = null,
            heightInches = null,
            weightKg = null,
            weightLbs = null,
            exerciseLevel = null,
            goal = null,
            goalWeightKg = null,
            goalWeightLbs = null,
            dailyStepGoal = null,
            referralSource = null,
            locale = null
        )
        val resp = api.upsertMyProfile(req)
        // 同步回本機（讓下次進來預設選項更準）
        runCatching { resp.gender?.let { store.setGender(it) } }
        resp
    }

    /**
     * ✅ 只更新 dailyStepGoal（沿用既有 upsert endpoint）
     * 後端規則：非 null 才覆寫，所以其他欄位一律給 null，不會蓋掉資料。
     */
    suspend fun updateDailyStepGoalOnly(v: Int): Result<UserProfileDto> = runCatching {
        val safe = v.coerceIn(0, 200000)
        val req = UpsertProfileRequest(
            gender = null,
            age = null,
            heightCm = null,
            heightFeet = null,
            heightInches = null,
            weightKg = null,
            weightLbs = null,
            exerciseLevel = null,
            goal = null,
            goalWeightKg = null,
            goalWeightLbs = null,
            dailyStepGoal = safe,
            referralSource = null,
            locale = null
        )
        val resp = api.upsertMyProfile(req)
        // ✅ 同步回本機（優先用伺服器回來的值）
        runCatching {
            store.setDailyStepGoal(resp.dailyStepGoal ?: safe)
        }
        resp
    }

}
