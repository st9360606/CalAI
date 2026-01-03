package com.calai.app.data.activity.sync

import com.calai.app.data.activity.model.DailyActivityStatus
import java.time.LocalDate
import java.time.ZoneId

interface DailyReader {
    suspend fun getStatus(): DailyActivityStatus
    suspend fun hasAnyRecord(localDate: LocalDate, zoneId: ZoneId, originPackage: String): Boolean
    suspend fun readSteps(localDate: LocalDate, zoneId: ZoneId, originPackage: String): Long?
    suspend fun resolveOriginName(packageName: String): String?

    /**
     * ✅ NEW：直接回傳「當天各來源的 steps 加總」
     * - key = metadata.dataOrigin.packageName
     * - value = steps sum
     *   預設回傳 emptyMap()，避免破壞其他實作。
     */
    suspend fun readStepsByOrigin(localDate: LocalDate, zoneId: ZoneId): Map<String, Long> = emptyMap()
}

