package com.calai.app.data.profile

import android.util.Log
import com.calai.app.data.auth.store.UserProfileStore
import com.calai.app.ui.onboarding.exercise.bucketFreq
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

    suspend fun existsOnServer(): Boolean = try {
        api.getMyProfile(); true
    } catch (e: HttpException) {
        when (e.code()) { 401, 404 -> false; else -> throw e }
    } catch (e: IOException) { throw e }

    /** 同時支援 raw 次數(0..7+) 與 bucket code(0/2/4/6/7) 的對映 */
    private fun toExerciseLevel(freqOrBucket: Int?): String? = when (freqOrBucket) {
        null -> null
        in Int.MIN_VALUE..0 -> "sedentary"   // 0 或更小
        in 1..3              -> "light"       // 1–3（含 bucket=2）
        in 4..5              -> "moderate"    // 4–5（含 bucket=4）
        6                    -> "active"      // 6（含 bucket=6）
        else                 -> "very_active" // 7 以上（含 bucket=7）
    }

    suspend fun upsertFromLocal(): Result<UserProfileDto> = runCatching {
        val p = store.snapshot()
        Log.d("ProfileRepo", "freq=${p.exerciseFreqPerWeek} -> level=${toExerciseLevel(p.exerciseFreqPerWeek)}")
        // ★ 關鍵：若 DataStore 沒有/空字串，就退回裝置語系，確保不為 null
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
        api.upsertMyProfile(req)
    }
}
