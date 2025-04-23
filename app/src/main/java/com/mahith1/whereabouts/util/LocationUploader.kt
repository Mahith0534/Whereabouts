package com.mahith1.whereabouts.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.firebase.firestore.FirebaseFirestore
import com.mahith1.whereabouts.LocationData

object LocationUploader {
    private const val TAG = "LocationUploader"
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private val firestore = FirebaseFirestore.getInstance()

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(context: Context, userId: String) {
        try {
            // Check if sharing is enabled before starting updates
            firestore.collection("shares")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val isSharing = document?.getBoolean("isSharing") ?: true
                    if (!isSharing) {
                        Log.d(TAG, "Location sharing is disabled for user: $userId")
                        return@addOnSuccessListener
                    }

                    Log.d(TAG, "Starting location updates for user: $userId")
                    fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

                    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                        .setWaitForAccurateLocation(false)
                        .setMinUpdateIntervalMillis(5000)
                        .setMaxUpdateDelayMillis(15000)
                        .build()

                    locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            locationResult.lastLocation?.let { location ->
                                uploadLocationToFirestore(LocationData(
                                    name = userId,
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    timestamp = System.currentTimeMillis()
                                ))
                            }
                        }
                    }

                    try {
                        fusedLocationClient?.requestLocationUpdates(
                            locationRequest,
                            locationCallback!!,
                            Looper.getMainLooper()
                        )?.addOnFailureListener { e ->
                            Log.e(TAG, "Failed to request location updates", e)
                            // Handle the error appropriately
                            when {
                                e is SecurityException -> {
                                    Log.e(TAG, "Location permission denied", e)
                                    // Handle permission denied
                                }
                                e.message?.contains("GooglePlayServices") == true -> {
                                    Log.e(TAG, "Google Play Services error", e)
                                    // Try to recover
                                    val googleApiAvailability = GoogleApiAvailability.getInstance()
                                    if (context is Activity) {
                                        googleApiAvailability.makeGooglePlayServicesAvailable(context)
                                    }
                                }
                                else -> {
                                    Log.e(TAG, "Unknown error requesting location updates", e)
                                }
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "Security exception while requesting location updates", e)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error checking sharing status", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in startLocationUpdates", e)
        }
    }

    private fun uploadLocationToFirestore(locationData: LocationData) {
        if (!locationData.isValid()) {
            Log.e(TAG, "Invalid location data: $locationData")
            return
        }

        Log.d(TAG, "Uploading location: $locationData")
        
        firestore.collection("locations")
            .document(locationData.name)
            .set(locationData)
            .addOnSuccessListener {
                Log.d(TAG, "Location successfully uploaded for user: ${locationData.name}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error uploading location for user: ${locationData.name}", e)
            }
    }

    fun stopLocationUpdates() {
        try {
            locationCallback?.let { callback ->
                fusedLocationClient?.removeLocationUpdates(callback)
            }
            locationCallback = null
            fusedLocationClient = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }
}
