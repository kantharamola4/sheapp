package com.example.shesafe

import android.content.Context
import android.hardware.Camera
import android.os.Handler
import android.util.Log

object CameraCapture {
    private val handler = Handler()

    fun startCapturing(context: Context) {
        capture(Camera.CameraInfo.CAMERA_FACING_BACK)
        capture(Camera.CameraInfo.CAMERA_FACING_FRONT)
    }

    private fun capture(cameraId: Int) {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    val camera = Camera.open(cameraId)
                    camera.takePicture(null, null, Camera.PictureCallback { _, _ ->
                        Log.d("Camera", "Captured from camera $cameraId")
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
