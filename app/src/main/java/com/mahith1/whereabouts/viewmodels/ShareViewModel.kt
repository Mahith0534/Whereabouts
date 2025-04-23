package com.mahith1.whereabouts.viewmodels

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ShareViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val _sharedUsers = MutableStateFlow<List<String>>(emptyList())
    val sharedUsers: StateFlow<List<String>> = _sharedUsers
    private val _isSharing = MutableStateFlow(true)
    val isSharing: StateFlow<Boolean> = _isSharing

    fun loadSharedUsers(userId: String) {
        firestore.collection("shares")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val users = document.get("sharedWith") as? List<String> ?: emptyList()
                    _sharedUsers.value = users
                    // Explicitly cast to Boolean to avoid type mismatch
                    _isSharing.value = (document.get("isSharing") as? Boolean) ?: true
                } else {
                    // Initialize document if it doesn't exist
                    val initialData = mapOf(
                        "sharedWith" to emptyList<String>(),
                        "isSharing" to true
                    )
                    firestore.collection("shares")
                        .document(userId)
                        .set(initialData)
                    _sharedUsers.value = emptyList()
                    _isSharing.value = true
                }
            }
    }

    fun toggleLocationSharing(
        userId: String,
        isSharing: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // First update Firestore
        firestore.collection("shares")
            .document(userId)
            .update(mapOf(
                "isSharing" to isSharing,
                "lastUpdated" to FieldValue.serverTimestamp()
            ))
            .addOnSuccessListener {
                _isSharing.value = isSharing
                // If turning off sharing, also clear location
                if (!isSharing) {
                    firestore.collection("locations")
                        .document(userId)
                        .delete()
                        .addOnSuccessListener {
                            onSuccess()
                        }
                        .addOnFailureListener { e ->
                            onError(e.message ?: "Failed to clear location")
                        }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { e ->
                _isSharing.value = !isSharing // Revert local state
                onError(e.message ?: "Failed to update sharing status")
            }
    }

    fun shareLocation(userId: String, targetEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentSharedUsers = _sharedUsers.value.toMutableList()
        if (!currentSharedUsers.contains(targetEmail)) {
            currentSharedUsers.add(targetEmail)
            updateSharedUsers(userId, currentSharedUsers, onSuccess, onError)
        }
    }

    fun stopSharing(userId: String, targetEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentSharedUsers = _sharedUsers.value.toMutableList()
        if (currentSharedUsers.contains(targetEmail)) {
            currentSharedUsers.remove(targetEmail)
            updateSharedUsers(userId, currentSharedUsers, onSuccess, onError)
        }
    }

    private fun updateSharedUsers(
        userId: String, 
        sharedUsers: List<String>, 
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore.collection("shares")
            .document(userId)
            .set(mapOf("sharedWith" to sharedUsers))
            .addOnSuccessListener {
                _sharedUsers.value = sharedUsers
                onSuccess()
            }
            .addOnFailureListener {
                onError(it.message ?: "Failed to update sharing permissions")
            }
    }
}


