@file:OptIn(ExperimentalMaterial3Api::class)

package com.calai.app.ui.landing

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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

/** iOS 風：小卡片 + pill 列表 */
@Composable
fun LanguageDialog(
    title: String,
    currentTag: String,
    onPick: (LangItem) -> Unit,
    onDismiss: () -> Unit,
    langs: List<LangItem> = LANGS,
    maxWidth: Dp = 320.dp,
    maxHeightFraction: Float = 0.44f      // 想更矮就再調小
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
                .padding(24.dp)
                .widthIn(max = maxWidth)
                // ★ 關鍵：強制高度上限，讓內容取得「已界定高度」
                .requiredHeightIn(max = maxHeight)
                .fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFFF7F6FB),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.padding(16.dp)) {

                // 標題（置中）+ 右上關閉
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 10.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF111114),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(end = 40.dp)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                // ★ 清單：拿到剩餘高度 → 能在卡片內捲動
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true),   // ← 佔滿剩餘高度（有上限）
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(langs) { lang ->
                        val selected = lang.tag.equals(currentTag, ignoreCase = true)
                        val bg = if (selected) Color(0xFF111114) else Color.White
                        val fg = if (selected) Color.White else Color(0xFF111114)
                        val border = if (selected) Color.Transparent else Color(0xFFE5E5EA)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bg)
                                .border(BorderStroke(1.dp, border), RoundedCornerShape(16.dp))
                                .clickable { onPick(lang) }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = lang.flag, fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = lang.label,
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
