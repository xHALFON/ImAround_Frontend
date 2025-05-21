package com.example.myapplication.ui.profile

import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val scrollState = rememberScrollState()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        user?.let {
            ProfileContent(
                user = it,
                onEditProfileClick = { navController.navigate("edit_profile") },
                onLogoutClick = {
                    // Handle logout logic
                    viewModel.logout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: User,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
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

        // Edit profile button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, end = 16.dp, top = 0.dp, bottom = 8.dp)
                .clickable(onClick = onEditProfileClick),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8ECF6) // Light blue background
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = null,
                        tint = Color(0xFF4F9EC8), // Blue icon color
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Edit Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                }

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Logout button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 0.dp, end = 16.dp, top = 8.dp, bottom = 24.dp)
                .clickable(onClick = onLogoutClick),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8ECF6) // Light blue background
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = null,
                        tint = Color(0xFFE57373), // Red icon color
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.Black
                    )
                }

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
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
                .clip(CircleShape),
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
fun GenderInterestChip(genderInterest: String) {
    val (color, icon, displayText) = when (genderInterest.lowercase()) {
        "male" -> Triple(Color(0xFF90CAF9), Icons.Filled.Male, "Interested in Men")
        "female" -> Triple(Color(0xFFFFAB91), Icons.Filled.Female, "Interested in Women")
        "both" -> Triple(Color(0xFFCE93D8), Icons.Filled.Groups, "Interested in Everyone")
        else -> Triple(Color(0xFFE0E0E0), Icons.Filled.Person, "Open to All")
    }

    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = color
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
        }
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