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
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.model.User
import com.example.myapplication.ui.hobbies.HobbyViewModel
import com.example.myapplication.ui.components.GenderInterestSelector

// Define theme colors
val PrimaryColor = Color(0xFF6F75E8)
val SecondaryColor = Color(0xFFFF4081)
val BackgroundColor = Color(0xFFF9F9F9)
val CardBackgroundColor = Color.White
val TextPrimaryColor = Color(0xFF212121)
val TextSecondaryColor = Color(0xFF757575)
val BorderColor = Color(0xFFEEEEEE)

@Composable
fun EditProfileScreen(
    navController: NavController,
    user: User,
    viewModel: ProfileViewModel = viewModel(),
    hobbyViewModel: HobbyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Load fresh user data when screen is opened
    LaunchedEffect(Unit) {
        viewModel.loadUserProfile()
    }

    // Observe the refreshed user data from viewModel
    val refreshedUser by viewModel.userProfile.observeAsState()

    // Check if there's saved form state in the ViewModel
    val savedFormState = viewModel.getSavedFormState()

    // Log form state for debugging
    LaunchedEffect(savedFormState) {
        Log.d("EditProfileScreen", "Loaded savedFormState: firstName=${savedFormState.firstName}, lastName=${savedFormState.lastName}")
    }

    // Use rememberSaveable with values from either savedFormState, prioritizing saved form data
    var firstName by rememberSaveable {
        mutableStateOf(
            if (savedFormState.firstName.isNotEmpty()) savedFormState.firstName
            else user.firstName ?: ""
        )
    }
    var lastName by rememberSaveable {
        mutableStateOf(
            if (savedFormState.lastName.isNotEmpty()) savedFormState.lastName
            else user.lastName ?: ""
        )
    }
    var email by rememberSaveable {
        mutableStateOf(
            if (savedFormState.email.isNotEmpty()) savedFormState.email
            else user.email ?: ""
        )
    }
    var about by rememberSaveable {
        mutableStateOf(
            if (savedFormState.about.isNotEmpty()) savedFormState.about
            else user.about ?: ""
        )
    }
    var occupation by rememberSaveable {
        mutableStateOf(
            if (savedFormState.occupation.isNotEmpty()) savedFormState.occupation
            else user.occupation ?: ""
        )
    }

    // Add genderInterest state
    var genderInterest by rememberSaveable {
        mutableStateOf(
            if (savedFormState.genderInterest.isNotEmpty()) savedFormState.genderInterest
            else user.genderInterest ?: ""
        )
    }

    // For image handling
    var tempImageUri by rememberSaveable { mutableStateOf<Uri?>(null) } // Temporary URI for preview only
    var selectedImageUri by rememberSaveable { mutableStateOf<Uri?>(savedFormState.selectedImageUri) } // Final selected image for display
    var currentAvatarUrl by rememberSaveable { mutableStateOf(user.avatar) }
    var showImageOptions by rememberSaveable { mutableStateOf(false) }
    var showImagePreview by rememberSaveable { mutableStateOf(false) }

    val initialized = rememberSaveable { mutableStateOf(false) }

    // Update local state variables when refreshed user data is available
    LaunchedEffect(refreshedUser) {
        refreshedUser?.let { newUser ->
            // Only update if no form state exists or if it's the initial load
            if (savedFormState.firstName.isEmpty() && savedFormState.lastName.isEmpty()) {
                // Only update if the new data is different from what we have
                if (firstName.isEmpty() || firstName != newUser.firstName) {
                    firstName = newUser.firstName ?: ""
                }
                if (lastName.isEmpty() || lastName != newUser.lastName) {
                    lastName = newUser.lastName ?: ""
                }
                if (email.isEmpty() || email != newUser.email) {
                    email = newUser.email ?: ""
                }
                if (about.isEmpty() || about != newUser.about) {
                    about = newUser.about ?: ""
                }
                if (occupation.isEmpty() || occupation != newUser.occupation) {
                    occupation = newUser.occupation ?: ""
                }
                if (genderInterest.isEmpty() || genderInterest != newUser.genderInterest) {
                    genderInterest = newUser.genderInterest ?: ""
                }

                // Update avatar URL if changed
                if (currentAvatarUrl != newUser.avatar) {
                    currentAvatarUrl = newUser.avatar
                }
            }

            // Update hobbies
            if (newUser.hobbies != null && !initialized.value) {
                hobbyViewModel.initializeWithExistingHobbies(newUser.hobbies)
                initialized.value = true
            }
        }
    }

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
        Dialog(onDismissRequest = { showImageOptions = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Change Profile Picture",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gallery option
                    ElevatedButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                            showImageOptions = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = CardBackgroundColor,
                            contentColor = PrimaryColor
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                tint = PrimaryColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Choose from Gallery",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Camera option
                    ElevatedButton(
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = CardBackgroundColor,
                            contentColor = PrimaryColor
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                tint = PrimaryColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Take a Photo",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // Image preview dialog
    if (showImagePreview && tempImageUri != null) {
        Dialog(onDismissRequest = {
            showImagePreview = false
            tempImageUri = null // Clear temporary URI if cancelled
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackgroundColor),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Profile Picture Preview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .shadow(8.dp, CircleShape)
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(tempImageUri),
                            contentDescription = "Profile Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                tempImageUri = null // Clear temporary URI
                                showImagePreview = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = TextSecondaryColor
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                // Only assign to selectedImageUri when "Use Photo" is clicked
                                selectedImageUri = tempImageUri
                                showImagePreview = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryColor
                            )
                        ) {
                            Text(
                                text = "Use Photo",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }

    // Main Screen Content
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 16.dp)
        ) {
            // Profile Header without Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 16.dp)
            ) {
                // Back button in top left
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 16.dp)
                        .size(40.dp)
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
                    color = TextPrimaryColor,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )

                // Profile Photo - BIGGER SIZE
                Box(
                    modifier = Modifier
                        .size(160.dp)  // Increased from 120dp to 160dp
                        .align(Alignment.Center)
                ) {
                    // Profile image with shadow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
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
                                .padding(4.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Edit icon
                    IconButton(
                        onClick = { showImageOptions = true },
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .shadow(6.dp, CircleShape)
                            .background(
                                color = SecondaryColor,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Profile Picture",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Form Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp)
            ) {
                // Form Section Title - Personal Info
                Text(
                    text = "Personal Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Form Fields in styled Cards
                ModernTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = "First Name",
                    icon = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModernTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = "Last Name",
                    icon = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModernTextField(
                    value = email,
                    onValueChange = { /* Disabled */ },
                    label = "Email",
                    icon = Icons.Outlined.Email,
                    enabled = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                ModernTextField(
                    value = occupation,
                    onValueChange = { occupation = it },
                    label = "Occupation",
                    icon = Icons.Outlined.Work
                )

                Spacer(modifier = Modifier.height(30.dp))

                // About Me Section
                Text(
                    text = "About Me",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
                ) {
                    OutlinedTextField(
                        value = about,
                        onValueChange = { about = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        placeholder = { Text("Tell us about yourself...") },
                        minLines = 4,
                        maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = CardBackgroundColor,
                            unfocusedContainerColor = CardBackgroundColor
                        )
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Dating Preferences Section - NEW
                Text(
                    text = "Dating Preferences",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {

                        GenderInterestSelector(
                            selectedGender = genderInterest,
                            onGenderSelected = { genderInterest = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Interests section - Redesigned to match ProfileScreen
                Text(
                    text = "Interests",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Interests header with edit button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected Interests",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryColor
                            )

                            TextButton(
                                onClick = {
                                    // Save form data to viewModel before navigating
                                    viewModel.saveFormState(
                                        firstName = firstName,
                                        lastName = lastName,
                                        email = email,
                                        about = about,
                                        occupation = occupation,
                                        selectedImageUri = selectedImageUri,
                                        genderInterest = genderInterest  // Add gender interest to saved form state
                                    )

                                    navController.navigate("hobby_selection") {
                                        launchSingleTop = true
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = PrimaryColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Interests",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit")
                            }
                        }

                        // Interests/hobbies chips
                        if (selectedHobbies.isNotEmpty()) {
                            // Using a FlowRow-like arrangement with multiple rows as needed
                            selectedHobbies.chunked(3).forEachIndexed { index, rowHobbies ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowHobbies.forEach { hobby ->
                                        EditProfileHobbyChip(
                                            hobby = hobby,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // Fill empty spaces if needed
                                    repeat(3 - rowHobbies.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No interests selected",
                                fontSize = 16.sp,
                                color = TextSecondaryColor,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Modern Save Changes Button
                Button(
                    onClick = {
                        viewModel.updateUserProfile(
                            id = user._id ?: "",
                            updatedUser = user.copy(
                                firstName = firstName,
                                lastName = lastName,
                                email = user.email ?: email,
                                about = about,
                                occupation = occupation,
                                hobbies = selectedHobbies,
                                genderInterest = genderInterest  // Add gender interest to updated user
                            ),
                            imageUri = selectedImageUri,
                            onSuccess = {
                                viewModel.clearFormState()
                                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = {
                                Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryColor
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SAVE CHANGES",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) PrimaryColor else TextSecondaryColor.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (enabled) TextSecondaryColor else TextSecondaryColor.copy(alpha = 0.5f)
                )

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (enabled) TextPrimaryColor else TextSecondaryColor.copy(alpha = 0.7f),
                        fontSize = 16.sp
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
                )
            }
        }
    }
}

@Composable
fun EditProfileHobbyChip(
    hobby: String,
    modifier: Modifier = Modifier
) {
    // Convert the hobby string to a HobbyItem for consistent styling
    val hobbyItem = getHobbyItemForName(hobby)

    val backgroundColor = hobbyItem.color
    val borderColor = hobbyItem.color
    val textColor = Color.Black
    val iconColor = Color.Black

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = hobbyItem.icon,
            contentDescription = hobbyItem.name,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )

        Text(
            text = hobbyItem.name,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 14.sp
        )
    }
}

// Function to match hobby string to HobbyItem for consistency
fun getHobbyItemForName(hobbyName: String): com.example.myapplication.ui.components.HobbyItem {
    val hobbies = listOf(
        com.example.myapplication.ui.components.HobbyItem("gaming", "Gaming", Icons.Filled.SportsEsports, Color(0xFFE0E0E0)),
        com.example.myapplication.ui.components.HobbyItem("dancing", "Dancing", Icons.Filled.MusicNote, Color(0xFFFFEBEE)),
        com.example.myapplication.ui.components.HobbyItem("language", "Language", Icons.Filled.Translate, Color(0xFFE0F7FA)),
        com.example.myapplication.ui.components.HobbyItem("music", "Music", Icons.Filled.MusicNote, Color(0xFFE1BEE7)),
        com.example.myapplication.ui.components.HobbyItem("movie", "Movie", Icons.Filled.Movie, Color(0xFFF8BBD0)),
        com.example.myapplication.ui.components.HobbyItem("photography", "Photography", Icons.Filled.PhotoCamera, Color(0xFFE0E0E0)),
        com.example.myapplication.ui.components.HobbyItem("architecture", "Architecture", Icons.Filled.Architecture, Color(0xFFE0E0E0)),
        com.example.myapplication.ui.components.HobbyItem("fashion", "Fashion", Icons.Filled.Checkroom, Color(0xFFF8BBD0)),
        com.example.myapplication.ui.components.HobbyItem("book", "Book", Icons.Filled.MenuBook, Color(0xFFE1BEE7)),
        com.example.myapplication.ui.components.HobbyItem("writing", "Writing", Icons.Filled.Create, Color(0xFFE0E0E0)),
        com.example.myapplication.ui.components.HobbyItem("nature", "Nature", Icons.Filled.Park, Color(0xFFDCEDC8)),
        com.example.myapplication.ui.components.HobbyItem("painting", "Painting", Icons.Filled.Palette, Color(0xFFFFF9C4)),
        com.example.myapplication.ui.components.HobbyItem("football", "Football", Icons.Filled.SportsSoccer, Color(0xFFE0E0E0)),
        com.example.myapplication.ui.components.HobbyItem("people", "People", Icons.Filled.People, Color(0xFFFFF9C4)),
        com.example.myapplication.ui.components.HobbyItem("animals", "Animals", Icons.Filled.Pets, Color(0xFFE1BEE7)),
        com.example.myapplication.ui.components.HobbyItem("fitness", "Gym & Fitness", Icons.Filled.FitnessCenter, Color(0xFFFFF9C4)),
        // Fallback for other hobbies
        com.example.myapplication.ui.components.HobbyItem("travel", "Travel", Icons.Filled.Flight, Color(0xFFDCEDC8)),
        com.example.myapplication.ui.components.HobbyItem("food", "Food", Icons.Filled.Restaurant, Color(0xFFE0F7FA)),
        com.example.myapplication.ui.components.HobbyItem("cooking", "Cooking", Icons.Filled.Fastfood, Color(0xFFFFEBEE)),
        com.example.myapplication.ui.components.HobbyItem("technology", "Technology", Icons.Filled.Devices, Color(0xFFE0E0E0))
    )

    // Try to find exact match
    val exactMatch = hobbies.find { it.id.equals(hobbyName.lowercase()) || it.name.equals(hobbyName, ignoreCase = true) }
    if (exactMatch != null) {
        return exactMatch
    }

    // If no exact match, try partial match
    val partialMatch = hobbies.find {
        hobbyName.lowercase().contains(it.id) ||
                it.id.contains(hobbyName.lowercase()) ||
                hobbyName.lowercase().contains(it.name.lowercase()) ||
                it.name.lowercase().contains(hobbyName.lowercase())
    }
    if (partialMatch != null) {
        return partialMatch.copy(name = hobbyName)  // Use the original hobby name but keep the icon/color
    }

    // Default fallback
    return com.example.myapplication.ui.components.HobbyItem(
        hobbyName.lowercase(),
        hobbyName,
        Icons.Filled.Star,
        Color(0xFFE0E0E0)
    )
}