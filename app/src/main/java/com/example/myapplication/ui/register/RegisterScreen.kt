package com.example.myapplication.ui.register

import androidx.compose.material.icons.filled.Edit
import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.ui.draw.scale
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
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
import com.example.myapplication.ui.components.ProfilePhotoAnalysisTip

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
    var showImageOptions by remember { mutableStateOf(false) }
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // Observe selected hobbies from the HobbyViewModel
    val selectedHobbies by hobbyViewModel.selectedHobbies.observeAsState(emptyList())

    val authResponse by registerViewModel.authResponse.observeAsState()
    val errorMessage by registerViewModel.errorMessage.observeAsState()
    val isAnalyzingPhoto by registerViewModel.isAnalyzingPhoto.observeAsState(false)
    val photoAnalysisFeedback by registerViewModel.photoAnalysisFeedback.observeAsState()
    var showPhotoTip by remember { mutableStateOf(false) }
    // Create camera output Uri
    fun createImageUri(): Uri? {
        try {
            val contentResolver = context.contentResolver
            val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val newImageDetails = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "profile_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }

            return contentResolver.insert(imageCollection, newImageDetails)
        } catch (e: Exception) {
            Log.e("RegisterScreen", "Error creating image URI: ${e.message}")
            Toast.makeText(
                context,
                "Cannot access camera: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            return null
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(GetContent()) { uri ->
        uri?.let {
            tempImageUri = it
            showPreviewDialog = true
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success && tempImageUri != null) {
            showPreviewDialog = true
        } else {
            Toast.makeText(
                context,
                "Failed to capture image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // For permission checking
    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted, try launching camera again
            val cameraUri = createImageUri()
            if (cameraUri != null) {
                tempImageUri = cameraUri
                cameraLauncher.launch(cameraUri)
            } else {
                Toast.makeText(
                    context,
                    "Failed to create image file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                context,
                "Camera permission denied",
                Toast.LENGTH_SHORT
            ).show()
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

    // Image options dialog
    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            title = { Text("Upload Profile Picture") },
            text = { Text("Choose a source for your profile picture") },
            confirmButton = {
                TextButton(
                    onClick = {
                        imagePickerLauncher.launch("image/*")
                        showImageOptions = false
                    }
                ) {
                    Text("Choose from Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Simple approach without using early returns
                        var shouldLaunchCamera = true

                        // Request camera permission first if needed
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val hasPermission = context.checkSelfPermission(Manifest.permission.CAMERA) ==
                                    PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                // Request camera permission
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                shouldLaunchCamera = false
                            }
                        }

                        // Only proceed if we should launch camera
                        if (shouldLaunchCamera) {
                            try {
                                // Create a Uri for the camera to save the photo to
                                val photoUri = createImageUri()
                                if (photoUri != null) {
                                    tempImageUri = photoUri
                                    Log.d("RegisterScreen", "Launching camera with URI: $photoUri")
                                    cameraLauncher.launch(photoUri)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to create image file",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.e("RegisterScreen", "Error launching camera: ${e.message}", e)
                                Toast.makeText(
                                    context,
                                    "Camera error: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }

                        showImageOptions = false
                    }
                ) {
                    Text("Take Photo")
                }
            }
        )
    }

    if (showPreviewDialog && tempImageUri != null) {
        AlertDialog(
            onDismissRequest = {
                showPreviewDialog = false
                tempImageUri = null // Clear temporary URI if cancelled
                registerViewModel.clearPhotoAnalysisFeedback() // ניקוי הפידבק
                showPhotoTip = false
            },
            title = { Text("Profile Picture Preview") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(tempImageUri),
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )

                        if (photoAnalysisFeedback == null && !isAnalyzingPhoto && !showPhotoTip) {
                            // כפתור מנורה להפעלת ניתוח התמונה
                            FloatingActionButton(
                                onClick = {
                                    tempImageUri?.let {
                                        registerViewModel.analyzeProfilePhoto(it)
                                        showPhotoTip = true
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .size(48.dp),
                                containerColor = Color(0xFF6366F1)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Analyze photo",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // הצגת הטיפ אם יש צורך
                    if (showPhotoTip) {
                        ProfilePhotoAnalysisTip(
                            tip = photoAnalysisFeedback ?: "", // הוספת הניהול של null
                            isLoading = isAnalyzingPhoto,
                            onDismiss = {
                                showPhotoTip = false
                                registerViewModel.clearPhotoAnalysisFeedback()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Save the image URI to the ViewModel
                        registerViewModel.imageUri.value = tempImageUri
                        showPreviewDialog = false
                        registerViewModel.clearPhotoAnalysisFeedback()
                        showPhotoTip = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(text = "Use Photo", fontSize = 18.sp)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        tempImageUri = null
                        showPreviewDialog = false
                        registerViewModel.clearPhotoAnalysisFeedback()
                        showPhotoTip = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray
                    )
                ) {
                    Text(text = "Cancel", fontSize = 18.sp)
                }
            }
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

                // Profile picture upload - Modern styled button
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showImageOptions = true }
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
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
                                text = "Profile Picture",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF4A148C)
                            )
                            Text(
                                text = if (imageUri != null) "Change your photo" else "Upload a photo of yourself",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    color = Color(0xFF6F75E8),
                                    shape = RoundedCornerShape(percent = 50)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Upload photo",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Show selected image if available
                if (imageUri != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )

                            // Overlay edit button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                                    .size(36.dp)
                                    .background(
                                        color = Color(0xFF6F75E8),
                                        shape = RoundedCornerShape(percent = 50)
                                    )
                                    .clickable { showImageOptions = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Change photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
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