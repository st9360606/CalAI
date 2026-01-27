package com.calai.bitecal.data.profile.repo

import com.calai.bitecal.data.common.RepoInvalidationBus
import com.calai.bitecal.data.profile.api.ProfileApi
import com.calai.bitecal.data.profile.api.UpsertProfileRequest
import com.calai.bitecal.data.profile.api.UserProfileDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ProfileRepositoryStartingWeightTest {

    @Test
    fun `updateStartingWeight KG should send weightKg only and sync store`() = runTest {
        val api = mockk<ProfileApi>()
        val store = mockk<UserProfileStore>(relaxed = true)
        val bus = mockk<RepoInvalidationBus>(relaxed = true)

        val captured = mutableListOf<UpsertProfileRequest>()
        coEvery { api.upsertMyProfile(capture(captured), any()) } returns UserProfileDto(
            weightKg = 70.1,
            weightLbs = 154.5
        )

        val repo = ProfileRepository(api = api, store = store, bus = bus)

        val res = repo.updateStartingWeight(70.19, UserProfileStore.WeightUnit.KG)

        assert(res.isSuccess)
        val req = captured.single()
        assert(req.weightKg != null)
        assert(req.weightLbs == null)

        coVerify { store.setWeightKg(any()) }
        coVerify { store.setWeightLbs(any()) }
        coVerify { bus.invalidateProfile() }
    }

    @Test
    fun `updateStartingWeight LBS should send weightLbs only`() = runTest {
        val api = mockk<ProfileApi>()
        val store = mockk<UserProfileStore>(relaxed = true)
        val bus = mockk<RepoInvalidationBus>(relaxed = true)

        val captured = mutableListOf<UpsertProfileRequest>()
        coEvery { api.upsertMyProfile(capture(captured), any()) } returns UserProfileDto(
            weightKg = 70.0,
            weightLbs = 154.3
        )

        val repo = ProfileRepository(api = api, store = store, bus = bus)

        val res = repo.updateStartingWeight(154.39, UserProfileStore.WeightUnit.LBS)

        assert(res.isSuccess)
        val req = captured.single()
        assert(req.weightKg == null)
        assert(req.weightLbs != null)

        coVerify { bus.invalidateProfile() }
    }
}
