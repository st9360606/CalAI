package com.calai.app.ui.landing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// 語言清單（BCP-47）
data class LangItem(val label: String, val tag: String, val flag: String)

val LANGS = listOf(
    LangItem("繁體中文", "zh-TW", "🇹🇼"),
    LangItem("English", "en", "🇺🇸"),
    LangItem("Español", "es", "🇪🇸"),
    LangItem("العربية", "ar", "🇸🇦"),
    LangItem("বাংলা", "bn", "🇧🇩"),
    LangItem("Português", "pt", "🇵🇹"),
    LangItem("Русский", "ru", "🇷🇺"),
    LangItem("日本語", "ja", "🇯🇵"),
    LangItem("Deutsch", "de", "🇩🇪"),
    LangItem("ਪੰਜਾਬੀ", "pa", "🇮🇳"),
    LangItem("Basa Jawa", "jv", "🇮🇩"),
    LangItem("Français", "fr", "🇫🇷"),
    LangItem("Tiếng Việt", "vi", "🇻🇳"),
    LangItem("ไทย", "th", "🇹🇭"),
    LangItem("Bahasa Melayu", "ms", "🇲🇾"),
    LangItem("한국어", "ko", "🇰🇷"),
    LangItem("Bahasa Indonesia", "id", "🇮🇩"),
    LangItem("简体中文", "zh-CN", "🇨🇳")
)

@Composable
fun LanguageDialog(
    title: String,
    currentTag: String,
    // ⚠️ 關鍵：這裡是「一般函式」，不要加 @Composable
    onPick: (LangItem) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text(title) },
        text = {
            Column {
                LANGS.forEach { lang ->
                    val selected = lang.tag.equals(currentTag, true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            // clickable 區塊不是 Composable context，因此 onPick 不能是 @Composable
                            .clickable { onPick(lang) }
                    ) {
                        Text(text = "${lang.flag}  ${lang.label}")
                        if (selected) Spacer(Modifier.weight(1f))
                        if (selected) Text("✓")
                    }
                }
            }
        }
    )
}