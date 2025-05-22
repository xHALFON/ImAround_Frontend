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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import com.example.myapplication.ui.components.DatePickerField
import com.example.myapplication.ui.components.GenderInterestSelector
import com.example.myapplication.ui.hobbies.HobbyViewModel
import java.util.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.ui.components.HobbyItem
import com.example.myapplication.ui.components.ModernTextField
import com.example.myapplication.ui.profile.*

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
    val genderInterest by remember { registerViewModel.genderInterest }
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
                        text = "Upload Profile Picture",
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
    if (showPreviewDialog && tempImageUri != null) {
        Dialog(onDismissRequest = {
            showPreviewDialog = false
            tempImageUri = null // Clear temporary URI if cancelled
            registerViewModel.clearPhotoAnalysisFeedback()
            showPhotoTip = false
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

                        if (photoAnalysisFeedback == null && !isAnalyzingPhoto && !showPhotoTip) {
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
                                containerColor = PrimaryColor
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "Analyze photo",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Show photo analysis feedback if available
                    if (showPhotoTip) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF0F4FF)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = PrimaryColor
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Photo Analysis",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimaryColor
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            showPhotoTip = false
                                            registerViewModel.clearPhotoAnalysisFeedback()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = TextSecondaryColor
                                        )
                                    }
                                }

                                if (isAnalyzingPhoto) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            color = PrimaryColor,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = "Analyzing your photo...")
                                    }
                                } else {
                                    Text(
                                        text = photoAnalysisFeedback ?: "",
                                        modifier = Modifier.padding(top = 8.dp),
                                        color = TextPrimaryColor
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                tempImageUri = null
                                showPreviewDialog = false
                                registerViewModel.clearPhotoAnalysisFeedback()
                                showPhotoTip = false
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
                                registerViewModel.imageUri.value = tempImageUri
                                showPreviewDialog = false
                                registerViewModel.clearPhotoAnalysisFeedback()
                                showPhotoTip = false
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
            // Header with welcome text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {

                    Text(
                        text = "Create an Account",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Profile Photo in center
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                ) {
                    // Profile image with shadow
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(10.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { showImageOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(imageUri),
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Add Photo",
                                    modifier = Modifier.size(40.dp),
                                    tint = PrimaryColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Add Photo",
                                    color = PrimaryColor,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Edit icon if image exists
                    if (imageUri != null) {
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
            }

            // Form Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp)
            ) {
                // Personal Information
                Text(
                    text = "Personal Information",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryColor,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // First Name
                ModernTextField(
                    value = firstName,
                    onValueChange = { registerViewModel.firstName.value = it },
                    label = "First Name",
                    icon = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Last Name
                ModernTextField(
                    value = lastName,
                    onValueChange = { registerViewModel.lastName.value = it },
                    label = "Last Name",
                    icon = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email
                ModernTextField(
                    value = email,
                    onValueChange = { registerViewModel.email.value = it },
                    label = "Email",
                    icon = Icons.Outlined.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password - Using ModernTextField instead of custom implementation
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
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = "Password",
                                fontSize = 12.sp,
                                color = TextSecondaryColor
                            )

                            BasicTextField(
                                value = password,
                                onValueChange = { registerViewModel.password.value = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = TextPrimaryColor,
                                    fontSize = 16.sp
                                ),
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Date of Birth
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clickable { showDatePicker() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBackgroundColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Date of Birth",
                                fontSize = 12.sp,
                                color = TextSecondaryColor
                            )
                            Text(
                                text = if (dob.isNotEmpty()) dob else "Select date",
                                fontSize = 16.sp,
                                color = if (dob.isNotEmpty()) TextPrimaryColor else TextSecondaryColor,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Removed duplicate calendar icon
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Occupation
                ModernTextField(
                    value = occupation,
                    onValueChange = { registerViewModel.occupation.value = it },
                    label = "I work as",
                    icon = Icons.Outlined.Work
                )

                Spacer(modifier = Modifier.height(24.dp))

                // About Me
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
                        value = aboutMe,
                        onValueChange = { registerViewModel.aboutMe.value = it },
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

                Spacer(modifier = Modifier.height(24.dp))

                // Gender Interest Selector
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
                            onGenderSelected = { registerViewModel.genderInterest.value = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Interests section
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
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .clickable { navController.navigate("hobby_selection") },
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
                                text = "Select Your Interests",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimaryColor
                            )

                            Icon(
                                imageVector = if (selectedHobbies.isEmpty()) Icons.Default.Add else Icons.Default.Check,
                                contentDescription = if (selectedHobbies.isEmpty()) "Add interests" else "Interests selected",
                                tint = if (selectedHobbies.isEmpty()) PrimaryColor else Color(0xFF4CAF50)
                            )
                        }

                        // Display selected hobbies or placeholder
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
                                        InterestChip(
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
                                text = "Tap to select your interests",
                                fontSize = 14.sp,
                                color = TextSecondaryColor,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Terms and Conditions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = true, // You can make this stateful if needed
                        onCheckedChange = { },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryColor
                        )
                    )

                    Text(
                        text = "By signing up, you agree to our Terms of Service and Privacy Policy",
                        fontSize = 12.sp,
                        color = TextSecondaryColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Register Button
                Button(
                    onClick = {
                        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank() || dob.isBlank()) {
                            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                        } else if (selectedHobbies.isEmpty()) {
                            Toast.makeText(context, "Please select at least one interest", Toast.LENGTH_SHORT).show()
                            navController.navigate("hobby_selection")
                        } else if (genderInterest.isBlank()) {
                            Toast.makeText(context, "Please select who you're interested in", Toast.LENGTH_SHORT).show()
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
                                genderInterest = genderInterest,
                                hobbies = selectedHobbies
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondaryColor
                    )
                ) {
                    Text(
                        text = "Create Account",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Already have an account
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        fontSize = 14.sp,
                        color = TextSecondaryColor
                    )

                    Text(
                        text = "Login",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryColor,
                        modifier = Modifier.clickable {
                            navController.navigate("login") {
                                popUpTo("register") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

// InterestChip matches the HobbySelectionDialog style
@Composable
fun InterestChip(
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

// Function to match hobby string to HobbyItem
fun getHobbyItemForName(hobbyName: String): HobbyItem {
    val hobbies = listOf(
        HobbyItem("gaming", "Gaming", Icons.Filled.SportsEsports, Color(0xFFE0E0E0)),
        HobbyItem("dancing", "Dancing", Icons.Filled.MusicNote, Color(0xFFFFEBEE)),
        HobbyItem("language", "Language", Icons.Filled.Translate, Color(0xFFE0F7FA)),
        HobbyItem("music", "Music", Icons.Filled.MusicNote, Color(0xFFE1BEE7)),
        HobbyItem("movie", "Movie", Icons.Filled.Movie, Color(0xFFF8BBD0)),
        HobbyItem("photography", "Photography", Icons.Filled.PhotoCamera, Color(0xFFE0E0E0)),
        HobbyItem("architecture", "Architecture", Icons.Filled.Architecture, Color(0xFFE0E0E0)),
        HobbyItem("fashion", "Fashion", Icons.Filled.Checkroom, Color(0xFFF8BBD0)),
        HobbyItem("book", "Book", Icons.Filled.MenuBook, Color(0xFFE1BEE7)),
        HobbyItem("writing", "Writing", Icons.Filled.Create, Color(0xFFE0E0E0)),
        HobbyItem("nature", "Nature", Icons.Filled.Park, Color(0xFFDCEDC8)),
        HobbyItem("painting", "Painting", Icons.Filled.Palette, Color(0xFFFFF9C4)),
        HobbyItem("football", "Football", Icons.Filled.SportsSoccer, Color(0xFFE0E0E0)),
        HobbyItem("people", "People", Icons.Filled.People, Color(0xFFFFF9C4)),
        HobbyItem("animals", "Animals", Icons.Filled.Pets, Color(0xFFE1BEE7)),
        HobbyItem("fitness", "Gym & Fitness", Icons.Filled.FitnessCenter, Color(0xFFFFF9C4)),
        // Fallback for other hobbies
        HobbyItem("travel", "Travel", Icons.Filled.Flight, Color(0xFFDCEDC8)),
        HobbyItem("food", "Food", Icons.Filled.Restaurant, Color(0xFFE0F7FA)),
        HobbyItem("cooking", "Cooking", Icons.Filled.Fastfood, Color(0xFFFFEBEE)),
        HobbyItem("technology", "Technology", Icons.Filled.Devices, Color(0xFFE0E0E0))
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
    return HobbyItem(
        hobbyName.lowercase(),
        hobbyName,
        Icons.Filled.Star,
        Color(0xFFE0E0E0)
    )
}