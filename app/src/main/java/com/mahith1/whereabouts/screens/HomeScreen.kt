package com.mahith1.whereabouts.screens
import android.os.Looper
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.maps.android.compose.MapType
import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.mahith1.whereabouts.LocationData
import com.mahith1.whereabouts.LocationViewModel
import com.mahith1.whereabouts.LocationList
import com.mahith1.whereabouts.R
import android.util.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.location.*
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.mahith1.whereabouts.components.ShareBottomSheet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    userId: String,
    onProfileClick: () -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    val locationViewModel: LocationViewModel = viewModel()
    val locations by locationViewModel.locations.collectAsState()
    
    // Debug log
    LaunchedEffect(locations) {
        Log.d("HomeScreen", "Locations updated: ${locations.size}")
    }
    
    // Start listening when the screen is created
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "Starting location listening for user: $userId")
        locationViewModel.startListening(userId)
    }

    // Clean up when the screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            locationViewModel.stopListening()
        }
    }

    var showShareSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val cameraPositionState = rememberCameraPositionState()

    // Update camera position when current location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
        }
    }

    val permissionState = rememberPermissionState(
        Manifest.permission.ACCESS_FINE_LOCATION,
        onPermissionResult = { isGranted ->
            if (!isGranted) {
                showPermissionDialog = true
            }
        }
    )

    // Request location immediately when the screen is created
    LaunchedEffect(Unit) {
        if (permissionState.status is PermissionStatus.Granted) {
            getCurrentLocation(fusedLocationClient) { latLng ->
                currentLocation = latLng
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
            }
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    // Update location when permission is granted
    LaunchedEffect(permissionState.status) {
        if (permissionState.status is PermissionStatus.Granted) {
            getCurrentLocation(fusedLocationClient) { latLng ->
                currentLocation = latLng
                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
            }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Location Permission Required") },
            text = { Text("This app needs location permission to show your location on the map.") },
                confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    permissionState.launchPermissionRequest()
                }) {
                    Text("Request Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Whereabouts")
                    }
                },
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = onShareClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Share Location")
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = true,  // Enable the "My Location" button
                    mapToolbarEnabled = true
                ),
                properties = MapProperties(
                    isMyLocationEnabled = permissionState.status is PermissionStatus.Granted,  // Enable the "My Location" layer
                    mapType = MapType.NORMAL,
                    isTrafficEnabled = true
                )
            ) {
                // Show all locations from Firestore
                locations.forEach { location ->
                    Log.d("HomeScreen", "Displaying marker for: ${location.name}")
                    Marker(
                        state = MarkerState(
                            position = LatLng(location.latitude, location.longitude)
                        ),
                        title = location.name,
                        snippet = "Last updated: ${location.getFormattedTime()}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (location.name == userId) BitmapDescriptorFactory.HUE_BLUE 
                            else BitmapDescriptorFactory.HUE_RED
                        )
                    )
                }
            }
        }
    }

    if (showShareSheet) {
        ShareBottomSheet(
            userId = userId,
            onDismiss = { showShareSheet = false }
        )
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationFetched: (LatLng) -> Unit
) {
    try {
        // First try to get last location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("HomeScreen", "Last location fetched: ${location.latitude}, ${location.longitude}")
                    onLocationFetched(LatLng(location.latitude, location.longitude))
                } else {
                    // If last location is null, request location updates
                    Log.d("HomeScreen", "Last location is null, requesting updates")
                    requestLocationUpdates(fusedLocationClient, onLocationFetched)
                }
            }
            .addOnFailureListener { e ->
                Log.e("HomeScreen", "Error getting last location", e)
                // Fallback to location updates on failure
                requestLocationUpdates(fusedLocationClient, onLocationFetched)
            }
    } catch (e: Exception) {
        Log.e("HomeScreen", "Error in getCurrentLocation", e)
    }
}

@SuppressLint("MissingPermission")
private fun requestLocationUpdates(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationFetched: (LatLng) -> Unit
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
        .setWaitForAccurateLocation(false)
        .setMinUpdateIntervalMillis(5000)
        .build()

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                Log.d("HomeScreen", "Location update received: ${location.latitude}, ${location.longitude}")
                onLocationFetched(LatLng(location.latitude, location.longitude))
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
    }

    fusedLocationClient.requestLocationUpdates(
        locationRequest,
        locationCallback,
        Looper.getMainLooper()
    )
}
