package com.calai.bitecal.ui.home.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calai.bitecal.R
import com.calai.bitecal.ui.home.ui.camera.barcode.BarcodeScannerProcessor
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

enum class CameraMode { FOOD, BARCODE, LABEL }

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CameraScreen(
    onClose: () -> Unit,
    onImagePicked: (mode: CameraMode, uri: Uri) -> Unit,
    onShutterCaptured: (mode: CameraMode, file: File) -> Unit,
    onBarcodeScanned: (barcode: String) -> Unit,
    busy: Boolean = false, // ✅ 上傳中鎖 UI
    enableCameraX: Boolean = true,
    initialMode: CameraMode = CameraMode.FOOD, // ✅ NEW：外部指定初始模式
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 你之前踩過的坑：LocalActivityResultRegistryOwner 可能為 null
    val registryOwner = LocalActivityResultRegistryOwner.current

    val lastBarcode = remember { mutableStateOf<String?>(null) }
    val lastBarcodeAtMs = remember { mutableLongStateOf(0L) }

    // ===== 模式 =====
    var mode by rememberSaveable { mutableStateOf(initialMode) }

    // ✅ 外部要求切模式時同步（例如從 Detail 點「改用 Label/Barcode」回來）
    LaunchedEffect(initialMode) {
        mode = initialMode
    }

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
                if (uri != null) onImagePicked(mode, uri)
            }
        }

    // ===== CameraX PreviewView =====
    val previewView = remember {
        PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    // ✅ UseCases（都用 remember，避免重建）
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setJpegQuality(90)
            .build()
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    val mainExecutor: Executor = remember(ctx) { ContextCompat.getMainExecutor(ctx) }

    // ===== Barcode Analyzer（只在 BARCODE 模式開）=====
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val latestBusy by rememberUpdatedState(busy)
    val latestOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)

    val barcodeAnalyzer = remember {
        BarcodeScannerProcessor(
            onBarcode = { code ->
                // ✅ 上傳中不送
                if (latestBusy) return@BarcodeScannerProcessor

                // ✅ 同碼 3 秒內只吃一次（防重送）
                val now = System.currentTimeMillis()
                val prev = lastBarcode.value
                val prevAt = lastBarcodeAtMs.longValue
                if (prev == code && (now - prevAt) < 3000) return@BarcodeScannerProcessor

                lastBarcode.value = code
                lastBarcodeAtMs.longValue = now

                latestOnBarcodeScanned(code)
            },
            shouldAccept = {
                // ✅ 只要「不在 busy」就允許 analyzer 吃碼
                !latestBusy
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { barcodeAnalyzer.close() }
            runCatching { analysisExecutor.shutdown() }
        }
    }

    // ✅ 切到 BARCODE 時把節流清掉，讓使用者回來可立刻掃
    LaunchedEffect(mode) {
        if (mode == CameraMode.BARCODE) {
            lastBarcode.value = null
            lastBarcodeAtMs.longValue = 0L
        }
    }

    // ===== Torch (Flash) =====
    var torchOn by rememberSaveable { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }

    val boundCamera = remember { mutableStateOf<Camera?>(null) }
    val boundProvider = remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(enableCameraX, hasCameraPerm, lifecycleOwner, mode) {
        if (!enableCameraX || !hasCameraPerm) {
            torchOn = false
            boundCamera.value = null
            boundProvider.value = null
            hasFlashUnit = false
            return@DisposableEffect onDispose { }
        }

        val active = AtomicBoolean(true)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
        val executor = ContextCompat.getMainExecutor(ctx)

        val listener = Runnable {
            if (!active.get()) return@Runnable

            runCatching {
                val provider = cameraProviderFuture.get()
                boundProvider.value = provider

                provider.unbindAll()

                preview.setSurfaceProvider(previewView.surfaceProvider)

                runCatching { imageCapture.targetRotation = previewView.display.rotation }
                runCatching { imageAnalysis.targetRotation = previewView.display.rotation }

                val useCases = mutableListOf(preview, imageCapture)
                if (mode == CameraMode.BARCODE) {
                    imageAnalysis.setAnalyzer(analysisExecutor, barcodeAnalyzer)
                    useCases.add(imageAnalysis)
                } else {
                    imageAnalysis.clearAnalyzer()
                }

                val camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    *useCases.toTypedArray()
                )

                boundCamera.value = camera
                hasFlashUnit = camera.cameraInfo.hasFlashUnit()

                if (hasFlashUnit) {
                    runCatching { camera.cameraControl.enableTorch(torchOn) }
                        .onFailure {
                            Log.w("CameraScreen", "enableTorch failed after bind", it)
                            torchOn = false
                        }
                } else {
                    torchOn = false
                }
            }.onFailure {
                Log.e("CameraScreen", "CameraX bind failed", it)
                boundCamera.value = null
                boundProvider.value = null
                hasFlashUnit = false
                torchOn = false
            }
        }

        cameraProviderFuture.addListener(listener, executor)

        onDispose {
            active.set(false)
            runCatching { imageAnalysis.clearAnalyzer() }
            runCatching { boundProvider.value?.unbindAll() }
            boundCamera.value = null
            boundProvider.value = null
            hasFlashUnit = false
            torchOn = false
        }
    }

    LaunchedEffect(torchOn, enableCameraX, hasCameraPerm) {
        val camera = boundCamera.value ?: return@LaunchedEffect

        val flashOk = runCatching { camera.cameraInfo.hasFlashUnit() }.getOrDefault(false)
        hasFlashUnit = flashOk

        if (!enableCameraX || !hasCameraPerm || !flashOk) {
            if (torchOn) torchOn = false
            return@LaunchedEffect
        }

        runCatching { camera.cameraControl.enableTorch(torchOn) }
            .onFailure {
                Log.w("CameraScreen", "enableTorch failed", it)
                torchOn = false
            }
    }

    // ===== UI tokens ...（你原本 UI 完整保留）=====
    val overlayWhite = Color(0xFFEDEFF4).copy(alpha = 0.95f)
    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val frameSizeRatio = 0.78f
    val frameOffsetYRatio = -0.12f

    val sidePadding = 18.dp
    val tileGap = 6.dp
    val tileH = 72.dp
    val tileCorner = 14.dp

    val tileBg = Color(0xFFE9EBEF).copy(alpha = 0.92f)
    val tileText = Color.Black
    val tileIcon = Color.Black

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        val maxW = maxWidth

        val tileW: Dp = remember(maxW, sidePadding, tileGap) {
            (maxW - (sidePadding * 2) - (tileGap * 3)) / 4f
        }

        if (enableCameraX) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().background(Color.Black))
        }

        val closeBtnSize = 40.dp
        val closeIconSize = 26.dp

        Surface(
            color = Color(0xFF6C6C70).copy(alpha = 0.70f),
            shape = CircleShape,
            modifier = Modifier
                .padding(start = 24.dp, top = topPadding + 5.dp)
                .size(closeBtnSize)
                .clip(CircleShape)
                .clickable(role = Role.Button) { onClose() }
                .testTag("camera_close")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "close",
                    tint = Color.White,
                    modifier = Modifier.size(closeIconSize)
                )
            }
        }

        ScanFrameOverlay(
            modifier = Modifier.fillMaxSize(),
            mode = mode,
            color = overlayWhite,
            frameSizeRatio = frameSizeRatio,
            frameOffsetYRatio = frameOffsetYRatio,
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 43.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(tileGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModeTile(
                    width = tileW,
                    height = tileH,
                    corner = tileCorner,
                    label = stringResource(R.string.camera_mode_food),
                    icon = Icons.Outlined.Restaurant,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = mode == CameraMode.FOOD,
                    onClick = { mode = CameraMode.FOOD },
                    modifier = Modifier.testTag("mode_food")
                )
                ModeTile(
                    width = tileW,
                    height = tileH,
                    corner = tileCorner,
                    label = stringResource(R.string.camera_mode_barcode),
                    icon = Icons.Outlined.QrCodeScanner,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = mode == CameraMode.BARCODE,
                    onClick = { mode = CameraMode.BARCODE },
                    modifier = Modifier.testTag("mode_barcode")
                )
                ModeTile(
                    width = tileW,
                    height = tileH,
                    corner = tileCorner,
                    label = stringResource(R.string.camera_mode_label),
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    bg = tileBg,
                    textColor = tileText,
                    iconTint = tileIcon,
                    selected = mode == CameraMode.LABEL,
                    onClick = { mode = CameraMode.LABEL },
                    modifier = Modifier.testTag("mode_label")
                )
                ModeTile(
                    width = tileW,
                    height = tileH,
                    corner = tileCorner,
                    label = stringResource(R.string.camera_mode_album),
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

            Spacer(Modifier.height(26.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = sidePadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val flashEnabled = enableCameraX && hasCameraPerm && hasFlashUnit && (boundCamera.value != null)

                CircleIconButton(
                    modifier = Modifier
                        .testTag("camera_flash")
                        .offset(x = 8.dp, y = (-3).dp),
                    icon = if (torchOn) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    tint = Color(0xFF2B2F36).copy(alpha = 0.7f),
                    bg = Color.White,
                    size = 37.dp,
                    enabled = flashEnabled,
                    onClick = {
                        torchOn = nextTorchState(
                            current = torchOn,
                            enableCameraX = enableCameraX,
                            hasCameraPerm = hasCameraPerm,
                            hasFlashUnit = hasFlashUnit
                        )
                    }
                )

                val shutterEnabled =
                    !busy && enableCameraX && hasCameraPerm && (boundCamera.value != null) && (mode != CameraMode.BARCODE)

                ShutterButton(
                    enabled = shutterEnabled,
                    onClick = {
                        if (!hasCameraPerm) {
                            requestCameraPermLauncher?.launch(Manifest.permission.CAMERA) ?: openAppSettings(ctx)
                            return@ShutterButton
                        }
                        if (mode == CameraMode.BARCODE) return@ShutterButton

                        val outFile = File(ctx.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
                        val output = ImageCapture.OutputFileOptions.Builder(outFile).build()

                        imageCapture.takePicture(
                            output,
                            mainExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onShutterCaptured(mode, outFile)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraScreen", "capture failed", exception)
                                    runCatching { outFile.delete() }
                                }
                            }
                        )
                    },
                    modifier = Modifier.testTag("camera_shutter")
                )

                Spacer(Modifier.size(28.dp))
            }

            if (!hasCameraPerm) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.camera_need_permission),
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

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ScanFrameOverlay(
    modifier: Modifier = Modifier,
    mode: CameraMode,
    color: Color,
    frameSizeRatio: Float,
    frameOffsetYRatio: Float,
    foodOffsetYRatio: Float = -0.06f,
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
                CornerBrackets(
                    color = color,
                    boxSize = frameSize,
                    stroke = 5.dp,
                    cornerLen = 36.dp,
                    arcRadius = 12.dp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = foodOffsetY)
                        .testTag("scan_frame_food")
                )
            }

            CameraMode.BARCODE -> {
                // === Tokens（想更像圖就調這裡）===
                val barcodeW = maxW * 0.85f
                val barcodeH = 180.dp
                val barcodeRadius = 18.dp
                val barcodeStroke = 4.dp

                // ✅ NEW：框往下移的量（你要再下就 +，要再上就 -）
                val barcodeExtraDown = 68.dp

                // ✅ 文字大小：大一點點（titleSmall -> titleMedium）
                val titleText = stringResource(R.string.camera_barcode_title)
                val titleGap = 12.dp        // 文字與框的距離
                val titleNudgeUp = 10.dp    // 微調：讓它更像你圖上的位置（想更靠近框就減小）

                // ✅ 框的實際 Y（讓文字跟著框走）
                val barcodeFrameY = frameOffsetY + barcodeExtraDown

                // 標題：固定在「框上方」
                Text(
                    text = titleText,
                    color = color,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = barcodeFrameY - (barcodeH / 2) - titleGap - titleNudgeUp)
                        .testTag("barcode_title")
                )

                // 框：往下移 + 加粗
                RoundedFrame(
                    color = color,
                    width = barcodeW,
                    height = barcodeH,
                    radius = barcodeRadius,
                    strokeWidth = barcodeStroke,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = barcodeFrameY)
                        .testTag("scan_frame_barcode")
                )
            }

            CameraMode.LABEL -> {
                // === Tokens（想更像圖就調這裡）===
                val labelW = maxW * 0.78f              // 寬度維持原本 0.70
                val labelH = maxW * 1.1f              // ✅ 高度加大（直向更高）
                val labelRadius = 18.dp
                val labelStroke = 4.dp                // ✅ 框線加粗（原本 2.dp）

                // ✅ NEW：框往下移的量（需要更下就 +，要更上就 -）
                val labelExtraDown = 56.dp

                // ✅ 標題（框上方）
                val titleText = stringResource(R.string.camera_label_title)
                val titleGap = 12.dp                  // 文字與框的距離
                val titleNudgeUp = 10.dp               // 微調：想更靠近框就減小

                // ✅ 框的實際 Y（文字跟著框走）
                val labelFrameY = frameOffsetY + labelExtraDown

                // 標題：固定在「框上方」
                Text(
                    text = titleText,
                    color = color,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = labelFrameY - (labelH / 2) - titleGap - titleNudgeUp)
                        .testTag("label_title")
                )

                // 框：高度更大 + 加粗
                RoundedFrame(
                    color = color,
                    width = labelW,
                    height = labelH,
                    radius = labelRadius,
                    strokeWidth = labelStroke,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = labelFrameY)
                        .testTag("scan_frame_label")
                )
            }

        }
    }
}

