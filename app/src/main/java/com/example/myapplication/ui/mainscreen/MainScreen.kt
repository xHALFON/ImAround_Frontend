package com.example.myapplication.ui.mainscreen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.myapplication.R

import com.example.myapplication.ui.search.SearchScreen
import com.example.myapplication.ui.profile.ProfileScreen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.local.SessionManager
import com.example.myapplication.ui.chat.ChatViewModel
import com.example.myapplication.ui.chat.compose.ChatListScreen
import com.google.gson.Gson
import java.net.URLEncoder

// 🔥 NEW IMPORTS for Session Management
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

sealed class BottomNavItem(val route: String, val icon: Any, val label: String) {
    object Profile : BottomNavItem("profileTab", Icons.Default.AccountCircle, "Profile")
    object Scan : BottomNavItem("scanTab", R.drawable.iamaround_navbar_icon, "Scan")
    object Chat : BottomNavItem("chatTab", Icons.Outlined.ChatBubbleOutline, "Chat")
}

@Composable
fun MainScreen(navController: NavHostController, startWithChatTab: Boolean = false) {
    val context = LocalContext.current

    // 🔥 NEW - Session Manager
    val sessionManager = remember { SessionManager(context) }

    // 🆕 NEW - Reactive session state using StateFlow
    val sessionState by sessionManager.sessionState.collectAsStateWithLifecycle()

    val items = listOf(
        BottomNavItem.Chat,
        BottomNavItem.Scan,
        BottomNavItem.Profile
    )

    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // 🆕 React to session state changes
    LaunchedEffect(sessionState) {
        Log.d("MainScreen", "🔄 Session state changed to: $sessionState")

        when (sessionState) {
            SessionManager.SessionState.LOGGED_OUT,
            SessionManager.SessionState.EXPIRED -> {
                Log.d("MainScreen", "❌ Session invalid - redirecting to login")
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
                return@LaunchedEffect
            }
            SessionManager.SessionState.LOGGED_IN -> {
                Log.d("MainScreen", "✅ Session valid - staying in main screen")
            }
            SessionManager.SessionState.UNKNOWN -> {
                Log.d("MainScreen", "❓ Session state unknown - checking...")
                sessionManager.refreshSessionState()
            }
        }
    }

    // 🔥 NEW - LIFECYCLE-AWARE SESSION CHECK
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Log.d("MainScreen", "🔄 App resumed - refreshing session state...")
                    sessionManager.refreshSessionState()
                }
                Lifecycle.Event.ON_START -> {
                    Log.d("MainScreen", "▶️ App started - refreshing session state...")
                    sessionManager.refreshSessionState()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(startWithChatTab) {
        if (startWithChatTab && sessionState == SessionManager.SessionState.LOGGED_IN) {
            bottomNavController.navigate(BottomNavItem.Chat.route) {
                popUpTo(bottomNavController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // 🆕 Don't render MainScreen if session is not valid
    if (sessionState != SessionManager.SessionState.LOGGED_IN) {
        Log.d("MainScreen", "⏳ Waiting for valid session state. Current: $sessionState")
        return
    }

    Scaffold(
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {
                // Top curved part of the navigation bar with border
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .height(70.dp),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    shadowElevation = 0.dp, // Remove shadow
                    color = Color.White
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top border line
                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            color = Color.LightGray,
                            thickness = 1.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chat button
                            BottomNavItemView(
                                item = BottomNavItem.Chat,
                                isSelected = currentDestination?.hierarchy?.any { it.route == BottomNavItem.Chat.route } == true,
                                onClick = {
                                    Log.d("MainScreen", "Clicking Chat button")
                                    bottomNavController.navigate(BottomNavItem.Chat.route) {
                                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )

                            // Space for center icon
                            Spacer(modifier = Modifier.weight(1f))

                            // Profile button
                            BottomNavItemView(
                                item = BottomNavItem.Profile,
                                isSelected = currentDestination?.hierarchy?.any { it.route == BottomNavItem.Profile.route } == true,
                                onClick = {
                                    Log.d("MainScreen", "Clicking Profile button")

                                    // 🆕 SIMPLIFIED - Just check current session state
                                    if (sessionState != SessionManager.SessionState.LOGGED_IN) {
                                        Log.d("MainScreen", "❌ Session not valid when clicking profile")
                                        return@BottomNavItemView
                                    }

                                    bottomNavController.navigate(BottomNavItem.Profile.route) {
                                        popUpTo(bottomNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Floating icon without background
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-20).dp)
                        .size(60.dp)
                        .zIndex(1f)
                        .clickable {
                            Log.d("MainScreen", "Clicking Scan Icon")
                            bottomNavController.navigate(BottomNavItem.Scan.route) {
                                popUpTo(bottomNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Just the icon itself with no background
                    Icon(
                        painter = painterResource(id = R.drawable.iamaround_navbar_icon),
                        contentDescription = BottomNavItem.Scan.label,
                        modifier = Modifier.size(60.dp), // Full size without constraints
                        tint = Color.Unspecified // Preserve original colors
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = BottomNavItem.Scan.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(BottomNavItem.Profile.route) {
                Log.d("InfoTrack", "MainScreen: Showing Profile Screen")
                ProfileScreen(navController)
            }

            composable(BottomNavItem.Scan.route) {
                Log.d("InfoTrack", "MainScreen: Showing Scan/Search Screen")
                SearchScreen(navController)
            }

            composable(BottomNavItem.Chat.route) {
                Log.d("InfoTrack", "MainScreen: Showing Chat Screen")

                // השג את ה-context וה-sessionManager
                val context = LocalContext.current
                val sessionManager = SessionManager(context)

                // אתחל את ה-viewModel
                val chatViewModel: ChatViewModel = viewModel()

                ChatListScreen(
                    onChatSelected = { matchId, chatPartner ->
                        // ניווט למסך צ'אט ספציפי
                        val safeMatchId = matchId ?: ""
                        val userId = chatPartner._id ?: ""
                        val firstName = chatPartner.firstName ?: "User"
                        val lastName = chatPartner.lastName ?: ""
                        val avatar = URLEncoder.encode(chatPartner.avatar ?: "", "UTF-8")
                        navController.navigate("chat_detail/$safeMatchId/$userId/$firstName/$lastName/$avatar")
                    },
                    sessionManager = sessionManager,
                    viewModel = chatViewModel,
                    onBackClick = {
                        // אפשר להשאיר ריק או להוסיף לוגיקה נוספת כאן אם צריך
                    }
                )
            }
        }
    }
}

@Composable
private fun BottomNavItemView(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        val tint = if (isSelected) Color(0xFF6F75E8) else Color.Gray

        when (val icon = item.icon) {
            is ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = item.label,
                    tint = tint,
                    modifier = Modifier.size(32.dp) // Increased icon size from 24dp to 32dp
                )
            }
            is Int -> {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = item.label,
                    tint = tint,
                    modifier = Modifier.size(32.dp) // Increased icon size from 24dp to 32dp
                )
            }
        }

        // Removed text labels
    }
}