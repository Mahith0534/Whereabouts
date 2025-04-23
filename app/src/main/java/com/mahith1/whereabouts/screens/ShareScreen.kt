package com.mahith1.whereabouts.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahith1.whereabouts.viewmodels.ShareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    userId: String,
    onNavigateBack: () -> Unit
) {
    val shareViewModel: ShareViewModel = viewModel()
    val sharedUsers by shareViewModel.sharedUsers.collectAsState()
    val isSharing by shareViewModel.isSharing.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Show error dialog if needed
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    // Add email validation
    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    // Initialize with current shared users
    var selectedEmails by remember(sharedUsers) { 
        mutableStateOf(sharedUsers.toSet())
    }

    LaunchedEffect(Unit) {
        shareViewModel.loadSharedUsers(userId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Share Location") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Contact")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Location sharing toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Location Sharing",
                    style = MaterialTheme.typography.titleMedium
                )
                Switch(
                    checked = isSharing,
                    onCheckedChange = { newValue ->
                        shareViewModel.toggleLocationSharing(
                            userId = userId,
                            isSharing = newValue,
                            onSuccess = { /* Optional: Show success message */ },
                            onError = { error -> errorMessage = error }
                        )
                    }
                )
            }

            Divider(modifier = Modifier.padding(bottom = 16.dp))

            if (isSharing) {
                Text(
                    text = "Shared With",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sharedUsers) { email ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = email in selectedEmails,
                                onCheckedChange = { checked ->
                                    if (!checked) {
                                        // Remove sharing
                                        shareViewModel.stopSharing(
                                            userId = userId,
                                            targetEmail = email,
                                            onSuccess = {
                                                selectedEmails = selectedEmails - email
                                            },
                                            onError = { error -> errorMessage = error }
                                        )
                                    } else {
                                        // Add to selected emails
                                        selectedEmails = selectedEmails + email
                                    }
                                }
                            )
                            Text(
                                text = email,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            )
                        }
                        Divider()
                    }
                }

                // Only show update button if there are changes to apply
                if (selectedEmails != sharedUsers.toSet()) {
                    Button(
                        onClick = {
                            selectedEmails.forEach { email ->
                                if (email !in sharedUsers) {
                                    shareViewModel.shareLocation(
                                        userId = userId,
                                        targetEmail = email,
                                        onSuccess = { /* Handle success */ },
                                        onError = { error -> errorMessage = error }
                                    )
                                }
                            }
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Text("Update Sharing")
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Location sharing is currently disabled",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add New Contact") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it.trim() },
                        label = { Text("Email Address") },
                        singleLine = true,
                        isError = newEmail.isNotEmpty() && !isValidEmail(newEmail)
                    )
                    if (newEmail.isNotEmpty() && !isValidEmail(newEmail)) {
                        Text(
                            "Invalid email format",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEmail.isEmpty()) {
                            errorMessage = "Email cannot be empty"
                            return@Button
                        }
                        if (!isValidEmail(newEmail)) {
                            errorMessage = "Invalid email format"
                            return@Button
                        }
                        if (newEmail == userId) {
                            errorMessage = "Cannot share location with yourself"
                            return@Button
                        }
                        shareViewModel.shareLocation(
                            userId = userId,
                            targetEmail = newEmail,
                            onSuccess = {
                                showAddDialog = false
                                newEmail = ""
                            },
                            onError = { error -> errorMessage = error }
                        )
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}




