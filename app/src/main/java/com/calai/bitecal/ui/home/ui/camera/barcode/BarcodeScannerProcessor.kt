package com.calai.bitecal.ui.home.ui.camera.barcode

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

class BarcodeScannerProcessor(
    private val onBarcode: (rawValue: String) -> Unit
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

        // 避免同時多個 frame 丟進 ML Kit（手機會燙、也會狂觸發）
        if (!busy.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                if (!first.isNullOrBlank()) onBarcode(first)
            }
            .addOnFailureListener {
                // ignore (你可以加 log)
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
