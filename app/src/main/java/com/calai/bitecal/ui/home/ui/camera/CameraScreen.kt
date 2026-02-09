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

    // ===== 模式 =====
    var mode by rememberSaveable { mutableStateOf(CameraMode.FOOD) }

    // ✅ 外部要求切模式時同步（只在外部改 initialMode 時發生）
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
    val scannedOnce = remember { AtomicBoolean(false) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    val latestBusy by rememberUpdatedState(busy)
    val latestOnBarcodeScanned by rememberUpdatedState(onBarcodeScanned)

    val barcodeAnalyzer = remember {
        BarcodeScannerProcessor { code ->
            if (latestBusy) return@BarcodeScannerProcessor
            if (scannedOnce.compareAndSet(false, true)) {
                latestOnBarcodeScanned(code)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { barcodeAnalyzer.close() }
            runCatching { analysisExecutor.shutdown() }
        }
    }

    // 模式切換時，重置一次掃描鎖
    LaunchedEffect(mode) {
        scannedOnce.set(false)
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

private fun openAppSettings(ctx: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", ctx.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { ctx.startActivity(intent) }
}
