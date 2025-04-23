package com.mahith1.whereabouts.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahith1.whereabouts.viewmodels.ShareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    userId: String,
    onDismiss: () -> Unit
) {
    val shareViewModel: ShareViewModel = viewModel()
    val sharedUsers by shareViewModel.sharedUsers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newEmail by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        shareViewModel.loadSharedUsers(userId)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Share Location With",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Add new email button
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Email")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of shared users
            LazyColumn {
                items(sharedUsers) { email ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(email)
                        IconButton(
                            onClick = {
                                shareViewModel.stopSharing(
                                    userId,
                                    email,
                                    onSuccess = { /* Handle success */ },
                                    onError = { /* Handle error */ }
                                )
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Stop sharing")
                        }
                    }
                    Divider()
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Email") },
            text = {
                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Email Address") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newEmail.isNotEmpty()) {
                            shareViewModel.shareLocation(
                                userId,
                                newEmail,
                                onSuccess = {
                                    showAddDialog = false
                                    newEmail = ""
                                },
                                onError = { /* Handle error */ }
                            )
                        }
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