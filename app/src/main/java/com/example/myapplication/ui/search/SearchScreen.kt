package com.example.myapplication.ui.search

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
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

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)) {

        // Full screen radar
        ModernRadarBackground(
            modifier = Modifier.fillMaxSize(),
            users = users,
            onUserTap = { selectedUser = it }
        )

        // Matches icon with animation
        MatchesIcon(
            confirmedCount = matches.size,
            pendingCount = pendingMatches.size + receivedMatches.size,
            hasNewMatch = hasNewMatch,
            onMatchesClick = {
                viewModel.clearNewMatchFlag()
                showMatchesList = true
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
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

        // Matches list dialog
        if (showMatchesList) {
            Dialog(onDismissRequest = { showMatchesList = false }) {
                EnhancedMatchesList(
                    confirmedMatches = matches,
                    pendingMatches = pendingMatches,
                    receivedMatches = receivedMatches,
                    onMatchClick = { match ->
                        selectedMatch = match
                        showMatchesList = false
                    },
                    onApproveMatch = { match ->
                        viewModel.approveMatch(match.user._id)
                        showMatchesList = false
                    },
                    onClose = { showMatchesList = false }
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
                        viewModel.unmatchUser(match.user._id)
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
                    // Navigate to chat with the matched user
                    // navController.navigate("chat/${newMatchUser?.id}")
                }
            )
        }
    }
}

@Composable
fun MatchesIcon(
    confirmedCount: Int,
    pendingCount: Int,
    hasNewMatch: Boolean,
    onMatchesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()

    // Animation for new match notification
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (hasNewMatch) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .size(56.dp)
                .scale(if (hasNewMatch) scale else 1f)
                .graphicsLayer {
                    if (hasNewMatch) rotationZ = rotationAngle
                }
                .clickable { onMatchesClick() },
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Matches",
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Badge for number of matches
        if (confirmedCount > 0 || pendingCount > 0) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE91E63))
                    .border(1.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if ((confirmedCount + pendingCount) > 9) "9+" else (confirmedCount + pendingCount).toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun EnhancedMatchesList(
    confirmedMatches: List<UserMatch>,
    pendingMatches: List<UserMatch>,
    receivedMatches: List<UserMatch>,
    onMatchClick: (UserMatch) -> Unit,
    onApproveMatch: (UserMatch) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .fillMaxHeight(0.7f),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header with title and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your Matches",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1976D2)
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show different match states with sections
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Received matches (waiting for your approval)
                if (receivedMatches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Waiting for Your Approval",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(receivedMatches) { match ->
                        MatchItem(
                            match = match,
                            onClick = { onMatchClick(match) },
                            onApprove = { onApproveMatch(match) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Pending matches (you liked, waiting for their approval)
                if (pendingMatches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Approval",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(pendingMatches) { match ->
                        MatchItem(
                            match = match,
                            onClick = { onMatchClick(match) },
                            onApprove = null
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Confirmed matches
                if (confirmedMatches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Confirmed Matches",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(confirmedMatches) { match ->
                        MatchItem(
                            match = match,
                            onClick = { onMatchClick(match) },
                            onApprove = null
                        )
                    }
                }

                // No matches message
                if (confirmedMatches.isEmpty() && pendingMatches.isEmpty() && receivedMatches.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matches yet. Keep searching!",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchItem(
    match: UserMatch,
    onClick: () -> Unit,
    onApprove: (() -> Unit)?
) {
    val loadingDots by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Restart
        )
    )

    val dots = when {
        loadingDots < 0.33f -> "."
        loadingDots < 0.66f -> ".."
        else -> "..."
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(match.user.avatar),
                contentDescription = "User Avatar",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .border(2.dp,
                        when (match.status) {
                            MatchStatus.CONFIRMED -> Color(0xFF4CAF50)
                            MatchStatus.PENDING -> Color(0xFFFFA000)
                            MatchStatus.RECEIVED -> Color(0xFF2196F3)
                        },
                        CircleShape
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${match.user.firstName} ${match.user.lastName}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (match.status) {
                        MatchStatus.CONFIRMED -> "Matched"
                        MatchStatus.PENDING -> "Pending approval$dots"
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
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }
            }

            // Action buttons based on status
            when (match.status) {
                MatchStatus.RECEIVED -> {
                    if (onApprove != null) {
                        IconButton(
                            onClick = onApprove,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Approve",
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
                MatchStatus.CONFIRMED -> {
                    IconButton(
                        onClick = { /* Navigate to chat */ },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2196F3).copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = "Chat",
                            tint = Color(0xFF2196F3)
                        )
                    }
                }
                MatchStatus.PENDING -> {
                    // Show loading dots animation for pending
                    Box(
                        modifier = Modifier
                            .size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dots,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFFFA000)
                        )
                    }
                }
            }
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
    onAnimationComplete: () -> Unit
) {
    // Track animation progress
    var animationProgress by remember { mutableStateOf(0f) }

    // Animation timing
    val animationDuration = 3000 // 3 seconds total

    // Launch animation
    LaunchedEffect(matchedUser) {
        if (matchedUser != null) {
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < animationDuration) {
                animationProgress =
                    (System.currentTimeMillis() - startTime).toFloat() / animationDuration
                delay(16) // ~60fps
            }
            onAnimationComplete()
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
                        targetValue = if (animationProgress > delay)
                            1f - ((animationProgress - delay) * 1.5f).coerceIn(0f, 1f)
                        else 0f
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
                val username = "${it.firstName} ${it.lastName}"

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

            // Start messaging button
            Button(
                onClick = onAnimationComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (animationProgress > 0.8f) 1f else 0f
                        ).value
                    ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Messaging",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
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