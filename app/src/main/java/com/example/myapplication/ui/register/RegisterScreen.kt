package com.example.myapplication.ui.register

import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.ui.components.BottomComponent
import com.example.myapplication.ui.components.CheckboxComponent
import com.example.myapplication.ui.components.DatePickerField
import com.example.myapplication.ui.components.HeadingTextComponent
import com.example.myapplication.ui.components.MyTextFieldComponent
import com.example.myapplication.ui.components.NormalTextComponent
import com.example.myapplication.ui.components.PasswordTextFieldComponent
import com.example.myapplication.ui.hobbies.HobbyViewModel
import java.util.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.unit.sp

@Composable
fun RegisterScreen(
    navController: NavHostController,
    registerViewModel: RegisterViewModel = viewModel(),
    hobbyViewModel: HobbyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Use values stored in ViewModel
    val firstName by remember { registerViewModel.firstName }
    val lastName by remember { registerViewModel.lastName }
    val email by remember { registerViewModel.email }
    val password by remember { registerViewModel.password }
    val dob by remember { registerViewModel.dob }
    val imageUri by remember { registerViewModel.imageUri }
    val aboutMe by remember { registerViewModel.aboutMe }
    val occupation by remember { registerViewModel.occupation }
    var showPreviewDialog by remember { mutableStateOf(false) }

    // Observe selected hobbies from the HobbyViewModel
    val selectedHobbies by hobbyViewModel.selectedHobbies.observeAsState(emptyList())

    val authResponse by registerViewModel.authResponse.observeAsState()
    val errorMessage by registerViewModel.errorMessage.observeAsState()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(GetContent()) { uri ->
        uri?.let {
            registerViewModel.imageUri.value = it
            showPreviewDialog = true
        }
    }

    // Date picker
    fun showDatePicker() {
        Log.d("RegisterScreen", "showDatePicker() called")
        val calendar = Calendar.getInstance()
        DatePickerDialog(context,
            { _, y, m, d -> registerViewModel.dob.value = "$d/${m + 1}/$y" },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Handle auth response
    LaunchedEffect(authResponse) {
        authResponse?.let {
            Toast.makeText(context, "Registered successfully!", Toast.LENGTH_SHORT).show()
            navController.navigate("login")
        }
    }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Profile picture preview dialog
    if (showPreviewDialog && imageUri != null) {
        AlertDialog(
            onDismissRequest = { showPreviewDialog = false },
            title = { Text("Profile Picture Preview") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showPreviewDialog = false },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(48.dp)
                    ) {
                        Text(text = "Save", fontSize = 18.sp)
                    }
                }
            },
            confirmButton = {},
        )
    }

    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            NormalTextComponent(value = "Hello there,")
            HeadingTextComponent(value = "Create an Account")
            Spacer(modifier = Modifier.height(25.dp))

            Column {
                // Basic info fields
                MyTextFieldComponent(
                    labelValue = "First Name",
                    icon = Icons.Outlined.Person,
                    value = firstName,
                    onValueChange = { registerViewModel.firstName.value = it }
                )
                Spacer(modifier = Modifier.height(10.dp))

                MyTextFieldComponent(
                    labelValue = "Last Name",
                    icon = Icons.Outlined.Person,
                    value = lastName,
                    onValueChange = { registerViewModel.lastName.value = it }
                )
                Spacer(modifier = Modifier.height(10.dp))

                MyTextFieldComponent(
                    labelValue = "Email",
                    icon = Icons.Outlined.Email,
                    value = email,
                    onValueChange = { registerViewModel.email.value = it }
                )
                Spacer(modifier = Modifier.height(10.dp))

                PasswordTextFieldComponent(
                    labelValue = "Password",
                    icon = Icons.Outlined.Lock,
                    value = password,
                    onValueChange = { registerViewModel.password.value = it }
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Date of birth field
                DatePickerField(
                    label = "Date of Birth",
                    value = dob,
                    onClick = { showDatePicker() }
                )
                Spacer(modifier = Modifier.height(10.dp))

                // "I work as" field - now right after Date of Birth
                MyTextFieldComponent(
                    labelValue = "I work as",
                    icon = Icons.Outlined.Work,
                    value = occupation,
                    onValueChange = { registerViewModel.occupation.value = it }
                )
                Spacer(modifier = Modifier.height(10.dp))

                // About Me field - now appears right after "I work as"
                MyTextFieldComponent(
                    labelValue = "About Me",
                    icon = Icons.Outlined.Person,
                    value = aboutMe,
                    onValueChange = { registerViewModel.aboutMe.value = it },
                    singleLine = false,
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Profile picture upload
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload Profile Picture")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Interests selection card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("hobby_selection") },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Select Your Interests",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (selectedHobbies.isNotEmpty()) {
                                Text(
                                    text = "${selectedHobbies.size} interests selected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4A148C)
                                )
                            }
                        }

                        if (selectedHobbies.isEmpty()) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add interests",
                                tint = Color(0xFF4A148C)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Interests selected",
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                CheckboxComponent()

                Spacer(modifier = Modifier.height(10.dp))

                BottomComponent(
                    textQuery = "Already have an account? ",
                    textClickable = "Login",
                    action = "Register",
                    navController = navController,
                    onActionClick = {
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || dob.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        } else if (selectedHobbies.isEmpty()) {
                            Toast.makeText(context, "Please select at least one interest", Toast.LENGTH_SHORT).show()
                            navController.navigate("hobby_selection")
                        } else {
                            registerViewModel.registerUser(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                password = password,
                                dob = dob,
                                imageUri = imageUri,
                                aboutMe = aboutMe,
                                occupation = occupation,
                                hobbies = selectedHobbies
                            )
                        }
                    }
                )
            }
        }
    }
}