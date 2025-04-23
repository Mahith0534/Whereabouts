package com.mahith1.whereabouts

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LocationData(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isValid(): Boolean {
        return name.isNotBlank() && 
               latitude != 0.0 && 
               longitude != 0.0 &&
               latitude >= -90 && latitude <= 90 &&
               longitude >= -180 && longitude <= 180
    }

    fun getFormattedTime(): String {
        return try {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val date = Date(timestamp)
            sdf.format(date)
        } catch (e: Exception) {
            "Time unavailable"
        }
    }
}
