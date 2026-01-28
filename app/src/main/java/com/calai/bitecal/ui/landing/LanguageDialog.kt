@file:OptIn(ExperimentalMaterial3Api::class)
package com.calai.bitecal.ui.landing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// 語言清單（BCP-47）
data class LangItem(val label: String, val tag: String, val flag: String)

// ✅ 已加入 it/nl/sv/da/nb/he/tr/pl/zh-HK/fil + 其他
val LANGS = listOf(
    LangItem("English", "en", "🇺🇸"),
    LangItem("Español", "es", "🇪🇸"),
    LangItem("العربية", "ar", "🇸🇦"),
    LangItem("বাংলা", "bn", "🇧🇩"),
    LangItem("Русский", "ru", "🇷🇺"),
    LangItem("Français", "fr", "🇫🇷"),
    LangItem("Deutsch", "de", "🇩🇪"),
    LangItem("日本語", "ja", "🇯🇵"),
    LangItem("한국어", "ko", "🇰🇷"),
    LangItem("Tiếng Việt", "vi", "🇻🇳"),
    LangItem("ไทย", "th", "🇹🇭"),
    LangItem("Bahasa Melayu", "ms", "🇲🇾"),
    LangItem("繁體中文", "zh-TW", "🇹🇼"),
    LangItem("简体中文", "zh-CN", "🇨🇳"),

    LangItem("Italiano", "it", "🇮🇹"),
    LangItem("Nederlands", "nl", "🇳🇱"),
    LangItem("Svenska", "sv", "🇸🇪"),
    LangItem("Dansk", "da", "🇩🇰"),
    LangItem("Norsk (Bokmål)", "nb", "🇳🇴"),
    LangItem("עברית", "he", "🇮🇱"),
    LangItem("Türkçe", "tr", "🇹🇷"),
    LangItem("Polski", "pl", "🇵🇱"),
    LangItem("繁體中文", "zh-HK", "🇭🇰"),
    LangItem("Filipino", "fil", "🇵🇭"),

    LangItem("Português (Brasil)", "pt-BR", "🇧🇷"),
    LangItem("Português (Portugal)", "pt-PT", "🇵🇹"),
    LangItem("Suomi", "fi", "🇫🇮"),
    LangItem("Română", "ro", "🇷🇴"),
    LangItem("Čeština", "cs", "🇨🇿"),
    LangItem("हिन्दी", "hi", "🇮🇳"),

    LangItem("Basa Jawa", "jv", "🇮🇩")
)

/** iOS 風：小卡片 + pill 列表 */
@Composable
fun LanguageDialog(
    title: String,
    currentTag: String,
    onPick: (LangItem) -> Unit,
    onDismiss: () -> Unit,
    lang: List<LangItem> = LANGS,
    widthFraction: Float = 0.92f,     // 用比例控制寬度
    maxHeightFraction: Float = 0.60f  // 高度用比例
) {
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenH * maxHeightFraction

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .fillMaxWidth(widthFraction)
                .requiredHeightIn(max = maxHeight),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.padding(16.dp)) {

                // 標題置中 + 關閉按鈕
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 10.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111114),
                        modifier = Modifier.align(Alignment.Center),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close"
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(lang) { langItem ->
                        val selected = langItem.tag.equals(currentTag, ignoreCase = true)
                        val bg = if (selected) Color(0xFF111114) else Color.White
                        val fg = if (selected) Color.White else Color(0xFF111114)
                        val border = if (selected) Color.Transparent else Color(0xFFE5E5EA)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bg)
                                .border(BorderStroke(1.dp, border), RoundedCornerShape(16.dp))
                                .clickable { onPick(langItem) }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = langItem.flag, fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = langItem.label,
                                color = fg,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(Color.White)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
