package com.example.myapplication.ui.profile

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = about,
            onValueChange = { about = it },
            label = { Text("About Me") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = occupation,
            onValueChange = { occupation = it },
            label = { Text("Occupation") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Selected Interests: ${selectedHobbies.joinToString(", ")}")

        Button(
            onClick = {
                navController.navigate("hobby_selection"){
                    launchSingleTop = true
                }

            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
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
                        email = email,
                        about = about,
                        occupation = occupation,
                        hobbies = selectedHobbies // ✅ Pass updated hobbies
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
                .padding(top = 16.dp)
                .fillMaxWidth()
        ) {
            Text("Save Changes")
        }
    }
}

