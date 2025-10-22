package com.example.shesafe

import android.content.Context
import android.hardware.Camera
import android.os.Handler
import android.util.Log

object CameraCapture {
    private val handler = Handler()

    fun startCapturing(context: Context) {
        capture(context, Camera.CameraInfo.CAMERA_FACING_BACK)
        capture(context, Camera.CameraInfo.CAMERA_FACING_FRONT)
    }

    private fun capture(context: Context, cameraId: Int) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    val camera = Camera.open(cameraId)
                    camera.takePicture(null, null, Camera.PictureCallback { data, _ ->
                        try {
                            val file = java.io.File(context.filesDir, "img_${cameraId}_${System.currentTimeMillis()}.jpg")
                            java.io.FileOutputStream(file).use { it.write(data) }
                            Log.d("Camera", "Captured from camera $cameraId: ${file.absolutePath}")
                        } catch (e: Exception) {
                            Log.e("Camera", "Save error: ${e.message}")
                        }
                        camera.release()
                    })
                } catch (e: Exception) {
                    Log.e("Camera", "Error: ${e.message}")
                }
                handler.postDelayed(this, 5000)
            }
        }, 1000)
    }
}
