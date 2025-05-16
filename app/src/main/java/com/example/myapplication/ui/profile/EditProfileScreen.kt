package com.example.myapplication.ui.profile

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
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

    // Check if there's saved form state in the ViewModel
    val savedFormState = viewModel.getSavedFormState()

    // Use rememberSaveable with values from either savedFormState or user
    var firstName by rememberSaveable { mutableStateOf(savedFormState.firstName.ifEmpty { user.firstName ?: "" }) }
    var lastName by rememberSaveable { mutableStateOf(savedFormState.lastName.ifEmpty { user.lastName ?: "" }) }
    var email by rememberSaveable { mutableStateOf(savedFormState.email.ifEmpty { user.email ?: "" }) }
    var about by rememberSaveable { mutableStateOf(savedFormState.about.ifEmpty { user.about ?: "" }) }
    var occupation by rememberSaveable { mutableStateOf(savedFormState.occupation.ifEmpty { user.occupation ?: "" }) }

    // For image handling
    var tempImageUri by rememberSaveable { mutableStateOf<Uri?>(null) } // Temporary URI for preview only
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(savedFormState.selectedImageUri) } // Final selected image for display
    var currentAvatarUrl by rememberSaveable { mutableStateOf(user.avatar) }
    var showImageOptions by rememberSaveable { mutableStateOf(false) }
    var showImagePreview by rememberSaveable { mutableStateOf(false) }

    val initialized = rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!initialized.value) {
            hobbyViewModel.initializeWithExistingHobbies(user.hobbies ?: emptyList())
            initialized.value = true
        }
    }

    val selectedHobbies by hobbyViewModel.selectedHobbies.observeAsState(emptyList())

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
            Log.e("EditProfileScreen", "Error creating image URI: ${e.message}")
            Toast.makeText(
                context,
                "Cannot access camera: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            return null
        }
    }

    // Image pickers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            tempImageUri = it // Store in temporary URI first
            showImagePreview = true
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            showImagePreview = true
        } else {
            Toast.makeText(
                context,
                "Failed to capture image",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // For permission checking
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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

    // Image options dialog
    if (showImageOptions) {
        AlertDialog(
            onDismissRequest = { showImageOptions = false },
            title = { Text("Change Profile Picture") },
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
                                    Log.d("EditProfileScreen", "Launching camera with URI: $photoUri")
                                    cameraLauncher.launch(photoUri)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Failed to create image file",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Log.e("EditProfileScreen", "Error launching camera: ${e.message}", e)
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

    // Image preview dialog - FIXED: ensuring perfect circle shape with aspectRatio(1f)
    if (showImagePreview && tempImageUri != null) {
        Dialog(onDismissRequest = {
            showImagePreview = false
            tempImageUri = null // Clear temporary URI if cancelled
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Profile Picture Preview",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .aspectRatio(1f) // Ensure perfect circle by maintaining 1:1 aspect ratio
                            .clip(CircleShape)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(tempImageUri),
                            contentDescription = "Profile Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                tempImageUri = null // Clear temporary URI
                                showImagePreview = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Gray
                            )
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                // Only assign to selectedImageUri when "Use Photo" is clicked
                                selectedImageUri = tempImageUri
                                showImagePreview = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6F75E8)
                            )
                        ) {
                            Text("Use Photo")
                        }
                    }
                }
            }
        }
    }

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

        // Profile image with change option - FIXED: ensure circular shape with aspectRatio(1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .aspectRatio(1f) // Ensure perfect circle by maintaining 1:1 aspect ratio
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = selectedImageUri ?: currentAvatarUrl.ifEmpty {
                            "https://ui-avatars.com/api/?name=${firstName}&background=random"
                        }
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )

                // Edit icon for changing picture
                IconButton(
                    onClick = { showImageOptions = true },
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.BottomEnd)
                        .background(
                            color = Color(0xFF6F75E8),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Change Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
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
                    // Save form data to viewModel before navigating
                    viewModel.saveFormState(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        about = about,
                        occupation = occupation,
                        selectedImageUri = selectedImageUri
                    )

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
                        imageUri = selectedImageUri, // Use the confirmed selectedImageUri
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