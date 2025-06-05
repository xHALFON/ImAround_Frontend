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

// 🔥 NEW IMPORTS for Session Management
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.myapplication.data.local.SessionManager

// Modern Color Palette
object ModernColors {
    val PrimaryBlue = Color(0xFF2563EB)
    val SecondaryBlue = Color(0xFF3B82F6)
    val LightBlue = Color(0xFFDBEAFE)
    val SoftGray = Color(0xFFF8FAFC)
    val MediumGray = Color(0xFF64748B)
    val DarkGray = Color(0xFF1E293B)
    val Success = Color(0xFF10B981)
    val Danger = Color(0xFFEF4444)
    val Warning = Color(0xFFF59E0B)
    val CardBackground = Color(0xFFFFFFFF)
    val DividerColor = Color(0xFFE2E8F0)
}

@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = viewModel(LocalContext.current as ViewModelStoreOwner)
) {
    val context = LocalContext.current

    // 🔥 NEW - Session Manager
    val sessionManager = remember { SessionManager(context) }
    val profileViewModel: ProfileViewModel = viewModel()
    val user by viewModel.userProfile.observeAsState()
    val error by viewModel.errorMessage.observeAsState()
    val logoutSuccess by viewModel.logoutSuccess.observeAsState()
    val deleteAccountSuccess by viewModel.deleteAccountSuccess.observeAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        profileViewModel.logoutSuccess.value = false
        Log.d("ProfileScreen", "🔄 Reset logoutSuccess to false")
    }

    // 🔥 NEW - SESSION VALIDATION on screen load
    LaunchedEffect(Unit) {
        Log.d("ProfileScreen", "🔍 Checking session validity...")

        val isLoggedIn = sessionManager.isLoggedIn()
        val userId = sessionManager.getUserId()

        Log.d("ProfileScreen", "🔍 Session check: isLoggedIn=$isLoggedIn, userId=$userId")

        if (!isLoggedIn || userId == null) {
            Log.d("ProfileScreen", "❌ Session invalid - redirecting to login")
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }
            return@LaunchedEffect
        }

        Log.d("ProfileScreen", "✅ Session valid - loading profile")
        Log.d("InfoTrack", "ProfileScreen: Going To viewModel.loadUserProfile()")

        viewModel.loadUserProfile()
    }

    // 🔥 NEW - LIFECYCLE-AWARE SESSION CHECK
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("ProfileScreen", "🔄 App resumed - re-checking session...")

                val isLoggedIn = sessionManager.isLoggedIn()
                val userId = sessionManager.getUserId()

                if (!isLoggedIn || userId == null) {
                    Log.d("ProfileScreen", "❌ Session expired on resume - redirecting to login")
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 🔥 ENHANCED - Error handling with auth error detection
    LaunchedEffect(error) {
        error?.let { errorMessage ->
            Log.d("ProfileScreen", "🔍 Checking error: $errorMessage")

            // Check for various auth error patterns
            val authErrorPatterns = listOf(
                "unauthorized",
                "401",
                "403",
                "token",
                "session",
                "authentication",
                "invalid credentials",
                "access denied"
            )

            val isAuthError = authErrorPatterns.any { pattern ->
                errorMessage.contains(pattern, ignoreCase = true)
            }

            if (isAuthError) {
                Log.d("ProfileScreen", "❌ Auth error detected: $errorMessage - clearing session")
                sessionManager.clearSession()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(logoutSuccess) {
        if (logoutSuccess == true) {
            Log.d("ProfileScreen", "🚪 Logout success detected - navigating to login")

            // Navigate to login
            navController.navigate("login") {
                popUpTo(0) { inclusive = true }
            }

            // 🆕 נקה את logoutSuccess אחרי שהניווט הושלם
            viewModel.afterLogout()
            Log.d("ProfileScreen", "🔄 Called afterLogout() to clear state")
        }
    }

    // Modern Background with subtle gradient
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
        user?.let {
            ModernProfileContent(
                user = it,
                onEditProfileClick = {
                    // 🔥 NEW - SESSION CHECK before navigation
                    if (sessionManager.isLoggedIn()) {
                        navController.navigate("edit_profile")
                    } else {
                        Log.d("ProfileScreen", "❌ Session invalid before edit - redirecting to login")
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onLogoutClick = { viewModel.logout() }
            )
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = ModernColors.PrimaryBlue,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}

@Composable
fun ModernProfileContent(
    user: User,
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Modern Profile Header Card
        ModernProfileHeader(user)

        Spacer(modifier = Modifier.height(32.dp))

        // Content Cards
        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // About section in card
            if (!user.about.isNullOrEmpty()) {
                ModernAboutCard(user.about)
            }

            // Interests section in card
            if (user.hobbies?.isNotEmpty() == true) {
                ModernInterestsCard(user.hobbies)
            }

            // Action Buttons Card
            ModernActionButtonsCard(
                onEditProfileClick = onEditProfileClick,
                onLogoutClick = onLogoutClick
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun ModernProfileHeader(user: User) {
    val age = calculateAge(user.birthDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = ModernColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile image with modern styling
            Box(
                contentAlignment = Alignment.Center
            ) {
                // Outer ring
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    ModernColors.PrimaryBlue.copy(alpha = 0.2f),
                                    ModernColors.SecondaryBlue.copy(alpha = 0.1f)
                                )
                            )
                        )
                )

                // Profile image
                Image(
                    painter = rememberAsyncImagePainter(
                        model = user.avatar.ifEmpty {
                            "https://ui-avatars.com/api/?name=${user.firstName}&background=2563EB&color=fff&size=200"
                        }
                    ),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = ModernColors.PrimaryBlue.copy(alpha = 0.25f)
                        ),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Name and age with modern typography
            Text(
                text = "${user.firstName}, $age",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ModernColors.DarkGray,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Occupation with elegant styling
            if (!user.occupation.isNullOrEmpty()) {
                Text(
                    text = user.occupation,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = ModernColors.MediumGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ModernAboutCard(about: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ModernColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "About",
                    tint = ModernColors.PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "About Me",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ModernColors.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = about,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = ModernColors.MediumGray,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun ModernInterestsCard(hobbies: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ModernColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Interests",
                    tint = ModernColors.PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "My Interests",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ModernColors.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Modern grid layout for interests
            val chunkedHobbies = hobbies.chunked(2)

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chunkedHobbies.forEach { hobbyPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        hobbyPair.forEach { hobby ->
                            ModernHobbyChip(
                                hobby = hobby,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Fill empty space if odd number
                        if (hobbyPair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernHobbyChip(
    hobby: String,
    modifier: Modifier = Modifier
) {
    // Modern icon mapping
    val (color, icon) = when (hobby.lowercase()) {
        "gaming" -> Pair(Color(0xFF667EEA), Icons.Filled.SportsEsports)
        "dancing" -> Pair(Color(0xFFf093fb), Icons.Filled.MusicNote)
        "language" -> Pair(Color(0xFF4facfe), Icons.Filled.Translate)
        "music" -> Pair(Color(0xFFa8edea), Icons.Filled.MusicNote)
        "movie" -> Pair(Color(0xFFfad0c4), Icons.Filled.Movie)
        "photography" -> Pair(Color(0xFFa8caba), Icons.Filled.PhotoCamera)
        "architecture" -> Pair(Color(0xFF85d8ce), Icons.Filled.Architecture)
        "fashion" -> Pair(Color(0xFFfbc2eb), Icons.Filled.Checkroom)
        "book" -> Pair(Color(0xFF667eea), Icons.Filled.MenuBook)
        "writing" -> Pair(Color(0xFF764ba2), Icons.Filled.Create)
        "nature" -> Pair(Color(0xFF56ab2f), Icons.Filled.Park)
        "painting" -> Pair(Color(0xFFf7971e), Icons.Filled.Palette)
        "football" -> Pair(Color(0xFF56CCF2), Icons.Filled.SportsSoccer)
        "people" -> Pair(Color(0xFFFF8A80), Icons.Filled.People)
        "animals" -> Pair(Color(0xFF9c88ff), Icons.Filled.Pets)
        "fitness" -> Pair(Color(0xFF11998e), Icons.Filled.FitnessCenter)
        else -> Pair(ModernColors.MediumGray, Icons.Filled.Star)
    }

    Surface(
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            color.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = hobby.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ModernActionButtonsCard(
    onEditProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = ModernColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Edit Profile Button - Primary Modern Style
            Button(
                onClick = onEditProfileClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ModernColors.PrimaryBlue
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Edit Profile",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Logout Button - Modern Outlined Style
            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    ModernColors.Danger
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ModernColors.Danger,
                    containerColor = Color.Transparent
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = "Logout",
                        tint = ModernColors.Danger,
                        modifier = Modifier.size(22.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Logout",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ModernColors.Danger,
                        letterSpacing = 0.5.sp
                    )
                }
            }
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
        25
    }
}