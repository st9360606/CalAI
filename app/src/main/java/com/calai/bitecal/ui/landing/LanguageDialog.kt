@file:OptIn(ExperimentalMaterial3Api::class)

package com.calai.bitecal.ui.landing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.calai.bitecal.R
import com.calai.bitecal.ui.common.haptic.biteCalClickable
import com.calai.bitecal.i18n.LanguageManager

data class LangItem(val label: String, val tag: String, val flag: String)

val LANGS = listOf(
    // Tier 1: 頂級變現市場 (超大營收規模 / 極高人均付費)
    LangItem("English", "en", "🇺🇸"),
    LangItem("简体中文", "zh-CN", "🇨🇳"),
    LangItem("日本語", "ja", "🇯🇵"),
    LangItem("한국어", "ko", "🇰🇷"),

    // Tier 2: 成熟發達市場 (高購買力、高訂閱轉化率)
    LangItem("Deutsch", "de", "🇩🇪"),
    LangItem("Français", "fr", "🇫🇷"),
    LangItem("繁體中文", "zh-HK", "🇭🇰"),
    LangItem("Nederlands", "nl", "🇳🇱"),
    LangItem("עברית", "he", "🇮🇱"),
    LangItem("Svenska", "sv", "🇸🇪"),
    LangItem("Norsk (Bokmål)", "nb", "🇳🇴"),
    LangItem("Dansk", "da", "🇩🇰"),
    LangItem("Suomi", "fi", "🇫🇮"),
    LangItem("Italiano", "it", "🇮🇹"),

    // Tier 3: 中度消費與高潛力市場 (基數龐大或局部高收入)
    LangItem("Español", "es", "🇪🇸"),
    LangItem("العربية", "ar", "🇸🇦"),
    LangItem("Português (Brasil)", "pt-BR", "🇧🇷"),
    LangItem("Türkçe", "tr", "🇹🇷"),
    LangItem("Polski", "pl", "🇵🇱"),
    LangItem("Čeština", "cs", "🇨🇿"),
    LangItem("Română", "ro", "🇷🇴"),
    LangItem("Português (Portugal)", "pt-PT", "🇵🇹"),
    LangItem("Русский", "ru", "🇷🇺"),

    // Tier 4: 高下載量但低訂閱轉化市場 (以免費或廣告變現為主)
    LangItem("ไทย", "th", "🇹🇭"),
    LangItem("Bahasa Melayu", "ms", "🇲🇾"),
    LangItem("Tiếng Việt", "vi", "🇻🇳"),
    LangItem("Filipino", "fil", "🇵🇭"),
    LangItem("हिन्दी", "hi", "🇮🇳"),
    LangItem("Basa Jawa", "jv", "🇮🇩")
)

@Composable
fun LanguageDialog(
    title: String,
    currentTag: String,
    onPick: (LangItem) -> Unit,
    onDismiss: () -> Unit,
    lang: List<LangItem> = LANGS,
    widthFraction: Float = 0.92f,
    maxHeightFraction: Float = 0.60f
) {
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenH * maxHeightFraction
    val surface = Color.White
    val onSurface = Color(0xFF111114)
    val selectedContainer = Color(0xFF111114)
    val selectedContent = Color.White
    val outline = Color(0xFFE5E7EB)

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
            color = surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Column(Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 10.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface,
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
                            contentDescription = stringResource(R.string.close),
                            tint = onSurface
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
                        val supported = LanguageManager.isSupported(langItem.tag)
                        val selected = LanguageManager.isSelectedOption(
                            optionTag = langItem.tag,
                            currentTag = currentTag
                        )
                        val bg = if (selected) selectedContainer else surface
                        val fg = if (selected) selectedContent else onSurface
                        val border = if (selected) Color.Transparent else outline

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(bg)
                                .border(BorderStroke(1.dp, border), RoundedCornerShape(16.dp))
                                .biteCalClickable(enabled = supported) {
                                    onPick(langItem)
                                }
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
                                        .background(selectedContent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
