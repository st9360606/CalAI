package com.calai.bitecal.ui.home.ui.camera.barcode

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ✅ 不做「永久只吃一次」鎖（避免掃一次就死）
 * ✅ 是否要接受（例如 busy 上傳中）交給 shouldAccept
 */
class BarcodeScannerProcessor(
    private val onBarcode: (rawValue: String) -> Unit,
    private val shouldAccept: () -> Boolean = { true },
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    private val busy = AtomicBoolean(false)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // ✅ 外層不允許就略過（例如正在上傳）
        if (!shouldAccept()) {
            imageProxy.close()
            return
        }

        // ✅ 同時只跑一個 MLKit 任務
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (!shouldAccept()) return@addOnSuccessListener

                val raw = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue ?: return@addOnSuccessListener
                val cleaned = raw.filter { it.isDigit() }

                if (cleaned.length in 6..32) {
                    onBarcode(cleaned)
                }
            }
            .addOnCompleteListener {
                busy.set(false)
                imageProxy.close()
            }
    }

    fun close() {
        scanner.close()
    }
}
