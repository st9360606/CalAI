package com.calai.bitecal.i18n

import java.util.Locale

data class LangOption(
    val tag: String,
    val name: String,
    val flag: String,
    val label: String
)

val LANGS: List<LangOption> = listOf(
    LangOption("en",      "English",         "🇺🇸", "EN"),
    LangOption("es",      "Español",         "🇪🇸", "ES"),
    LangOption("ar",      "العربية",          "🇸🇦", "AR"),
    LangOption("bn",      "বাংলা",             "🇧🇩", "BN"),
    LangOption("ru",      "Русский",         "🇷🇺", "RU"),
    LangOption("fr",      "Français",        "🇫🇷", "FR"),
    LangOption("de",      "Deutsch",         "🇩🇪", "DE"),
    LangOption("ja",      "日本語",            "🇯🇵", "JP"),
    LangOption("ko",      "한국어",             "🇰🇷", "KR"),
    LangOption("vi",      "Tiếng Việt",      "🇻🇳", "VI"),
    LangOption("th",      "ไทย",              "🇹🇭", "TH"),
    LangOption("ms",      "Bahasa Melayu",   "🇲🇾", "MS"),
    LangOption("zh-TW",   "繁體中文",         "🇹🇼", "CH"),
    LangOption("zh-CN",   "简体中文",         "🇨🇳", "CH"),

    // 先前新增
    LangOption("it",      "Italiano",        "🇮🇹", "IT"),
    LangOption("nl",      "Nederlands",      "🇳🇱", "NL"),
    LangOption("sv",      "Svenska",         "🇸🇪", "SV"),
    LangOption("da",      "Dansk",           "🇩🇰", "DA"),
    LangOption("nb",      "Norsk (Bokmål)",  "🇳🇴", "NO"),
    LangOption("he",      "עברית",            "🇮🇱", "HE"),
    LangOption("tr",      "Türkçe",          "🇹🇷", "TR"),
    LangOption("pl",      "Polski",          "🇵🇱", "PL"),
    LangOption("zh-HK",   "繁體中文（香港）",  "🇭🇰", "CH"),
    LangOption("fil",     "Filipino",        "🇵🇭", "PH"),

    // 本次必補
    LangOption("pt-BR",   "Português (Brasil)",   "🇧🇷", "BR"),
    LangOption("pt-PT",   "Português (Portugal)", "🇵🇹", "PT"),
    LangOption("fi",      "Suomi",           "🇫🇮", "FI"),
    LangOption("ro",      "Română",          "🇷🇴", "RO"),
    LangOption("cs",      "Čeština",         "🇨🇿", "CS"),
    LangOption("hi",      "हिन्दी",           "🇮🇳", "HI"),
    LangOption("jv",      "Basa Jawa",       "🇮🇩", "JV")
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

        // 補充
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

    // 中文分流
    if (t.startsWith("zh")) {
        return if (t.contains("hant") || t.contains("tw") || t.contains("hk") || t.contains("mo")) {
            if (t.contains("hk")) "🇭🇰" to "CH" else "🇹🇼" to "CH"
        } else {
            "🇨🇳" to "CH"
        }
    }

    // 葡萄牙語分流
    if (t.startsWith("pt-")) {
        return if (t.contains("br")) "🇧🇷" to "BR" else "🇵🇹" to "PT"
    }

    // 菲律賓 Tagalog 別名
    if (t.startsWith("tl")) return "🇵🇭" to "PH"

    LANGS.firstOrNull { t.startsWith(it.tag.lowercase(Locale.ROOT)) }?.let { return it.flag to it.label }
    return "🏳️" to t.take(2).uppercase(Locale.ROOT)
}
