package com.calai.app.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineRule = BaselineProfileRule()

    @Test
    fun generateStartupProfile() = baselineRule.collect(
        packageName = "com.calai.app",
        includeInStartupProfile = true // 收錄啟動相關路徑
        // 註：有些版本還支援 maxIterations；若你加了報參數錯誤，就拿掉它。
    ) {
        // 1) 冷啟 App（先回桌面，再啟動）
        pressHome()
        startActivityAndWait()

        // 2) 等待首頁關鍵元素（先用文字，之後可改 By.res/By.desc 做語系無關）
        device.wait(Until.hasObject(By.textContains("Get Started")), 5_000)

        // 3) 互動一下，讓更多熱路徑被收錄（可改 By.res/By.desc）
        device.findObject(By.textContains("Get Started"))?.click()

        // 4) 等登入面板顯示
        device.wait(Until.hasObject(By.textContains("Sign in")), 3_000)

        // 5) 返回（可選）
        device.pressBack()
    }
}
