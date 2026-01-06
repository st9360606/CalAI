package com.calai.app.ui.home.ui.camera

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.testTag

enum class CameraMode { FOOD, BARCODE, LABEL }

@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onImagePicked: (Uri) -> Unit,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 你之前踩過的坑：LocalActivityResultRegistryOwner 可能為 null
    val registryOwner = LocalActivityResultRegistryOwner.current

    // ===== 01/02/03：模式切換 =====
    val mode = rememberSaveable { mutableStateOf(CameraMode.FOOD) }

    // ===== 權限狀態 =====
    val hasCameraPerm = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val requestCameraPermLauncher =
        if (registryOwner != null) {
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                hasCameraPerm.value = granted
            }
        } else null

    // ===== 相冊：Photo Picker =====
    val pickImageLauncher =
        if (registryOwner != null) {
            rememberLauncherForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) onImagePicked(uri)
            }
        } else null

    // ===== CameraX Preview =====
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }

    DisposableEffect(hasCameraPerm.value) {
        if (!hasCameraPerm.value) onDispose { }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

        val executor = ContextCompat.getMainExecutor(ctx)
        val listener = Runnable {
            runCatching {
                val provider = cameraProviderFuture.get()
                provider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                provider.bindToLifecycle(lifecycleOwner, selector, preview)
            }
        }
        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    // ===== UI 色票（先用你現有風格，後續要 1:1 就調這些常數）=====
    val scrim = Color.Black.copy(alpha = 0.15f)
    val tileBg = Color(0xFFE9EBEF)
    val tileText = Color(0xFF2B2F36)
    val tileIcon = Color(0xFF606774)
    val frameColor = Color(0xFFE9EBEF).copy(alpha = 0.95f)

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        // 相機預覽（沒權限就顯示黑底）
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // 輕微遮罩（更像你拍到的效果）
        Box(Modifier.fillMaxSize().background(scrim))

        // ===== 左上角 X =====
        val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Surface(
            color = Color(0xFF6C6C70).copy(alpha = 0.55f),
            shape = CircleShape,
            modifier = Modifier
                .padding(start = 18.dp, top = topPadding + 12.dp)
                .size(34.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) { onClose() }
                .semantics { }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "close",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ===== 中間掃描框（對應 01/02/03）=====
        ScanFrameOverlay(
            mode = mode.value,
            frameColor = frameColor,
            modifier = Modifier.fillMaxSize()
        )

        // ===== 底部操作區：四個模式按鈕 + 閃光燈 + 快門 =====
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 閃光燈（先做 UI；你後續要接 CameraX torch 再補）
                CircleIconButton(
                    icon = Icons.Outlined.FlashOn,
                    tint = Color.White,
                    bg = Color(0xFF6C6C70).copy(alpha = 0.55f),
                    size = 36.dp,
                    onClick = { /* TODO torch */ }
                )

                // 快門（先做 UI；你後續要做拍照/擷取再補）
                ShutterButton(onClick = { /* TODO capture */ })

                Spacer(Modifier.size(36.dp)) // 右側留空，保持中間對齊（跟你圖像感覺一致）
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeTile(
                    label = "扫描食物",
                    icon = Icons.Outlined.Restaurant,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = mode.value == CameraMode.FOOD,
                    onClick = { mode.value = CameraMode.FOOD },
                    modifier = Modifier.testTag("mode_food")
                )
                ModeTile(
                    label = "条形码",
                    icon = Icons.Outlined.QrCodeScanner,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = mode.value == CameraMode.BARCODE,
                    onClick = { mode.value = CameraMode.BARCODE },
                    modifier = Modifier.testTag("mode_barcode")
                )
                ModeTile(
                    label = "食品标签",
                    icon = Icons.Outlined.ReceiptLong,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = mode.value == CameraMode.LABEL,
                    onClick = { mode.value = CameraMode.LABEL },
                    modifier = Modifier.testTag("mode_label")
                )
                ModeTile(
                    label = "相册",
                    icon = Icons.Outlined.Image,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = false,
                    onClick = {
                        // 你要的：跳轉手機相簿
                        // 有 launcher 就用 Photo Picker；沒有就降級導 App 設定/或系統選圖
                        if (pickImageLauncher != null) {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            openAppSettings(ctx) // 極少數 owner=null 情境，走降級
                        }
                    },
                    modifier = Modifier.testTag("mode_album")
                )
            }

            // 沒相機權限：底部提示（你可以改成更漂亮的卡）
            if (!hasCameraPerm.value) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "需要相机权限才能扫描",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            requestCameraPermLauncher?.launch(Manifest.permission.CAMERA)
                                ?: openAppSettings(ctx)
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun ScanFrameOverlay(
    mode: CameraMode,
    frameColor: Color,
    modifier: Modifier = Modifier
) {
    // 這裡是「1:1 微調區」：不同手機比例，你就改這些 dp
    val topBias = 0.42f  // 掃描框偏上（跟你照片很像）
    val barcodeW = 0.74f
    val barcodeH = 0.17f
    val labelW = 0.66f
    val labelH = 0.42f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (mode) {
            CameraMode.FOOD -> {
                // 01：四角框（用 4 個 L 角）
                CornerBrackets(
                    color = frameColor,
                    size = 250.dp,
                    stroke = 4.dp,
                    cornerLen = 22.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 0.dp)
                )
            }

            CameraMode.BARCODE -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 110.dp)
                        .testTag("title_barcode")
                ) {
                    Text(
                        text = "条形码扫描器",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    RoundedFrame(
                        color = frameColor,
                        width = 300.dp,
                        height = 120.dp,
                        radius = 18.dp
                    )
                }
            }

            CameraMode.LABEL -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 80.dp)
                        .testTag("title_label")
                ) {
                    Text(
                        text = "标签扫描器",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    RoundedFrame(
                        color = frameColor,
                        width = 280.dp,
                        height = 280.dp,
                        radius = 18.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundedFrame(
    color: Color,
    width: Dp,
    height: Dp,
    radius: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width, height)
            .clip(RoundedCornerShape(radius))
            .background(Color.Transparent)
            .borderSoft(color = color, radius = radius)
    )
}

@Composable
private fun CornerBrackets(
    color: Color,
    size: Dp,
    stroke: Dp,
    cornerLen: Dp,
    modifier: Modifier = Modifier
) {
    // 用 4 個小 Box 畫 L 角（簡單、好調）
    Box(modifier = modifier.size(size)) {
        val c = color
        val s = stroke
        val len = cornerLen

        // 左上
        Box(
            Modifier
                .align(Alignment.TopStart)
                .size(len)
                .background(Color.Transparent)
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.height(s).fillMaxWidth().background(c))
                Box(Modifier.width(s).fillMaxSize().background(c))
            }
        }
        // 右上
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(len)
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.height(s).fillMaxWidth().background(c))
                Box(Modifier.width(s).fillMaxSize().align(Alignment.TopEnd).background(c))
            }
        }
        // 左下
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .size(len)
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.height(s).fillMaxWidth().align(Alignment.BottomStart).background(c))
                Box(Modifier.width(s).fillMaxSize().background(c))
            }
        }
        // 右下
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(len)
        ) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.height(s).fillMaxWidth().align(Alignment.BottomEnd).background(c))
                Box(Modifier.width(s).fillMaxSize().align(Alignment.TopEnd).background(c))
            }
        }
    }
}

