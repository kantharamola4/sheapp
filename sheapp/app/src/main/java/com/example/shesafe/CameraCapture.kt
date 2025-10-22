package com.example.shesafe

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.os.Handler
import android.util.Log

object CameraCapture {
    private val handler = Handler()

    fun startCapturing(context: Context, onFaceDetected: ((Boolean) -> Unit)? = null) {
        capture(context, Camera.CameraInfo.CAMERA_FACING_BACK, onFaceDetected)
        capture(context, Camera.CameraInfo.CAMERA_FACING_FRONT, onFaceDetected)
    }

    private fun capture(context: Context, cameraId: Int, onFaceDetected: ((Boolean) -> Unit)? = null) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    val camera = Camera.open(cameraId)
                    camera.takePicture(null, null, Camera.PictureCallback { data, _ ->
                        try {
                            val file = java.io.File(context.filesDir, "img_${cameraId}_${System.currentTimeMillis()}.jpg")
                            java.io.FileOutputStream(file).use { it.write(data) }
                            Log.d("Camera", "Captured from camera $cameraId: ${file.absolutePath}")
                            
                            // Perform face detection if callback provided
                            onFaceDetected?.let { callback ->
                                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                                if (bitmap != null) {
                                    FaceVerifier.detectFace(bitmap) { faceDetected ->
                                        Log.d("Camera", "Face detected: $faceDetected")
                                        callback(faceDetected)
                                    }
                                } else {
                                    callback(false)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("Camera", "Save error: ${e.message}")
                            onFaceDetected?.invoke(false)
                        }
                        camera.release()
                    })
                } catch (e: Exception) {
                    Log.e("Camera", "Error: ${e.message}")
                    onFaceDetected?.invoke(false)
                }
                handler.postDelayed(this, 5000)
            }
        }, 1000)
    }
}
