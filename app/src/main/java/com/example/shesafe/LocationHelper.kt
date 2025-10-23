package com.example.shesafe

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices

object LocationHelper {

    fun getCurrentLocation(context: Context, callback: (Location?) -> Unit) {
        val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

        // Check permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            callback(null)
            return
        }

        // Check if location services are enabled
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && 
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Location services are disabled
            Toast.makeText(context, "Location services are disabled. Please enable GPS.", Toast.LENGTH_LONG).show()
            callback(null)
            return
        }

        // Try to get last known location first
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    callback(location)
                } else {
                    // If last location is null, request a fresh location
                    requestFreshLocation(fusedLocationClient, callback)
                }
            }
            .addOnFailureListener { exception ->
                // If getting last location fails, try to request fresh location
                requestFreshLocation(fusedLocationClient, callback)
            }
    }

    private fun requestFreshLocation(fusedLocationClient: FusedLocationProviderClient, callback: (Location?) -> Unit) {
        val locationRequest = LocationRequest.Builder(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            10000 // 10 seconds
        ).apply {
            setMaxUpdateDelayMillis(15000) // 15 seconds max delay
            setMinUpdateIntervalMillis(5000) // 5 seconds min interval
        }.build()

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location = locationResult.lastLocation
                    callback(location)
                }
            },
            android.os.Looper.getMainLooper()
        )
    }
}
