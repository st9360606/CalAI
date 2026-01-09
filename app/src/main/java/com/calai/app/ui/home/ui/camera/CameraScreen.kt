package com.calai.app.ui.home.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

enum class CameraMode { FOOD, BARCODE, LABEL }

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onImagePicked: (Uri) -> Unit,
    enableCameraX: Boolean = true // ✅ 測試/Preview 可關閉，避免 AndroidView + 相機干擾
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 你之前踩過的坑：LocalActivityResultRegistryOwner 可能為 null
    val registryOwner = LocalActivityResultRegistryOwner.current

    // ===== 模式 =====
    var mode by rememberSaveable { mutableStateOf(CameraMode.FOOD) }

    // ===== 權限 =====
    var hasCameraPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val requestCameraPermLauncher =
        registryOwner?.let {
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                hasCameraPerm = granted
            }
        }

    val pickImageLauncher =
        registryOwner?.let {
            rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                if (uri != null) onImagePicked(uri)
            }
        }

    // ===== CameraX PreviewView =====
    val previewView = remember {
        PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    // ✅ 修正：沒有權限就不要 bind
    DisposableEffect(enableCameraX, hasCameraPerm, lifecycleOwner) {
        if (!enableCameraX || !hasCameraPerm) {
            return@DisposableEffect onDispose { }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        val executor = ContextCompat.getMainExecutor(ctx)

        val listener = Runnable {
            runCatching {
                val provider = cameraProviderFuture.get()
                provider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview
                )
            }
        }

        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    // ===== 1:1 Tokens（你要微調就改這裡）=====
    val overlayWhite = Color(0xFFEDEFF4).copy(alpha = 0.95f)
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 掃描框：偏上 + 佔寬比例
    val frameSizeRatio = 0.78f
    val frameOffsetYRatio = -0.12f

    // tile：左右 padding + 間距固定，寬度用螢幕自動算
    val sidePadding = 18.dp
    val tileGap = 10.dp
    val tileH = 58.dp
    val tileCorner = 14.dp

    val tileBg = Color(0xFFE9EBEF).copy(alpha = 0.92f)
    val tileText = Color(0xFF2B2F36)
    val tileIcon = Color(0xFF606774)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ✅ 讓 IDE 明確看到「使用了 BoxWithConstraints scope」
        val maxW = maxWidth

        // ✅ 用 remember，尺寸變化才重算
        val tileW: Dp = remember(maxW, sidePadding, tileGap) {
            (maxW - (sidePadding * 2) - (tileGap * 3)) / 4f
        }

        // 相機預覽（可關閉以便測試/preview）
        if (enableCameraX) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black))
        }

        // ===== 左上角 X =====
        // ✅ Tokens：想再大就改這兩個
        val closeBtnSize = 40.dp
        val closeIconSize = 26.dp

        Surface(
            color = Color(0xFF6C6C70).copy(alpha = 0.70f),
            shape = CircleShape,
            modifier = Modifier
                .padding(start = 24.dp, top = topPadding + 5.dp) // ✅ 稍微往上貼一點點（按鈕變大時更協調）
                .size(closeBtnSize) // ✅ 34 -> 44
                .clip(CircleShape)
                .clickable(role = Role.Button) { onClose() }
                .testTag("camera_close")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "close",
                    tint = Color.White,
                    modifier = Modifier.size(closeIconSize) // ✅ 18 -> 22
                )
            }
        }

        // ===== 中間掃描框 =====
        ScanFrameOverlayV2(
            mode = mode,
            color = overlayWhite,
            frameSizeRatio = frameSizeRatio,
            frameOffsetYRatio = frameOffsetYRatio,
            modifier = Modifier.fillMaxSize()
        )

        // ===== 底部：tiles（上）+ 快門列（下）=====
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 4 個 tiles（在上）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(tileGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeTileV2(
                    width = tileW,
                    height = tileH,
                    corner = tileCorner,
                    label = "扫描食物",
                    icon = Icons.Outlined.Restaurant,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = mode == CameraMode.FOOD,
                    onClick = { mode = CameraMode.FOOD },
                    modifier = Modifier.testTag("mode_food")
                )
                ModeTileV2(
                    width = tileW,
                    height = tileH,
                    corner = tileCorner,
                    label = "条形码",
                    icon = Icons.Outlined.QrCodeScanner,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = mode == CameraMode.BARCODE,
                    onClick = { mode = CameraMode.BARCODE },
                    modifier = Modifier.testTag("mode_barcode")
                )
                ModeTileV2(
                    width = tileW,
                    height = tileH,
                    corner = tileCorner,
                    label = "食品标签",
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = mode == CameraMode.LABEL,
                    onClick = { mode = CameraMode.LABEL },
                    modifier = Modifier.testTag("mode_label")
                )
                ModeTileV2(
                    width = tileW,
                    height = tileH,
                    corner = tileCorner,
                    label = "相册",
                    icon = Icons.Outlined.Image,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = false,
                    onClick = {
                        if (pickImageLauncher != null) {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            openAppSettings(ctx)
                        }
                    },
                    modifier = Modifier.testTag("mode_album")
                )
            }

            Spacer(Modifier.height(14.dp))

            // 快門列（在下）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sidePadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButtonV2(
                    icon = Icons.Outlined.FlashOn,
                    tint = Color.White,
                    bg = Color(0xFF6C6C70).copy(alpha = 0.55f),
                    size = 34.dp,
                    onClick = { /* TODO torch */ },
                    modifier = Modifier.testTag("camera_flash")
                )

                ShutterButtonV2(
                    onClick = { /* TODO capture */ },
                    modifier = Modifier.testTag("camera_shutter")
                )

                Spacer(Modifier.size(34.dp))
            }

            // 沒相機權限提示
            if (!hasCameraPerm) {
                Spacer(Modifier.height(8.dp))
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
                        .testTag("camera_need_permission")
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ScanFrameOverlayV2(
    mode: CameraMode,
    color: Color,
    frameSizeRatio: Float,
    frameOffsetYRatio: Float,
    foodOffsetYRatio: Float = -0.05f,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        // ✅ 讓 IDE 明確看到「使用了 BoxWithConstraints scope」
        val maxW = maxWidth
        val maxH = maxHeight

        // ✅ 用 remember，尺寸變化才重算
        val frameSize: Dp = remember(maxW, frameSizeRatio) { maxW * frameSizeRatio }
        val frameOffsetY: Dp = remember(maxH, frameOffsetYRatio) { maxH * frameOffsetYRatio }
        val foodOffsetY: Dp = remember(maxH, foodOffsetYRatio) { maxH * foodOffsetYRatio }
        when (mode) {
            CameraMode.FOOD -> {
                CornerBracketsV2(
                    color = color,
                    size = frameSize,
                    stroke = 3.dp,
                    cornerLen = 26.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = foodOffsetY)
                        .offset(y = 0.dp) // ✅ 置中：不要再用 frameOffsetY
                        .testTag("scan_frame_food")
                )
            }

            CameraMode.BARCODE -> {
                RoundedFrameV2(
                    color = color,
                    width = maxW * 0.74f,
                    height = 110.dp,
                    radius = 18.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = frameOffsetY) // 維持原本偏上
                        .testTag("scan_frame_barcode")
                )
            }

            CameraMode.LABEL -> {
                RoundedFrameV2(
                    color = color,
                    width = maxW * 0.70f,
                    height = maxW * 0.70f,
                    radius = 18.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = frameOffsetY) // 維持原本偏上
                        .testTag("scan_frame_label")
                )
            }
        }

    }
}

