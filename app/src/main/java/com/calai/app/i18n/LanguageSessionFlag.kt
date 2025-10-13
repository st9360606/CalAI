package com.calai.app.i18n

/**
 * 追蹤「本次 App session 是否有手動切過語言」。
 * - markChanged(): 標記本次 session 已改語言
 * - consumeChanged(): 讀取並清除旗標（用過即清，避免下次污染）
 */
object LanguageSessionFlag {
    @Volatile
    private var changedThisSession: Boolean = false

    fun markChanged() {
        changedThisSession = true
    }

    /** 讀取並重置旗標；回傳呼叫當下是否有改語言紀錄 */
    fun consumeChanged(): Boolean {
        val was = changedThisSession
        changedThisSession = false
        return was
    }
}
