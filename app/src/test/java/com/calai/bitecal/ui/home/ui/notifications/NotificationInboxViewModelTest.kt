package com.calai.bitecal.ui.home.ui.notifications

import com.calai.bitecal.data.notifications.api.NotificationItemDto
import com.calai.bitecal.data.notifications.repo.NotificationInboxRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationInboxViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository: NotificationInboxRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refresh_success_loads_notifications() = runTest(dispatcher) {
        val notification = NotificationItemDto(
            id = 1L,
            type = "REFERRAL_REWARD_GRANTED",
            title = "Referral reward granted",
            message = "Your Premium has been extended by 30 days.",
            deepLink = "bitecal://referrals",
            createdAtUtc = "2026-05-15T01:02:03Z",
            read = false
        )
        coEvery { repository.list() } returns listOf(notification)

        val vm = NotificationInboxViewModel(repository)
        advanceUntilIdle()

        val state = vm.ui.value
        assertFalse(state.loading)
        assertEquals(listOf(notification), state.items)
        assertEquals(null, state.error)
    }

    @Test
    fun refresh_success_with_empty_list_shows_empty_state_data() = runTest(dispatcher) {
        coEvery { repository.list() } returns emptyList()

        val vm = NotificationInboxViewModel(repository)
        advanceUntilIdle()

        val state = vm.ui.value
        assertFalse(state.loading)
        assertTrue(state.items.isEmpty())
        assertEquals(null, state.error)
    }

    @Test
    fun refresh_failure_sets_error_state() = runTest(dispatcher) {
        coEvery { repository.list() } throws IllegalStateException("network failed")

        val vm = NotificationInboxViewModel(repository)
        advanceUntilIdle()

        val state = vm.ui.value
        assertFalse(state.loading)
        assertTrue(state.items.isEmpty())
        assertEquals("network failed", state.error)
    }
}
