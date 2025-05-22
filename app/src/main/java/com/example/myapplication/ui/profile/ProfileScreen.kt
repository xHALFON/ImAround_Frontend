package com.example.myapplication.ui.profile

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.model.User
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(LocalContext.current as ViewModelStoreOwner)
) {
    val context = LocalContext.current
    val user by viewModel.userProfile.observeAsState()
    val error by viewModel.errorMessage.observeAsState()
    val logoutSuccess by viewModel.logoutSuccess.observeAsState()
    val deleteAccountSuccess by viewModel.deleteAccountSuccess.observeAsState()
    val scrollState = rememberScrollState()

    // State for delete account confirmation dialog
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Load profile
    LaunchedEffect(Unit) {
        Log.d("InfoTrack", "ProfileScreen: Going To viewModel.loadUserProfile()")
        viewModel.loadUserProfile()
    }

    // Handle errors
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    // Handle logout success
    LaunchedEffect(logoutSuccess) {
        if (logoutSuccess == true) {
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Handle delete account success
    LaunchedEffect(deleteAccountSuccess) {
        if (deleteAccountSuccess == true) {
            Toast.makeText(context, "Account deleted successfully", Toast.LENGTH_SHORT).show()
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // Delete account confirmation dialog
    if (showDeleteDialog) {
        DeleteAccountDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAccount()
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        user?.let {
            ProfileContent(
                user = it,
                onEditProfileClick = { navController.navigate("edit_profile") },
                onLogoutClick = { viewModel.logout() },
                onDeleteAccountClick = { showDeleteDialog = true }
            )
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Delete Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently deleted.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: User,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Keep the header space, but without the back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Empty box just for spacing
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Profile image and basic info
        ProfileHeader(user)

        Spacer(modifier = Modifier.height(24.dp))

        // About section
        if (!user.about.isNullOrEmpty()) {
            AboutSection(user.about)
            Spacer(modifier = Modifier.height(24.dp))
        }



        // Interests section
        if (user.hobbies?.isNotEmpty() == true) {
            InterestsSection(user.hobbies)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Action Buttons
        ActionButtonsSection(
            onEditProfileClick = onEditProfileClick,
            onLogoutClick = onLogoutClick,
            onDeleteAccountClick = onDeleteAccountClick
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ActionButtonsSection(
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Edit profile button - Modern gradient style
        Button(
            onClick = onEditProfileClick,
            modifier = Modifier
                .width(225.dp) // Fixed width instead of fillMaxWidth
                .height(40.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color.White
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Edit Profile",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Logout button - Outlined style
        OutlinedButton(
            onClick = onLogoutClick,
            modifier = Modifier
                .width(225.dp) // Fixed width instead of fillMaxWidth
                .height(40.dp),
            shape = RoundedCornerShape(28.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                width = 2.dp
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Logout,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Logout",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(15.dp))

        // Delete account button - Danger style (kept full width)
        TextButton(
            onClick = onDeleteAccountClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = Color.Red
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Account",
                    tint = Color.Red
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Delete Account",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ProfileHeader(user: User) {
    val age = calculateAge(user.birthDate)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile image
        Image(
            painter = rememberAsyncImagePainter(
                model = user.avatar.ifEmpty { "https://ui-avatars.com/api/?name=${user.firstName}&background=random" }
            ),
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(250.dp)
                .clip(CircleShape)
                .shadow(elevation = 8.dp, shape = CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Name and age
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${user.firstName}, $age",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Occupation if available
        if (!user.occupation.isNullOrEmpty()) {
            Text(
                text = user.occupation,
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun AboutSection(about: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "About Me",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Text(
            text = about,
            fontSize = 16.sp,
            color = Color.DarkGray
        )
    }
}



@Composable
fun InterestsSection(hobbies: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Interests",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Interests/hobbies grid
        if (hobbies.isNotEmpty()) {
            // First row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                hobbies.take(3).forEach { hobby ->
                    HobbyChip(
                        hobby = hobby,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Fill empty spaces if needed
                repeat(3 - minOf(hobbies.size, 3)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            // Second row if needed
            if (hobbies.size > 3) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    hobbies.drop(3).take(3).forEach { hobby ->
                        HobbyChip(
                            hobby = hobby,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Fill empty spaces if needed
                    repeat(3 - minOf(hobbies.size - 3, 3)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        } else {
            Text(
                text = "No interests added yet",
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
fun HobbyChip(
    hobby: String,
    modifier: Modifier = Modifier
) {
    // Map hobby name to color and icon (same as in registration screen)
    val (color, icon) = when (hobby.lowercase()) {
        "gaming" -> Pair(Color(0xFFBDBDBD), Icons.Filled.SportsEsports)
        "dancing" -> Pair(Color(0xFFFF8A80), Icons.Filled.MusicNote)
        "language" -> Pair(Color(0xFF80DEEA), Icons.Filled.Translate)
        "music" -> Pair(Color(0xFFCE93D8), Icons.Filled.MusicNote)
        "movie" -> Pair(Color(0xFFF48FB1), Icons.Filled.Movie)
        "photography" -> Pair(Color(0xFFCFD8DC), Icons.Filled.PhotoCamera)
        "architecture" -> Pair(Color(0xFFA5D6A7), Icons.Filled.Architecture)
        "fashion" -> Pair(Color(0xFFF48FB1), Icons.Filled.Checkroom)
        "book" -> Pair(Color(0xFFCE93D8), Icons.Filled.MenuBook)
        "writing" -> Pair(Color(0xFF80CBC4), Icons.Filled.Create)
        "nature" -> Pair(Color(0xFFC5E1A5), Icons.Filled.Park)
        "painting" -> Pair(Color(0xFFFFF59D), Icons.Filled.Palette)
        "football" -> Pair(Color(0xFFB0BEC5), Icons.Filled.SportsSoccer)
        "people" -> Pair(Color(0xFFFFF59D), Icons.Filled.People)
        "animals" -> Pair(Color(0xFFCE93D8), Icons.Filled.Pets)
        "fitness" -> Pair(Color(0xFFFFF59D), Icons.Filled.FitnessCenter)
        else -> Pair(Color(0xFFE0E0E0), Icons.Filled.Star)
    }

    Surface(
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = color
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = hobby.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.8f),
                maxLines = 1
            )
        }
    }
}

// Function to calculate age from birthdate
fun calculateAge(birthDate: String): Int {
    return try {
        // If birthDate is in DD/MM/YYYY format
        val parts = birthDate.split("/")
        if (parts.size == 3) {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH) + 1
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            val birthYear = parts[2].toInt()
            val birthMonth = parts[1].toInt()
            val birthDay = parts[0].toInt()

            var age = currentYear - birthYear

            // Adjust age if birthday hasn't occurred yet this year
            if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                age--
            }

            age
        } else {
            // Attempt to parse as ISO format (YYYY-MM-DD)
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDateParsed = format.parse(birthDate)
            val calendar = Calendar.getInstance()

            val birthCalendar = Calendar.getInstance()
            birthCalendar.time = birthDateParsed

            var age = calendar.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)

            if (calendar.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            age
        }
    } catch (e: Exception) {
        // Return a placeholder age if parsing fails
        0
    }
}