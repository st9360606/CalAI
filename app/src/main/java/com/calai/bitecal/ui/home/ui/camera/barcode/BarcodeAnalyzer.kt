package com.calai.bitecal.ui.home.ui.camera.barcode

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.common.InputImage

/**
 * ✅ 只做一件事：從 ImageProxy -> ML Kit -> rawValue -> callback
 * 注意：imageProxy.close() 一定要在 complete 時呼叫，否則畫面會卡死
 */
class BarcodeAnalyzer(
    private val scanner: BarcodeScanner,
    private val onBarcode: (String) -> Unit,
    private val shouldAccept: () -> Boolean, // 用來擋重複觸發
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(image)
            .addOnSuccessListener { list ->
                if (!shouldAccept()) return@addOnSuccessListener
                val raw = list.firstOrNull()?.rawValue
                if (!raw.isNullOrBlank()) {
                    onBarcode(raw.trim())
                }
            }
            .addOnFailureListener {
                // MVP：忽略，讓下一幀繼續掃
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}
