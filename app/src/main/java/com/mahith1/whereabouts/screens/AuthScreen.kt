package com.mahith1.whereabouts.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mahith1.whereabouts.R
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

@Composable
fun AuthScreen(
    onLoginSuccess: (String) -> Unit
) {
    val auth = FirebaseAuth.getInstance()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Check if user is already logged in
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            onLoginSuccess(currentUser.email ?: "")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo), // Replace with your logo's actual name
            contentDescription = "App Logo",
            modifier = Modifier
                .height(100.dp)
                .padding(bottom = 8.dp)
        )

        // App Name
        Text(
            text = "Whereabouts",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(if (isLogin) "Login" else "Sign Up", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it
                errorMessage = null
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (email.isEmpty() || password.isEmpty()) {
                    errorMessage = "Please fill in all fields"
                    return@Button
                }
                
                isLoading = true
                errorMessage = null

                if (isLogin) {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                                onLoginSuccess(email)
                            } else {
                                errorMessage = task.exception?.message ?: "Login failed"
                                Log.e("AuthScreen", "Login failed", task.exception)
                            }
                        }
                } else {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                // Create initial shares document for the new user
                                FirebaseFirestore.getInstance()
                                    .collection("shares")
                                    .document(email)
                                    .set(mapOf(
                                        "sharedWith" to listOf<String>(),
                                        "isSharing" to true
                                    ))
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "Signup Successful", Toast.LENGTH_SHORT).show()
                                        onLoginSuccess(email)
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = e.message ?: "Failed to initialize user data"
                                        Log.e("AuthScreen", "Failed to initialize user data", e)
                                    }
                            } else {
                                errorMessage = task.exception?.message ?: "Signup failed"
                                Log.e("AuthScreen", "Signup failed", task.exception)
                            }
                        }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(if (isLogin) "Login" else "Sign Up")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = { 
                isLogin = !isLogin
                errorMessage = null
            }
        ) {
            Text(if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Login")
        }
    }
}
