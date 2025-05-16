package com.example.myapplication.ui.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.model.User
import com.example.myapplication.ui.hobbies.HobbyViewModel

@Composable
fun EditProfileScreen(
    navController: NavController,
    user: User,
    viewModel: ProfileViewModel = viewModel(),
    hobbyViewModel: HobbyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var firstName by remember { mutableStateOf(user.firstName ?: "") }
    var lastName by remember { mutableStateOf(user.lastName ?: "") }
    var email by remember { mutableStateOf(user.email ?: "") }
    var about by remember { mutableStateOf(user.about ?: "") }
    var occupation by remember { mutableStateOf(user.occupation ?: "") }

    val initialized = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!initialized.value) {
            hobbyViewModel.initializeWithExistingHobbies(user.hobbies ?: emptyList())
            initialized.value = true
        }
    }

    val selectedHobbies by hobbyViewModel.selectedHobbies.observeAsState(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Header with back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Back button
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(32.dp)
                    .background(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.DarkGray
                )
            }

            // Page title
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Form fields
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6F75E8),
                    focusedLabelColor = Color(0xFF6F75E8)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6F75E8),
                    focusedLabelColor = Color(0xFF6F75E8)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { /* Disabled */ },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,  // Disable the field
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = Color.Gray.copy(alpha = 0.3f),
                    disabledTextColor = Color.Gray,
                    disabledLabelColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = occupation,
                onValueChange = { occupation = it },
                label = { Text("Occupation") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6F75E8),
                    focusedLabelColor = Color(0xFF6F75E8)
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("About Me") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6F75E8),
                    focusedLabelColor = Color(0xFF6F75E8)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Interests section
            Text(
                text = "Interests",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (selectedHobbies.isNotEmpty()) {
                Text(
                    text = selectedHobbies.joinToString(", ") {
                        it.replaceFirstChar { c -> c.uppercase() }
                    },
                    fontSize = 16.sp,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Text(
                    text = "No interests selected",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Button(
                onClick = {
                    navController.navigate("hobby_selection") {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6F75E8)
                )
            ) {
                Text("Edit Interests")
            }

            Button(
                onClick = {
                    viewModel.updateUserProfile(
                        id = user._id ?: "",
                        updatedUser = user.copy(
                            firstName = firstName,
                            lastName = lastName,
                            // Keep original email, don't update it
                            email = user.email ?: email,
                            about = about,
                            occupation = occupation,
                            hobbies = selectedHobbies
                        ),
                        onSuccess = {
                            Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onError = {
                            Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF4081)
                )
            ) {
                Text("Save Changes")
            }
        }
    }
}