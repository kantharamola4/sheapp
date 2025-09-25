package com.example.shesafe

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.UUID

object EmergencyUploader {
    fun enqueueEmergencyUpload(context: Context, lat: Double?, lon: Double?) {
        val data = Data.Builder()
            .putDouble("lat", lat ?: Double.NaN)
            .putDouble("lon", lon ?: Double.NaN)
            .putLong("ts", System.currentTimeMillis())
            .build()

        val req = OneTimeWorkRequestBuilder<EmergencyUploadWorker>()
            .setInputData(data)
            .build()
        WorkManager.getInstance(context).enqueue(req)
    }
}

class EmergencyUploadWorker(appContext: Context, params: WorkerParameters) : Worker(appContext, params) {
    override fun doWork(): Result {
        val firestore = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance().reference

        val eventId = UUID.randomUUID().toString()
        val lat = inputData.getDouble("lat", Double.NaN)
        val lon = inputData.getDouble("lon", Double.NaN)
        val ts = inputData.getLong("ts", 0L)

        // Try to detect face presence from the most recent captured photo (best-effort)
        val latestPhoto = getLatestPhoto(applicationContext.filesDir)
        val facePresent = latestPhoto?.let { detectFacePresent(it) } ?: false

        val meta = hashMapOf(
            "eventId" to eventId,
            "timestamp" to ts,
            "latitude" to if (lat.isNaN()) null else lat,
            "longitude" to if (lon.isNaN()) null else lon,
            "facePresent" to facePresent
        )

        return try {
            firestore.collection("emergencies").document(eventId).set(meta).result

            // Upload recent audio and any captured photos if present
            val appFiles = applicationContext.filesDir
            appFiles.listFiles()?.filter { it.isFile && (it.name.endsWith(".3gp") || it.name.endsWith(".jpg")) }
                ?.sortedByDescending { it.lastModified() }
                ?.take(3)
                ?.forEach { file ->
                    val ref = storage.child("emergencies/$eventId/${file.name}")
                    ref.putFile(android.net.Uri.fromFile(file)).result
                }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun getLatestPhoto(dir: File): File? {
        return dir.listFiles()?.filter { it.isFile && it.name.endsWith(".jpg") }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun detectFacePresent(photo: File): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(photo.absolutePath) ?: return false
            val image = InputImage.fromBitmap(bitmap, 0)
            val detector = FaceDetection.getClient()
            val latch = CountDownLatch(1)
            var result = false
            detector.process(image)
                .addOnSuccessListener { faces ->
                    result = faces.isNotEmpty()
                    latch.countDown()
                }
                .addOnFailureListener {
                    result = false
                    latch.countDown()
                }
            latch.await(1500, TimeUnit.MILLISECONDS)
            result
        } catch (_: Exception) {
            false
        }
    }
}


