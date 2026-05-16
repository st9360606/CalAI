package com.calai.bitecal.ui.home.workoutgate

import kotlinx.coroutines.sync.Mutex

enum class WorkoutPremiumGateDecision {
    OpenWorkout,
    OpenSubscription,
    VerificationFailed
}

class WorkoutPremiumGate(
    private val hasActivePremiumAccess: suspend () -> Boolean
) {
    private val inFlight = Mutex()

    suspend fun check(): WorkoutPremiumGateDecision? {
        if (!inFlight.tryLock()) return null

        return try {
            val hasAccess = runCatching {
                hasActivePremiumAccess()
            }.getOrElse {
                return WorkoutPremiumGateDecision.VerificationFailed
            }

            if (hasAccess) {
                WorkoutPremiumGateDecision.OpenWorkout
            } else {
                WorkoutPremiumGateDecision.OpenSubscription
            }
        } finally {
            inFlight.unlock()
        }
    }
}
