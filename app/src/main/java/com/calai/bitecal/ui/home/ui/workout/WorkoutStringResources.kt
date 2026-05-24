package com.calai.bitecal.ui.home.ui.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.calai.bitecal.R
import com.calai.bitecal.data.workout.api.PresetWorkoutDto
import java.util.Locale

@Composable
internal fun PresetWorkoutDto.localizedWorkoutName(): String {
    return localizedWorkoutName(
        activityId = activityId,
        iconKey = iconKey,
        rawName = name
    )
}

internal fun PresetWorkoutDto.workoutNameStringRes(): Int? {
    return workoutNameStringRes(
        activityId = activityId,
        iconKey = iconKey,
        rawName = name
    )
}

@Composable
internal fun localizedWorkoutName(
    activityId: Long? = null,
    iconKey: String? = null,
    rawName: String? = null
): String {
    val stringRes = workoutNameStringRes(
        activityId = activityId,
        iconKey = iconKey,
        rawName = rawName
    )

    return if (stringRes != null) {
        stringResource(stringRes)
    } else {
        rawName.orEmpty()
    }
}

internal fun workoutNameStringRes(
    activityId: Long?,
    iconKey: String?,
    rawName: String?
): Int? {
    return when (activityId) {
        1L -> R.string.workout_preset_walking
        2L -> R.string.workout_preset_running
        3L -> R.string.workout_preset_cycling
        4L -> R.string.workout_preset_swimming
        5L -> R.string.workout_preset_hiking
        6L -> R.string.workout_preset_aerobic_exercise
        7L -> R.string.workout_preset_strength_training
        8L -> R.string.workout_preset_weight_training
        9L -> R.string.workout_preset_basketball
        10L -> R.string.workout_preset_soccer
        11L -> R.string.workout_preset_tennis
        12L -> R.string.workout_preset_yoga
        13L -> R.string.workout_preset_badminton
        14L -> R.string.workout_preset_baseball
        15L -> R.string.workout_preset_softball
        16L -> R.string.workout_preset_volleyball
        17L -> R.string.workout_preset_table_tennis
        18L -> R.string.workout_preset_dancing_general
        19L -> R.string.workout_preset_zumba
        20L -> R.string.workout_preset_pilates
        else -> workoutNameStringResByKey(iconKey)
            ?: workoutNameStringResByKey(rawName)
    }
}

private fun workoutNameStringResByKey(value: String?): Int? {
    return when (value.normalizedWorkoutKey()) {
        "walk", "walking" -> R.string.workout_preset_walking
        "run", "running" -> R.string.workout_preset_running
        "bike", "cycling" -> R.string.workout_preset_cycling
        "swimming" -> R.string.workout_preset_swimming
        "hiking" -> R.string.workout_preset_hiking
        "aerobic_exercise" -> R.string.workout_preset_aerobic_exercise
        "strength", "strength_training" -> R.string.workout_preset_strength_training
        "weight_training" -> R.string.workout_preset_weight_training
        "basketball" -> R.string.workout_preset_basketball
        "soccer" -> R.string.workout_preset_soccer
        "tennis" -> R.string.workout_preset_tennis
        "yoga" -> R.string.workout_preset_yoga
        "badminton" -> R.string.workout_preset_badminton
        "baseball" -> R.string.workout_preset_baseball
        "softball" -> R.string.workout_preset_softball
        "volleyball" -> R.string.workout_preset_volleyball
        "table_tennis", "tabletennis", "ping_pong", "pingpong" -> R.string.workout_preset_table_tennis
        "dancing", "dancing_general", "dance", "dance_general" -> R.string.workout_preset_dancing_general
        "zumba" -> R.string.workout_preset_zumba
        "pilates" -> R.string.workout_preset_pilates
        else -> null
    }
}

private fun String?.normalizedWorkoutKey(): String {
    return this
        ?.trim()
        ?.lowercase(Locale.US)
        ?.replace("&", "and")
        ?.replace(Regex("[^a-z0-9]+"), "_")
        ?.trim('_')
        .orEmpty()
}
