package com.calai.app.data.activity.api

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActivitySerializationTest {

    @Test
    fun `nulls should be encoded for PUT full overwrite`() {
        val json = Json { explicitNulls = true; encodeDefaults = false }

        val s = json.encodeToString(
            DailyActivityUpsertRequest(
                localDate = "2026-01-02",
                timezone = "Asia/Taipei",
                steps = null,
                activeKcal = null,
                ingestSource = "HEALTH_CONNECT",
                dataOriginPackage = "com.google.android.apps.fitness",
                dataOriginName = null
            )
        )

        assertTrue(s.contains("\"steps\":null"))
        assertTrue(s.contains("\"activeKcal\":null"))
    }
}
