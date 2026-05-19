package com.calai.bitecal.i18n

import java.util.Locale

data class LangOption(
    val tag: String,
    val name: String,
    val flag: String,
    val label: String
)

val LANGS: List<LangOption> = listOf(
    // Tier 1: 頂級變現市場 (超高規模 / 極高 ARPU)
    LangOption("en", "English", "🇺🇸", "EN"),
    LangOption("zh-CN", "简体中文", "🇨🇳", "CH"),
    LangOption("ja", "日本語", "🇯🇵", "JP"),
    LangOption("ko", "한국어", "🇰🇷", "KR"),

    // Tier 2: 成熟發達市場 (高購買力歐美與亞洲地區)
    LangOption("de", "Deutsch", "🇩🇪", "DE"),
    LangOption("fr", "Français", "🇫🇷", "FR"),
    LangOption("zh-HK", "繁體中文", "🇭🇰", "CH"),
    LangOption("nl", "Nederlands", "🇳🇱", "NL"),
    LangOption("sv", "Svenska", "🇸🇪", "SV"),
    LangOption("nb", "Norsk (Bokmål)", "🇳🇴", "NO"),
    LangOption("da", "Dansk", "🇩🇰", "DA"),
    LangOption("fi", "Suomi", "🇫🇮", "FI"),
    LangOption("it", "Italiano", "🇮🇹", "IT"),

    // Tier 3: 中度消費與高潛力市場 (基數大或局部高 ARPU)
    LangOption("es", "Español", "🇪🇸", "ES"),
    LangOption("ar", "العربية", "🇸🇦", "AR"),
    LangOption("pt-BR", "Português (Brasil)", "🇧🇷", "BR"),
    LangOption("tr", "Türkçe", "🇹🇷", "TR"),
    LangOption("pl", "Polski", "🇵🇱", "PL"),
    LangOption("cs", "Čeština", "🇨🇿", "CS"),
    LangOption("ro", "Română", "🇷🇴", "RO"),
    LangOption("pt-PT", "Português (Portugal)", "🇵🇹", "PT"),
    LangOption("ru", "Русский", "🇷🇺", "RU"),

    // Tier 4: 高下載量但訂閱變現率較低市場 (新興市場)
    LangOption("th", "ไทย", "🇹🇭", "TH"),
    LangOption("ms", "Bahasa Melayu", "🇲🇾", "MS"),
    LangOption("vi", "Tiếng Việt", "🇻🇳", "VI"),
    LangOption("fil", "Filipino", "🇵🇭", "PH"),
    LangOption("hi", "हिन्दी", "🇮🇳", "HI"),
    LangOption("jv", "Basa Jawa", "🇮🇩", "JV")
)

fun langShortLabelFromTag(tag: String): String {
    val t = tag.replace('_', '-').lowercase(Locale.ROOT)
    LANGS.firstOrNull { t.startsWith(it.tag.lowercase(Locale.ROOT)) }?.let { return it.label }
    return when {
        t.startsWith("en") -> "EN"
        t.startsWith("es") -> "ES"
        t.startsWith("ar") -> "AR"
        t.startsWith("bn") -> "BN"
        t.startsWith("ru") -> "RU"
        t.startsWith("fr") -> "FR"
        t.startsWith("de") -> "DE"
        t.startsWith("ja") -> "JP"
        t.startsWith("ko") -> "KR"
        t.startsWith("vi") -> "VI"
        t.startsWith("th") -> "TH"
        t.startsWith("id") -> "ID"
        t.startsWith("ms") -> "MS"
        t.startsWith("zh") -> "CH"
        t.startsWith("it") -> "IT"
        t.startsWith("nl") -> "NL"
        t.startsWith("sv") -> "SV"
        t.startsWith("da") -> "DA"
        t.startsWith("nb") || t.startsWith("no") -> "NO"
        t.startsWith("he") -> "HE"
        t.startsWith("tr") -> "TR"
        t.startsWith("pl") -> "PL"
        t.startsWith("pt-br") -> "BR"
        t.startsWith("pt-pt") || t == "pt" -> "PT"
        t.startsWith("fi") -> "FI"
        t.startsWith("ro") -> "RO"
        t.startsWith("cs") -> "CS"
        t.startsWith("hi") -> "HI"
        t.startsWith("fil") || t.startsWith("tl") -> "PH"
        else -> t.take(2).uppercase(Locale.ROOT)
    }
}

fun flagAndLabelFromTag(tag: String): Pair<String, String> {
    val exact = LANGS.firstOrNull { it.tag.equals(tag, ignoreCase = true) }
    if (exact != null) return exact.flag to exact.label

    val t = tag.replace('_', '-').lowercase(Locale.ROOT)

    if (t.startsWith("zh")) {
        return if (t.contains("cn") || t.contains("sg") || t.contains("hans")) {
            "🇨🇳" to "CH"
        } else {
            "🇭🇰" to "CH"
        }
    }

    if (t.startsWith("pt-")) {
        return if (t.contains("br")) "🇧🇷" to "BR" else "🇵🇹" to "PT"
    }

    if (t.startsWith("tl")) return "🇵🇭" to "PH"

    LANGS.firstOrNull { t.startsWith(it.tag.lowercase(Locale.ROOT)) }?.let {
        return it.flag to it.label
    }
    return "🏳️" to t.take(2).uppercase(Locale.ROOT)
}