@Composable
private fun RoundedFrame(
    modifier: Modifier = Modifier,
    color: Color,
    width: Dp,
    height: Dp,
    radius: Dp,
    strokeWidth: Dp = 2.dp, // ✅ NEW：可調框線粗細
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .size(width, height)
            .clip(shape)
            .border(width = strokeWidth, color = color, shape = shape)
    )
}

@Composable
private fun CornerBrackets(
    color: Color,
    boxSize: Dp,
    stroke: Dp,
    cornerLen: Dp,
    arcRadius: Dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(boxSize)) {
        val w = size.width
        val h = size.height

        val s = stroke.toPx().coerceAtLeast(1f)
        val pad = s / 2f // ✅ 避免粗線貼邊被裁切

        val maxLen = (min(w, h) / 2f - pad).coerceAtLeast(1f)
        val len = cornerLen.toPx().coerceIn(1f, maxLen)

        // ✅ 圓弧半徑不能大於角長
        val r = arcRadius.toPx().coerceIn(1f, len)

        val style = Stroke(
            width = s,
            cap = StrokeCap.Round // ✅ 線段端點圓滑（更像 iOS）
        )

        // ---- Top-Left ----
        drawPath(
            path = Path().apply {
                moveTo(pad, pad + len)
                lineTo(pad, pad + r)
                // 圓弧：從 (pad, pad+r) 轉到 (pad+r, pad)
                quadraticTo(pad, pad, pad + r, pad)
                lineTo(pad + len, pad)
            },
            color = color,
            style = style
        )

        // ---- Top-Right ----
        drawPath(
            path = Path().apply {
                moveTo(w - pad - len, pad)
                lineTo(w - pad - r, pad)
                // 圓弧：轉到 (w-pad, pad+r)
                quadraticTo(w - pad, pad, w - pad, pad + r)
                lineTo(w - pad, pad + len)
            },
            color = color,
            style = style
        )

        // ---- Bottom-Left ----
        drawPath(
            path = Path().apply {
                moveTo(pad, h - pad - len)
                lineTo(pad, h - pad - r)
                // 圓弧：轉到 (pad+r, h-pad)
                quadraticTo(pad, h - pad, pad + r, h - pad)
                lineTo(pad + len, h - pad)
            },
            color = color,
            style = style
        )

        // ---- Bottom-Right ----
        drawPath(
            path = Path().apply {
                moveTo(w - pad - len, h - pad)
                lineTo(w - pad - r, h - pad)
                // 圓弧：轉到 (w-pad, h-pad-r)
                quadraticTo(w - pad, h - pad, w - pad, h - pad - r)
                lineTo(w - pad, h - pad - len)
            },
            color = color,
            style = style
        )
    }
}



@Composable
private fun ModeTile(
    width: Dp,
    height: Dp,
    corner: Dp,
    label: String,
    icon: ImageVector,
    bg: Color,
    textColor: Color,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(corner)
    val interactionSource = remember { MutableInteractionSource() }

    // ✅ selected：淡淡邊框（最不破壞風格）
    val border = if (selected) BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f)) else null

    // ✅ 可選：selected 時背景稍微更實（想更明顯再開）
    val bgAlpha = if (selected) 0.96f else 0.92f

    Surface(
        color = bg.copy(alpha = bgAlpha),
        shape = shape,
        border = border,                 // ✅ NEW
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(width, height)
            .clip(shape)                 // ✅ 保持圓角一致
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.size(3.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CircleIconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    size: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        color = bg,
        shape = CircleShape,
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button
            ) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ShutterButton(
    enabled: Boolean,          // ✅ NEW：補上 enabled
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outer = 72.dp
    val inner = 60.dp

    Box(
        modifier = modifier
            .size(outer)
            .clip(CircleShape)
            .alpha(if (enabled) 1f else 0.45f) // ✅ disabled 變淡
            .clickable(
                enabled = enabled,            // ✅ disabled 不可點（解掉誤觸）
                role = Role.Button
            ) { onClick() },
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
