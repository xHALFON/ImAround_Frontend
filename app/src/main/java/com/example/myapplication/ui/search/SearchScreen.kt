package com.example.myapplication.ui.search

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.data.model.MatchStatus
import com.example.myapplication.data.model.UserMatch
import com.example.myapplication.data.model.UserResponse
import kotlin.math.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.Key.Companion.Calendar
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.example.myapplication.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val users by viewModel.usersResponse.observeAsState(emptyList())
    val matches by viewModel.matches.observeAsState(emptyList())
    val pendingMatches by viewModel.pendingMatches.observeAsState(emptyList())
    val receivedMatches by viewModel.receivedMatches.observeAsState(emptyList())
    val hasNewMatch by viewModel.hasNewMatch.observeAsState(false)
    val newMatchUser by viewModel.newMatchUser.observeAsState(null)
    val error by viewModel.errorMessage.observeAsState()
    var isSearching by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserResponse?>(null) }
    var showMatchesList by remember { mutableStateOf(false) }
    var selectedMatch by remember { mutableStateOf<UserMatch?>(null) }
    var showMatchConfirmation by remember { mutableStateOf(false) }
    val newMatchId by viewModel.newMatchId.observeAsState()
    // Match confirmed animation
    LaunchedEffect(hasNewMatch) {
        if (hasNewMatch && newMatchUser != null) {
            showMatchConfirmation = true
            // After showing the animation, navigate to chat (you can implement this)
        }
    }

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

//    LaunchedEffect(error) {
//        error?.let {
//            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
//        }
//    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = EaseInOutQuad),
                    repeatMode = RepeatMode.Reverse
                )
            )

            val pulseAnim by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = EaseInOutQuad),
                    repeatMode = RepeatMode.Restart
                )
            )

            // Subtle radar ping effect
            Canvas(
                modifier = Modifier
                    .size(240.dp)
                    .alpha(0.1f)
            ) {
                drawCircle(
                    color = Color(0xFF1976D2),
                    radius = size.minDimension * 0.3f * pulseAnim,
                    style = Stroke(width = 2f)
                )
            }

            // החלפת הטקסט בלוגו
            Image(
                painter = painterResource(id = R.drawable.iamaround_logo_new),
                contentDescription = "I'm Around Logo",
                modifier = Modifier
                    .scale(pulseScale)
                    .height(120.dp) // התאם את הגודל לפי הצורך
            )
        }

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
                        viewModel.stopSearch()
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
                        // Handle dislike
                        viewModel.dislikeUser(user._id)
                    },
                    onSwipedRight = {
                        selectedUser = null
                        // Handle like - send to backend
                        viewModel.likeUser(user._id)
                    }
                )
            }
        }



        // Selected match user card dialog
        selectedMatch?.let { match ->
            Dialog(onDismissRequest = { selectedMatch = null }) {
                SwipeableMatchCard(
                    match = match,
                    onSwipedLeft = {
                        selectedMatch = null
                        // Remove match
                            //  viewModel.unmatchUser(match.user._id)
                    },
                    onSwipedRight = {
                        selectedMatch = null
                        // Navigate to chat
                        // navController.navigate("chat/${match.user.id}")
                    },
                    onApprove = if (match.status == MatchStatus.RECEIVED) {
                        {
                            selectedMatch = null
                            viewModel.approveMatch(match.user._id)
                        }
                    } else null
                )
            }
        }

        // Match confirmation animation overlay
        if (showMatchConfirmation) {
            MatchConfirmationAnimation(
                matchedUser = newMatchUser,
                onAnimationComplete = {
                    showMatchConfirmation = false
                    viewModel.clearNewMatchFlag()
                },
                onGoToChat = {
                    showMatchConfirmation = false
                    viewModel.clearNewMatchFlag()
                    navController.navigate("main_with_chat")
                },
                onClose = {
                    // סגירת המסך וחזרה לחיפוש
                    showMatchConfirmation = false
                    viewModel.clearNewMatchFlag()
                }
            )
        }
    }
}


