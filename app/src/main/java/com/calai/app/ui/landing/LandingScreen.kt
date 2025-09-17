package com.calai.app.ui.landing

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.calai.app.R
import com.calai.app.i18n.LanguageManager
import com.calai.app.i18n.LanguageStore
import com.calai.app.ui.VideoPlayerRaw
// 如果 LanguageDialog / LANGS 與本檔同 package 可省略 import
// import com.calai.app.ui.landing.LanguageDialog
// import com.calai.app.ui.landing.LANGS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun LandingScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    val context = LocalContext.current
    val activity = context as Activity
    val scope = rememberCoroutineScope()
    val store = remember { LanguageStore(context) }

    var showLang by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }   // 防抖：切換進行中

    // 直接從 AppCompat 讀取目前語系（避免舊 state）
    val currentTag: String =
        AppCompatDelegate.getApplicationLocales().toLanguageTags()
            .ifBlank { Locale.getDefault().toLanguageTag() }

    val currentLang = LANGS.find { it.tag.equals(currentTag, true) }
        ?: LANGS.firstOrNull { it.tag.startsWith("en", true) }
        ?: LANGS.first()

    Box(Modifier.fillMaxSize().background(Color.White)) {

        // 右上角：語言按鈕（旗幟）
        Text(
            text = currentLang.flag,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF2F2F2))
                .clickable(enabled = !switching) { showLang = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // 中間：示範影片 + 標題
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VideoPlayerRaw(
                resId = R.raw.intro,
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .aspectRatio(9f / 16f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.landing_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF111114)
            )
        }

        // 底部：CTA
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) { Text(stringResource(R.string.cta_get_started)) }

            Spacer(Modifier.height(8.dp))

            Row {
                Text(stringResource(R.string.cta_login_prefix))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.cta_login),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onLogin)
                )
            }
        }

        if (showLang) {
            LanguageDialog(
                title = stringResource(R.string.choose_language),
                currentTag = currentTag,
                onPick = { picked ->
                    if (switching) return@LanguageDialog
                    switching = true
                    showLang = false

                    // ✅ 先保存 → 再套用 → 再重建，避免偶發要按兩次
                    scope.launch {
                        // 1) 先把新語言寫入 DataStore（等寫完）
                        store.save(picked.tag)

                        // 2) 主執行緒套用語言（AppCompat 也會持久化）
                        withContext(Dispatchers.Main) {
                            LanguageManager.applyLanguage(picked.tag)
                        }

                        // 3) 強制重建，確保資源立刻切換
                        withContext(Dispatchers.Main) {
                            activity.recreate()
                            switching = false
                        }
                    }
                },
                onDismiss = { showLang = false }
            )
        }
    }
}