@Composable
private fun RoundedFrameV2(
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
            .border(width = 2.dp, color = color, shape = RoundedCornerShape(radius))
    )
}

@Composable
private fun CornerBracketsV2(
    color: Color,
    size: Dp,
    stroke: Dp,
    cornerLen: Dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(size)) {
        val c = color
        val s = stroke
        val len = cornerLen

        // 左上
        Box(Modifier.align(Alignment.TopStart).size(len)) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.height(s).fillMaxWidth().background(c))
                Box(Modifier.width(s).fillMaxSize().background(c))
            }
        }
        // 右上
        Box(Modifier.align(Alignment.TopEnd).size(len)) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.height(s).fillMaxWidth().background(c))
                Box(Modifier.width(s).fillMaxSize().align(Alignment.TopEnd).background(c))
            }
        }
        // 左下
        Box(Modifier.align(Alignment.BottomStart).size(len)) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.height(s).fillMaxWidth().align(Alignment.BottomStart).background(c))
                Box(Modifier.width(s).fillMaxSize().background(c))
            }
        }
        // 右下
        Box(Modifier.align(Alignment.BottomEnd).size(len)) {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.height(s).fillMaxWidth().align(Alignment.BottomEnd).background(c))
                Box(Modifier.width(s).fillMaxSize().align(Alignment.TopEnd).background(c))
            }
        }
    }
}

@Composable
private fun ModeTileV2(
    width: Dp,
    height: Dp,
    corner: Dp,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    textColor: Color,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg.copy(alpha = if (selected) 1.0f else 0.92f),
        shape = RoundedCornerShape(corner),
        modifier = modifier
            .size(width, height)
            .clickable(role = Role.Button) { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 6.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
            )
        }
    }
}

@Composable
private fun CircleIconButtonV2(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bg: Color,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bg,
        shape = CircleShape,
        modifier = modifier
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
private fun ShutterButtonV2(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outer = 72.dp
    val inner = 60.dp

    Box(
        modifier = modifier
            .size(outer)
            .clip(CircleShape)
            .clickable(role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // 外圈（描邊）
        Box(
            modifier = Modifier
                .size(outer)
                .border(3.dp, Color.White.copy(alpha = 0.80f), CircleShape)
        )
        // 內圈（實心）
        Surface(
            color = Color.White.copy(alpha = 0.92f),
            shape = CircleShape,
            modifier = Modifier.size(inner)
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