@Composable
fun SwipeableMatchCard(
    match: UserMatch,
    modifier: Modifier = Modifier,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    onApprove: (() -> Unit)?
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
                contentDescription = "Unmatch",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }

        // Right swipe background (blue - for chat)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF2196F3).copy(alpha = if (offsetX > 0) alpha else 0f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = "Chat",
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
                        .border(4.dp,
                            when (match.status) {
                                MatchStatus.CONFIRMED -> Color(0xFF4CAF50)
                                MatchStatus.PENDING -> Color(0xFFFFA000)
                                MatchStatus.RECEIVED -> Color(0xFF2196F3)
                            },
                            CircleShape
                        )
                        .padding(4.dp)
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(match.user.avatar),
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "${match.user.firstName} ${match.user.lastName}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val statusText = when (match.status) {
                        MatchStatus.CONFIRMED -> "Matched"
                        MatchStatus.PENDING -> "Pending approval..."
                        MatchStatus.RECEIVED -> "Waiting for your approval"
                    }

                    val statusColor = when (match.status) {
                        MatchStatus.CONFIRMED -> Color(0xFF4CAF50)
                        MatchStatus.PENDING -> Color(0xFFFFA000)
                        MatchStatus.RECEIVED -> Color(0xFF2196F3)
                    }

                    Icon(
                        imageVector = when (match.status) {
                            MatchStatus.CONFIRMED -> Icons.Default.Favorite
                            MatchStatus.PENDING -> Icons.Default.Schedule
                            MatchStatus.RECEIVED -> Icons.Default.PersonAdd
                        },
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = match.user.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (match.status == MatchStatus.RECEIVED && onApprove != null) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Approve Match")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF44336).copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Unmatch",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Right action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3).copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "Chat",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Swipe left to unmatch, right to chat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun MatchConfirmationAnimation(
    matchedUser: UserResponse?,
    onAnimationComplete: () -> Unit,
    onGoToChat: () -> Unit = {},  // חדש - פונקציה לעבור לצ'אט
    onClose: () -> Unit = {}      // חדש - פונקציה לסגור את המסך
) {
    // Track animation progress
    var animationProgress by remember { mutableStateOf(0f) }
    var animationCompleted by remember { mutableStateOf(false) }

    // Animation timing
    val animationDuration = 2000 // קיצרתי ל-2 שניות

    // Launch animation
    LaunchedEffect(matchedUser) {
        if (matchedUser != null) {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < animationDuration) {
                animationProgress =
                    (System.currentTimeMillis() - startTime).toFloat() / animationDuration
                delay(16) // ~60fps
            }
            animationCompleted = true // סימון שהאנימציה הסתיימה
            // לא קוראים ל-onAnimationComplete אוטומטית יותר
        }
    }

    // Full screen overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1976D2).copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // X button בפינה הימנית העליונה
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        onClose()
                        onAnimationComplete() // לנקות את הסטייט
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .alpha(
                            animateFloatAsState(
                                targetValue = if (animationProgress > 0.3f) 1f else 0f
                            ).value
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Hearts animation around the avatars
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Animated hearts
                repeat(8) { index ->
                    val delay = index * 0.125f
                    val animatedScale by animateFloatAsState(
                        targetValue = if (animationProgress > delay) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )

                    val animatedAlpha by animateFloatAsState(
                        targetValue = if (animationProgress > delay) {
                            if (animationCompleted) 0.8f // שמירה על שקיפות קבועה אחרי האנימציה
                            else 1f - ((animationProgress - delay) * 1.5f).coerceIn(0f, 1f)
                        } else 0f
                    )

                    val angle = index * 45f
                    val radius = 90f
                    val x = cos(Math.toRadians(angle.toDouble())).toFloat() * radius
                    val y = sin(Math.toRadians(angle.toDouble())).toFloat() * radius

                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = animatedAlpha),
                        modifier = Modifier
                            .size(24.dp)
                            .offset(x = x.dp, y = y.dp)
                            .scale(animatedScale)
                    )
                }

                // User avatars
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .scale(
                            animateFloatAsState(
                                targetValue = if (animationProgress > 0.2f) 1f else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ).value
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    matchedUser?.let {
                        Image(
                            painter = rememberAsyncImagePainter(it.avatar),
                            contentDescription = "Matched User",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // "It's a Match!" text animation
            Text(
                text = "It's a Match!",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(2f, 2f),
                        blurRadius = 4f
                    )
                ),
                modifier = Modifier.scale(
                    animateFloatAsState(
                        targetValue = if (animationProgress > 0.4f) 1f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ).value
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Message with username
            matchedUser?.let {
                val username = "${it.firstName}"

                Text(
                    text = "You matched with $username",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .alpha(
                            animateFloatAsState(
                                targetValue = if (animationProgress > 0.6f) 1f else 0f
                            ).value
                        )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // כפתורים - יופיעו רק אחרי שהאנימציה תסתיים
            if (animationCompleted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start messaging button
                    Button(
                        onClick = {
                            onGoToChat()
                            onAnimationComplete() // לנקות את הסטייט
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Start Messaging",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Keep searching button
                    OutlinedButton(
                        onClick = {
                            onClose()
                            onAnimationComplete() // לנקות את הסטייט
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(2.dp, Color.White),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Keep Searching",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
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
            val random = Random(user._id.hashCode())
            val randomAngle = remember { (user._id.hashCode() % 360) }
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
                    .border(2.dp, Color(0xFF1976D2), CircleShape)
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
                        color = Color(0xFF1976D2).copy(alpha = 0.5f - (pulseAnim * 0.5f)),
                        radius = size.minDimension / 2 * (1f + pulseAnim * 0.3f),
                        style = Stroke(width = 2f)
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
            val calendar = java.util.Calendar.getInstance()
            val currentYear = calendar.get(java.util.Calendar.YEAR)
            val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
            val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

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
            val calendar = java.util.Calendar.getInstance()

            val birthCalendar = java.util.Calendar.getInstance()
            birthCalendar.time = birthDateParsed

            var age = calendar.get(java.util.Calendar.YEAR) - birthCalendar.get(java.util.Calendar.YEAR)

            if (calendar.get(java.util.Calendar.DAY_OF_YEAR) < birthCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                age--
            }

            age
        }
    } catch (e: Exception) {
        // Return a placeholder age if parsing fails
        0
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

    // Calculate age from date of birth
    val age = remember(user.birthDate) { calculateAge(user.birthDate ?: "") }

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
                        .border(4.dp, Color(0xFF1976D2), CircleShape)
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

                // Display first name and age
                Text(
                    text = if (age > 0) "${user.firstName}, $age" else user.firstName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Display occupation instead of email
                Text(
                    text = user.occupation ?: "No occupation specified",
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
                            .background(Color(0xFFF44336).copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dislike",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Swipe right indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Like",
                            tint = Color(0xFF4CAF50),
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

@Composable
fun ModernSearchButton(
    isSearching: Boolean,
    onClick: () -> Unit
) {
    val buttonColor = if (isSearching) Color(0xFFF44336) else Color(0xFF1976D2)  // Red when active, blue when inactive
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