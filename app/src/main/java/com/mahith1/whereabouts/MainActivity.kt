package com.mahith1.whereabouts

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import com.mahith1.whereabouts.screens.AuthScreen
import com.mahith1.whereabouts.screens.HomeScreen
import com.mahith1.whereabouts.ui.theme.WhereaboutsTheme
import com.google.firebase.FirebaseApp
import com.mahith1.whereabouts.LocationList
import com.mahith1.whereabouts.util.LocationUploader
import com.mahith1.whereabouts.screens.ShareScreen
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import android.widget.Toast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check Google Play Services availability
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        
        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode, 9000)?.show()
            } else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }
        
        FirebaseApp.initializeApp(this)
        setContent {
            WhereaboutsTheme {
                var isLoggedIn by remember { mutableStateOf(false) }
                var userId by remember { mutableStateOf("") }
                var showShareScreen by remember { mutableStateOf(false) }

                DisposableEffect(isLoggedIn) {
                    onDispose {
                        if (!isLoggedIn) {
                            LocationUploader.stopLocationUpdates()
                        }
                    }
                }

                if (isLoggedIn) {
                    if (showShareScreen) {
                        ShareScreen(
                            userId = userId,
                            onNavigateBack = { showShareScreen = false }
                        )
                    } else {
                        HomeScreen(
                            userId = userId,
                            onProfileClick = {
                                // Handle profile click
                            },
                            onShareClick = { showShareScreen = true }
                        )
                        // Start location updates when logged in
                        LocationUploader.startLocationUpdates(
                            context = this@MainActivity,
                            userId = userId
                        )
                    }
                } else {
                    AuthScreen(
                        onLoginSuccess = { email ->
                            userId = email
                            isLoggedIn = true
                            LocationUploader.startLocationUpdates(
                                context = this@MainActivity,
                                userId = email
                            )
                        }
                    )
                }
            }
        }
    }
    @Composable
    fun MainScreen() {
        LocationList()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocationUploader.stopLocationUpdates()
    }
}





