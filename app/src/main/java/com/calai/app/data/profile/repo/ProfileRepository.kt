package com.calai.app.data.profile.repo

import android.util.Log
import com.calai.app.data.common.RepoInvalidationBus
import com.calai.app.data.profile.api.ProfileApi
import com.calai.app.data.profile.api.UpdateGoalWeightRequest
import com.calai.app.data.profile.api.UpsertProfileRequest
import com.calai.app.data.profile.api.UserProfileDto
import retrofit2.HttpException
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class ProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val store: UserProfileStore,
    private val bus: RepoInvalidationBus
) {
    private companion object {
        const val PROFILE_SOURCE_ONBOARDING = "ONBOARDING"
    }

    /** 試取雲端 Profile；若能取到，順便把本機 hasServerProfile 標成 true */
    suspend fun existsOnServer(): Boolean = try {
        api.getMyProfile()
        runCatching { store.setHasServerProfile(true) }
        true
    } catch (e: HttpException) {
        when (e.code()) {
            401, 404 -> false
            else -> throw e
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
            else -> throw e
        }
    } catch (e: IOException) {
        throw e
    }

    /** 將伺服器 locale（若有且非空）同步到本機 DataStore。回傳是否有同步 */
    @Suppress("unused")
    suspend fun syncLocaleFromServerToStore(): Boolean {
        val p = getServerProfileOrNull() ?: return false
        val tag = p.locale?.takeIf { it.isNotBlank() } ?: return false
        runCatching { store.setLocaleTag(tag) }

        // ✅ store 寫入後也發出 profile invalidation（讓 UI 有機會強制 refresh）
        bus.invalidateProfile()
        return true
    }

    /** 同時支援 raw 次數(0..7+) 與 bucket(0/2/4/6/7) 的對映 */
    private fun toExerciseLevel(freqOrBucket: Int?): String? = when (freqOrBucket) {
        null -> null
        in Int.MIN_VALUE..0 -> "sedentary"   // 0 或更小
        in 1..3 -> "light"                  // 1–3（含 bucket=2）
        in 4..5 -> "moderate"               // 4–5（含 bucket=4）
        6 -> "active"                       // 6（含 bucket=6）
        else -> "very_active"               // 7 以上（含 bucket=7）
    }

    private fun quantize1dpFromFloat(v: Float): Double {
        // 先變成「以 0.1 為單位的整數」再除回來，避免 178.199996 這種誤差被 floor 吃掉
        return (v * 10f).roundToInt() / 10.0
    }

    private fun quantize(v: Double): Double =
        (v * 10.0).roundToInt() / 10.0

    /**
     * 上傳策略：
     * - 身高：一律送 cm；若使用者是 ft/in，就另外送 feet/inches。
     * - 體重：只送「使用者選的主單位」，另一個單位交給 Server 自己換算。
     */
    suspend fun upsertFromLocal(): Result<UserProfileDto> = runCatching {
        val p = store.snapshot()

        val localeTag = p.locale?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().toLanguageTag()

        // ✅ height cm：若走 cm 模式，就量化後再送
        val heightCmToSend: Double? = when (p.heightUnit) {
            UserProfileStore.HeightUnit.FT_IN -> null
            else -> p.heightCm?.toDouble()?.let { quantize(it) }
        }

        val (feet, inches) = when (p.heightUnit) {
            UserProfileStore.HeightUnit.FT_IN -> p.heightFeet to p.heightInches
            else -> null to null
        }

        // 原始 current / goal 體重
        val rawWeightKg: Double? = p.weightKg?.toDouble()
        val rawWeightLbs: Double? = p.weightLbs?.toDouble()
        val rawGoalKg: Double? = p.goalWeightKg?.toDouble()
        val rawGoalLbs: Double? = p.goalWeightLbs?.toDouble()

        // 使用者偏好的主單位
        val weightUnit = p.weightUnit ?: UserProfileStore.WeightUnit.KG
        val goalWeightUnit = p.goalWeightUnit ?: weightUnit

        // ✅ unitPreference / workoutsPerWeek（給後端 AUTO 計算）
        val unitPrefToSend = (p.weightUnit ?: UserProfileStore.WeightUnit.KG).name
        val workoutsToSend = p.exerciseFreqPerWeek?.coerceIn(0, 7)

        // --- current：只送主單位 ---
        val (weightKgToSend, weightLbsToSend) = when (weightUnit) {
            UserProfileStore.WeightUnit.KG -> rawWeightKg to null
            UserProfileStore.WeightUnit.LBS -> null to rawWeightLbs
        }

        // --- goal：只送主單位 ---
        val (goalKgToSend, goalLbsToSend) = when (goalWeightUnit) {
            UserProfileStore.WeightUnit.KG -> rawGoalKg to null
            UserProfileStore.WeightUnit.LBS -> null to rawGoalLbs
        }

        Log.d("ProfileRepo", "upsert heightUnit=${p.heightUnit} heightCm(raw)=${p.heightCm} heightCmToSend=$heightCmToSend ft=$feet in=$inches")

        val req = UpsertProfileRequest(
            gender = p.gender,
            age = p.ageYears,
            heightCm = heightCmToSend,
            heightFeet = feet,
            heightInches = inches,
            weightKg = weightKgToSend,
            weightLbs = weightLbsToSend,
            exerciseLevel = toExerciseLevel(workoutsToSend),
            goal = p.goal,
            goalWeightKg = goalKgToSend,
            goalWeightLbs = goalLbsToSend,
            dailyStepGoal = p.dailyStepGoal,
            referralSource = p.referralSource,
            locale = localeTag,
            unitPreference = unitPrefToSend,
            workoutsPerWeek = workoutsToSend
        )

        val resp = api.upsertMyProfile(req, source = null)
        runCatching { store.setHasServerProfile(true) }

        // ✅ 寫入成功 -> invalidate
        bus.invalidateProfile()

        resp
    }

    /**
     * ✅ 給 NavHost / ViewModel 用：如果你外層用 runCatching，要能抓到失敗就用這個
     * （因為 upsertFromLocal() 自己回傳 Result，外層 runCatching 可能抓不到 HttpException）
     */
    @Suppress("unused")
    suspend fun upsertFromLocalOrThrow(): UserProfileDto =
        upsertFromLocal().getOrThrow()

    /** 只更新 locale（語系切換時使用） */
    suspend fun updateLocaleOnly(newLocale: String): Result<UserProfileDto> = runCatching {
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
            dailyStepGoal = null,
            referralSource = null,
            locale = newLocale,
            unitPreference = null,
            workoutsPerWeek = null
        )
        val resp = api.upsertMyProfile(req, source = null)

        // ✅ 寫入成功 -> invalidate
        bus.invalidateProfile()

        resp
    }

    @Suppress("unused")
    suspend fun updateLocaleOnlyOrThrow(newLocale: String): UserProfileDto =
        updateLocaleOnly(newLocale).getOrThrow()

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
        val trimmed = round1Floor(value)
        val body = UpdateGoalWeightRequest(
            value = trimmed,
            unit = unit.name // "KG" or "LBS"
        )
        val resp = api.updateGoalWeight(body)

        // 同步回本機 DataStore（快照用）
        runCatching {
            resp.goalWeightKg?.let { store.setGoalWeightKg(it.toFloat()) }
            resp.goalWeightLbs?.let { store.setGoalWeightLbs(it.toFloat()) }
        }

        // ✅ 寫入成功 -> invalidate
        bus.invalidateProfile()

        resp
    }

    @Suppress("unused")
    suspend fun updateGoalWeightOrThrow(
        value: Double,
        unit: UserProfileStore.WeightUnit
    ): UserProfileDto = updateGoalWeight(value, unit).getOrThrow()

    /**
     * ✅ 更新「Starting Weight」：寫入 DB user_profiles.weight_kg & weight_lbs
     * 做法：沿用 PUT /users/me/profile，只送主單位，另一單位交給後端換算。
     * - source=null：避免觸發 onboarding 才允許的宏量重算
     */
    suspend fun updateStartingWeight(
        value: Double,
        unit: UserProfileStore.WeightUnit
    ): Result<UserProfileDto> = runCatching {

        // ✅ 與你其他 profile 寫入一致：先在 client 做一次 0.1 無條件捨去
        val trimmed = round1Floor(value)

        val (kgToSend, lbsToSend) = when (unit) {
            UserProfileStore.WeightUnit.KG -> trimmed to null
            UserProfileStore.WeightUnit.LBS -> null to trimmed
        }

        val req = UpsertProfileRequest(
            gender = null,
            age = null,
            heightCm = null,
            heightFeet = null,
            heightInches = null,

            // ✅ 關鍵：只更新 starting weight
            weightKg = kgToSend,
            weightLbs = lbsToSend,

            exerciseLevel = null,
            goal = null,
            goalWeightKg = null,
            goalWeightLbs = null,
            dailyStepGoal = null,
            referralSource = null,
            locale = null,
            unitPreference = null,
            workoutsPerWeek = null
        )

        val resp = api.upsertMyProfile(req, source = null)

        // ✅ 成功後同步回本機 DataStore（讓 UI 立即更新 & fallback 更準）
        runCatching {
            resp.weightKg?.let { store.setWeightKg(roundKg1(it)) }
            resp.weightLbs?.let { store.setWeightLbs(roundLbs1(it)) }
        }

        // ✅ 成功：讓依賴 profile 的 VM（包含 WeightVM merge(bus.profile)）可以強制 refresh
        bus.invalidateProfile()

        resp
    }

    /**
     * 下載 server profile 並寫入本機 store（只同步數值，不改偏好）
     * ✅ 寫入 store 後也 invalidate，讓 UI 視需要強制刷新。
     */
    suspend fun syncServerProfileToStore(): Boolean {
        val p: UserProfileDto = getServerProfileOrNull() ?: return false

        runCatching {
            p.gender?.let { store.setGender(it) }
            p.age?.let { store.setAge(it) }
            p.locale?.let { store.setLocaleTag(it) }
            p.referralSource?.let { store.setReferralSource(it) }
            p.goal?.let { store.setGoal(it) }
            p.dailyStepGoal?.let { store.setDailyStepGoal(it) }
            p.waterMl?.let { store.setWaterGoalMl(it) }
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
            p.dailyWorkoutGoalKcal?.let { store.applyRemoteDailyWorkoutGoal(it) }
        }

        // ✅ 寫入 store 完 -> invalidate（讓依賴 profile 的 VM 可 force refresh）
        bus.invalidateProfile()

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
            locale = null,
            unitPreference = null,
            workoutsPerWeek = null
        )
        val resp = api.upsertMyProfile(req, source = null)

        // 同步回本機（讓下次進來預設選項更準）
        runCatching { resp.gender?.let { store.setGender(it) } }

        // ✅ 寫入成功 -> invalidate
        bus.invalidateProfile()

        resp
    }

    @Suppress("unused")
    suspend fun updateGenderOnlyOrThrow(newGender: String): UserProfileDto =
        updateGenderOnly(newGender).getOrThrow()

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
            locale = null,
            unitPreference = null,
            workoutsPerWeek = null
        )
        val resp = api.upsertMyProfile(req, source = null)
        runCatching { store.setDailyStepGoal(resp.dailyStepGoal ?: safe) }

        // ✅ 寫入成功 -> invalidate
        bus.invalidateProfile()

        resp
    }

    @Suppress("unused")
    suspend fun updateDailyStepGoalOnlyOrThrow(v: Int): UserProfileDto =
        updateDailyStepGoalOnly(v).getOrThrow()

    /**
     * 只抓 dailyStepGoal（從 DB/Server）
     * - 401/404：視為沒有資料，回 Success(null)
     * - 其他 Http error：回 Failure
     * - IOException：回 Failure（讓 ViewModel 決定要不要忽略）
     */
    suspend fun getDailyStepGoalFromServer(): Result<Int?> = try {
        val p = api.getMyProfile()
        Result.success(p.dailyStepGoal)
    } catch (e: HttpException) {
        when (e.code()) {
            401, 404 -> Result.success(null)
            else -> Result.failure(e)
        }
    } catch (e: IOException) {
        Result.failure(e)
    }

    /**
     * ✅ Onboarding 專用：送 X-Profile-Source: ONBOARDING
     * 後端才會允許重算 kcal / P / C / F（planMode=AUTO 時）。
     */
    suspend fun upsertFromLocalForOnboarding(): Result<UserProfileDto> = runCatching {
        val p = store.snapshot()

        val localeTag = p.locale?.takeIf { it.isNotBlank() }
            ?: Locale.getDefault().toLanguageTag()

        val heightCmToSend: Double? = when (p.heightUnit) {
            UserProfileStore.HeightUnit.FT_IN -> null
            else -> p.heightCm?.let { quantize1dpFromFloat(it) }  // ✅ 178.2f -> 178.2
        }

        val (feet, inches) = when (p.heightUnit) {
            UserProfileStore.HeightUnit.FT_IN -> p.heightFeet to p.heightInches
            else -> null to null
        }

        val rawWeightKg: Double? = p.weightKg?.toDouble()
        val rawWeightLbs: Double? = p.weightLbs?.toDouble()
        val rawGoalKg: Double? = p.goalWeightKg?.toDouble()
        val rawGoalLbs: Double? = p.goalWeightLbs?.toDouble()

        val weightUnit = p.weightUnit ?: UserProfileStore.WeightUnit.KG
        val goalWeightUnit = p.goalWeightUnit ?: weightUnit

        val unitPrefToSend = (p.weightUnit ?: UserProfileStore.WeightUnit.KG).name
        val workoutsToSend = p.exerciseFreqPerWeek?.coerceIn(0, 7)

        val (weightKgToSend, weightLbsToSend) = when (weightUnit) {
            UserProfileStore.WeightUnit.KG -> rawWeightKg to null
            UserProfileStore.WeightUnit.LBS -> null to rawWeightLbs
        }

        val (goalKgToSend, goalLbsToSend) = when (goalWeightUnit) {
            UserProfileStore.WeightUnit.KG -> rawGoalKg to null
            UserProfileStore.WeightUnit.LBS -> null to rawGoalLbs
        }

        val req = UpsertProfileRequest(
            gender = p.gender,
            age = p.ageYears,
            heightCm = heightCmToSend,
            heightFeet = feet,
            heightInches = inches,
            weightKg = weightKgToSend,
            weightLbs = weightLbsToSend,
            exerciseLevel = toExerciseLevel(workoutsToSend),
            goal = p.goal,
            goalWeightKg = goalKgToSend,
            goalWeightLbs = goalLbsToSend,
            dailyStepGoal = p.dailyStepGoal,
            referralSource = p.referralSource,
            locale = localeTag,
            unitPreference = unitPrefToSend,
            workoutsPerWeek = workoutsToSend
        )

        // ✅ 關鍵差異：帶 ONBOARDING header
        val resp = api.upsertMyProfile(req, source = PROFILE_SOURCE_ONBOARDING)

        runCatching { store.setHasServerProfile(true) }

        // ✅ 寫入成功 -> invalidate
        bus.invalidateProfile()

        resp
    }

    @Suppress("unused")
    suspend fun upsertFromLocalForOnboardingOrThrow(): UserProfileDto =
        upsertFromLocalForOnboarding().getOrThrow()


    /**
     * 只抓 waterMl（從 DB/Server）
     * - 401/404：視為沒有資料，回 Success(null)
     * - 其他 Http error：回 Failure
     */
    suspend fun getWaterGoalFromServer(): Result<Int?> = try {
        val p = api.getMyProfile()
        Result.success(p.waterMl)
    } catch (e: HttpException) {
        when (e.code()) {
            401, 404 -> Result.success(null)
            else -> Result.failure(e)
        }
    } catch (e: IOException) {
        Result.failure(e)
    }

    /**
     * ✅ 只更新每日目標飲水量（ml）
     * 後端規則：非 null 才覆寫，所以其他欄位一律給 null，不會蓋掉資料。
     */
    suspend fun updateDailyWaterGoalOnly(waterMl: Int): Result<UserProfileDto> = runCatching {
        val safe = waterMl.coerceIn(0, 20000)

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
            dailyStepGoal = null,
            referralSource = null,
            locale = null,
            unitPreference = null,
            workoutsPerWeek = null,
            waterMl = safe
        )

        val resp = api.upsertMyProfile(req, source = null)

        // ✅ 同步回本機 DataStore（讓 Water 模組立即吃到）
        runCatching { store.setWaterGoalMl(resp.waterMl ?: safe) }

        // ✅ 寫入成功 -> invalidate
        bus.invalidateProfile()

        resp
    }

    suspend fun getDailyWorkoutGoalFromServer(): Result<Int?> = try {
        val p = api.getMyProfile()
        Result.success(p.dailyWorkoutGoalKcal)
    } catch (e: HttpException) {
        when (e.code()) {
            401, 404 -> Result.success(null)
            else -> Result.failure(e)
        }
    } catch (e: IOException) {
        Result.failure(e)
    }

    suspend fun updateDailyWorkoutGoalOnly(kcal: Int): Result<UserProfileDto> = runCatching {
        val safe = kcal.coerceIn(0, 5000)

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
            dailyStepGoal = null,
            referralSource = null,
            locale = null,
            unitPreference = null,
            workoutsPerWeek = null,
            waterMl = null,
            dailyWorkoutGoalKcal = safe
        )

        val resp = api.upsertMyProfile(req, source = null)

        runCatching { store.applyRemoteDailyWorkoutGoal(resp.dailyWorkoutGoalKcal ?: safe) }

        bus.invalidateProfile()
        resp
    }

}
