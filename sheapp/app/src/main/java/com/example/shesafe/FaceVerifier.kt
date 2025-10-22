package com.example.shesafe

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection

object FaceVerifier {
    fun detectFace(bitmap: Bitmap, onResult: (Boolean) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val detector = FaceDetection.getClient()
        detector.process(image)
            .addOnSuccessListener { faces -> onResult(faces.isNotEmpty()) }
            .addOnFailureListener { onResult(false) }
    }
}


