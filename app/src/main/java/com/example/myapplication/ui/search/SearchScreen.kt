package com.example.myapplication.ui.search

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.data.model.UserResponse
import kotlin.math.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import kotlin.random.Random

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val users by viewModel.usersResponse.observeAsState(emptyList())
    val error by viewModel.errorMessage.observeAsState()
    var isSearching by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserResponse?>(null) }

    val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.all { it.value }
        if (granted) {
            viewModel.startSearch()
            isSearching = true
        } else {
            Toast.makeText(context, "Bluetooth permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {  // Changed background to white

        // Full screen radar
        ModernRadarBackground(
            modifier = Modifier.fillMaxSize(),
            users = users,
            onUserTap = { selectedUser = it }
        )

        // Modern search button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            ModernSearchButton(
                isSearching = isSearching,
                onClick = {
                    if (isSearching) {
                        // viewModel.stopSearch()
                        isSearching = false
                    } else {
                        permissionLauncher.launch(requiredPermissions)
                    }
                }
            )
        }

        // User card dialog
        selectedUser?.let { user ->
            Dialog(onDismissRequest = { selectedUser = null }) {
                SwipeableUserCard(
                    user = user,
                    onSwipedLeft = {
                        selectedUser = null
                        // Optionally handle remove
                    },
                    onSwipedRight = {
                        selectedUser = null
                        // Optionally handle match logic
                    }
                )
            }
        }
    }
}

@Composable
fun ModernRadarBackground(
    modifier: Modifier = Modifier,
    users: List<UserResponse>,
    onUserTap: (UserResponse) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Radar sweep animation
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing)
        )
    )

    // Floating animation for users
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier) {
        // Draw the radar
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val maxRadius = min(centerX, centerY) * 0.85f

            // Draw radar circles with lighter colors for white background
            for (i in 1..4) {
                val radius = maxRadius * (i / 4f)
                drawCircle(
                    color = Color(0xFF90CAF9).copy(alpha = 0.5f),  // Light blue for white background
                    center = Offset(centerX, centerY),
                    radius = radius,
                    style = Stroke(width = 2f)
                )
            }

            // Draw coordinate lines
            for (i in 0 until 8) {
                rotate(i * 45f, Offset(centerX, centerY)) {
                    drawLine(
                        color = Color(0xFF90CAF9).copy(alpha = 0.3f),  // Light blue for white background
                        start = Offset(centerX, centerY),
                        end = Offset(centerX, centerY - maxRadius),
                        strokeWidth = 1.5f
                    )
                }
            }

            // Draw radar center point
            drawCircle(
                color = Color(0xFF1976D2),  // Darker blue for center point
                center = Offset(centerX, centerY),
                radius = 8f
            )

            // Draw radar sweep
            drawArc(
                color = Color(0xFF1976D2).copy(alpha = 0.3f),  // Darker blue for sweep
                startAngle = angle,
                sweepAngle = 60f,
                useCenter = true,
                topLeft = Offset(centerX - maxRadius, centerY - maxRadius),
                size = Size(maxRadius * 2, maxRadius * 2)
            )
        }

        // Draw users on radar with floating animation
        users.forEachIndexed { index, user ->
            // Using random with seed based on index for consistent positioning
            val random = Random(index)
            val randomAngle = remember { (index * 45 + random.nextInt(15, 30)) % 360 }
            val randomDistance = remember { random.nextInt(40, 85) / 100f }

            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeight = with(density) { configuration.screenHeightDp.dp.toPx() }

            val centerX = screenWidth / 2f
            val centerY = screenHeight / 2f
            val maxRadius = min(centerX, centerY) * 0.85f

            val angleDegrees = randomAngle.toDouble()
            val angleRadians = Math.toRadians(angleDegrees)
            val distance = maxRadius * randomDistance

            val x = centerX + distance * cos(angleRadians).toFloat()
            val y = centerY + distance * sin(angleRadians).toFloat()

            // User avatar with floating animation
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (x - with(density) { 24.dp.toPx() }).toInt(),
                            y = (y - floatOffset - with(density) { 24.dp.toPx() }).toInt()
                        )
                    }
                    .size(48.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .border(2.dp, Color(0xFF1976D2), CircleShape)  // Changed border color to match theme
                    .background(Color.White)
                    .clickable { onUserTap(user) }
            ) {
                Image(
                    painter = rememberAsyncImagePainter(user.avatar),
                    contentDescription = "User Avatar",
                    modifier = Modifier.fillMaxSize()
                )

                // Pulsating circle effect
                val pulseAnim by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000, easing = EaseInOutQuad),
                        repeatMode = RepeatMode.Restart
                    )
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0xFF1976D2).copy(alpha = 0.5f - (pulseAnim * 0.5f)),  // Changed color to match theme
                        radius = size.minDimension / 2 * (1f + pulseAnim * 0.3f),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernSearchButton(
    isSearching: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = if (isSearching) Color(0xFFF44336) else Color(0xFF1976D2)  // Changed colors to red/blue
    val buttonText = if (isSearching) "Stop" else "Start Searching"

    Button(
        onClick = onClick,
        modifier = Modifier
            .height(56.dp)
            .widthIn(min = 220.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonColor
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun SwipeableUserCard(
    user: UserResponse,
    modifier: Modifier = Modifier,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 150f
    val alpha = (abs(offsetX) / swipeThreshold).coerceIn(0f, 1f)

    // Background colors for swipe indication
    Box(
        modifier = Modifier
            .size(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        // Left swipe background (red)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF44336).copy(alpha = if (offsetX < 0) alpha else 0f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dislike",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        // Right swipe background (green)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF4CAF50).copy(alpha = if (offsetX > 0) alpha else 0f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Like",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        // User card
        Card(
            modifier = modifier
                .size(300.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .graphicsLayer {
                    translationX = offsetX
                    rotationZ = offsetX / 30f
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            when {
                                offsetX > swipeThreshold -> onSwipedRight()
                                offsetX < -swipeThreshold -> onSwipedLeft()
                                else -> offsetX = 0f
                            }
                        },
                        onDrag = { _, dragAmount ->
                            offsetX += dragAmount.x
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color(0xFF1976D2), CircleShape)  // Changed to blue
                        .padding(4.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(user.avatar),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${user.firstName} ${user.lastName}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Swipe left indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF44336).copy(alpha = 0.1f))  // Changed to red
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dislike",
                            tint = Color(0xFFF44336),  // Changed to red
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Swipe right indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f))  // Changed to green
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color(0xFF4CAF50),  // Changed to green
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Swipe or tap to decide",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}