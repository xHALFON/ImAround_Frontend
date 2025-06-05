package com.example.myapplication.ui.login

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.ui.components.GenderInterestSelector
import com.example.myapplication.ui.components.GenderSelector
import com.example.myapplication.ui.components.ModernTextField
import com.example.myapplication.ui.hobbies.HobbyViewModel
import com.example.myapplication.ui.profile.*
import com.example.myapplication.ui.register.InterestChip
import java.util.*

@Composable
fun CompleteProfileScreen(
    navController: NavHostController,
    viewModel: CompleteProfileViewModel = viewModel(),
    hobbyViewModel: HobbyViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val sessionManager = SessionManager(context)

    // Get current user info from session
    val currentUserId = sessionManager.getUserId() ?: ""
    val userFirstName = sessionManager.getUserFirstName() ?: ""
    val userEmail = sessionManager.getUserEmail() ?: ""

    // 🆕 Check if there's saved form state in the ViewModel
    val savedFormState = viewModel.getSavedFormState()

    // 🆕 Log form state for debugging
    LaunchedEffect(savedFormState) {
        Log.d("CompleteProfileScreen", "Loaded savedFormState: dob=${savedFormState.dob}, gender=${savedFormState.gender}")
    }

    // Form state - Use saved form state if available
    var dob by remember {
        mutableStateOf(
            if (savedFormState.dob.isNotEmpty()) savedFormState.dob else ""
        )
    }
    var gender by remember {
        mutableStateOf(
            if (savedFormState.gender.isNotEmpty()) savedFormState.gender else ""
        )
    }
    var genderInterest by remember {
        mutableStateOf(
            if (savedFormState.genderInterest.isNotEmpty()) savedFormState.genderInterest else ""
        )
    }
    var aboutMe by remember {
        mutableStateOf(
            if (savedFormState.aboutMe.isNotEmpty()) savedFormState.aboutMe else ""
        )
    }
    var occupation by remember {
        mutableStateOf(
            if (savedFormState.occupation.isNotEmpty()) savedFormState.occupation else ""
        )
    }

    // Observe selected hobbies from the HobbyViewModel
    val selectedHobbies by hobbyViewModel.selectedHobbies.observeAsState(emptyList())

    // ViewModel observables
    val completionResponse by viewModel.completionResponse.observeAsState()
    val errorMessage by viewModel.errorMessage.observeAsState()
    val isCompleting by viewModel.isCompleting.observeAsState(false)

    // Date picker
    fun showDatePicker() {
        Log.d("CompleteProfileScreen", "showDatePicker() called")
        val calendar = Calendar.getInstance()
        DatePickerDialog(context,
            { _, y, m, d -> dob = "$d/${m + 1}/$y" },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Handle completion response
    LaunchedEffect(completionResponse) {
        completionResponse?.let {
            // 🆕 Clear form state on successful completion
            viewModel.clearFormState()

            Toast.makeText(context, "Profile completed successfully!", Toast.LENGTH_SHORT).show()
            navController.navigate("main") {
                popUpTo("complete_profile") { inclusive = true }
            }
        }
    }

    // Handle error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    // Main Screen Content
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BackgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            ModernColors.SoftGray,
                            Color.White
                        ),
                        startY = 0f,
                        endY = 800f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = 16.dp)
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column {
                        Text(
                            text = "Welcome, $userFirstName! 👋",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = ModernColors.DarkGray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Let's complete your profile to get started",
                            fontSize = 16.sp,
                            color = ModernColors.MediumGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Form Content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Basic Information
                    Text(
                        text = "Basic Information",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernColors.DarkGray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    // Date of Birth
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clickable { showDatePicker() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ModernColors.CardBackground)
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
                                tint = ModernColors.PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Date of Birth",
                                    fontSize = 12.sp,
                                    color = ModernColors.MediumGray
                                )
                                Text(
                                    text = if (dob.isNotEmpty()) dob else "Select your birthday",
                                    fontSize = 16.sp,
                                    color = if (dob.isNotEmpty()) ModernColors.DarkGray else ModernColors.MediumGray,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gender Selection
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ModernColors.CardBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            GenderSelector(
                                selectedGender = gender,
                                onGenderSelected = { gender = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Occupation
                    ModernTextField(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = "What do you do for work?",
                        icon = Icons.Outlined.Work
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // About Me
                    Text(
                        text = "About You",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernColors.DarkGray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ModernColors.CardBackground)
                    ) {
                        OutlinedTextField(
                            value = aboutMe,
                            onValueChange = { aboutMe = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            placeholder = { Text("Tell us about yourself...") },
                            minLines = 4,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = ModernColors.CardBackground,
                                unfocusedContainerColor = ModernColors.CardBackground
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dating Preferences
                    Text(
                        text = "Dating Preferences",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernColors.DarkGray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ModernColors.CardBackground)
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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Interests section
                    Text(
                        text = "Your Interests",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ModernColors.DarkGray,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp))
                            .clickable {
                                // 🆕 Save form state before navigating
                                viewModel.saveFormState(
                                    dob = dob,
                                    gender = gender,
                                    genderInterest = genderInterest,
                                    aboutMe = aboutMe,
                                    occupation = occupation
                                )

                                navController.navigate("hobby_selection")
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ModernColors.CardBackground)
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
                                    color = ModernColors.DarkGray
                                )

                                Icon(
                                    imageVector = if (selectedHobbies.isEmpty()) Icons.Default.Add else Icons.Default.Check,
                                    contentDescription = if (selectedHobbies.isEmpty()) "Add interests" else "Interests selected",
                                    tint = if (selectedHobbies.isEmpty()) ModernColors.PrimaryBlue else Color(0xFF4CAF50)
                                )
                            }

                            // Display selected hobbies or placeholder
                            if (selectedHobbies.isNotEmpty()) {
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
                                    color = ModernColors.MediumGray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Complete Profile Button
                    Button(
                        onClick = {
                            if (dob.isBlank()) {
                                Toast.makeText(context, "Please select your date of birth", Toast.LENGTH_SHORT).show()
                            } else if (gender.isBlank()) {
                                Toast.makeText(context, "Please select your gender", Toast.LENGTH_SHORT).show()
                            } else if (selectedHobbies.isEmpty()) {
                                Toast.makeText(context, "Please select at least one interest", Toast.LENGTH_SHORT).show()

                                // 🆕 Save form state before navigating to hobbies
                                viewModel.saveFormState(
                                    dob = dob,
                                    gender = gender,
                                    genderInterest = genderInterest,
                                    aboutMe = aboutMe,
                                    occupation = occupation
                                )

                                navController.navigate("hobby_selection")
                            } else if (genderInterest.isBlank()) {
                                Toast.makeText(context, "Please select who you're interested in", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.completeProfile(
                                    context = context,
                                    userId = currentUserId,
                                    birthDate = dob,
                                    gender = gender,
                                    genderInterest = genderInterest,
                                    about = aboutMe,
                                    occupation = occupation,
                                    hobbies = selectedHobbies
                                )
                            }
                        },
                        enabled = !isCompleting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCompleting) Color.Gray else ModernColors.PrimaryBlue,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        if (isCompleting) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Completing Profile...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = "Complete Profile",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Debug info
                    Text(
                        text = "Logged in as: $userEmail",
                        fontSize = 12.sp,
                        color = ModernColors.MediumGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}