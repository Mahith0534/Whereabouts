package com.mahith1.whereabouts.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.mahith1.whereabouts.LocationData

class LocationRepository {
    private val TAG = "LocationRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val _locations = MutableStateFlow<List<LocationData>>(emptyList())
    val locations: StateFlow<List<LocationData>> = _locations

    private var listener: ListenerRegistration? = null

    fun startListening(userId: String) {
        try {
            stopListening()
            
            Log.d(TAG, "Starting location listening for user: $userId")
            
            // First get the shared users list and sharing status
            firestore.collection("shares")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val sharedUsers = document.get("sharedWith") as? List<String> ?: emptyList()
                    Log.d(TAG, "Shared users: $sharedUsers")
                    
                    // Then listen for location updates
                    listener = firestore.collection("locations")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.e(TAG, "Error listening to locations", error)
                                return@addSnapshotListener
                            }

                            try {
                                if (snapshot != null) {
                                    val locationsList = snapshot.documents
                                        .mapNotNull { doc ->
                                            try {
                                                val location = doc.toObject(LocationData::class.java)
                                                Log.d(TAG, "Parsed location: $location")
                                                location
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error converting document: ${doc.id}", e)
                                                null
                                            }
                                        }
                                        .filter { location ->
                                            // Show location if it's the user's own location
                                            // or if it's from a shared user
                                            val shouldShow = location.name == userId || location.name in sharedUsers
                                            Log.d(TAG, "Location ${location.name} should show: $shouldShow")
                                            shouldShow
                                        }
                                    
                                    _locations.value = locationsList
                                    Log.d(TAG, "Updated locations. Count: ${locationsList.size}")
                                } else {
                                    _locations.value = emptyList()
                                    Log.d(TAG, "No locations found")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing snapshot", e)
                                _locations.value = emptyList()
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error getting shared users", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location listener", e)
        }
    }

    fun stopListening() {
        listener?.remove()
    }
}
