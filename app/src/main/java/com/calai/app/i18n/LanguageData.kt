package com.calai.app.i18n

// 單一語言選項
data class LangOption(
    val tag: String,   // IETF 語言碼（例如 zh-Hant / en / es）
    val name: String,  // 在語言選單中顯示的名稱
    val flag: String,  // 旗幟 emoji（或之後換成你自己的圖示也行）
    val label: String  // 短標籤（CH/EN/ES…）
)

/**
 * 你的 18 種語言（順序照你提供的）
 * - 繁體中文 zh-Hant（旗幟預設 🇹🇼；若你想用 🇭🇰 把 flag 換掉即可）
 * - 簡體中文 zh-CN（旗幟 🇨🇳）
 */
val LANGS: List<LangOption> = listOf(

    LangOption("en",      "English",       "🇺🇸", "EN"),
    LangOption("es",      "Español",       "🇪🇸", "ES"),
    LangOption("ar",      "العربية",        "🇸🇦", "AR"),
    LangOption("bn",      "বাংলা",           "🇧🇩", "BN"),
    LangOption("pt",      "Português",     "🇵🇹", "PT"), // 若以巴西葡萄牙語為主，改 "🇧🇷"
    LangOption("ru",      "Русский",       "🇷🇺", "RU"),
    LangOption("ja",      "日本語",          "🇯🇵", "JP"),
    LangOption("zh-Hant", "繁體中文",      "🇹🇼", "CH"),
    LangOption("de",      "Deutsch",       "🇩🇪", "DE"),
    LangOption("pa",      "ਪੰਜਾਬੀ",          "🇮🇳", "PA"), // 旁遮普語（印度/巴基斯坦皆可）
    LangOption("jv",      "Basa Jawa",     "🇮🇩", "JV"),
    LangOption("fr",      "Français",      "🇫🇷", "FR"),
    LangOption("vi",      "Tiếng Việt",    "🇻🇳", "VI"),
    LangOption("th",      "ไทย",            "🇹🇭", "TH"),
    LangOption("ms",      "Bahasa Melayu", "🇲🇾", "MS"),
    LangOption("ko",      "한국어",           "🇰🇷", "KR"),
    LangOption("id",      "Bahasa Indonesia","🇮🇩","ID"),
    LangOption("zh-CN",   "简体中文",       "🇨🇳", "CH"),
)

/** 依 tag 回傳短標籤（CH/EN/ES…）*/
fun langShortLabelFromTag(tag: String): String {
    val t = tag.lowercase()
    // 先嘗試在清單中找前綴匹配
    LANGS.firstOrNull { t.startsWith(it.tag.lowercase()) }?.let { return it.label }
    return when {
        t.startsWith("en") -> "EN"
        t.startsWith("es") -> "ES"
        t.startsWith("ar") -> "AR"
        t.startsWith("bn") -> "BN"
        t.startsWith("pt") -> "PT"
        t.startsWith("ru") -> "RU"
        t.startsWith("ja") -> "JP"
        t.startsWith("zh") -> "CH"
        t.startsWith("de") -> "DE"
        t.startsWith("pa") -> "PA"
        t.startsWith("jv") -> "JV"
        t.startsWith("fr") -> "FR"
        t.startsWith("vi") -> "VI"
        t.startsWith("th") -> "TH"
        t.startsWith("ms") -> "MS"
        t.startsWith("ko") -> "KR"
        t.startsWith("id") -> "ID"
        else -> t.take(2).uppercase()
    }
}

/**
 * 依 tag 回傳 (旗幟, 短標籤)。
 * - 處理 zh-Hant/zh-CN 與 TW/HK/MO、Hans/Hant 等常見別名
 * - 否則用 LANGS 的前綴匹配；找不到回傳預設旗與兩碼大寫
 */
fun flagAndLabelFromTag(tag: String): Pair<String, String> {
    val exact = LANGS.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
    if (exact != null) return exact.flag to exact.label

    val t = tag.lowercase()

    // 中文分流：繁（Hant/TW/HK/MO）與簡（Hans/CN）
    if (t.startsWith("zh")) {
        return if (t.contains("hant") || t.contains("tw") || t.contains("hk") || t.contains("mo")) {
            "🇹🇼" to "CH"   // 繁中預設 🇹🇼；需要可換 🇭🇰
        } else {
            "🇨🇳" to "CH"   // 其它情況當作簡中
        }
    }

    // 一般語言用前綴匹配
    LANGS.firstOrNull { t.startsWith(it.tag.lowercase()) }?.let { return it.flag to it.label }

    // 找不到時的保底
    return "🏳️" to t.take(2).uppercase()
}
