package com.mahith1.whereabouts

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.mahith1.whereabouts.data.LocationRepository
class LocationViewModel : ViewModel() {
    private val repository = LocationRepository()
    private var isListening = false

    val locations = repository.locations

    fun startListening(userId: String) {
        if (!isListening) {
            repository.startListening(userId)
            isListening = true
        }
    }

    fun stopListening() {
        if (isListening) {
            repository.stopListening()
            isListening = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}
