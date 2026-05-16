package com.calai.bitecal.ui.home.workoutgate

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutPremiumGateTest {

    @Test
    fun premiumClickOpensWorkoutSheet() = runTest {
        val gate = WorkoutPremiumGate { true }

        assertEquals(
            WorkoutPremiumGateDecision.OpenWorkout,
            gate.check()
        )
    }

    @Test
    fun trialClickOpensWorkoutSheet() = runTest {
        val gate = WorkoutPremiumGate { true }

        assertEquals(
            WorkoutPremiumGateDecision.OpenWorkout,
            gate.check()
        )
    }

    @Test
    fun freeClickOpensWorkoutSubscription() = runTest {
        val gate = WorkoutPremiumGate { false }

        assertEquals(
            WorkoutPremiumGateDecision.OpenSubscription,
            gate.check()
        )
    }

    @Test
    fun trialExpiredClickOpensWorkoutSubscription() = runTest {
        val gate = WorkoutPremiumGate { false }

        assertEquals(
            WorkoutPremiumGateDecision.OpenSubscription,
            gate.check()
        )
    }

    @Test
    fun entitlementFailureDoesNotOpenWorkoutSheet() = runTest {
        val gate = WorkoutPremiumGate { error("network failed") }

        assertEquals(
            WorkoutPremiumGateDecision.VerificationFailed,
            gate.check()
        )
    }

    @Test
    fun repeatedClickWhileCheckingRunsOnlyOneGate() = runTest {
        var checkCount = 0
        val enteredCheck = CompletableDeferred<Unit>()
        val releaseCheck = CompletableDeferred<Unit>()
        val gate = WorkoutPremiumGate {
            checkCount += 1
            enteredCheck.complete(Unit)
            releaseCheck.await()
            true
        }

        val first = async { gate.check() }
        enteredCheck.await()
        val second = async { gate.check() }

        assertNull(second.await())

        releaseCheck.complete(Unit)

        assertEquals(
            WorkoutPremiumGateDecision.OpenWorkout,
            first.await()
        )
        assertEquals(1, checkCount)
    }

    @Test
    fun purchaseSuccessRequestsWorkoutSheetOnce() {
        val purchaseSuccessTick = 123L

        assertTrue(WorkoutSheetOpenRequest.shouldOpen(purchaseSuccessTick))
        assertFalse(WorkoutSheetOpenRequest.shouldOpen(WorkoutSheetOpenRequest.ConsumedTick))
    }

    @Test
    fun closingPaywallDoesNotRequestWorkoutSheet() {
        assertFalse(WorkoutSheetOpenRequest.shouldOpen(WorkoutSheetOpenRequest.ConsumedTick))
    }
}