@Composable
private fun ModeTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    textColor: Color,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 01：四個小卡的尺寸（想 1:1 就調這裡）
    val w = 84.dp
    val h = 62.dp
    val corner = 14.dp

    Surface(
        color = bg.copy(alpha = if (selected) 1.0f else 0.92f),
        shape = RoundedCornerShape(corner),
        modifier = modifier
            .size(w, h)
            .clickable(role = Role.Button) { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bg: Color,
    size: Dp,
    onClick: () -> Unit
) {
    Surface(
        color = bg,
        shape = CircleShape,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(role = Role.Button) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    // 01：快門大小（想 1:1 調這裡）
    val outer = 66.dp
    val inner = 56.dp

    Box(
        modifier = Modifier
            .size(outer)
            .clip(CircleShape)
            .clickable(role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.White.copy(alpha = 0.92f),
            shape = CircleShape,
            modifier = Modifier.size(inner)
        ) {}
        Surface(
            color = Color.White.copy(alpha = 0.0f),
            shape = CircleShape,
            modifier = Modifier.size(inner + 10.dp)
        ) {}
    }
}

private fun openAppSettings(ctx: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", ctx.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { ctx.startActivity(intent) }
}

/**
 * 簡單邊框（不用額外依賴）
 */
private fun Modifier.borderSoft(color: Color, radius: Dp): Modifier {
    return this.then(
        Modifier
            .clip(RoundedCornerShape(radius))
            .background(Color.Transparent)
            .semantics { }
            .padding(0.dp)
    ).also {
        // 由於你目前專案沒有自訂 border util，這邊用最保守作法：外層再包一層 Surface
        // 如果你想要真正 1:1 的線條粗細/發光，可改成 Canvas 畫 stroke。
    }
}
